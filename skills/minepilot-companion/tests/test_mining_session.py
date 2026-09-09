import unittest
from test_session import Client, Worker
from companion_session import SessionLoop
import codex_decisions

class MiningClient(Client):
    def __init__(self):
        super().__init__()
        self.mining={'phase':'IDLE'}
    def poll_events(self, chat, system, inventory):
        return {'chat':{'messages':[m for m in self.messages if m['sequence']>chat]},
                'inventoryEvents':{'events':[]},'navigation':self.state,'mining':dict(self.mining)}

class MiningSessionTests(unittest.TestCase):
    def setUp(self):
        self.client=MiningClient();self.events=[];self.workers=[]
        def launch(event,sequence):
            self.events.append(event);worker=Worker();self.workers.append(worker);return worker
        self.loop=SessionLoop(self.client,launch)
    def test_completion_delivered_once_without_repeating_break(self):
        self.client.mining={'phase':'EXECUTING','requestId':'mine-one'};self.loop.tick()
        self.assertFalse(self.events)
        self.client.mining['phase']='COMPLETED';self.loop.tick()
        self.assertEqual('mining_event',self.events[-1]['type'])
        self.workers[-1].returncode=0;self.workers[-1].decision=lambda:{'action':'wait'}
        self.loop.tick();self.loop.tick()
        self.assertEqual(1,len(self.events));self.assertFalse(any(n=='choose_mining' for n,_ in self.client.calls))
    def test_locally_handled_stop_discards_old_decision_and_duplicate_terminal(self):
        self.client.mining={'phase':'EXECUTING','requestId':'mine-one'}
        self.client.messages=[{'sequence':1,'text':'聊聊天'}];self.loop.tick()
        old=self.workers[-1];old.returncode=0;old.decision=lambda:self.fail('Stale mining decision read')
        self.client.messages.append({'sequence':2,'text':'停下','handledLocally':True})
        self.client.mining['phase']='CANCELLED';self.loop.tick();self.loop.tick()
        self.assertEqual(1,len(self.events));self.assertFalse(any(n=='say' for n,_ in self.client.calls))
    def test_body_fast_command_does_not_run_over_active_mining(self):
        self.client.mining={'phase':'EXECUTING','requestId':'mine-one'}
        self.client.messages=[{'sequence':1,'text':'跳一下'}];self.loop.tick()
        self.assertEqual('player_chat',self.events[-1]['type'])
        self.assertFalse(any(n=='jump_once' for n,_ in self.client.calls))
    def test_tool_execution_preserves_exact_plan_identity(self):
        decision={'action':'tool','tool_name':'choose_mining','arguments_json':'{"request_id":"one","option_id":"held-tool"}'}
        codex_decisions.apply(self.client,decision)
        self.assertEqual(('choose_mining',{'request_id':'one','option_id':'held-tool'}),self.client.calls[-1])
