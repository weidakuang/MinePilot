import unittest
from test_session import Client, Worker
from companion_session import SessionLoop
import codex_decisions

class ExcavationClient(Client):
    def __init__(self):
        super().__init__();self.excavation={'phase':'IDLE'};self.mining={'phase':'IDLE'};self.placement={'phase':'IDLE'};self.survey={'phase':'IDLE'}
    def poll_events(self,chat,system,inventory):
        return {'chat':{'messages':[m for m in self.messages if m['sequence']>chat]},'inventoryEvents':{'events':[]},'navigation':self.state,'excavation':dict(self.excavation),'mining':dict(self.mining),'placement':dict(self.placement),'miningSurvey':dict(self.survey)}

class ExcavationSessionTests(unittest.TestCase):
    def setUp(self):
        self.c=ExcavationClient();self.events=[];self.workers=[]
        def launch(event,sequence):
            self.events.append(event);w=Worker();self.workers.append(w);return w
        self.loop=SessionLoop(self.c,launch)
    def test_async_plan_event_and_terminal_each_delivered_once(self):
        self.c.excavation={'phase':'PLANNING','requestId':'job','ownsBody':True};self.loop.tick();self.assertFalse(self.events)
        self.c.excavation.update(phase='PLAN_READY',ownsBody=False);self.loop.tick();self.assertEqual('excavation_event',self.events[-1]['type'])
        self.workers[-1].returncode=0;self.workers[-1].decision=lambda:{'action':'wait'};self.loop.tick();self.loop.tick();self.assertEqual(1,len(self.events))
        self.c.excavation.update(phase='COMPLETED');self.loop.tick();self.assertEqual(2,len(self.events))
    def test_children_quiet_and_chat_during_excavation(self):
        self.c.excavation={'phase':'EXECUTING','requestId':'job','ownsBody':True,'childMiningRequestIds':['dig'],'childPlacementRequestIds':['support']}
        self.c.mining={'phase':'COMPLETED','requestId':'dig'};self.c.placement={'phase':'COMPLETED','requestId':'support'};self.loop.tick();self.assertFalse(self.events)
        self.c.messages=[{'sequence':1,'text':'跳一下'}];self.loop.tick();self.assertEqual('player_chat',self.events[-1]['type']);self.assertFalse(any(n=='jump_once' for n,_ in self.c.calls))
    def test_exact_scope_passes_to_public_tool(self):
        import json
        args={'mode':'region','relative':True,'from':{'x':1,'y':0,'z':0},'to':{'x':3,'y':1,'z':0}}
        codex_decisions.apply(self.c,{'action':'tool','tool_name':'plan_excavation','arguments_json':json.dumps(args)})
        self.assertEqual(('plan_excavation',args),self.c.calls[-1])

    def test_survey_async_completion_does_not_end_listener(self):
        self.c.survey={'phase':'CAPTURING','requestId':'survey'};self.loop.tick();self.assertFalse(self.events)
        self.c.survey['phase']='COMPLETED';self.loop.tick();self.assertEqual('mining_survey_event',self.events[-1]['type'])
        self.workers[-1].returncode=0;self.workers[-1].decision=lambda:{'action':'wait'};self.loop.tick();self.loop.tick();self.assertEqual(1,len(self.events))
        self.c.messages=[{'sequence':1,'text':'接下来呢'}];self.loop.tick();self.assertEqual('player_chat',self.events[-1]['type'])

    def test_sense_empty_entity_page_preserves_cursor_offset(self):
        class Pages(Client):
            def call_tool(self,name,args):
                super().call_tool(name,args)
                if len(self.calls)==1:return {'complete':False,'results':[],'cursor':'page','nextOffset':4}
                return {'complete':True,'results':[{'type':'minecraft:villager'}]}
        client=Pages();codex_decisions.apply(client,{'action':'tool','tool_name':'sense','arguments_json':'{"kind":"entities","offset":4,"radius":16}'})
        self.assertEqual({'kind':'entities','offset':4,'radius':16,'cursor':'page'},client.calls[-1][1])

    def test_background_search_waits_without_model_and_yields_to_chat(self):
        self.loop.pending_scan={'args':{'kind':'blocks','cursor':'slow'},'request':{'message':'找矿'},'started':__import__('time').monotonic(),'pages':0}
        original=self.c.call_tool
        def slow(name,args):
            if name=='sense':return {'complete':False,'results':[],'cursor':'slow'}
            return original(name,args)
        self.c.call_tool=slow
        self.loop.tick();self.assertFalse(self.events);self.assertIsNotNone(self.loop.pending_scan)
        self.c.messages=[{'sequence':1,'text':'先别搜了'}];self.loop.tick();self.assertIsNone(self.loop.pending_scan);self.assertEqual('player_chat',self.events[-1]['type'])

    def test_background_search_delivers_one_actual_result(self):
        self.loop.pending_scan={'args':{'kind':'blocks','cursor':'slow'},'request':{'message':'找矿'},'started':__import__('time').monotonic(),'pages':0}
        original=self.c.call_tool
        self.c.call_tool=lambda name,args: {'complete':True,'results':[{'block':'minecraft:coal_ore'}]} if name=='sense' else original(name,args)
        self.loop.tick();self.assertIsNone(self.loop.pending_scan);self.assertEqual('tool_result',self.events[-1]['type']);self.assertEqual('找矿',self.events[-1]['request']['message'])
