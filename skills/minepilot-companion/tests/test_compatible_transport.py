import http.server
import json
from pathlib import Path
import sys
import tempfile
import threading
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "scripts"))
from compatible_transport import CompatibleModel
from codex_decisions import SCHEMA as GAME_SCHEMA

SCHEMA = {"type": "object", "additionalProperties": False,
          "properties": {"action": {"type": "string", "enum": ["say"]},
                         "message": {"type": "string"}}, "required": ["action", "message"]}


class CompatibleTransportTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        key = Path(self.temp.name) / "key"
        key.write_text("private-test-credential")
        self.requests = []
        self.entered = threading.Event()
        self.release = threading.Event()
        self.mode = "ok"
        fixture = self

        class Handler(http.server.BaseHTTPRequestHandler):
            def log_message(self, *args): pass

            def respond(self, value, status=200):
                body = json.dumps(value).encode()
                try:
                    self.send_response(status)
                    self.send_header("Content-Length", str(len(body)))
                    self.end_headers()
                    self.wfile.write(body)
                except (BrokenPipeError, ConnectionResetError):
                    pass

            def do_GET(self):
                self.respond({"data": [{"id": "test-model"}]})

            def do_POST(self):
                payload = json.loads(self.rfile.read(int(self.headers["Content-Length"])))
                fixture.requests.append((self.path, self.headers["Authorization"], payload))
                if fixture.mode in {"named", "invalid_named", "deferred"}:
                    name = "collect"
                    arguments = {"resource": "wood" if fixture.mode != "invalid_named" else 42}
                    if fixture.mode == "deferred":
                        name = "game_tool_help" if len(fixture.requests)==1 else "inventory"
                        arguments = {"tool_name":"inventory"} if len(fixture.requests)==1 else {}
                    self.respond({"choices":[{"finish_reason":"tool_calls", "message":{
                        "content":None, "tool_calls":[{"id":"call_named", "type":"function", "function":{
                            "name":name, "arguments":json.dumps(arguments)}}]}}]})
                    return
                if fixture.mode in {"stream", "tool_stream", "tool_truncated", "two_tools"}:
                    self.send_response(200)
                    self.send_header("Content-Type", "text/event-stream")
                    self.end_headers()
                    def frame(value):
                        self.wfile.write(("data: " + json.dumps(value) + "\n\n").encode())
                        self.wfile.flush()
                    native = fixture.mode != "stream"
                    first = {"tool_calls":[{"index":0,"id":"call_1","function":{"name":"minepilot_action","arguments":"{\"action\":\"say\","}}],
                             "reasoning_content":"private reasoning must never become chat"} if native else {"content":"{\"action\":\"say\","}
                    frame({"choices":[{"index":0,"delta":first}]})
                    fixture.entered.set()
                    fixture.release.wait(5)
                    try:
                        second = {"tool_calls":[{"index":0,"function":{"arguments":"\"message\":\"hello\"}"}}]} if native else {"content":"\"message\":\"hello\"}"}
                        if fixture.mode == "two_tools":
                            second["tool_calls"].append({"index":1,"function":{"name":"minepilot_action","arguments":"{\"action\":\"say\",\"message\":\"again\"}"}})
                        frame({"choices":[{"index":0,"delta":second,"finish_reason":"length" if fixture.mode == "tool_truncated" else "tool_calls" if native else "stop"}]})
                        self.wfile.write(b"data: [DONE]\n\n")
                    except (BrokenPipeError, ConnectionResetError): pass
                    return
                if fixture.mode == "stall_first" and len(fixture.requests) == 1:
                    fixture.entered.set()
                    fixture.release.wait(5)
                if fixture.mode == "error":
                    self.respond({"error": "private-test-credential"}, 401)
                    return
                content = {"action": "say", "message": payload["messages"][-1]["content"]}
                if fixture.mode == "invalid": content["action"] = "navigate"
                if fixture.mode == "missing": del content["message"]
                self.respond({"choices": [{"finish_reason": "stop", "message": {
                    "content": "你好，我在这里。" if fixture.mode == "native_text" else json.dumps(content), "reasoning_content": "never retain provider reasoning"}}]})

        self.server = http.server.ThreadingHTTPServer(("127.0.0.1", 0), Handler)
        threading.Thread(target=self.server.serve_forever, daemon=True).start()
        self.model = CompatibleModel("test-model", {
            "baseUrl": "http://127.0.0.1:" + str(self.server.server_port) + "/v1",
            "apiKeyFile": str(key), "contextWindow": 128000, "maxOutputTokens": 4096, "nativeTools":False})

    def tearDown(self):
        self.release.set()
        self.model.close()
        self.server.shutdown()
        self.server.server_close()
        self.temp.cleanup()

    def test_nonstreaming_schema_decision_and_readiness(self):
        self.model._prepare()
        self.assertTrue(self.model.ready.is_set())
        worker = self.model.launch("system instructions", "hello", SCHEMA)
        self.assertEqual(0, worker.wait(3))
        self.assertEqual({"action": "say", "message": "hello"}, worker.decision())
        path, auth, body = self.requests[0]
        self.assertEqual("/v1/chat/completions", path)
        self.assertEqual("Bearer private-test-credential", auth)
        self.assertTrue(body["stream"])
        self.assertEqual(SCHEMA, body["response_format"]["json_schema"]["schema"])
        self.assertNotIn("reasoning", worker.text)

    def test_untrusted_endpoint_cannot_bypass_schema(self):
        for mode in ("invalid", "missing"):
            self.mode = mode
            worker = self.model.launch("instructions", "hello", SCHEMA)
            self.assertEqual(1, worker.wait(3))
            self.assertEqual("", worker.text)

    def test_cancelled_reply_cannot_replace_new_request(self):
        self.mode = "stall_first"
        old = self.model.launch("instructions", "old", SCHEMA)
        self.assertTrue(self.entered.wait(3))
        old.terminate()
        old.cleanup()
        new = self.model.launch("instructions", "new", SCHEMA)
        self.assertEqual(0, new.wait(3))
        self.release.set()
        self.assertEqual(-15, old.wait(1))
        self.assertEqual("", old.text)
        self.assertEqual("new", new.decision()["message"])

    def test_http_failure_is_not_replayed_and_does_not_expose_body(self):
        self.mode = "error"
        worker = self.model.launch("instructions", "hello", SCHEMA)
        self.assertEqual(1, worker.wait(3))
        self.assertEqual(1, len(self.requests))
        self.assertEqual("Model endpoint returned HTTP 401", self.model.last_error)
        self.assertNotIn("private-test-credential", str(worker.metrics()) + self.model.last_error + worker.text)

    def test_oversized_context_is_rejected_without_truncation_or_request(self):
        worker = self.model.launch("instructions", "x" * 128000, SCHEMA)
        self.assertEqual(1, worker.wait(3))
        self.assertEqual([], self.requests)

    def test_stream_fragments_never_become_player_decisions(self):
        self.mode = "stream"
        worker = self.model.launch("instructions", "hello", SCHEMA)
        self.assertTrue(self.entered.wait(3))
        self.assertIsNone(worker.poll())
        self.assertEqual("", worker.text)
        self.release.set()
        self.assertEqual(0, worker.wait(3))
        self.assertEqual({"action":"say", "message":"hello"}, worker.decision())
        self.assertIsNotNone(worker.metrics()["firstTextMs"])

    def test_native_text_needs_no_json_envelope_or_extra_model_round(self):
        self.mode = "native_text"
        self.model.native_tools = True
        self.model.chat_template_kwargs = {"enable_thinking":False}
        worker = self.model.launch("instructions", "hello", SCHEMA)
        self.assertEqual(0, worker.wait(3))
        self.assertEqual("你好，我在这里。", worker.decision()["message"])
        payload = self.requests[0][2]
        self.assertEqual("auto", payload["tool_choice"])
        self.assertNotIn("response_format", payload)
        self.assertEqual({"enable_thinking":False}, payload["chat_template_kwargs"])
        self.assertEqual(1, len(self.requests))

    def test_native_tool_fragments_are_assembled_privately_then_validated(self):
        self.mode = "tool_stream"
        self.model.native_tools = True
        worker = self.model.launch("instructions", "hello", SCHEMA)
        self.assertTrue(self.entered.wait(3))
        self.assertEqual("", worker.text)
        self.assertIsNone(worker.poll())
        self.release.set()
        self.assertEqual(0, worker.wait(3))
        self.assertEqual({"action":"say", "message":"hello"}, worker.decision())
        self.assertNotIn("reasoning", worker.text)

    def test_truncated_or_multiple_native_calls_cannot_execute(self):
        self.model.native_tools = True
        self.release.set()
        for mode in ("tool_truncated",):
            self.mode = mode
            worker = self.model.launch("instructions", "hello", SCHEMA)
            self.assertEqual(1, worker.wait(3))
            self.assertEqual("", worker.text)

    def test_multiple_calls_dispatch_only_the_first_complete_action(self):
        self.model.native_tools = True
        self.release.set()
        self.mode = "two_tools"
        worker = self.model.launch("instructions", "hello", SCHEMA)
        self.assertEqual(0, worker.wait(3))
        self.assertEqual({"action":"say", "message":"hello"}, worker.decision())

    def test_named_tool_uses_published_game_schema_and_internal_envelope(self):
        self.model.native_tools = True
        self.model.configure_tools([{"name":"collect", "inputSchema":{"type":"object", "additionalProperties":False,
            "properties":{"resource":{"type":"string"}}, "required":["resource"]}}])
        self.mode = "named"
        worker = self.model.launch("instructions", "collect wood", GAME_SCHEMA)
        self.assertEqual(0, worker.wait(3))
        self.assertEqual("collect", worker.decision()["tool_name"])
        self.assertEqual({"resource":"wood"}, json.loads(worker.decision()["arguments_json"]))
        self.mode = "invalid_named"
        invalid = self.model.launch("instructions", "collect wood", GAME_SCHEMA)
        self.assertEqual(1, invalid.wait(3))
        self.assertEqual("", invalid.text)

    def test_deferred_tool_is_disclosed_before_its_action_returns(self):
        self.model.native_tools = True
        self.model.configure_tools([{"name":"inventory", "inputSchema":{"type":"object", "properties":{}, "additionalProperties":False}}])
        self.mode = "deferred"
        worker = self.model.launch("instructions", "inventory", GAME_SCHEMA)
        self.assertEqual(0, worker.wait(3))
        self.assertEqual("inventory", worker.decision()["tool_name"])
        self.assertEqual(2, self.model.last_request_count)
        self.assertNotIn("inventory", [tool["function"]["name"] for tool in self.requests[0][2]["tools"]])
        self.assertIn("inventory", [tool["function"]["name"] for tool in self.requests[1][2]["tools"]])


if __name__ == "__main__": unittest.main()
