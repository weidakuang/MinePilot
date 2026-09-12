"""Typed decisions from Codex; only the host executes the public game protocol."""
import json
import shutil
import time

import minepilot
from codex_transport import PersistentModel
from decision_context import instructions

_MODELS = {}

class InvalidNavigationArguments(ValueError):
    """A rejected request that has not reached any mutating game tool."""

def close():
    for model in _MODELS.values(): model.close()
    _MODELS.clear()


PROPERTIES = {
    "action": {"type": "string", "enum": ["say", "navigate", "choose", "cancel", "plan", "jump", "wait", "tool", "organize"]},
    "tool_name": {"type": ["string", "null"], "enum": ["turn", "sense", "listen", "inventory", "drop_items", "reclaim_drop", "item_origins", "waypoint", "survey_mining", "mining_survey_status", "plan_excavation", "choose_excavation", "excavation_status", "pause_excavation", "resume_excavation", "cancel_excavation", "equip_tool", "plan_mining", "choose_mining", "mining_status", "pause_mining", "resume_mining", "cancel_mining", "inspect_tree", "tree_farm", "collect", "plan_collection", "choose_collection", "collection_status", "pause_collection", "resume_collection", "cancel_collection", "set_hand", "inventory_capacity", "inspect_placement", "place_block", "plan_placement", "choose_placement", "placement_status", "pause_placement", "resume_placement", "cancel_placement", "resolve_placement", "craft", "interact_block", "inspect_container", "transfer_items", "close_container", "smelt", "eat", "survival_status", "cancel_survival", "gather", "gather_status", "cancel_gather", "find_resources", "remember_context", "companion_mode", "build_camp", "camp_status", "resume_camp", "cancel_camp", None]},
    "arguments_json": {"type": ["string", "null"]},
    "annotations": {"type": "array", "maxItems": 16, "items": {"type": "object", "additionalProperties": False,
        "properties": {"entry_id": {"type": "string"}, "importance": {"type": "integer", "minimum": 0, "maximum": 5}, "note": {"type": "string", "maxLength": 256}},
        "required": ["entry_id", "importance", "note"]}},
    "message": {"type": "string"},
    "speech_reason": {"type": "string", "enum": ["none", "player_gift_or_loan", "requested_collection", "requested_report", "direct_player_relevance"]},
    "target_kind": {"type": ["string", "null"], "enum": ["coordinates", "player", "entity", "dropped_item", "waypoint", "world_spawn", "respawn_point", "death_point", None]},
    "dimension": {"type": ["string", "null"]},
    "target_name": {"type": ["string", "null"], "description": "Required for player/entity/dropped_item: observed UUID or exact name; dropped_item requires its observed entity UUID. For waypoint use its saved name."},
    "x": {"type": ["number", "null"]}, "y": {"type": ["number", "null"]}, "z": {"type": ["number", "null"]},
    "acceptance_radius": {"type": ["number", "null"]},
    "forward_blocks": {"type": ["number", "null"]},
    "request_id": {"type": ["string", "null"]},
    "option_id": {"type": ["string", "null"]},
    "pace": {"type": "string", "enum": ["auto", "walk", "sprint", "sprint_jump", "sneak"]},
}
GENERAL_PROPERTIES = {k: PROPERTIES[k] for k in ("action", "message", "tool_name", "arguments_json", "pace", "target_kind")}
GENERAL_PROPERTIES["arguments_json"] = {"type": ["string", "null"], "description": "JSON object containing only arguments for the chosen action; null for say/wait/jump. Navigation uses x/y/z or target_name, acceptance_radius and optional replace_request_id. pace and target_kind stay typed top-level fields. Tool calls use the selected tool's arguments."}
GENERAL_PROPERTIES["objective"] = {"type":["string","null"],"description":"Concise overall task including unfinished later steps; null preserves the existing goal. Casual chat and future wishes preserve it.","maxLength":512}
GENERAL_PROPERTIES["goal_status"] = {"type":"string","enum":["KEEP","ACTIVE","COMPLETED","BLOCKED","PAUSED"],"description":"KEEP during casual chat/intermediate actions. ACTIVE starts/replaces the task. COMPLETED requires evidence for every requested step."}
SCHEMA = {"type": "object", "additionalProperties": False, "properties": GENERAL_PROPERTIES, "required": list(GENERAL_PROPERTIES)}


