import unittest
from test_session import Worker, Client
from companion_session import SessionLoop, observation_for_event


class BatchClient(Client):
    def __init__(self):
        super().__init__()
        self.batch = {'chat': {'messages': []}, 'inventoryEvents': {'events': []},
                      'navigation': {'phase': 'IDLE'}, 'autonomy': {}}
    def poll_events(self, *args):
        return self.batch


class SurvivalSessionTests(unittest.TestCase):
    def test_current_reference_uses_fresh_same_speaker_hand_without_old_provenance(self):
        focus = [{'speaker':'Alice','ageTicks':0,'heldItem':{'item':'minecraft:emerald','count':3,'origins':{'old':'noise'}}},
                 {'speaker':'Bob','ageTicks':0,'heldItem':{'item':'minecraft:apple'}},
                 {'speaker':'Alice','ageTicks':100,'heldItem':{'item':'minecraft:white_wool'}}]
        compact = observation_for_event({'playerFocus':focus}, {'type':'player_chat','messages':[{'player':'Alice','text':'What is this?'}]})
        refs = compact['currentPlayerReferences']
        self.assertEqual(1,len(refs))
        self.assertEqual({'item':'minecraft:emerald','count':3},refs[0]['heldItem'])
        self.assertIn('origins',focus[0]['heldItem'])

    def test_blocked_fast_reclaim_keeps_request_through_navigation_completion(self):
        from unittest.mock import patch
        import codex_decisions
        class Game(Client):
            def call_tool(self,name,args):
                if name=='collect': return {'requestId':'bench','phase':'BLOCKED','reason':'Outside local radius'}
                return super().call_tool(name,args)
        game=Game();loop,events,workers=self.setup_loop(game)
        game.messages=[{'sequence':1,'text':'收回工作台','player':'Alice'}]
        loop.tick();loop.tick()
        self.assertEqual('tool_result',events[-1]['type'])
        request=events[-1]['request']
        self.assertEqual('收回工作台',request['messages'][0]['text'])
        workers[-1].returncode=0
        workers[-1].decision=lambda:{'action':'navigate','target_kind':'coordinates','x':10,'y':64,'z':0}
        game.state={'requestId':'walk','phase':'PLANNING'}
        with patch.object(codex_decisions,'apply'): loop.tick()
        self.assertEqual(request,loop.navigation_origin_request)
        game.state={'requestId':'walk','phase':'COMPLETED'};loop.tick()
        self.assertEqual(request,events[-1]['request'])
        self.assertIn('tool',codex_decisions.event_schema(events[-1])['properties']['action']['enum'])

    def test_intermediate_actions_are_quiet_but_chat_and_final_results_remain(self):
        loop,_,_=self.setup_loop(BatchClient())
        for event in ['tool_result','gather_event','survival_event','autonomy_event']:
            loop.worker_event={'type':event}
            d=loop.quiet_inventory_decision({'action':'tool','tool_name':'craft','message':'I will do the next step'})
            self.assertEqual('',d['message'])
            self.assertEqual('tool',d['action'])
            self.assertEqual('Done',loop.quiet_inventory_decision({'action':'say','message':'Done','goal_status':'COMPLETED'})['message'])
        loop.worker_event={'type':'player_chat'}
        self.assertEqual('Starting',loop.quiet_inventory_decision({'action':'tool','message':'Starting'})['message'])

    def test_autonomy_standby_is_quiet_but_meaningful_companion_speech_can_pass(self):
        loop,_,_=self.setup_loop(BatchClient());loop.worker_event={"type":"autonomy_event"}
        self.assertEqual("wait",loop.quiet_inventory_decision({"action":"say","message":"Standing by"})["action"])
        self.assertEqual("Look at the sunset",loop.quiet_inventory_decision({"action":"say","message":"Look at the sunset","speech_reason":"direct_player_relevance"})["message"])

    def test_gather_followup_keeps_the_whole_survival_request(self):
        game=BatchClient();loop,events,workers=self.setup_loop(game)
        request={'type':'initial_request','message':'Get stone, make a furnace and collect charcoal'}
        loop.gather_origin_request=request
        game.batch['gather']={'requestId':'g','phase':'COMPLETED'}
        loop.tick();workers[-1].returncode=0
        workers[-1].decision=lambda:{'action':'tool','tool_name':'craft','arguments_json':'{"item":"minecraft:furnace"}'}
        loop.tick()
        self.assertEqual('tool_result',events[-1]['type'])
        self.assertEqual(request,events[-1]['request'])

    def test_casual_chat_preserves_a_pending_action_result(self):
        game=BatchClient();loop,events,workers=self.setup_loop(game)
        pending={'type':'tool_result','tool':'craft','result':{'success':True},'request':{'message':'Make charcoal'}}
        loop.pending_event=pending
        game.batch['chat']['messages']=[{'sequence':1,'text':'How are you?'}]
        loop.tick();self.assertEqual('player_chat',events[-1]['type'])
        game.batch['chat']['messages']=[];workers[-1].returncode=0
        workers[-1].decision=lambda:{'action':'say','message':'Doing well','goal_status':'KEEP'}
        loop.tick();self.assertEqual(pending, {k:events[-1][k] for k in pending})

    def test_missing_material_failure_allows_a_bounded_repair(self):
        import codex_decisions
        game=BatchClient();loop,_,_=self.setup_loop(game)
        loop.last_player_event={'message':'Craft a furnace'}
        for i in range(4):
            loop.recover_failure('Missing cobblestone',{'action':'tool','tool_name':'craft'})
            schema=codex_decisions.event_schema(loop.pending_event)
            self.assertEqual(i<3,'tool' in schema['properties']['action']['enum'])

    def setup_loop(self, game):
        events, workers = [], []
        def launch(event, seq):
            events.append(event)
            worker = Worker()
            workers.append(worker)
            return worker
        return SessionLoop(game, launch, None), events, workers

    def test_blocked_crafting_does_not_broadcast_an_optimistic_success(self):
        import codex_decisions
        class Missing(Client):
            def call_tool(self,name,args):
                self.calls.append((name,args))
                return {'status':'BLOCKED','message':'Need two more cobblestone'} if name=='craft' else {}
        game=Missing()
        codex_decisions.apply(game,{'action':'tool','tool_name':'craft','arguments_json':'{"item":"minecraft:furnace"}','message':'The materials are enough; I made it.'})
        self.assertFalse(any(name=='say' for name,_ in game.calls))

    def test_camp_children_do_not_trigger_extra_model_turns(self):
        game = BatchClient()
        game.batch.update(camp={'requestId': 'camp', 'phase': 'EXECUTING', 'ownsBody': True,
                                'childGatherRequestIds': ['gather'], 'childPlacementRequestIds': ['place']},
                          gather={'requestId': 'gather', 'phase': 'COMPLETED', 'childCollectionRequestIds': ['collect']},
                          collection={'requestId': 'collect', 'phase': 'COMPLETED', 'childMiningRequestIds': ['mine']},
                          mining={'requestId': 'mine', 'phase': 'COMPLETED'},
                          placement={'requestId': 'place', 'phase': 'COMPLETED'})
        loop, events, _ = self.setup_loop(game)
        loop.tick()
        self.assertEqual([], events)
        game.batch['camp'].update(phase='COMPLETED', ownsBody=False)
        loop.tick()
        self.assertEqual('camp_event', events[-1]['type'])

    def test_paused_autonomy_stays_paused_after_local_stop(self):
        game = BatchClient()
        loop, events, _ = self.setup_loop(game)
        game.batch['autonomy'] = {'sequence': 1, 'events': ['hungry'], 'onlinePlayers': 1,
                                  'idle': True, 'paused': True}
        loop.tick()
        self.assertEqual([], events)
        game.batch['autonomy'].update(sequence=2, paused=False)
        loop.tick()
        self.assertEqual('autonomy_event', events[-1]['type'])

    def test_new_chat_has_one_current_goal_and_no_old_receipt_payloads(self):
        memory = {'summary': 'The player prefers riverside camps.'}
        current = observation_for_event({'conversationMemory': memory,
            'camp': {'phase': 'BLOCKED', 'requestId': 'old', 'receipts': ['large history']},
            'gather': {'phase': 'EXECUTING', 'requestId': 'current'}}, {'type': 'player_chat'})
        self.assertEqual({'phase':'BLOCKED'}, current['camp'])
        self.assertEqual('current', current['gather']['requestId'])
        self.assertEqual(memory, current['conversationMemory'])


if __name__ == '__main__':
    unittest.main()
