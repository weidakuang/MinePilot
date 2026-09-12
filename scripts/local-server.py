#!/usr/bin/env python3
"""Start an installed local Forge server independently of a temporary tool terminal.

Worlds, configuration, credentials and jars are never replaced by this launcher.
SIGTERM uses Minecraft's native save/shutdown hook. There is no restart loop.
"""
import argparse
import fcntl
import json
import os
from pathlib import Path
import signal
import socket
import stat
import subprocess
import time


def properties(directory):
    values = {}
    for line in (directory / "server.properties").read_text().splitlines():
        if line.strip() and not line.lstrip().startswith("#") and "=" in line:
            key, value = line.split("=", 1)
            values[key.strip()] = value.strip()
    if values.get("server-ip") != "127.0.0.1":
        raise ValueError("This launcher requires server-ip=127.0.0.1")
    return int(values.get("server-port", "25565"))


def alive(record, directory):
    pid = record.get("pid")
    if not isinstance(pid, int) or pid <= 1:
        return False
    result = subprocess.run(["/bin/ps", "-p", str(pid), "-o", "command="], capture_output=True, text=True)
    return result.returncode == 0 and f"-Dminepilot.localServerDirectory={directory}" in result.stdout


def listening(port):
    try:
        with socket.create_connection(("127.0.0.1", port), timeout=.3):
            return True
    except OSError:
        return False


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=["start", "status", "stop"])
    parser.add_argument("--directory", type=Path, required=True)
    parser.add_argument("--java", type=Path)
    parser.add_argument("--mcp-port", type=int, default=25766)
    args = parser.parse_args()
    directory = args.directory.expanduser().resolve()
    port = properties(directory)
    state_path = directory / "local-server-state.json"
    with (directory / "local-server.lock").open("a") as lock:
        fcntl.flock(lock, fcntl.LOCK_EX)
        record = json.loads(state_path.read_text()) if state_path.exists() else {}
        running = alive(record, directory)
        if args.action == "start" and not running:
            if listening(port):
                raise RuntimeError("Port already has a server; refusing to launch a competing world")
            if args.java is None or not args.java.is_file():
                raise ValueError("An existing Java executable is required")
            if not 1 <= args.mcp_port <= 65535:
                raise ValueError("Invalid MCP port")
            shim = directory / "forge-26.2-65.0.9-shim.jar"
            if not shim.is_file():
                raise ValueError("Installed Forge 65.0.9 shim is missing")
            command = [str(args.java.resolve()), "-Xms1G", "-Xmx3G",
                       f"-Dminepilot.localServerDirectory={directory}",
                       "-jar", str(shim), "nogui"]
            env = dict(os.environ, MINEPILOT_MCP_PORT=str(args.mcp_port))
            log = directory / f"server-detached-{time.time_ns()}.log"
            # An open FIFO keeps the dedicated-server console from spinning on
            # EOF when JLine is present. It also permits normal local console input.
            console = directory / "server-console.fifo"
            if not console.exists(): os.mkfifo(console, 0o600)
            if not stat.S_ISFIFO(console.lstat().st_mode):
                raise ValueError("Server console path is not a FIFO")
            console_fd = os.open(console, os.O_RDWR)
            try:
                with log.open("xb") as output:
                    process = subprocess.Popen(command, cwd=directory, env=env,
                                               stdin=console_fd, stdout=output,
                                               stderr=subprocess.STDOUT, start_new_session=True)
            finally:
                os.close(console_fd)
            record = {"pid": process.pid, "port": port, "mcpPort": args.mcp_port,
                      "startedAt": time.time(), "log": str(log), "directory": str(directory)}
            temp = state_path.with_suffix(".tmp")
            temp.write_text(json.dumps(record, indent=2) + "\n")
            temp.chmod(0o600)
            temp.replace(state_path)
            running = True
        elif args.action == "stop" and running:
            os.kill(record["pid"], signal.SIGTERM)
            print(json.dumps({**record, "status": "STOP_REQUESTED"}))
            return
        print(json.dumps({**record, "status": "LISTENING" if running and listening(port)
                          else "STARTING" if running else "STOPPED"}))


if __name__ == "__main__":
    main()
