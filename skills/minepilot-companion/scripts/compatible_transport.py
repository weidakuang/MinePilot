"""Receive model streams privately; deliver only complete decisions to the host."""
import http.client
import copy
import json
from pathlib import Path
import socket
import threading
import time
import uuid
from urllib.parse import urlsplit

from codex_transport import ModelWorker
from decision_context import native_instructions, native_messages, tool_help


def validate_decision(value, schema):
    """Validate the bounded schema vocabulary used by the companion protocol."""
    kinds = schema.get("type", [])
    kinds = [kinds] if isinstance(kinds, str) else kinds
    matches = {"null": value is None, "string": isinstance(value, str),
               "object": isinstance(value, dict), "array": isinstance(value, list),
               "number": type(value) in (int, float), "integer": type(value) is int,
               "boolean": type(value) is bool}
    if kinds and not any(matches.get(kind, False) for kind in kinds):
        raise ValueError("Decision field has an invalid type")
    if "enum" in schema and value not in schema["enum"]:
        raise ValueError("Decision field is outside its allowed values")
    if isinstance(value, dict):
        properties = schema.get("properties", {})
        if not set(schema.get("required", [])).issubset(value):
            raise ValueError("Decision is missing required fields")
        if schema.get("additionalProperties") is False and set(value) - set(properties):
            raise ValueError("Decision contains unknown fields")
        for key, item in value.items():
            if key in properties:
                validate_decision(item, properties[key])
    if isinstance(value, list):
        if len(value) > schema.get("maxItems", 65536):
            raise ValueError("Decision array exceeded its limit")
        for item in value:
            validate_decision(item, schema.get("items", {}))
    if isinstance(value, str) and len(value) > schema.get("maxLength", 65536):
        raise ValueError("Decision text exceeded its limit")
    if type(value) in (int, float):
        import math
        if not math.isfinite(value) or value < schema.get("minimum", -float("inf")) or value > schema.get("maximum", float("inf")):
            raise ValueError("Decision number is outside its limit")


