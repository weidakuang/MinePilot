import json
import unittest
from unittest.mock import patch
from test_session import Client, Worker
from companion_session import SessionLoop, observation_for_event
import codex_decisions
import fast_commands

class PlayRepairTests(unittest.TestCase):
    def test_current_state_deduplicates_without_removing_active_job_or_gift_details(self):
        slots=[{'slot':7,'item':'minecraft:emerald','count':5}]
        obs={'x':3,'inventory':slots,'hotbar':slots,
             'navigation':{'x':3,'inventory':slots,'phase':'FOLLOWING','requestId':'live'},
             'collection':{'phase':'IDLE','ownsBody':False,'staleTrace':[1,2]},
             'playerFocus':{'kind':'item','itemStack':{'item':'minecraft:emerald','count':5}}}
        compact=observation_for_event(obs,{'type':'player_chat'})
        self.assertEqual({'phase':'FOLLOWING','requestId':'live'}, compact['navigation'])
        self.assertEqual({'phase':'IDLE','ownsBody':False},compact['collection'])
        self.assertEqual(slots,compact['inventory'])
        self.assertNotIn('hotbar',compact)
        self.assertEqual(obs['playerFocus'],compact['playerFocus'])
        self.assertIn('inventory',obs['navigation'])
        self.assertIn('staleTrace',obs['collection'])

    def test_chat_compacts_lineage_without_losing_items_or_mutating_public_inventory(self):
        entry={'entryId':'wood','count':3,'item':'minecraft:oak_log','slots':[4],
               'origins':{'category':'mixed','allOriginsKnown':False,'lineage':[{'count':3,'source':{'category':'unknown'}}]}}
        observation={'inventorySummary':{'entries':[entry]}}
        compact=observation_for_event(observation,{'type':'player_chat'})['inventorySummary']['entries'][0]
        self.assertEqual(('wood',3,[4]),(compact['entryId'],compact['count'],compact['slots']))
        self.assertNotIn('lineage',compact['origins'])
        self.assertEqual('item_origins',compact['origins']['detailsTool'])
        self.assertIn('lineage',entry['origins'])

    def test_workbench_command_starts_without_a_model_round_trip(self):
        class Game(Client):
            def call_tool(self, name, args):
                if name == 'place_block':
                    self.calls.append((name, args))
                    return {'requestId':'place-one', 'phase':'EXECUTING'}
                return super().call_tool(name, args)
        game=Game()
        loop=SessionLoop(game, lambda *args:self.fail('Unnecessary model turn before placement'))
        game.messages=[{'sequence':1,'text':'放下工作台','player':'TestHuman'}]
        loop.tick()
        self.assertEqual([{'item':'minecraft:crafting_table'}], [a for n,a in game.calls if n=='place_block'])
        self.assertEqual('放下工作台', loop.placement_origin_request['messages'][0]['text'])
        self.assertEqual(('place-one','EXECUTING',None), loop.last_placement)

    def test_fast_placement_does_not_guess_coordinates_or_ignore_negation(self):
        for text in ['别放下工作台','放下工作台吗','放工作台在我指定的地方','放下两个工作台']:
            self.assertIsNone(fast_commands.parse(text), text)

    def test_exact_workbench_reclaim_approves_one_checked_block_without_model(self):
        class Game(Client):
            def call_tool(self,name,args):
                if name=='collect':
                    self.calls.append((name,args));return {'phase':'EXECUTING','requestId':'bench'}
                return super().call_tool(name,args)
        game=Game();loop=SessionLoop(game,lambda *a:self.fail('Reclaim must not wait for a model'))
        game.messages=[{'sequence':1,'text':'把工作台收回来，挖完记得捡上。','player':'TestHuman'}]
        loop.tick()
        self.assertEqual(1, len([a for n,a in game.calls if n=='collect']))
        self.assertFalse(any(n=='choose_collection' for n,a in game.calls))
        self.assertEqual(('bench','EXECUTING'),loop.last_collection)

    def test_yield_chooses_an_observed_side_away_from_player(self):
        class Game(Client):
            def call_tool(self, name, args):
                if name == 'observe':return {'onlinePlayers':[{'name':'TestHuman','x':0,'y':64,'z':2}]}
                if name == 'sense':return {'results':[{'x':0,'y':64,'z':3},{'x':2,'y':64,'z':0}]}
                return super().call_tool(name,args)
        decision=fast_commands.resolve_yield(Game(), fast_commands.parse('让开，我来砍'), 'TestHuman')
        self.assertEqual((2,0), (decision['x'],decision['z']))
        self.assertGreaterEqual(decision['acceptance_radius'], .5)
        self.assertLessEqual(decision['acceptance_radius'], 32)

    def test_silent_pickup_retains_original_intent(self):
        game=Client();workers=[]
        def launch(event,seq):
            w=Worker();workers.append(w);return w
        loop=SessionLoop(game,launch,'把工作台收回来');loop.tick()
        workers[-1].decision=lambda:{'action':'navigate','target_kind':'dropped_item','target_name':'observed-drop','message':''}
        workers[-1].returncode=0
        with patch.object(codex_decisions,'apply') as execute:
            loop.tick()
        self.assertEqual('把工作台收回来',execute.call_args.args[1]['player_intent'])

    def test_completed_short_move_cannot_replay_its_original_chat(self):
        game=Client()
        loop=SessionLoop(game,lambda *args:self.fail('Completed short control must not repeat through a model turn'))
        loop.authorized_request='yield-one'
        loop.navigation_origin_request={'type':'player_chat','directControl':True,'messages':[{'text':'让开，我来砍'}]}
        loop.last_navigation=('yield-one','EXECUTING')
        loop.inventory_review=False
        game.state={'phase':'COMPLETED','requestId':'yield-one'}
        loop.tick()
        loop.tick()
        self.assertEqual(('yield-one','COMPLETED'),loop.last_navigation)

    def test_new_work_releases_follow_but_queries_do_not(self):
        for name,expected in [('plan_collection',True),('sense',False),('drop_items',False)]:
            game=Client();game.state={'phase':'FOLLOWING','requestId':'follow'}
            codex_decisions.apply(game,{'action':'tool','tool_name':name,'arguments_json':'{}','message':''})
            cancelled=[a for n,a in game.calls if n=='cancel_navigation']
            self.assertEqual(expected,bool(cancelled))
            if cancelled:self.assertEqual('follow',cancelled[0]['request_id'])

    def test_chat_gets_recent_gifts_without_old_unrelated_acquisitions(self):
        class Game(Client):
            def initialize(self): pass
            def call_tool(self,name,args):
                if name=='observe':return {'world':{'gameTick':5000},'inventorySummary':{'entries':[]}}
                return super().call_tool(name,args)
        game=Game();events=[];loop=SessionLoop(game,lambda e,s:events.append(e) or Worker())
        loop.recent_acquisitions=[{'sequence':1,'gameTick':100,'acquired':[{'item':'egg','count':1}]},
                                  {'sequence':2,'gameTick':4999,'acquired':[{'item':'emerald','count':3}]}]
        game.messages=[{'sequence':1,'text':'这是什么','player':'TestHuman'}];loop.tick()
        self.assertEqual([2],[e['sequence'] for e in events[-1]['recentAcquisitions']])
