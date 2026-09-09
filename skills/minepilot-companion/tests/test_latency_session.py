import json
import unittest
from pathlib import Path
from test_session import Client, Worker
from test_collection_session import CollectionClient
from companion_session import SessionLoop, cancel_body_work, observation_for_event
from codex_transport import PersistentModel, ModelWorker
import codex_decisions
import minepilot


class LatencyTests(unittest.TestCase):
    def test_navigation_completion_keeps_the_collection_request(self):
        from unittest.mock import patch
        game=Client();events=[];workers=[]
        def launch(event,seq):
            events.append(event);w=Worker();workers.append(w);return w
        loop=SessionLoop(game,launch,'Fell the spruce tree and gather every log')
        loop.tick();workers[-1].returncode=0
        workers[-1].decision=lambda:{'action':'navigate','message':'I will approach','pace':'auto'}
        def submitted(client,decision):
            client.state={'requestId':'approach-1','phase':'EXECUTING'}
        with patch.object(codex_decisions,'apply',side_effect=submitted):loop.tick()
        game.state={'requestId':'approach-1','phase':'COMPLETED'};loop.tick()
        self.assertEqual('navigation_event',events[-1]['type'])
        self.assertEqual('Fell the spruce tree and gather every log',events[-1]['request']['message'])
        self.assertIn('tool',codex_decisions.event_schema(events[-1])['properties']['action']['enum'])

    def test_arrival_allows_continuing_an_original_collection_goal(self):
        root={'type':'initial_request','message':'Fell the spruce tree and collect its logs'}
        event={'type':'navigation_event','state':{'phase':'COMPLETED'},'request':root}
        schema=codex_decisions.event_schema(event)
        self.assertIn('tool',schema['properties']['action']['enum'])
        self.assertIn('plan_collection',schema['properties']['tool_name']['enum'])
        self.assertNotIn('tool',codex_decisions.event_schema({'type':'navigation_event','state':{'phase':'COMPLETED'}})['properties']['action']['enum'])
        self.assertNotIn('navigate',codex_decisions.event_schema({**event,'state':{'phase':'FAILED'}})['properties']['action']['enum'])

    def test_sparse_search_advances_empty_pages_without_model_round_trips(self):
        class Search:
            def __init__(self): self.calls=[]
            def call_tool(self,name,args):
                self.calls.append((name,args))
                if len(self.calls)==1: return {'complete':False,'cursor':'scan-1','results':[]}
                return {'complete':True,'cursor':'scan-1','results':[{'block':'minecraft:bell'}]}
        game=Search()
        result=codex_decisions.apply(game,{'action':'tool','tool_name':'sense','arguments_json':'{"kind":"blocks","radius":96}','message':''})
        self.assertEqual(2,len(game.calls))
        self.assertEqual('scan-1',game.calls[1][1]['cursor'])
        self.assertTrue(result['result']['complete'])
        self.assertEqual('minecraft:bell',result['result']['results'][0]['block'])

    def test_structure_scan_keeps_native_filter_and_radius_when_continuing(self):
        class Search:
            def __init__(self): self.calls=[]
            def call_tool(self,name,args):
                self.calls.append((name,args))
                if len(self.calls)==1:
                    return {'complete':False,'cursor':'structure-1','results':[],
                            'source':'server_structure_records','radius':96,'filter':'#minecraft:village'}
                return {'complete':True,'coverageComplete':True,'cursor':'structure-1','totalMatched':1,
                        'results':[{'structure':'minecraft:village_plains','distance':32,'safeToStandVerified':False}]}
        game=Search()
        result=codex_decisions.apply(game,{'action':'tool','tool_name':'sense','arguments_json':'{"kind":"structures","filter":"村庄"}','message':''})
        self.assertEqual(2,len(game.calls));continued=game.calls[1][1]
        self.assertEqual(96,continued['radius']);self.assertEqual('#minecraft:village',continued['filter'])
        self.assertEqual('structure-1',continued['cursor'])
        self.assertFalse(result['result']['results'][0]['safeToStandVerified'])

    def test_fresh_chat_does_not_inherit_prior_success_but_keeps_active_work(self):
        observation={'collection':{'phase':'COMPLETED','requestId':'old'},'mining':{'phase':'COMPLETED'},
            'navigation':{'phase':'EXECUTING','requestId':'active'},'inventory':[]}
        current=observation_for_event(observation,{'type':'player_chat'})
        self.assertNotIn('collection',current);self.assertNotIn('mining',current)
        self.assertEqual('active',current['navigation']['requestId'])
        self.assertIn('collection_status',current['historicalJobStatusTools'])
        self.assertEqual([],current['inventory'])
        terminal=observation_for_event(observation,{'type':'collection_event'})
        self.assertEqual('old',terminal['collection']['requestId'])

    def test_planning_message_is_delivered_only_after_tool_success(self):
        client = Client()
        value = {'action':'tool','tool_name':'plan_collection','arguments_json':'{}','message':'好，我来收集木头。'}
        codex_decisions.apply(client, value)
        self.assertEqual(['plan_collection','say'], [n for n,_ in client.calls])
        class Reject(Client):
            def call_tool(self, name, args):
                self.calls.append((name,args))
                raise minepilot.ToolError('Source is unavailable')
        rejected = Reject()
        with self.assertRaises(minepilot.ToolError): codex_decisions.apply(rejected, value)
        self.assertEqual(['plan_collection'], [n for n,_ in rejected.calls])

    def test_collection_requires_model_approval_then_executes_without_extra_turn(self):
        class Game(CollectionClient):
            def call_tool(self, name, args):
                if name in {'plan_collection','choose_collection'}:
                    self.calls.append((name,args))
                    self.collection = {'requestId':'tree','phase':'PLAN_READY' if name == 'plan_collection' else 'EXECUTING', 'ownsBody':name=='choose_collection', 'options':[{'optionId':'A'}]}
                    return dict(self.collection)
                return super().call_tool(name,args)
        game = Game(); events = []; workers = []
        def launch(event, seq):
            events.append(event); worker=Worker();workers.append(worker);return worker
        loop = SessionLoop(game,launch,'Collect three logs')
        loop.tick()
        workers[-1].returncode=0
        workers[-1].decision=lambda:{'action':'tool','tool_name':'plan_collection','arguments_json':'{}','message':'我来收集。'}
        loop.tick();loop.tick()
        self.assertEqual(2,len(workers))
        self.assertFalse(any(n=='choose_collection' for n,_ in game.calls))
        self.assertEqual(['approve','reject'],codex_decisions.event_schema(events[-1])['properties']['action']['enum'])
        workers[-1].returncode=0
        workers[-1].decision=lambda:{'action':'approve','option_id':'A','message':''}
        loop.tick();loop.tick()
        self.assertEqual(2,len(workers));self.assertIsNone(loop.worker)
        self.assertEqual('initial_request',loop.collection_origin_request['type'])
        self.assertEqual('我来收集。',loop.dialogue[-1]['text'])
        game.messages=[{'sequence':1,'text':'今天怎么样？'}];loop.tick()
        self.assertEqual('player_chat',events[-1]['type'])
        self.assertEqual('EXECUTING',game.collection['phase'])

    def test_approval_cannot_invent_options_or_change_request_identity(self):
        event={'type':'tool_result','tool':'plan_collection','result':{'phase':'PLAN_READY','requestId':'owned','options':[{'optionId':'A'}]}}
        decision={'action':'approve','option_id':'A','request_id':'foreign'}
        bound=codex_decisions.bind_plan_decision(decision,event)
        self.assertEqual({'request_id':'owned','option_id':'A'},json.loads(bound['arguments_json']))
        for changed,context in [({**decision,'option_id':'invented'},event),(decision,{'type':'player_chat'})]:
            with self.assertRaises(ValueError):codex_decisions.bind_plan_decision(changed,context)

    def test_prepare_does_not_infer_and_first_request_reuses_connection(self):
        model = PersistentModel('unused',Path('/tmp'),'gpt-5.6-luna')
        model._start=lambda:None
        calls=[]
        def rpc(name,args):
            calls.append(name)
            return {'thread':{'id':'warm'}} if name=='thread/start' else {'turn':{'id':'first'}}
        model.rpc=rpc
        model._prepare('instructions')
        self.assertEqual(['thread/start'],calls)
        self.assertTrue(model.ready.is_set());self.assertIsNone(model.worker)
        worker=ModelWorker(model);model._begin(worker,'instructions','real player chat',{})
        self.assertEqual(['thread/start','turn/start'],calls)
        model.close()
        model._prepare('instructions')
        self.assertEqual(2,len(calls));self.assertFalse(model.ready.is_set())

    def test_session_stop_cancels_parent_before_children(self):
        class Game(Client):
            def call_tool(self,name,args):
                self.calls.append((name,args))
                return {'requestId':name,'phase':'EXECUTING'} if name.endswith('_status') else {}
        game=Game();cancel_body_work(game)
        self.assertEqual(['placement_status','cancel_placement','collection_status','cancel_collection','mining_status','cancel_mining','navigation_status','cancel_navigation'],[n for n,_ in game.calls])

if __name__ == '__main__': unittest.main()
