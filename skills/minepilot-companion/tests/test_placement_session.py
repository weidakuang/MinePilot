import json
import unittest
from test_collection_session import CollectionClient
from test_session import Worker
from companion_session import SessionLoop, observation_for_event
import codex_decisions

class PlacementClient(CollectionClient):
    def __init__(self):
        super().__init__(); self.placement={'phase':'IDLE'}
    def poll_events(self,*args):
        result=super().poll_events(*args);result['placement']=dict(self.placement);return result

class PlacementSessionTests(unittest.TestCase):
    def setUp(self):
        self.client=PlacementClient();self.events=[];self.workers=[]
        def launch(event,sequence):
            self.events.append(event);w=Worker();self.workers.append(w);return w
        self.loop=SessionLoop(self.client,launch)
    def test_child_break_silent_and_distinct_blockages_delivered(self):
        self.client.placement={'phase':'EXECUTING','requestId':'build','ownsBody':True,'childMiningRequestIds':['child']}
        self.client.mining={'phase':'COMPLETED','requestId':'child'};self.loop.tick();self.assertFalse(self.events)
        for revision in ['a','b']:
            self.client.placement.update(phase='BLOCKED',ownsBody=False,decisionId=revision);self.loop.tick()
            self.assertEqual('placement_event',self.events[-1]['type'])
            self.workers[-1].returncode=0;self.workers[-1].decision=lambda:{'action':'wait'};self.loop.tick()
        self.assertEqual(2,len(self.events))
    def test_chat_during_execution_not_fast_jump(self):
        self.client.placement={'phase':'EXECUTING','requestId':'build','ownsBody':True}
        self.client.messages=[{'sequence':1,'text':'跳一下'}];self.loop.tick()
        self.assertEqual('player_chat',self.events[-1]['type']);self.assertFalse(any(n=='jump_once' for n,a in self.client.calls))
    def test_plan_approval_binds_request_and_cost_option(self):
        event={'type':'tool_result','tool':'plan_placement','result':{'phase':'PLAN_READY','requestId':'exact','options':[{'optionId':'bounded-placement'}]}}
        result=codex_decisions.bind_plan_decision({'action':'approve','option_id':'bounded-placement'},event)
        self.assertEqual('choose_placement',result['tool_name']);self.assertEqual('exact',json.loads(result['arguments_json'])['request_id'])
        with self.assertRaises(ValueError):codex_decisions.bind_plan_decision({'action':'approve','option_id':'invented'},event)
    def test_batch_is_one_tool_call_and_preserves_orientation(self):
        cells=[{'x':i,'y':65,'z':0,'item':'minecraft:oak_log','state':{'axis':'x'}} for i in range(64)]
        codex_decisions.apply(self.client,{'action':'tool','tool_name':'plan_placement','arguments_json':json.dumps({'targets':cells})})
        self.assertEqual(('plan_placement',{'targets':cells}),self.client.calls[-1])
    def test_stop_discards_placement_approval(self):
        self.client.messages=[{'sequence':1,'text':'铺路'}];self.loop.tick();worker=self.workers[-1]
        worker.returncode=0;worker.decision=lambda:self.fail('Stale approval executed')
        self.client.placement={'phase':'CANCELLED','requestId':'build'}
        self.client.messages.append({'sequence':2,'text':'停下','handledLocally':True});self.loop.tick();self.loop.tick()
        self.assertEqual(1,len(self.events))
