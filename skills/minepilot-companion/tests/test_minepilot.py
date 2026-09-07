from __future__ import annotations

import importlib.util
import json
import os
import pathlib
import tempfile
import threading
import unittest
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from unittest import mock


SCRIPT = pathlib.Path(__file__).parents[1] / "scripts" / "minepilot.py"
SPEC = importlib.util.spec_from_file_location("minepilot_skill_client", SCRIPT)
assert SPEC is not None and SPEC.loader is not None
minepilot = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(minepilot)


class MinePilotClientTest(unittest.TestCase):
    def test_bearer_is_not_forwarded_through_redirect(self) -> None:
        requests = {"original": 0, "redirected": 0}

        class Handler(BaseHTTPRequestHandler):
            def do_POST(self) -> None:
                requests["original"] += 1
                self.send_response(302)
                self.send_header(
                    "Location",
                    f"http://127.0.0.1:{self.server.server_port}/redirected",
                )
                self.end_headers()

            def do_GET(self) -> None:
                requests["redirected"] += 1
                self.send_response(200)
                self.end_headers()

            def log_message(self, *_args: object) -> None:
                pass

        server = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        try:
            client = minepilot.McpClient(
                f"http://127.0.0.1:{server.server_port}/mcp", "TEST_SECRET"
            )
            with self.assertRaisesRegex(
                minepilot.ClientError, "redirects are forbidden"
            ):
                client.post({"jsonrpc": "2.0", "id": 1, "method": "ping"})
            self.assertEqual({"original": 1, "redirected": 0}, requests)
        finally:
            server.shutdown()
            server.server_close()

    def test_configure_writes_only_endpoint_and_token_path(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            profile = root / "profile.json"
            token = root / "runtime.token"
            arguments = minepilot.parser().parse_args(
                ["configure", "--token-file", str(token)]
            )
            with mock.patch.dict(
                os.environ, {"MINEPILOT_CODEX_CONFIG": str(profile)}
            ):
                result = minepilot.configure(arguments)
            saved = json.loads(profile.read_text(encoding="utf-8"))
            self.assertTrue(result["configured"])
            self.assertEqual("http://127.0.0.1:25766/mcp", saved["url"])
            self.assertEqual(str(token.resolve()), saved["tokenFile"])
            self.assertNotIn("token", {key.lower() for key in saved})
            if os.name == "posix":
                self.assertEqual(0o600, profile.stat().st_mode & 0o777)


if __name__ == "__main__":
    unittest.main()
