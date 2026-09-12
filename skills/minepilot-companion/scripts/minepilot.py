#!/usr/bin/env python3
"""Minimal authenticated MCP client for the local MinePilot Forge Mod."""

from __future__ import annotations

import argparse
import json
import http.client
import os
import stat
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path
from typing import Any

PROTOCOL_VERSION = "2025-06-18"
DEFAULT_CONFIG = Path.home() / ".codex" / "minepilot-companion.json"


class ClientError(RuntimeError):
    pass


class ToolError(ClientError):
    """A public gameplay operation failed, distinct from transport/configuration errors."""
    pass


class NoRedirectHandler(urllib.request.HTTPRedirectHandler):
    """Never forward the MinePilot bearer token through an HTTP redirect."""

    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


# The only supported destination is authenticated loopback. macOS system proxy
# settings can otherwise route even 127.0.0.1 through a proxy and break idle play.
HTTP = urllib.request.build_opener(urllib.request.ProxyHandler({}), NoRedirectHandler())


def config_path() -> Path:
    return Path(
        os.environ.get("MINEPILOT_CODEX_CONFIG", str(DEFAULT_CONFIG))
    ).expanduser()


def validate_url(url: str) -> None:
    parsed = urllib.parse.urlparse(url)
    if (
        parsed.scheme != "http"
        or parsed.hostname not in {"127.0.0.1", "localhost"}
        or parsed.path != "/mcp"
        or parsed.username is not None
        or parsed.password is not None
        or parsed.params
        or parsed.query
        or parsed.fragment
    ):
        raise ClientError("MinePilot MCP URL must be exact loopback HTTP at /mcp")


def load_connection() -> tuple[str, str]:
    path = config_path()
    try:
        config = json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError as exc:
        raise ClientError(
            f"MinePilot connection file is missing: {path}. Run configure first."
        ) from exc
    except (OSError, json.JSONDecodeError) as exc:
        raise ClientError(f"Cannot read MinePilot connection file: {exc}") from exc

    url = config.get("url")
    token_file_value = config.get("tokenFile")
    if not isinstance(url, str) or not isinstance(token_file_value, str):
        raise ClientError("Connection file requires string url and tokenFile fields")
    validate_url(url)

    token_path = Path(token_file_value).expanduser()
    try:
        token_stat = token_path.stat()
        if os.name == "posix" and token_stat.st_mode & (
            stat.S_IRWXG | stat.S_IRWXO
        ):
            raise ClientError(
                f"Refusing group/world-accessible token file: {token_path}"
            )
        token = token_path.read_text(encoding="utf-8").strip()
    except OSError as exc:
        raise ClientError(
            f"MinePilot is not running or its token cannot be read: {token_path}"
        ) from exc
    if not token or len(token) > 256:
        raise ClientError("MinePilot token file is empty or invalid")
    return url, token


class McpClient:
    def __init__(self, url: str, token: str) -> None:
        self.url = url
        self.token = token
        self.next_id = 1

    def poll_events(self, after_chat, after_system, after_inventory):
        return self.call_tool("poll_events", {"after_chat":after_chat,"after_system":after_system,"after_inventory":after_inventory})

    def post(self, payload: dict[str, Any], *, notification: bool = False) -> Any:
        data = json.dumps(payload, separators=(",", ":")).encode("utf-8")
        request = urllib.request.Request(
            self.url,
            data=data,
            method="POST",
            headers={
                "Authorization": f"Bearer {self.token}",
                "Content-Type": "application/json",
                "Accept": "application/json, text/event-stream",
                "MCP-Protocol-Version": PROTOCOL_VERSION,
            },
        )
        try:
            with HTTP.open(request, timeout=15) as response:
                body = response.read()
        except urllib.error.HTTPError as exc:
            if exc.code == 401:
                raise ClientError(
                    "MinePilot rejected the token; re-enter the world to refresh it"
                ) from exc
            if 300 <= exc.code < 400:
                raise ClientError("MinePilot MCP redirects are forbidden") from exc
            raise ClientError(f"MinePilot MCP returned HTTP {exc.code}") from exc
        except (OSError, http.client.HTTPException) as exc:
            # Do not retry a possibly applied mutation here. The listener can
            # reconnect and observe state; partial/closed responses must not kill it.
            raise ClientError(
                f"Cannot reach the local MinePilot MCP endpoint ({type(exc).__name__}); request outcome may be unknown"
            ) from exc
        if notification:
            return None
        try:
            decoded = json.loads(body)
        except json.JSONDecodeError as exc:
            raise ClientError("MinePilot MCP returned invalid JSON") from exc
        if "error" in decoded:
            error = decoded["error"]
            raise ClientError(
                f"MCP error {error.get('code')}: {error.get('message')}"
            )
        return decoded.get("result")

    def request(self, method: str, params: dict[str, Any] | None = None) -> Any:
        request_id = self.next_id
        self.next_id += 1
        payload: dict[str, Any] = {
            "jsonrpc": "2.0",
            "id": request_id,
            "method": method,
        }
        if params is not None:
            payload["params"] = params
        return self.post(payload)

    def notify(self, method: str) -> None:
        self.post(
            {"jsonrpc": "2.0", "method": method},
            notification=True,
        )

    def initialize(self) -> None:
        self.request(
            "initialize",
            {
                "protocolVersion": PROTOCOL_VERSION,
                "capabilities": {},
                "clientInfo": {
                    "name": "minepilot-codex-skill",
                    "version": "0.1.0",
                },
            },
        )
        self.notify("notifications/initialized")

    def call_tool(self, name: str, arguments: dict[str, Any]) -> dict[str, Any]:
        result = self.request(
            "tools/call",
            {"name": name, "arguments": arguments},
        )
        if not isinstance(result, dict):
            raise ClientError("MinePilot tool returned an invalid result")
        payload = result.get("structuredContent")
        if not isinstance(payload, dict):
            try:
                payload = json.loads(result["content"][0]["text"])
            except (KeyError, IndexError, TypeError, json.JSONDecodeError) as exc:
                raise ClientError("MinePilot tool returned no structured content") from exc
        if result.get("isError"):
            raise ToolError(str(payload.get("message", "MinePilot tool failed"))[:512])
        return payload


