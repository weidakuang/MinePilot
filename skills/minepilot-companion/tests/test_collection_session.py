import unittest
from test_session import Client, Worker
from companion_session import SessionLoop
import codex_decisions

class CollectionClient(Client):
    def __init__(self):
        super().__init__(); self.collection={'phase':'IDLE'};self.mining={'phase':'IDLE'}
    def poll_events(self,chat,system,inventory):
        return {'chat':{'messages':[m for m in self.messages if m['sequence']>chat]},
                'inventoryEvents':{'events':[]},'navigation':self.state,
                'mining':dict(self.mining),'collection':dict(self.collection)}

class CollectionSessionTests(unittest.TestCase):
    def setUp(self):
        self.client=CollectionClient();self.events=[];self.workers=[]
        def launch(event,sequence):
            self.events.append(event);w=Worker();self.workers.append(w);return w
        self.loop=SessionLoop(self.client,launch)
    def test_child_breaks_do_not_wake_model_and_parent_result_delivered_once(self):
        self.client.collection={'phase':'EXECUTING','requestId':'tree','ownsBody':True,'childMiningRequestIds':['a','b']}
        for request in ['a','b']:
            self.client.mining={'phase':'COMPLETED','requestId':request};self.loop.tick()
        self.assertFalse(self.events)
        self.client.collection.update(phase='COMPLETED',ownsBody=False);self.loop.tick()
        self.assertEqual('collection_event',self.events[-1]['type'])
        self.workers[-1].returncode=0;self.workers[-1].decision=lambda:{'action':'wait'}
        self.loop.tick();self.loop.tick();self.assertEqual(1,len(self.events))
    def test_player_chat_is_processed_during_job_and_fast_jump_cannot_steal_body(self):
        self.client.collection={'phase':'EXECUTING','requestId':'tree','ownsBody':True}
        self.client.messages=[{'sequence':1,'text':'跳一下'}];self.loop.tick()
        self.assertEqual('player_chat',self.events[-1]['type'])
        self.assertFalse(any(n=='jump_once' for n,_ in self.client.calls))
    def test_stop_discards_pending_approval_and_duplicate_result(self):
        self.client.messages=[{'sequence':1,'text':'采集木头'}];self.loop.tick()
        old=self.workers[-1];old.returncode=0;old.decision=lambda:self.fail('Obsolete decision executed')
        self.client.collection={'phase':'CANCELLED','requestId':'tree','ownsBody':False}
        self.client.messages.append({'sequence':2,'text':'停下','handledLocally':True});self.loop.tick();self.loop.tick()
        self.assertEqual(1,len(self.events))
    def test_source_restriction_and_tree_coordinates_preserved(self):
        import json
        args={'resource':'wood','source':'tree','species':'birch','tree_x':3,'tree_y':64,'tree_z':7,'count':4}
        codex_decisions.apply(self.client,{'action':'tool','tool_name':'plan_collection','arguments_json':json.dumps(args)})
        self.assertEqual(('plan_collection',args),self.client.calls[-1])
