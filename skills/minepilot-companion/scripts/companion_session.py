#!/usr/bin/env python3
"""Keep the game-chat listener alive independently of individual Codex turns."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import time

import minepilot
import codex_decisions
import fast_commands


TERMINAL = {"APPROACHED", "COMPLETED", "FAILED", "CANCELLED", "REPLAN_REQUIRED"}

class IncompatibleMod(RuntimeError):
    pass

def check_mod_tools(client):
    present={tool["name"] for tool in client.request("tools/list").get("tools",[])}
    missing={"observe","read_chat","navigation_status","poll_events","inventory","inventory_events","annotate_item","turn","sense","listen","waypoint"}-present
    if missing:
        raise IncompatibleMod("运行中的 MinePilot 模组版本缺少接口，请安装匹配的新版 JAR 后重启世界："+", ".join(sorted(missing)))


def observation_for_event(observation, event):
    observation = dict(observation)
    observation.pop("recentPlayerChat", None)
    if event["type"] in {"player_chat","initial_request","continue_player_request"}:
        historical = []
        for kind in ("navigation","mining","collection","placement"):
            if observation.get(kind, {}).get("phase") in {"COMPLETED","PARTIAL","APPROACHED","FAILED","BLOCKED","CANCELLED"}:
                observation.pop(kind)
                historical.append(kind + "_status")
        if historical: observation["historicalJobStatusTools"] = historical
    if event["type"] == "navigation_event": observation.pop("navigation", None)
    if event["type"] == "tool_result" and event.get("tool") in {"plan_collection", "plan_mining", "plan_placement"}:
        observation.pop(event["tool"].removeprefix("plan_"), None)
    return observation


class SessionLoop:
    """One worker owns decisions; the listener survives every worker completion."""
    def __init__(self, client, launch, initial_message=None):
        self.client = client
        self.launch = launch
        self.worker = None
        self.last_chat = 0
        self.last_navigation = None
        self.initial_message = initial_message
        self.worker_started = 0.0
        self.last_decision_timing = None
        self.decision_timings = []
        self.pending_event = None
        self.last_player_event = None
        self.worker_event = None
        self.repair_attempts = {}
        self.dialogue = []
        self.navigation_origin_request = None
        self.authorized_request = None
        self.requested_pace = "auto"
        self.last_placement = None
        self.placement_origin_request = None
        self.last_collection = None
        self.collection_origin_request = None
        self.last_mining = None
        self.mining_origin_request = None
        self.last_inventory = 0
        self.inventory_pending = []
        self.inventory_review = False
        self.query_turns = 0
        self.last_system = 0
        self.system_messages = []
        self.inventory_overflow = False
        self.argument_repairs = 0
        self.recent_acquisitions = []
        self.reported_acquisitions = set()

    def acquisition_context(self, event):
        if event.get("type") == "inventory_event":
            return event.get("events", [])
        if event.get("type") == "navigation_event":
            target = (event.get("state", {}).get("destination") or {}).get("targetIdentity")
            if not target: return []
            return [e for e in event.get("recentAcquisitions", [])
                    if any(row.get("entityId") == target for row in e.get("acquired", []))]
        return []

    def quiet_inventory_decision(self, decision):
        decision = {**decision, "message": (decision.get("message") or "").strip()}
        event = self.worker_event or {}
        if (event.get("type") == "navigation_event" and event.get("state", {}).get("phase") in {"FAILED", "APPROACHED"}
                and decision["action"] == "say" and not decision["message"]):
            reason = event.get("state", {}).get("lastEventMessage") or "没有获得完整到达的验证结果"
            decision["message"] = "这次未能到达完整目标。原因：" + reason[:384]
        quiet = False
        if event.get("type") in {"inventory_event", "inventory_review"}:
            reason = decision.get("speech_reason", "none")
            quiet = reason not in {"player_gift_or_loan", "requested_collection", "requested_report", "direct_player_relevance"}
            if event["type"] == "inventory_review" and reason != "requested_report": quiet = True
        batches = self.acquisition_context(event)
        if batches and all(e["sequence"] in self.reported_acquisitions for e in batches):
            quiet = quiet or decision.get("speech_reason") != "requested_report"
        if quiet:
            decision = {**decision, "message": ""}
            if decision["action"] == "say": decision["action"] = "wait"
        return decision

    def tick(self):
        batch = self.client.poll_events(self.last_chat,self.last_system,self.last_inventory) if hasattr(self.client,"poll_events") else None
        inventory = batch["inventoryEvents"] if batch is not None else self.client.call_tool("inventory_events", {"after_sequence":self.last_inventory,"limit":16})
        gains = inventory.get("events", [])
        if gains:
            self.recent_acquisitions = (self.recent_acquisitions + gains)[-32:]
            self.reported_acquisitions.intersection_update(e["sequence"] for e in self.recent_acquisitions)
            self.last_inventory = max(e["sequence"] for e in gains)
            self.inventory_pending.extend(gains)
            self.inventory_overflow |= len(self.inventory_pending)>32 or inventory.get("historyLost",False)
            self.inventory_pending = self.inventory_pending[-32:]
            self.inventory_review = True
        chat = batch["chat"] if batch is not None else self.client.call_tool("read_chat", {"after_sequence": self.last_chat, "after_system_sequence":self.last_system, "limit": 50})
        system = chat.get("systemChat", {}).get("messages", [])
        if system:
            self.last_system = max(m["sequence"] for m in system)
            self.system_messages = (self.system_messages + [m for m in system if m.get("origin")!="received_agent_chat"])[-16:]
        messages = chat.get("messages", [])
        if messages:
            self.last_chat = max(m["sequence"] for m in messages)
        status = batch["navigation"] if batch is not None else self.client.call_tool("navigation_status", {})
        marker = (status.get("requestId"), status.get("phase"))
        mining = batch.get("mining", {}) if batch is not None else {}
        mining_marker = (mining.get("requestId"), mining.get("phase"))
        collection = batch.get("collection", {}) if batch is not None else {}
        collection_marker = (collection.get("requestId"), collection.get("phase"))
        placement = batch.get("placement", {}) if batch is not None else {}
        placement_marker = (placement.get("requestId"), placement.get("phase"), placement.get("decisionId"))
        if mining.get("requestId") in collection.get("childMiningRequestIds", []) + placement.get("childMiningRequestIds", []):
            self.last_mining = mining_marker
        if any(m.get("handledLocally") for m in messages):
            self.stop_worker()
            self.pending_event = self.last_player_event = self.worker_event = None
            self.last_navigation = marker
            self.navigation_origin_request = None
            self.last_mining = mining_marker
            self.last_collection = collection_marker
            self.last_placement = placement_marker
            self.placement_origin_request = None
            self.collection_origin_request = None
            self.mining_origin_request = None
            messages = [m for m in messages if not m.get("handledLocally")]
        direct = fast_commands.parse(messages[0]["text"]) if len(messages)==1 else None
        if direct and not placement.get("ownsBody") and not collection.get("ownsBody") and mining.get("phase") not in {"EXECUTING", "PAUSED"} and status.get("phase") in {"IDLE","COMPLETED","APPROACHED","FAILED","CANCELLED"}:
            self.stop_worker()
            self.pending_event = self.last_player_event = self.worker_event = None
            try:
                codex_decisions.apply(self.client,direct)
                refreshed=self.client.call_tool("navigation_status",{})
                self.last_navigation=(refreshed.get("requestId"),refreshed.get("phase"))
                if direct["action"]=="navigate":
                    self.navigation_origin_request={"type":"player_chat","messages":messages}
                    self.authorized_request=refreshed.get("requestId");self.requested_pace=direct.get("pace","auto")
            except minepilot.ToolError as failure:
                self.pending_event={"type":"operation_failed","reason":str(failure)}
            return
        event = None
        if self.worker is not None and self.worker.poll() is not None and not messages:
            code = self.worker.returncode
            finished = self.worker
            if hasattr(finished,"metrics"):
                self.last_decision_timing = finished.metrics()
                self.decision_timings = (self.decision_timings + [{"finishedAt":time.time(), **self.last_decision_timing}])[-32:]
            self.worker = None
            if not code and hasattr(finished, "decision"):
                try:
                    decision = codex_decisions.normalize_decision(finished.decision())
                    if self.worker_event and decision["action"] not in codex_decisions.event_schema(self.worker_event)["properties"]["action"]["enum"]:
                        raise ValueError("Decision is not valid for this event")
                    if self.worker_event and decision["action"] == "tool":
                        allowed = codex_decisions.event_schema(self.worker_event)["properties"].get("tool_name", {}).get("enum", [])
                        if decision.get("tool_name") not in allowed: raise ValueError("Tool is not valid for this event")
                    decision = codex_decisions.bind_plan_decision(decision, self.worker_event or {})
                    if self.worker_event and self.worker_event.get("type") == "navigation_event":
                        expected = self.worker_event["state"]
                        current = self.client.call_tool("navigation_status", {})
                        if any(current.get(k) != expected.get(k) for k in ("requestId", "phase", "worldRevision")):
                            decision = {"action": "wait"}
                        elif decision["action"] not in codex_decisions.event_schema(self.worker_event)["properties"]["action"]["enum"]:
                            raise ValueError("A route event cannot replay a player command")
                    if decision["action"] == "plan":
                        request = decision.get("request_id")
                        count = self.repair_attempts.get(request, 0)
                        if count >= 2:
                            decision = {"action": "say", "message": "同一段路连续尝试后仍然卡住了，我先停在这里，不再重复起跳。你仍可以和我聊天或换个目标。"}
                        else:
                            self.repair_attempts = {request: count + 1}
                    if (self.worker_event and self.worker_event.get("type") == "navigation_event"
                            and self.worker_event.get("state", {}).get("phase") == "COMPLETED"
                            and decision["action"] in {"tool", "navigate"}):
                        self.last_player_event = self.worker_event.get("request")
                    decision = self.quiet_inventory_decision(decision)
                    tool_result = codex_decisions.apply(self.client, decision)
                    if decision["action"] == "tool" and decision.get("tool_name") in {"choose_placement","place_block"}:
                        self.placement_origin_request = self.last_player_event
                    if decision["action"] == "tool" and decision.get("tool_name") in {"cancel_placement","pause_placement","resume_placement","resolve_placement"} and tool_result:
                        result=tool_result.get("result",{})
                        self.last_placement=(result.get("requestId"),result.get("phase"),result.get("decisionId"))
                    if decision["action"] == "tool" and decision.get("tool_name") == "choose_collection":
                        self.collection_origin_request = self.last_player_event
                    if decision["action"] == "tool" and decision.get("tool_name") in {"cancel_collection","pause_collection","resume_collection"} and tool_result:
                        result=tool_result.get("result",{})
                        self.last_collection=(result.get("requestId"),result.get("phase"))
                    if decision["action"] == "tool" and decision.get("tool_name") == "choose_mining":
                        self.mining_origin_request = self.last_player_event
                    if decision["action"] == "tool" and decision.get("tool_name") in {"cancel_mining","pause_mining","resume_mining"} and tool_result:
                        result=tool_result.get("result",{})
                        self.last_mining=(result.get("requestId"),result.get("phase"))
                    if decision["action"] == "tool" and tool_result:
                        name = tool_result["tool"]
                        phase = tool_result.get("result", {}).get("phase")
                        if name in {"choose_collection","resume_collection","choose_mining","resume_mining","choose_placement","place_block","resume_placement","resolve_placement"} and phase == "EXECUTING" or name in {"pause_collection","cancel_collection","pause_mining","cancel_mining","pause_placement","cancel_placement"} and phase in {"PAUSED","CANCELLED"}:
                            # The accepted job proceeds in the game; its terminal event
                            # or fresh chat is the next reason to ask the model.
                            self.query_turns = 0
                            self.last_player_event = None
                        else:
                            self.query_turns += 1
                            if self.query_turns <= 8:
                                self.pending_event = {"type":"tool_result", **tool_result, "request":self.last_player_event}
                            else:
                                self.pending_event = {"type":"operation_failed", "reason":"Query budget reached; search coverage is incomplete. Do not claim absence beyond scanned results."}
                    if decision["action"] == "organize": self.inventory_review = False
                    status = self.client.call_tool("navigation_status", {})
                    marker = (status.get("requestId"), status.get("phase"))
                    if decision["action"] == "navigate":
                        self.navigation_origin_request = self.last_player_event or (self.worker_event or {}).get("request")
                        self.authorized_request = status.get("requestId")
                        self.requested_pace = decision.get("pace", "auto")
                    if decision.get("message") and decision.get("action") in {"say", "navigate", "cancel", "organize", "tool"}:
                        self.reported_acquisitions.update(e["sequence"] for e in self.acquisition_context(self.worker_event or {}))
                        self.dialogue.append({"speaker": "MinePilot", "text": decision["message"]})
                        self.dialogue = self.dialogue[-12:]
                    if decision.get("action") == "cancel":
                        # Its visible reply already addresses the player's stop request.
                        # Replacement navigation carries replace_request_id in one decision.
                        self.last_navigation = marker
                        self.last_player_event = None
                    elif decision.get("action") != "tool":
                        self.last_player_event = None
                except codex_decisions.InvalidNavigationArguments as failure:
                    if self.last_player_event and self.argument_repairs < 1:
                        self.argument_repairs += 1
                        self.pending_event = {"type": "repair_invalid_request", "reason": str(failure), "request": self.last_player_event}
                    else:
                        self.pending_event = {"type": "operation_failed", "reason": str(failure)}
                        self.last_player_event = None
                except minepilot.ToolError as failure:
                    self.pending_event = {"type": "operation_failed", "reason": str(failure)}
                    self.last_player_event = None
                except (ValueError, OSError, minepilot.ClientError):
                    self.client.call_tool("say", {"message": "这次操作没有成功，我会继续接收消息。"})
            if code:
                self.client.call_tool("say", {"message": "这次模型连接没有成功，我仍在这里接收聊天，请稍后再试。"})
        if not messages and self.worker is None and marker == (self.authorized_request, "PLAN_READY"):
            options = status.get("routeOptions", [])
            if len(options) == 1:
                option = options[0]
                pace = self.requested_pace if self.requested_pace != "auto" else option.get("suggestedPace")
                if (option.get("feasibleNow") and option.get("estimatedHealthLost") == 0
                        and option.get("supportBlocksRequired") == 0 and not option.get("hazards")
                        and pace in option.get("supportedPaces", []) and not status.get("partialDestination")):
                    self.client.call_tool("choose_navigation", {"request_id": marker[0], "option_id": option["optionId"], "pace": pace})
                    self.last_navigation = marker
                    return
        if (not messages and self.worker is None and marker == (self.authorized_request, "REPLAN_REQUIRED")
                and status.get("lastEventMessage", "").startswith("The moving destination left")):
            self.client.call_tool("plan_navigation", {"request_id": marker[0]})
            self.last_navigation = marker
            return
        if messages:
            self.query_turns = 0
            self.argument_repairs = 0
            event = {"type": "player_chat", "messages": messages}
            self.dialogue.extend({"speaker": m.get("player", "player"), "text": m["text"]} for m in messages)
            self.dialogue = self.dialogue[-12:]
            self.last_player_event = event
            self.pending_event = None
        elif self.initial_message:
            self.argument_repairs = 0
            self.query_turns = 0
            event = {"type": "initial_request", "message": self.initial_message}
            self.initial_message = None
            self.last_player_event = event
        elif self.worker is None and self.pending_event:
            event, self.pending_event = self.pending_event, None
        elif self.worker is None and marker != self.last_navigation and marker[1] in TERMINAL | {"PLAN_READY"}:
            event = {"type": "navigation_event", "state": status, "request": self.navigation_origin_request if marker[0]==self.authorized_request else None}
        elif self.worker is None and placement_marker != self.last_placement and placement_marker[1] in {"COMPLETED","PARTIAL","BLOCKED","CANCELLED"}:
            event = {"type":"placement_event", "state":placement, "request":self.placement_origin_request}
            self.last_placement = placement_marker
            self.query_turns = 0
        elif self.worker is None and collection_marker != self.last_collection and collection_marker[1] in {"COMPLETED","BLOCKED","CANCELLED"}:
            event = {"type":"collection_event", "state":collection, "request":self.collection_origin_request, "recentAcquisitions":self.recent_acquisitions}
            self.last_collection = collection_marker
            self.query_turns = 0
        elif self.worker is None and mining_marker != self.last_mining and mining_marker[1] in {"COMPLETED","BLOCKED","CANCELLED"}:
            event = {"type":"mining_event", "state":mining, "request":self.mining_origin_request, "recentAcquisitions":self.recent_acquisitions}
            self.last_mining = mining_marker
            self.query_turns = 0
        elif self.worker is None and self.inventory_pending:
            event = {"type":"inventory_event", "events":self.inventory_pending,"historyTruncated":self.inventory_overflow}
            self.inventory_pending = []
            self.inventory_overflow = False
        elif self.worker is None and self.inventory_review and not placement.get("ownsBody") and not collection.get("ownsBody") and mining.get("phase") not in {"EXECUTING","PAUSED"} and marker[1] in {"IDLE","COMPLETED","APPROACHED","FAILED","CANCELLED"}:
            event = {"type":"inventory_review"}
            self.inventory_review = False
        if marker[1] not in TERMINAL | {"PLAN_READY"} or event is not None and event["type"] == "navigation_event":
            self.last_navigation = marker
        if placement_marker[1] not in {"COMPLETED","PARTIAL","BLOCKED","CANCELLED"}: self.last_placement = placement_marker
        if collection_marker[1] not in {"COMPLETED","BLOCKED","CANCELLED"}: self.last_collection = collection_marker
        if mining_marker[1] not in {"COMPLETED","BLOCKED","CANCELLED"}: self.last_mining = mining_marker
        if event is not None:
            if event["type"]=="navigation_event" and "DROPPED_ITEM_UNAVAILABLE" in event.get("state",{}).get("lastEventMessage",""):
                event["recentAcquisitions"] = self.recent_acquisitions
                matched = self.acquisition_context(event)
                event["acquisitionAlreadyReported"] = bool(matched) and all(e["sequence"] in self.reported_acquisitions for e in matched)
                matched_sequences = {e["sequence"] for e in matched}
                self.inventory_pending = [e for e in self.inventory_pending if e["sequence"] not in matched_sequences]
            if self.worker is not None and self.worker_event and self.worker_event.get("type") == "navigation_event":
                self.last_navigation = None
            self.stop_worker()
            if hasattr(self.client, "initialize"):
                observation = observation_for_event(self.client.call_tool("observe", {}), event)
                event = {**event, "observation": observation}
                event["receivedSystemChat"] = self.system_messages
            if event["type"] != "navigation_event": event["conversation"] = self.dialogue
            self.worker_event = event
            try:
                self.worker = self.launch(event, self.last_chat)
                self.worker_started = time.monotonic()
            except (OSError, RuntimeError):
                self.worker = None
                self.client.call_tool("say", {"message": "模型暂时无法启动，我会继续接收聊天。"})
        elif self.worker is not None and time.monotonic() - self.worker_started > 120:
            self.stop_worker()
            self.client.call_tool("say", {"message": "这次处理超时了，我会继续接收聊天。请告诉我下一步。"})

    def stop_worker(self):
        if self.worker is None:
            return
        worker, self.worker = self.worker, None
        if worker.poll() is None:
            # Cancellation is asynchronous. The listener must immediately receive
            # the next chat; transport turn/thread IDs reject stale completions.
            worker.terminate()
        if hasattr(worker, "cleanup"):
            worker.cleanup()


def paths(profile):
    config = json.loads(profile.read_text())
    minepilot.validate_url(config["url"])
    identity = hashlib.sha256(config["url"].encode()).hexdigest()[:16]
    directory = Path.home() / ".codex" / "minepilot-sessions" / identity
    directory.mkdir(mode=0o700, parents=True, exist_ok=True)
    return directory


def write_state(directory, **values):
    temp = directory / "state.tmp"
    temp.write_text(json.dumps({"pid": os.getpid(), "updatedAt": time.time(), **values}))
    temp.chmod(0o600)
    temp.replace(directory / "state.json")


def lock_session(directory):
    stream = (directory / "listener.lock").open("a+b")
    if os.name == "nt":
        import msvcrt
        stream.write(b"0"); stream.flush(); stream.seek(0)
        msvcrt.locking(stream.fileno(), msvcrt.LK_NBLCK, 1)
    else:
        import fcntl
        fcntl.flock(stream, fcntl.LOCK_EX | fcntl.LOCK_NB)
    return stream


def launch_worker(args, directory, event, last_chat):
    return codex_decisions.launch(args, directory, event, last_chat)


def cancel_body_work(client):
    # Cancel the parent first so its next tick cannot restart a mining child.
    for kind in ("placement", "collection", "mining", "navigation"):
        try:
            current = client.call_tool(kind + "_status", {})
            if current.get("requestId") and current.get("phase") not in {"APPROACHED","COMPLETED","PARTIAL","FAILED","CANCELLED","IDLE"}:
                arguments = {"request_id":current["requestId"]}
                if kind == "navigation": arguments["reason"] = "Companion session stopped"
                client.call_tool("cancel_" + kind, arguments)
        except minepilot.ClientError:
            pass


def serve(args, directory):
    try:
        guard = lock_session(directory)
    except OSError:
        return 0
    os.environ["MINEPILOT_CODEX_CONFIG"] = str(args.profile)
    stop = directory / "stop"
    stop.unlink(missing_ok=True)
    loop = None
    incompatibility = None
    try:
        while not stop.exists():
            try:
                if loop is None:
                    url, token = minepilot.load_connection()
                    client = minepilot.McpClient(url, token)
                    client.initialize()
                    check_mod_tools(client)
                    observation = client.call_tool("observe", {})
                    if not observation.get("online"):
                        raise RuntimeError("MinePilot body is not alive and online")
                    if not observation.get("externalControlAvailable"):
                        raise RuntimeError("The in-mod model already owns control")
                    loop = SessionLoop(client, lambda e, s: launch_worker(args, directory, e, s), args.message)
                    args.message = None
                    # Do not replay conversations from before connection.
                    loop.last_chat = observation.get("latestChatSequence", 0)
                    loop.last_inventory = observation.get("inventorySummary", {}).get("latestEventSequence",0)
                    previous_navigation = observation.get("navigation", {})
                    loop.last_navigation = (previous_navigation.get("requestId"), previous_navigation.get("phase"))
                    previous_placement = observation.get("placement", {})
                    loop.last_placement = (previous_placement.get("requestId"), previous_placement.get("phase"), previous_placement.get("decisionId"))
                    previous_collection = observation.get("collection", {})
                    loop.last_collection = (previous_collection.get("requestId"), previous_collection.get("phase"))
                    previous_mining = observation.get("mining", {})
                    loop.last_mining = (previous_mining.get("requestId"), previous_mining.get("phase"))
                    codex_decisions.prepare(args, directory)
                inbox = directory / "inbox.json"
                if inbox.exists():
                    loop.initial_message = json.loads(inbox.read_text())["message"]
                    inbox.unlink()
                loop.tick()
                write_state(directory, status="LISTENING", model=args.model,
                            profile=str(args.profile), workerActive=loop.worker is not None,
                            lastChatSequence=loop.last_chat, lastDecisionTiming=loop.last_decision_timing,
                            decisionTimings=loop.decision_timings,
                            modelConnectionReady=codex_decisions.model_for(args, directory).ready.is_set())
            except IncompatibleMod as failure:
                incompatibility = str(failure)
                break
            except minepilot.ClientError:
                if loop is not None:
                    loop.stop_worker()
                    loop = None
                codex_decisions.close()
                write_state(directory, status="WAITING_FOR_WORLD", model=args.model, profile=str(args.profile))
            time.sleep(0.2)
    finally:
        if loop is not None:
            loop.stop_worker()
            cancel_body_work(loop.client)
        codex_decisions.close()
        write_state(directory, status="INCOMPATIBLE_MOD" if incompatibility else "STOPPED", model=args.model, reason=incompatibility)
        guard.close()
    return 0


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("command", choices=["start", "serve", "status", "stop"])
    p.add_argument("--profile", type=Path, default=minepilot.config_path())
    p.add_argument("--model", default="gpt-5.6-luna")
    p.add_argument("--codex")
    p.add_argument("--message", help="Optional initial game request; subsequent requests come from game chat")
    args = p.parse_args()
    args.profile = args.profile.expanduser().resolve()
    directory = paths(args.profile)
    if args.command == "serve":
        return serve(args, directory)
    if args.command == "stop":
        (directory / "stop").touch()
        print(json.dumps({"status": "STOP_REQUESTED"})); return 0
    if args.command == "start":
        state_file = directory / "state.json"
        if state_file.exists():
            state = json.loads(state_file.read_text())
            if state.get("status") in {"LISTENING", "WAITING_FOR_WORLD"} and not (directory / "stop").exists() and time.time() - state.get("updatedAt", 0) < 3:
                try:
                    os.kill(state["pid"], 0)
                except OSError:
                    pass
                else:
                    if state.get("profile") != str(args.profile):
                        raise RuntimeError("This endpoint already has a listener for a different world profile; stop it before switching worlds")
                    if args.message:
                        temp = directory / "inbox.tmp"
                        temp.write_text(json.dumps({"message": args.message}, ensure_ascii=False))
                        temp.chmod(0o600); temp.replace(directory / "inbox.json")
                    print(json.dumps(state)); return 0
        command = [sys.executable, str(Path(__file__).resolve()), "serve", "--profile", str(args.profile), "--model", args.model]
        if args.codex: command += ["--codex", args.codex]
        if args.message: command += ["--message", args.message]
        process = subprocess.Popen(command, stdin=subprocess.DEVNULL, stdout=subprocess.DEVNULL,
                                   stderr=subprocess.DEVNULL, start_new_session=True)
        for _ in range(40):
            state = directory / "state.json"
            if state.exists():
                value = json.loads(state.read_text())
                if value.get("pid")==process.pid and value.get("status")=="INCOMPATIBLE_MOD":
                    raise RuntimeError(value["reason"])
                if value.get("pid") == process.pid and value.get("status") == "LISTENING" and time.time() - value.get("updatedAt", 0) < 3:
                    print(json.dumps(value)); return 0
            time.sleep(0.1)
        raise RuntimeError("Listener did not become ready; check the world and model ownership")
    state = directory / "state.json"
    print(state.read_text() if state.exists() else json.dumps({"status": "NOT_STARTED"}))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
