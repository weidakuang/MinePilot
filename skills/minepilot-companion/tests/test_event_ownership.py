import json
import unittest
from unittest.mock import patch
from test_session import Client, Worker
from companion_session import SessionLoop, automatic_support_route
import codex_decisions

class OwnershipTests(unittest.TestCase):
    def test_automatic_support_requires_bounded_expendable_manifest(self):
        option={"supportBlocksRequired":6,"supportMaterials":[{"entryId":"stone","count":6,"importance":4}]}
        self.assertTrue(automatic_support_route(option))
        for invalid in ({"supportBlocksRequired":17},{"supportMaterials":[]},
                        {"supportMaterials":[{"entryId":"stone","count":5,"importance":4}]},
                        {"supportMaterials":[{"entryId":"stone","count":6,"importance":2}]}):
            self.assertFalse(automatic_support_route({**option,**invalid}))
        self.assertTrue(automatic_support_route({"supportBlocksRequired":0}))

    def test_drop_retry_keeps_host_generated_key(self):
        game=Client();decision={'action':'tool','tool_name':'drop_items','arguments_json':'{"item":"minecraft:wooden_sword","count":1}'}
        codex_decisions.apply(game,decision);codex_decisions.apply(game,decision)
        drops=[a for n,a in game.calls if n=='drop_items']
        self.assertEqual(2,len(drops));self.assertEqual(drops[0],drops[1]);self.assertTrue(drops[0]['request_key'])
        self.assertEqual([{'item':'minecraft:wooden_sword','count':1}],drops[0]['items'])
        self.assertNotIn('item',drops[0])

    def test_completion_is_not_repeated_by_child_arrival_but_player_can_ask(self):
        loop=SessionLoop(Client(),None)
        request={'type':'player_chat','messages':[{'sequence':7,'text':'Place a workbench'}]}
        loop.reported_requests.add(loop.request_key(request))
        loop.worker_event={'type':'navigation_event','state':{'phase':'COMPLETED'},'request':request}
        self.assertEqual('wait',loop.quiet_inventory_decision({'action':'say','message':'Workbench placed'})['action'])
        loop.worker_event=request
        self.assertEqual('say',loop.quiet_inventory_decision({'action':'say','message':'Workbench placed'})['action'])

    def test_replacement_does_not_replay_cancelled_gather_from_old_poll(self):
        class Game(Client):
            def initialize(self): pass
            def __init__(self):
                super().__init__();self.gather={'requestId':'old','phase':'EXECUTING','ownsBody':True}
            def poll_events(self,*args):
                return {'chat':{'messages':[]},'inventoryEvents':{'events':[]},'navigation':self.state,'gather':dict(self.gather)}
            def call_tool(self,name,args):
                if name=='observe':return {}
                return super().call_tool(name,args)
        game=Game();events=[];loop=SessionLoop(game,lambda e,s:events.append(e) or Worker())
        worker=Worker();worker.returncode=0;worker.decision=lambda:{'action':'tool','tool_name':'place_block','arguments_json':'{"item":"minecraft:crafting_table"}'}
        loop.worker=worker;loop.worker_event={'type':'player_chat','messages':[{'sequence':2,'text':'place table'}]}
        loop.last_gather=('old','EXECUTING')
        def cancel(*a):
            game.gather={'requestId':'old','phase':'CANCELLED','ownsBody':False}
            return {'gather':dict(game.gather)}
        with patch('companion_session.cancel_body_work',side_effect=cancel),patch('codex_decisions.apply',return_value={'tool':'place_block','result':{'requestId':'new','phase':'EXECUTING'}}): loop.tick()
        self.assertEqual(('old','CANCELLED'),loop.last_gather)
        self.assertFalse(any(e['type']=='gather_event' for e in events))
        self.assertIn('old',loop.retired_requests)

    def test_cancelled_event_is_not_a_new_instruction(self):
        game=Client();events=[];loop=SessionLoop(game,lambda e,s:events.append(e) or Worker())
        loop.pending_event={'type':'gather_event','state':{'requestId':'cancelled','phase':'CANCELLED'}}
        loop.tick();self.assertEqual([],events)

    def test_respawn_interrupts_old_work_and_delivers_death_point_once(self):
        class Game(Client):
            def __init__(self): super().__init__();self.life={"sequence":1,"phase":"DEAD","deathPoint":{"x":3,"y":64,"z":5,"dimension":"minecraft:the_nether"}}
            def poll_events(self,*args): return {"chat":{"messages":[]},"inventoryEvents":{"events":[]},"navigation":self.state,"lifecycle":dict(self.life)}
        game=Game();events=[];loop=SessionLoop(game,lambda e,s:events.append(e) or Worker())
        old=Worker();loop.worker=old;loop.pending_event={"type":"gather_event"}
        loop.tick();self.assertTrue(old.terminated);self.assertFalse(events);self.assertIsNone(loop.pending_event)
        game.life.update(sequence=2,phase="RESPAWNED");loop.tick()
        self.assertEqual("respawn_event",events[0]["type"])
        self.assertEqual("minecraft:the_nether",events[0]["state"]["deathPoint"]["dimension"])
        loop.worker.returncode=0;loop.tick();loop.tick();self.assertEqual(1,len(events))