def parser() -> argparse.ArgumentParser:
    root = argparse.ArgumentParser(description=__doc__)
    commands = root.add_subparsers(dest="command", required=True)
    configure = commands.add_parser("configure")
    configure.add_argument(
        "--url", default="http://127.0.0.1:25766/mcp"
    )
    configure.add_argument("--token-file", required=True)
    commands.add_parser("observe")

    game_tool = commands.add_parser("tool", help="Call a bounded public gameplay tool")
    game_tool.add_argument("--name", required=True, choices=["turn", "sense", "listen", "inventory", "drop_items", "reclaim_drop", "item_origins", "inventory_events", "annotate_item", "waypoint", "jump_once", "survey_mining", "mining_survey_status", "plan_excavation", "choose_excavation", "excavation_status", "pause_excavation", "resume_excavation", "cancel_excavation", "equip_tool", "plan_mining", "choose_mining", "mining_status", "pause_mining", "resume_mining", "cancel_mining", "inspect_tree", "tree_farm", "plan_collection", "choose_collection", "collection_status", "pause_collection", "resume_collection", "cancel_collection", "set_hand", "inventory_capacity", "inspect_placement", "place_block", "plan_placement", "choose_placement", "placement_status", "pause_placement", "resume_placement", "cancel_placement", "resolve_placement", "craft", "interact_block", "inspect_container", "transfer_items", "close_container", "smelt", "eat", "survival_status", "cancel_survival", "gather", "gather_status", "cancel_gather", "find_resources", "remember_context", "companion_mode", "build_camp", "camp_status", "resume_camp", "cancel_camp"])
    game_tool.add_argument("--arguments", default="{}", help="JSON object using the public tool schema")

    read_chat = commands.add_parser("read-chat")
    read_chat.add_argument("--after", type=int, default=0)
    read_chat.add_argument("--limit", type=int, default=20)

    say = commands.add_parser("say")
    say.add_argument("--message", required=True)
    say.add_argument("--navigation-request-id")

    request = commands.add_parser("request")
    request.add_argument(
        "--target-kind",
        required=True,
        choices=[
            "coordinates",
            "player",
            "entity",
            "dropped_item",
            "waypoint",
            "world_spawn",
            "respawn_point",
            "death_point",
        ],
    )
    request.add_argument("--dimension")
    request.add_argument("--x", type=float)
    request.add_argument("--y", type=float)
    request.add_argument("--z", type=float)
    request.add_argument("--target-name")
    request.add_argument("--acceptance-radius", type=float)
    request.add_argument("--arrival-heading", type=float)
    request.add_argument(
        "--pace",
        default="auto",
        choices=["auto", "walk", "sprint", "sprint_jump", "sneak"],
    )
    request.add_argument("--intent", required=True)
    request.add_argument("--continuous-follow", action="store_true", help="Keep following a player/entity until cancelled")

    prepare = commands.add_parser("prepare", parents=[request], add_help=False,
                                  help="Request, visibly acknowledge and plan in one client call")
    prepare.add_argument("--message", required=True)
    prepare.add_argument("--timeout", type=float, default=10.0)
    wait = commands.add_parser("wait", help="Wait for navigation result or new player chat")
    wait.add_argument("--timeout", type=float, default=30.0)
    wait.add_argument("--after-chat", type=int, default=0)

    plan = commands.add_parser("plan")
    plan.add_argument("--request-id", required=True)
    commands.add_parser("status")

    choose = commands.add_parser("choose")
    choose.add_argument("--request-id", required=True)
    choose.add_argument("--option-id", required=True)
    choose.add_argument(
        "--pace",
        default="auto",
        choices=["auto", "walk", "sprint", "sprint_jump", "sneak"],
    )

    cancel = commands.add_parser("cancel")
    cancel.add_argument("--request-id", required=True)
    cancel.add_argument("--reason", required=True)
    return root