def event_schema(event):
    if event.get("type") == "inventory_event" and event.get("speechFirst"):
        return {"type":"object", "additionalProperties":False,
                "properties":{"action":{"type":"string","enum":["say","wait"]}, "message":PROPERTIES["message"]},
                "required":["action","message"]}
    if event.get("type") == "tool_result" and event.get("tool") in {"plan_collection", "plan_mining", "plan_placement"} and event.get("result", {}).get("phase") == "PLAN_READY":
        options = event["result"].get("options", [])
        properties = {"action":{"type":"string","enum":["approve","reject"]},
            "option_id":{"type":["string","null"],"enum":[o["optionId"] for o in options] + [None]},
            "message":PROPERTIES["message"]}
        return {"type": "object", "additionalProperties": False, "properties": properties, "required": list(properties)}
    if event.get("type") in {"inventory_event", "inventory_review"}:
        properties = {k: PROPERTIES[k] for k in ("action", "message", "annotations", "speech_reason", "tool_name", "arguments_json")}
        properties["action"] = {"type": "string", "enum": ["say", "wait", "organize", "tool"]}
        properties["tool_name"] = {"type": ["string", "null"], "enum": ["inventory", "inventory_capacity", "drop_items", "reclaim_drop", None]}
        return {"type": "object", "additionalProperties": False, "properties": properties, "required": list(properties)}
    if event.get("type") == "autonomy_event":
        properties = {**GENERAL_PROPERTIES, "speech_reason": PROPERTIES["speech_reason"]}
        return {"type":"object", "additionalProperties":False, "properties":properties, "required":list(properties)}
    if event.get("type") == "operation_failed" and not event.get("recoverable"):
        properties = {"action": {"type": "string", "enum": ["say", "wait"]}, "message": PROPERTIES["message"]}
        return {"type": "object", "additionalProperties": False, "properties": properties, "required": list(properties)}
    if event.get("type") != "navigation_event": return SCHEMA
    phase = event.get("state", {}).get("phase")
    if phase == "COMPLETED" and event.get("request"):
        # Reaching an approach point need not finish the original player task.
        return SCHEMA
    actions = (["choose", "cancel"] if phase == "PLAN_READY" else
               ["plan", "cancel", "say", "wait"] if phase == "REPLAN_REQUIRED" else ["say"] if phase in {"FAILED", "APPROACHED"} else ["say", "wait"])
    names = ["action", "message", "request_id", "option_id", "pace"] if phase == "PLAN_READY" else ["action", "message", "request_id"]
    properties = {k: PROPERTIES[k] for k in names}
    properties["action"] = {"type": "string", "enum": actions}
    return {"type": "object", "additionalProperties": False, "properties": properties, "required": names}


def model_for(args, directory):
    config = getattr(args, "model_provider", {"type": "codex"})
    kind = config.get("type", "codex")
    if kind == "openai_compatible":
        from compatible_transport import CompatibleModel
        key = (str(directory), kind, args.model, json.dumps(config, sort_keys=True))
        if key not in _MODELS: _MODELS[key] = CompatibleModel(args.model, config)
        return _MODELS[key]
    if kind != "codex":
        raise ValueError("Unknown model provider")
    executable = args.codex or shutil.which("codex")
    if not executable:
        raise RuntimeError("Codex CLI not found; pass --codex ABSOLUTE_PATH")
    workspace = directory / "workspace"
    workspace.mkdir(exist_ok=True)
    tier = getattr(args, "service_tier", "default")
    key = (str(workspace), executable, args.model, tier)
    if key not in _MODELS: _MODELS[key] = PersistentModel(executable, workspace, args.model, tier)
    return _MODELS[key]



def prepare(args, directory):
    # Establish transport only; no inference or game action is used for warming.
    model_for(args, directory).prepare(instructions())


def launch(args, directory, event, last_chat):
    return model_for(args, directory).launch(instructions(),
        f"Last delivered chat sequence: {last_chat}. Event data:\n" + json.dumps(event, ensure_ascii=False, separators=(",", ":")), event_schema(event))


def bind_plan_decision(value, event):
    if value.get("action") not in {"approve","reject"}: return value
    if event.get("type") != "tool_result" or event.get("tool") not in {"plan_collection","plan_mining","plan_placement"} or event.get("result",{}).get("phase") != "PLAN_READY":
        raise ValueError("Approval requires an evaluated plan event")
    result = event["result"]
    args = {"request_id":result["requestId"]}
    if value["action"] == "approve":
        if value.get("option_id") not in {o["optionId"] for o in result.get("options",[])}:
            raise ValueError("Approval must choose a returned option")
        args["option_id"] = value["option_id"]
    kind = event["tool"].removeprefix("plan_")
    return {"action":"tool", "tool_name":("choose_" if value["action"] == "approve" else "cancel_") + kind,
        "arguments_json":json.dumps(args), "message":value.get("message", "")}



