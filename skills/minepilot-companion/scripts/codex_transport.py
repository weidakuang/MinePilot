"""Persistent local Codex model transport; game actions stay in the MCP host."""
import json
import queue
import subprocess
import threading
import time


class ModelWorker:
    def __init__(self, transport):
        self.transport = transport
        self.returncode = None
        self.text = ''
        self.thread_id = None
        self.turn_id = None
        self.done = threading.Event()
        self.cancelled = False
        self.created_at=time.monotonic()
        self.submitted_at=None
        self.first_text_at=None
        self.finished_at=None

    def poll(self): return self.returncode
    def finish(self, code):
        if not self.done.is_set():
            self.finished_at=time.monotonic()
            self.returncode = code
            self.done.set()
    def metrics(self):
        return {"totalMs":round(((self.finished_at or time.monotonic())-self.created_at)*1000),
                "setupMs":round((self.submitted_at-self.created_at)*1000) if self.submitted_at is not None else None,
                "modelMs":round((self.finished_at-self.submitted_at)*1000) if self.finished_at is not None and self.submitted_at is not None else None,
                "firstTextMs":round((self.first_text_at-self.submitted_at)*1000) if self.first_text_at is not None and self.submitted_at is not None else None,
                "outputCharacters":len(self.text),"cancelled":self.cancelled}
    def decision(self):
        if len(self.text) > 65536: raise ValueError('Decision exceeded size limit')
        value = json.loads(self.text)
        if not isinstance(value, dict) or value.get('action') not in {'say', 'navigate', 'choose', 'cancel', 'plan', 'jump', 'wait', 'tool', 'organize'}:
            raise ValueError('Invalid decision')
        return value
    def terminate(self):
        self.cancelled = True
        self.finish(-15)
        if self.thread_id and self.turn_id:
            threading.Thread(target=self.transport.interrupt, args=(self,), daemon=True).start()
    def kill(self): self.transport.close()
    def wait(self, timeout=None):
        if not self.done.wait(timeout): raise subprocess.TimeoutExpired('Codex decision', timeout)
        return self.returncode
    def cleanup(self): self.text = ''


class PersistentModel:
    def __init__(self, executable, workspace, model):
        self.executable, self.workspace, self.model = executable, workspace, model
        self.process = None
        self.sequence = 0
        self.pending = {}
        self.write_lock = threading.Lock()
        self.setup_lock = threading.Lock()
        self.worker = None
        self.thread_id = None
        self.turns = 0

    def launch(self, instructions, event_text, schema):
        worker = ModelWorker(self)
        threading.Thread(target=self._begin, args=(worker, instructions, event_text, schema), daemon=True).start()
        return worker

    def _start(self):
        if self.process is not None and self.process.poll() is None: return
        command = [self.executable, 'app-server', '--stdio']
        for feature in ['shell_tool', 'apps', 'browser_use', 'computer_use', 'skill_search']:
            command += ['--disable', feature]
        command += ['-c', 'web_search="disabled"']
        self.process = subprocess.Popen(command, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                                        stderr=subprocess.DEVNULL, text=True, bufsize=1)
        self.thread_id = None
        threading.Thread(target=self._read, args=(self.process,), daemon=True).start()
        self.rpc('initialize', {'clientInfo': {'name': 'minepilot-companion', 'version': '0.2.0'},
                                'capabilities': {'experimentalApi': True}})
        self.send({'method': 'initialized', 'params': {}})

    def _begin(self, worker, instructions, event_text, schema):
        try:
            with self.setup_lock:
                self._start()
                if worker.cancelled:
                    worker.finish(-15); return
                # Bound retained route snapshots while retaining warm conversation turns.
                if self.thread_id is None or self.turns >= 16 or self.worker is not None and self.worker.cancelled:
                    response = self.rpc('thread/start', {'model': self.model, 'ephemeral': True,
                        'cwd': str(self.workspace), 'sandbox': 'read-only', 'approvalPolicy': 'never',
                        'baseInstructions': instructions,
                        'config': {'model_reasoning_effort': 'low', 'web_search': 'disabled'}})
                    self.thread_id = response['thread']['id']; self.turns = 0
                if worker.cancelled:
                    worker.finish(-15); return
                self.worker = worker
                worker.thread_id = self.thread_id
                worker.submitted_at=time.monotonic()
                response = self.rpc('turn/start', {'threadId': self.thread_id,
                    'input': [{'type': 'text', 'text': event_text}], 'effort': 'low', 'outputSchema': schema})
                worker.turn_id = response['turn']['id']; self.turns += 1
                if worker.cancelled: self.interrupt(worker)
        except (OSError, ValueError, KeyError, RuntimeError, queue.Empty):
            worker.finish(1)

    def send(self, value):
        with self.write_lock:
            self.process.stdin.write(json.dumps(value, ensure_ascii=False) + '\n')
            self.process.stdin.flush()

    def rpc(self, method, params):
        with self.write_lock:
            self.sequence += 1; request = self.sequence
            inbox = queue.Queue(); self.pending[request] = inbox
            self.process.stdin.write(json.dumps({'id': request, 'method': method, 'params': params}) + '\n')
            self.process.stdin.flush()
        try:
            response = inbox.get(timeout=20)
            if 'error' in response: raise RuntimeError('Codex request failed: ' + method)
            return response['result']
        finally:
            self.pending.pop(request, None)

    def _read(self, process):
        try:
            for line in process.stdout:
                if self.process is not process: break
                message = json.loads(line)
                if 'id' in message and ('result' in message or 'error' in message):
                    inbox = self.pending.get(message['id'])
                    if inbox is not None: inbox.put(message)
                elif 'id' in message:
                    # No approval or external tool execution is delegated to the model.
                    self.send({'id': message['id'], 'error': {'code': -32601, 'message': 'Tools are unavailable in this model-only session'}})
                else:
                    worker = self.worker
                    params = message.get('params', {})
                    if worker is None or params.get('threadId') != worker.thread_id: continue
                    if message.get('method') == 'turn/started' and worker.turn_id is None:
                        worker.turn_id = params['turn']['id']
                    elif message.get('method') == 'item/agentMessage/delta':
                        if params.get('turnId')==worker.turn_id and worker.first_text_at is None: worker.first_text_at=time.monotonic()
                    elif message.get('method') == 'item/completed':
                        if params.get('turnId') != worker.turn_id: continue
                        item = params.get('item', {})
                        if item.get('type') == 'agentMessage': worker.text = item.get('text', '')
                    elif message.get('method') == 'turn/completed':
                        if worker.turn_id is None or params['turn']['id'] == worker.turn_id:
                            worker.finish(0 if params['turn']['status'] == 'completed' else -15 if worker.cancelled else 1)
        except (OSError, ValueError):
            pass
        finally:
            if self.process is process:
                if self.worker is not None: self.worker.finish(1)
                for inbox in list(self.pending.values()): inbox.put({'error': 'Disconnected'})

    def interrupt(self, worker):
        try: self.rpc('turn/interrupt', {'threadId': worker.thread_id, 'turnId': worker.turn_id})
        except (OSError, RuntimeError, queue.Empty): worker.finish(-15)

    def close(self):
        if self.process is not None and self.process.poll() is None:
            self.process.terminate()
            try: self.process.wait(timeout=3)
            except subprocess.TimeoutExpired: self.process.kill()
        if self.worker is not None: self.worker.finish(-15)
        self.thread_id = None