def compact(values: dict[str, Any]) -> dict[str, Any]:
    return {key: value for key, value in values.items() if value is not None}


def execute(client: McpClient, args: argparse.Namespace) -> dict[str, Any]:
    if args.command == "tool":
        if len(args.arguments) > 4096:
            raise ClientError("Tool arguments exceed 4096 characters")
        try:
            arguments = json.loads(args.arguments)
        except json.JSONDecodeError as failure:
            raise ClientError("Tool arguments must be a JSON object") from failure
        if not isinstance(arguments, dict):
            raise ClientError("Tool arguments must be a JSON object")
        return client.call_tool(args.name, arguments)
    if args.command == "prepare":
        timeout = min(30.0, max(0.1, args.timeout))
        request_args = argparse.Namespace(**vars(args))
        request_args.command = "request"
        accepted = execute(client, request_args)
        request_id = accepted["requestId"]
        try:
            client.call_tool("say", {"message": args.message, "navigation_request_id": request_id})
            result = client.call_tool("plan_navigation", {"request_id": request_id})
            deadline = time.monotonic() + timeout
            while result.get("phase") == "PLANNING" and time.monotonic() < deadline:
                time.sleep(0.1)
                result = client.call_tool("navigation_status", {})
                if result.get("requestId") != request_id:
                    raise ClientError("Navigation ownership changed while preparing")
            return result
        except ClientError:
            # An acknowledgement/plan failure must not strand the request barrier.
            state = client.call_tool("navigation_status", {})
            if state.get("requestId") == request_id and state.get("phase") not in {"APPROACHED", "COMPLETED", "FAILED", "CANCELLED"}:
                client.call_tool("cancel_navigation", {"request_id": request_id, "reason": "Preparation failed"})
            raise
    if args.command == "wait":
        deadline = time.monotonic() + min(60.0, max(0.1, args.timeout))
        while True:
            result = client.call_tool("navigation_status", {})
            chat = client.call_tool("read_chat", {"after_sequence": args.after_chat, "limit": 50})
            result["newChat"] = chat
            if chat.get("messages") or result.get("phase") not in {"PLANNING", "EXECUTING"} or time.monotonic() >= deadline:
                return result
            time.sleep(0.25)
    if args.command == "observe":
        return client.call_tool("observe", {})
    if args.command == "read-chat":
        return client.call_tool(
            "read_chat",
            {"after_sequence": args.after, "limit": args.limit},
        )
    if args.command == "say":
        return client.call_tool(
            "say",
            compact(
                {
                    "message": args.message,
                    "navigation_request_id": args.navigation_request_id,
                }
            ),
        )
    if args.command == "request":
        if args.target_kind == "coordinates" and None in (args.x, args.y, args.z):
            raise ClientError("Coordinate targets require --x, --y, and --z")
        if args.target_kind in {"player", "entity", "dropped_item", "waypoint"} and not args.target_name:
            raise ClientError("Named targets require --target-name (observed entity UUID or saved waypoint name)")
        return client.call_tool(
            "request_navigation",
            compact(
                {
                    "target_kind": args.target_kind,
                    "dimension": args.dimension,
                    "x": args.x,
                    "y": args.y,
                    "z": args.z,
                    "target_name": args.target_name,
                    "acceptance_radius": args.acceptance_radius,
                    "arrival_heading": args.arrival_heading,
                    "preferred_pace": args.pace,
                    "player_intent": args.intent,
                    "continuous_follow": args.continuous_follow,
                }
            ),
        )
    if args.command == "plan":
        return client.call_tool(
            "plan_navigation", {"request_id": args.request_id}
        )
    if args.command == "status":
        return client.call_tool("navigation_status", {})
    if args.command == "choose":
        return client.call_tool(
            "choose_navigation",
            {
                "request_id": args.request_id,
                "option_id": args.option_id,
                "pace": args.pace,
            },
        )
    if args.command == "cancel":
        return client.call_tool(
            "cancel_navigation",
            {"request_id": args.request_id, "reason": args.reason},
        )
    raise ClientError(f"Unsupported command: {args.command}")


def configure(args: argparse.Namespace) -> dict[str, Any]:
    validate_url(args.url)
    token_file = Path(args.token_file).expanduser().resolve(strict=False)
    path = config_path()
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_name(path.name + ".tmp")
    temporary.write_text(
        json.dumps(
            {"url": args.url, "tokenFile": str(token_file)},
            indent=2,
            sort_keys=True,
        )
        + "\n",
        encoding="utf-8",
    )
    if os.name == "posix":
        temporary.chmod(stat.S_IRUSR | stat.S_IWUSR)
    temporary.replace(path)
    return {"configured": True, "configFile": str(path), "url": args.url}


def main() -> int:
    args = parser().parse_args()
    try:
        if args.command == "configure":
            result = configure(args)
        else:
            url, token = load_connection()
            client = McpClient(url, token)
            client.initialize()
            result = execute(client, args)
    except ClientError as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 2
    print(json.dumps(result, ensure_ascii=False, indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