ACTION_ARGUMENTS = {
    "navigate": {"target_kind", "dimension", "target_name", "x", "y", "z", "acceptance_radius", "forward_blocks", "pace", "continuous_follow", "replace_request_id", "player_intent"},
    "cancel": {"request_id"}, "plan": {"request_id"}, "choose": {"request_id", "option_id", "pace"},
    "organize": {"annotations"}, "say": set(), "wait": set(), "jump": set(),
}

def normalize_decision(value):
    if value.get("action") == "tool" or not value.get("arguments_json"): return value
    raw=value["arguments_json"]
    if not isinstance(raw,str) or len(raw)>4096: raise ValueError("Action arguments too large")
    args=json.loads(raw)
    if not isinstance(args,dict) or not set(args)<=ACTION_ARGUMENTS.get(value.get("action"),set()):
        raise ValueError("Unexpected action arguments")
    # Never permit nested data to change the action, speech, or execution authority.
    if any(k in value and value[k] is not None and value[k]!=v for k,v in args.items()):
        raise ValueError("Conflicting action arguments")
    return {**{k:v for k,v in value.items() if k!="arguments_json"},**args}

def apply(client, value):
    # Idempotency is controller plumbing, not something the model must invent.
    if value.get("tool_name") == "drop_items":
        import uuid
        args = json.loads(value.get("arguments_json") or "{}")
        if isinstance(args,dict):
            if "items" not in args and "item" in args and "count" in args:
                selection = {k:args.pop(k) for k in ("item","count","slot","entry_id","expected_damage") if k in args}
                args["items"] = [selection]
            if not args.get("request_key"): args["request_key"] = "drop-" + uuid.uuid4().hex
            value["arguments_json"] = json.dumps(args)
    value = normalize_decision(value)
    action = value["action"]
    message = (value.get("message") or "").strip()
    if action == "tool":
        name = value.get("tool_name")
        if name not in {"turn", "sense", "listen", "inventory", "drop_items", "reclaim_drop", "item_origins", "waypoint", "survey_mining", "mining_survey_status", "plan_excavation", "choose_excavation", "excavation_status", "pause_excavation", "resume_excavation", "cancel_excavation", "equip_tool", "plan_mining", "choose_mining", "mining_status", "pause_mining", "resume_mining", "cancel_mining", "inspect_tree", "tree_farm", "collect", "plan_collection", "choose_collection", "collection_status", "pause_collection", "resume_collection", "cancel_collection", "set_hand", "inventory_capacity", "inspect_placement", "place_block", "plan_placement", "choose_placement", "placement_status", "pause_placement", "resume_placement", "cancel_placement", "resolve_placement", "craft", "interact_block", "inspect_container", "transfer_items", "close_container", "smelt", "eat", "survival_status", "cancel_survival", "gather", "gather_status", "cancel_gather", "find_resources", "remember_context", "companion_mode", "build_camp", "camp_status", "resume_camp", "cancel_camp"}: raise ValueError("Unexposed game tool")
        raw = value.get("arguments_json") or "{}"
        if len(raw) > (65536 if name == "plan_placement" else 8192): raise ValueError("Tool arguments too large")
        args = json.loads(raw)
        if not isinstance(args, dict): raise ValueError("Expected a game argument object")
        if name in {"gather", "collect", "craft", "smelt", "eat", "interact_block", "plan_mining", "plan_collection", "plan_placement", "place_block", "plan_excavation"}:
            navigation = client.call_tool("navigation_status", {})
            if navigation.get("requestId") and navigation.get("phase") not in {"IDLE", "COMPLETED", "APPROACHED", "FAILED", "CANCELLED"}:
                client.call_tool("cancel_navigation", {"request_id": navigation["requestId"], "reason": "Starting the requested work at this location"})
        result = client.call_tool(name, args)
        # Empty pages contain no decision. Advance them locally within a short
        # bound instead of spending a model inference on every scan cursor.
        if name == "find_resources" or name == "sense" and args.get("kind") in {"blocks", "trees", "structures", "entities", "items"}:
            deadline = time.monotonic() + .4
            for _ in range(32):
                if result.get("complete", True) or result.get("results") or not result.get("cursor") or time.monotonic() >= deadline:
                    break
                if not result.get("scannedCells", result.get("examinedCandidates", 1)):
                    time.sleep(min(.05, max(0, deadline-time.monotonic())))
                args = {**args, "cursor": result["cursor"]}
                if "nextOffset" in result: args["offset"] = result["nextOffset"]
                if result.get("source") == "server_structure_records":
                    args.update(radius=result["radius"], filter=result["filter"])
                result = client.call_tool(name, args)
        if message and result.get("status") not in {"BLOCKED","PARTIAL","TOOL_ERROR"} and result.get("phase") not in {"BLOCKED","FAILED"}:
            client.call_tool("say", {"message": message})
        return {"tool": name, "result": result}
    if action == "organize":
        annotations = value.get("annotations", [])
        if not isinstance(annotations,list) or len(annotations)>16: raise ValueError("Too many annotations")
        for entry in annotations: client.call_tool("annotate_item",entry)
        if message: client.call_tool("say",{"message":message})
        return
    if action == "wait": return
    if action == "say":
        if not message: return
        client.call_tool("say", {"message": message}); return
    if action == "jump":
        current = client.call_tool("navigation_status", {})
        if current.get("requestId") and current.get("phase") not in {"IDLE", "COMPLETED", "APPROACHED", "FAILED", "CANCELLED"}:
            client.call_tool("cancel_navigation", {"request_id": current["requestId"], "reason": "Player requested a single jump"})
        client.call_tool("jump_once", {})
        return
    if action == "navigate":
        requested_pace = value.get("pace") or "auto"
        if requested_pace not in {"auto","walk","sprint","sprint_jump","sneak"}:
            raise InvalidNavigationArguments("Invalid pace. Choose auto, walk, sprint, sprint_jump or sneak. No navigation was submitted.")
        if value.get("target_kind") not in {"coordinates","player","entity","dropped_item","waypoint","world_spawn","respawn_point","death_point"}:
            raise InvalidNavigationArguments("Missing or invalid target_kind. No navigation was submitted.")
        if "continuous_follow" in value and not isinstance(value["continuous_follow"],bool):
            raise InvalidNavigationArguments("continuous_follow must be a boolean. No navigation was submitted.")
        if value.get("target_kind") in {"player", "entity", "dropped_item", "waypoint"} and not (value.get("target_name") or "").strip():
            raise InvalidNavigationArguments("Missing target_name: use the observed target entity UUID, or the saved waypoint name. No navigation request was submitted.")
        parameters = minepilot.compact({k: value.get(k) for k in
                                      ("target_kind", "dimension", "target_name", "x", "y", "z", "acceptance_radius", "forward_blocks", "continuous_follow", "replace_request_id")})
        if value.get("forward_blocks") is not None:
            parameters["target_kind"] = "coordinates"
            parameters["acceptance_radius"] = .5
        if value.get("target_kind") == "dropped_item": parameters["acceptance_radius"] = .5
        intent = (value.get("player_intent") or message).strip()
        if not intent:
            raise InvalidNavigationArguments("Missing player_intent for navigation. Retain the current player request; no navigation was submitted.")
        parameters.update(preferred_pace=requested_pace, player_intent=intent)
        parameters["allow_partial"] = value.get("target_kind") == "player" and not value.get("continuous_follow",False)
        accepted = client.call_tool("request_navigation", parameters)
        request = accepted["requestId"]
        try:
            client.call_tool("say", {"message": message or "我继续去取。" if value.get("target_kind") == "dropped_item" else message or "我继续过去。", "navigation_request_id": request})
            client.call_tool("plan_navigation", {"request_id": request})
        except minepilot.ClientError:
            state = client.call_tool("navigation_status", {})
            if state.get("requestId") == request and state.get("phase") not in {"APPROACHED", "FAILED", "COMPLETED", "CANCELLED"}:
                client.call_tool("cancel_navigation", {"request_id": request, "reason": "Preparation failed"})
            raise
        return
    if action == "choose":
        current = client.call_tool("navigation_status", {})
        if current.get("partialDestination") and message: client.call_tool("say", {"message": message})
        client.call_tool("choose_navigation", {"request_id": value.get("request_id"),
                         "option_id": value.get("option_id"), "pace": value.get("pace", "auto")}); return
    if action == "cancel":
        client.call_tool("cancel_navigation", {"request_id": value.get("request_id"), "reason": message or "Request ended without additional chat"})
        if message: client.call_tool("say", {"message": message})
        return
    if action == "plan":
        client.call_tool("plan_navigation", {"request_id": value.get("request_id")})