class CompatibleModel:
    provider = "openai_compatible"

    def __init__(self, model, config):
        self.model = model
        self.base_url = config["baseUrl"].rstrip("/")
        self.endpoint = urlsplit(self.base_url)
        if (self.endpoint.scheme not in {"http", "https"} or not self.endpoint.hostname
                or self.endpoint.username or self.endpoint.password
                or self.endpoint.query or self.endpoint.fragment):
            raise ValueError("Model baseUrl must be an HTTP(S) endpoint without credentials or query")
        self.key_file = Path(config["apiKeyFile"]).expanduser()
        self.context_window = int(config.get("contextWindow", 128000))
        self.max_output_tokens = int(config.get("maxOutputTokens", 4096))
        self.stream = config.get("stream", True)
        self.native_tools = config.get("nativeTools", True)
        self.thinking = config.get("thinking")
        if self.thinking not in {None, "enabled", "disabled"}:
            raise ValueError("thinking must be enabled or disabled")
        self.chat_template_kwargs = config.get("chatTemplateKwargs")
        self.reasoning_budget = config.get("reasoningBudgetTokens")
        if self.reasoning_budget is not None and (type(self.reasoning_budget) is not int or not 0 <= self.reasoning_budget <= 4096):
            raise ValueError("reasoningBudgetTokens must be an integer from 0 to 4096")
        if self.chat_template_kwargs is not None and not isinstance(self.chat_template_kwargs, dict):
            raise ValueError("chatTemplateKwargs must be an object")
        if not 256 <= self.max_output_tokens < self.context_window <= 2000000:
            raise ValueError("Invalid model context or output limit")
        self.actual_service_tier = None
        self.reasoning_effort = None
        self.ready = threading.Event()
        self.closed = False
        self.last_error = None
        self.worker = None
        self.last_usage = None
        self.last_request_count = 0
        self.tool_catalog = {}
        self.lock = threading.Lock()
        self.connections = {}
        self._key()

    def _key(self):
        key = self.key_file.read_text().strip()
        if not key or "\n" in key or "\r" in key:
            raise ValueError("Model credential file is empty or invalid")
        return key

    def configure_tools(self, definitions):
        self.tool_catalog = {row["name"]:{"name":row["name"], "description":row.get("description",""),
                                            "parameters":copy.deepcopy(row["inputSchema"])}
                             for row in definitions if isinstance(row,dict) and isinstance(row.get("inputSchema"),dict)}

    def _disclose(self, name):
        function = copy.deepcopy(self.tool_catalog[name])
        if name == "drop_items":
            function["parameters"]["required"] = [k for k in function["parameters"].get("required",[]) if k != "request_key"]
        # Keep real schema constraints. Verbose rationale remains in tool help.
        def compact(value):
            if isinstance(value,dict):
                return {key:(item[:160] if key=="description" and isinstance(item,str) else compact(item)) for key,item in value.items()}
            if isinstance(value,list): return [compact(item) for item in value]
            return value
        return {"type":"function", "function":compact(function)}

    def _request(self, method, suffix, payload=None, worker=None):
        connection_type = http.client.HTTPSConnection if self.endpoint.scheme == "https" else http.client.HTTPConnection
        connection = connection_type(self.endpoint.hostname, self.endpoint.port, timeout=90)
        with self.lock:
            if self.closed or worker is not None and worker.cancelled:
                raise RuntimeError("Model request was cancelled")
            self.connections[worker] = connection
        try:
            body = None if payload is None else json.dumps(payload, ensure_ascii=False, allow_nan=False).encode()
            connection.request(method, self.endpoint.path + suffix, body,
                               {"Authorization": "Bearer " + self._key(), "Content-Type": "application/json"})
            response = connection.getresponse()
            if response.status != 200:
                # Neither provider bodies nor credentials enter status or journals.
                raise RuntimeError("Model endpoint returned HTTP " + str(response.status))
            if worker is not None and "text/event-stream" in response.getheader("Content-Type", ""):
                return self._read_stream(response, worker)
            raw = response.read(1024 * 1024 + 1)
            if len(raw) > 1024 * 1024:
                raise ValueError("Model response exceeded its size limit")
            return json.loads(raw)
        finally:
            with self.lock:
                self.connections.pop(worker, None)
            connection.close()

    def _read_stream(self, response, worker):
        # Numen's accumulator boundary: chunks remain private to this request;
        # the game receives a complete validated decision only after completion.
        content, data, calls = [], [], {}
        size = 0
        finish = None
        usage = None
        while not worker.cancelled and not self.closed:
            line = response.readline(65537)
            if not line:
                break
            size += len(line)
            if len(line) > 65536 or size > 4 * 1024 * 1024:
                raise ValueError("Model stream exceeded its size limit")
            if line.startswith(b"data:"):
                data.append(line[5:].strip())
                continue
            if line.strip() or not data:
                continue
            frame = b"\n".join(data)
            data.clear()
            if frame == b"[DONE]":
                break
            chunk = json.loads(frame)
            if chunk.get("usage"):
                usage = {k:v for k,v in chunk["usage"].items()
                         if k in {"prompt_tokens", "completion_tokens", "total_tokens"} and type(v) is int}
            for choice in chunk.get("choices", []):
                if choice.get("index", 0) != 0:
                    continue
                delta = choice.get("delta", {})
                piece = delta.get("content")
                if isinstance(piece, str) and piece:
                    if worker.first_text_at is None:
                        worker.first_text_at = time.monotonic()
                    content.append(piece)
                for fragment in delta.get("tool_calls", []):
                    index = fragment.get("index", 0)
                    if type(index) is not int or not 0 <= index < 8:
                        raise ValueError("Invalid streamed tool index")
                    call = calls.setdefault(index, {"id":"", "type":"function", "function":{"name":"", "arguments":""}})
                    if fragment.get("id"): call["id"] += fragment["id"]
                    for key in ("name", "arguments"):
                        part = fragment.get("function", {}).get(key)
                        if part:
                            if not isinstance(part, str): raise ValueError("Invalid tool fragment")
                            call["function"][key] += part
                            if worker.first_text_at is None: worker.first_text_at = time.monotonic()
                if choice.get("finish_reason"):
                    finish = choice["finish_reason"]
        if worker.cancelled or self.closed:
            raise RuntimeError("Model request was cancelled")
        if finish not in {"stop", "tool_calls"}:
            raise RuntimeError("Model stream ended without a complete decision")
        message = {"content": "".join(content)}
        if calls: message["tool_calls"] = [calls[index] for index in sorted(calls)]
        return {"choices": [{"finish_reason": finish, "message": message}], "usage": usage}

    @staticmethod
    def _tool_schema(schema):
        # The host's internal envelope has mandatory nulls for Codex structured
        # output. Native tool callers only need fields relevant to their action.
        native = copy.deepcopy(schema)
        native["required"] = ["action"]
        properties = native["properties"]
        if "arguments_json" in properties:
            properties.pop("arguments_json")
            properties["arguments"] = {"type":"object", "description":"Arguments for the selected game action or tool."}
        return native

    @staticmethod
    def _complete_decision(value, schema):
        value = dict(value)
        if "arguments" in value:
            args = value.pop("arguments")
            if not isinstance(args, dict): raise ValueError("Tool arguments must be an object")
            value["arguments_json"] = json.dumps(args, ensure_ascii=False, separators=(",", ":"))
        defaults = {"message":"", "pace":"auto", "goal_status":"KEEP", "speech_reason":"none", "annotations":[]}
        for key in schema.get("required", []):
            if key in value: continue
            if key in defaults: value[key] = defaults[key]
            elif "null" in schema["properties"][key].get("type", []): value[key] = None
        validate_decision(value, schema)
        return value

    def prepare(self, instructions):
        threading.Thread(target=self._prepare, daemon=True).start()

    def _prepare(self):
        try:
            result = self._request("GET", "/models")
            if self.model not in {row.get("id") for row in result.get("data", [])}:
                raise RuntimeError("Configured model is not listed by the endpoint")
            if not self.closed:
                self.last_error = None
                self.ready.set()
        except (OSError, ValueError, KeyError, TypeError, AttributeError, RuntimeError, http.client.HTTPException) as failure:
            self.ready.clear()
            self.last_error = str(failure) if type(failure) is RuntimeError else "Model endpoint preparation failed"

    def launch(self, instructions, event_text, schema):
        worker = ModelWorker(self)
        worker.thread_id = worker.turn_id = str(uuid.uuid4())
        self.worker = worker
        threading.Thread(target=self._begin, args=(worker, instructions, event_text, schema), daemon=True).start()
        return worker

    def _begin(self, worker, instructions, event_text, schema):
        try:
            # UTF-8 bytes give a conservative input bound without depending on a
            # provider-specific tokenizer. Never truncate the current world facts.
            input_bytes = len((instructions + event_text + json.dumps(schema)).encode())
            if input_bytes > self.context_window - self.max_output_tokens:
                raise RuntimeError("Decision exceeds the conservative model input budget")
            system = instructions
            if self.native_tools:
                system = native_instructions()
                if self.tool_catalog:
                    system += "\nOnly the first tool call in a response is executed; wait for its actual result before requesting dependent actions. Use the named game functions directly when available (especially collect for a nearby designated tree); minepilot_action is for navigation, goal bookkeeping, or a tool not yet disclosed. Do not navigate separately before collect/place_block/craft: these tasks approach internally. Never navigate to the center of a solid target block.\n"
            if 'Event data:\n' in event_text:
                current_event = json.loads(event_text.split('Event data:\n',1)[1])
                if current_event.get("speechFirst"):
                    from pickup_notifications import instructions as pickup_instructions
                    system = pickup_instructions()
            payload = {"model": self.model, "stream": self.stream,
                       "messages": [{"role": "system", "content": system}] +
                                   (native_messages(event_text) if self.native_tools else [{"role":"user", "content":event_text}]),
                       "max_tokens": self.max_output_tokens}
            if self.native_tools:
                payload["tools"] = [{"type":"function", "function":{
                    "name":"minepilot_action", "description":"Execute one authorized game action. Native jobs continue independently; use wait for silent intermediate events.",
                    "parameters":self._tool_schema(schema)}}]
                help_names = [name for name in schema.get("properties", {}).get("tool_name", {}).get("enum", []) if isinstance(name,str)]
                if help_names:
                    payload["tools"].append({"type":"function", "function":{
                        "name":"game_tool_help", "description":"Read a game tool's detailed contract; no game action is performed.",
                        "parameters":{"type":"object", "properties":{"tool_name":{"type":"string"}},
                                      "required":["tool_name"], "additionalProperties":False}}})
                disclosed = set(help_names) & set(self.tool_catalog) & {"collect","gather","place_block","craft","sense","drop_items"}
                payload["tools"].extend(self._disclose(name) for name in sorted(disclosed))
                payload["tool_choice"] = "auto"
                payload["parallel_tool_calls"] = False
            else:
                payload["response_format"] = {"type": "json_schema", "json_schema": {
                    "name": "minepilot_decision", "strict": True, "schema": schema}}
            if self.chat_template_kwargs is not None:
                payload["chat_template_kwargs"] = self.chat_template_kwargs
            if self.thinking is not None:
                payload["thinking"] = {"type":self.thinking}
            if self.reasoning_budget is not None:
                payload["reasoning_budget_tokens"] = self.reasoning_budget
                payload["reasoning_format"] = "deepseek"
            if self.stream:
                payload["stream_options"] = {"include_usage": True}
            worker.submitted_at = time.monotonic()
            total_usage = {}
            for attempt in range(3):
                result = self._request("POST", "/chat/completions", payload, worker)
                for key,value in (result.get("usage") or {}).items():
                    if type(value) is int: total_usage[key] = total_usage.get(key,0) + value
                message = result["choices"][0]["message"]
                calls = message.get("tool_calls") or []
                if not self.native_tools or not any(call.get("function",{}).get("name")=="game_tool_help" for call in calls): break
                if attempt == 2 or len(calls)!=1 or result["choices"][0].get("finish_reason") not in {"stop","tool_calls"}:
                    raise ValueError("Tool documentation request was incomplete or exceeded its bound")
                arguments = json.loads(calls[0]["function"]["arguments"])
                if not isinstance(arguments,dict) or set(arguments)!={"tool_name"} or arguments["tool_name"] not in help_names:
                    raise ValueError("Unknown tool documentation request")
                payload["messages"].extend([
                    {"role":"assistant", "content":message.get("content"), "tool_calls":calls},
                    {"role":"tool", "tool_call_id":calls[0]["id"], "content":tool_help(arguments["tool_name"])}])
                name = arguments["tool_name"]
                if name in self.tool_catalog and name not in disclosed:
                    payload["tools"].append(self._disclose(name))
                    disclosed.add(name)
                if len(json.dumps(payload,ensure_ascii=False).encode()) > self.context_window-self.max_output_tokens:
                    raise RuntimeError("Decision exceeds the conservative model input budget")
            if worker.cancelled or self.closed:
                return
            choice = result["choices"][0]
            if choice.get("finish_reason") not in ({"stop", "tool_calls"} if self.native_tools else {"stop"}):
                raise RuntimeError("Model did not complete a structured decision")
            message = choice["message"]
            content = message.get("content") or ""
            if not isinstance(content, str) or len(content) > 65536:
                raise ValueError("Invalid model decision text")
            calls = message.get("tool_calls") or []
            if self.native_tools and calls:
                # Providers can return several calls despite parallel_tool_calls=false.
                # Execute only the first; subsequent actions are re-decided from its
                # actual result, never run concurrently or assume it succeeded.
                calls = calls[:1]
                name = calls[0].get("function", {}).get("name")
                arguments = json.loads(calls[0]["function"]["arguments"])
                if name == "minepilot_action": decision = self._complete_decision(arguments, schema)
                elif name in disclosed:
                    if name == "drop_items" and not arguments.get("request_key"):
                        arguments["request_key"] = "drop-" + worker.turn_id
                    validate_decision(arguments, self.tool_catalog[name]["parameters"])
                    decision = self._complete_decision({"action":"tool", "tool_name":name, "arguments":arguments},schema)
                else: raise ValueError("Model selected a tool that was not disclosed")
                # Any prose accompanying an action stays private: only its
                # explicit message is delivered after the game accepts the call.
            elif self.native_tools:
                if choice.get("finish_reason") != "stop" or not content.strip():
                    raise RuntimeError("Model returned no complete response")
                if content.lstrip().startswith(("{", "```", "<think", "<tool_call")):
                    raise ValueError("Model printed protocol text instead of a response")
                decision = self._complete_decision({"action":"say", "message":content.strip()}, schema)
            else:
                decision = json.loads(content)
            validate_decision(decision, schema)
            if worker.cancelled or self.closed:
                return
            worker.text = json.dumps(decision, ensure_ascii=False, allow_nan=False)
            if worker.first_text_at is None:
                worker.first_text_at = time.monotonic()
            self.last_usage = total_usage
            self.last_request_count = attempt + 1
            self.last_error = None
            self.ready.set()
            worker.finish(0)
        except (OSError, ValueError, KeyError, IndexError, TypeError, AttributeError, RuntimeError, http.client.HTTPException) as failure:
            if not worker.cancelled and not self.closed:
                self.last_error = str(failure) if type(failure) in {RuntimeError, ValueError} else type(failure).__name__
                worker.error = self.last_error
                self.ready.clear()
            worker.finish(-15 if worker.cancelled or self.closed else 1)

    @staticmethod
    def _abort(connection):
        if connection is None:
            return
        try:
            if connection.sock is not None:
                connection.sock.shutdown(socket.SHUT_RDWR)
        except OSError:
            pass
        connection.close()

    def interrupt(self, worker):
        with self.lock:
            connection = self.connections.get(worker)
        self._abort(connection)

    def close(self):
        self.closed = True
        self.ready.clear()
        if self.worker is not None:
            self.worker.terminate()
        with self.lock:
            connections = list(self.connections.values())
        for connection in connections:
            self._abort(connection)
