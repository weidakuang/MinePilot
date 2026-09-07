import sys
from pathlib import Path
import unittest
from unittest.mock import patch
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'scripts'))
import minepilot
import codex_decisions
from companion_session import SessionLoop

class Worker:
    def __init__(self): self.returncode = None; self.terminated = False
    def poll(self): return self.returncode
    def terminate(self): self.terminated = True; self.returncode = -15
    def wait(self, timeout=None): return self.returncode

class Client:
    def __init__(self): self.messages = []; self.state = {'phase':'IDLE'}; self.calls = []
    def call_tool(self, name, args):
        self.calls.append((name,args))
        if name == 'read_chat': return {'messages':[m for m in self.messages if m['sequence'] > args['after_sequence']]}
        if name == 'navigation_status': return dict(self.state)
        return {}

class SessionTests(unittest.TestCase):
    def setUp(self):
        self.client = Client(); self.events = []; self.workers=[]
        def launch(event, sequence):
            self.events.append(event); worker=Worker(); self.workers.append(worker); return worker
        self.loop = SessionLoop(self.client, launch, 'hello')
    def test_completed_turn_does_not_end_listener(self):
        self.loop.tick(); self.workers[0].returncode=0; self.loop.tick()
        self.client.messages=[{'sequence':1,'text':'new destination'}]
        self.loop.tick()
        self.assertEqual(2,len(self.workers)); self.assertEqual('player_chat',self.events[-1]['type'])
        self.loop.tick(); self.assertEqual(2,len(self.workers))
    def test_new_chat_interrupts_old_model_work_without_cancelling_gameplay(self):
        self.loop.tick(); self.client.messages=[{'sequence':1,'text':'stop'}]; self.loop.tick()
        self.assertTrue(self.workers[0].terminated)
        self.assertEqual('player_chat',self.events[-1]['type'])
        self.assertFalse(any(name=='cancel_navigation' for name,_ in self.client.calls))
    def test_terminal_event_during_model_work_is_not_lost(self):
        self.loop.tick(); self.client.state={'requestId':'one','phase':'COMPLETED'}; self.loop.tick()
        self.workers[0].returncode=0; self.loop.tick()
        self.assertEqual('navigation_event',self.events[-1]['type'])
        self.workers[-1].returncode=0; self.loop.tick(); self.assertEqual(2,len(self.workers))

    def test_new_chat_discards_a_completed_stale_decision(self):
        self.loop.tick()
        stale = self.workers[0]
        stale.returncode = 0
        stale.decision = lambda: self.fail('Stale decision must not be read or applied')
        self.client.messages = [{'sequence': 1, 'text': 'stop'}]
        self.loop.tick()
        self.assertEqual('player_chat', self.events[-1]['type'])

    def test_model_launch_failure_does_not_end_listener(self):
        self.loop.launch = lambda *args: (_ for _ in ()).throw(OSError('offline'))
        self.loop.tick()
        self.assertIsNone(self.loop.worker)
        self.assertEqual('say', self.client.calls[-1][0])
        self.client.messages = [{'sequence': 1, 'text': 'hello again'}]
        self.loop.tick()
        self.assertEqual(1, self.loop.last_chat)

    def test_cancel_does_not_replay_stop_or_repeat_acknowledgement(self):
        self.loop.tick()
        self.workers[0].decision = lambda: {'action': 'cancel', 'request_id': 'old', 'message': 'changing destination'}
        self.workers[0].returncode = 0
        self.loop.tick()
        self.assertEqual(1, len(self.events))
        self.assertIsNone(self.loop.last_player_event)
        self.loop.tick()
        self.assertEqual(1, len(self.events))

class TypedDecisionTests(unittest.TestCase):
    def test_missing_dynamic_target_is_rejected_before_any_game_action(self):
        client = Client()
        with self.assertRaises(codex_decisions.InvalidNavigationArguments):
            codex_decisions.apply(client, {'action':'navigate', 'target_kind':'dropped_item', 'target_name':None})
        self.assertEqual([], client.calls)

    def test_argument_repair_preserves_intent_and_stops_after_one_correction(self):
        client = Client(); events = []; workers = []
        def launch(event, sequence):
            events.append(event); worker = Worker(); workers.append(worker); return worker
        loop = SessionLoop(client, launch, 'Find and collect the nearby apples')
        loop.tick()
        for expected in ('repair_invalid_request', 'operation_failed'):
            workers[-1].decision = lambda: {'action':'navigate', 'target_kind':'dropped_item'}
            workers[-1].returncode = 0
            loop.tick()
            self.assertEqual(expected, events[-1]['type'])
        self.assertEqual('Find and collect the nearby apples', events[1]['request']['message'])
        self.assertFalse(any(n == 'request_navigation' for n, _ in client.calls))
        self.assertEqual(['say', 'wait'], codex_decisions.event_schema(events[-1])['properties']['action']['enum'])

    def test_public_physical_failure_does_not_trigger_argument_repair(self):
        client = Client(); workers = []; events = []
        def launch(event, sequence):
            events.append(event); w = Worker(); workers.append(w); return w
        loop = SessionLoop(client, launch, 'collect apples'); loop.tick()
        workers[-1].returncode = 0
        workers[-1].decision = lambda: {'action':'navigate','target_kind':'dropped_item','target_name':'observed-uuid'}
        with patch.object(codex_decisions, 'apply', side_effect=minepilot.ToolError('DROPPED_ITEM_UNAVAILABLE')):
            loop.tick()
        self.assertEqual('operation_failed', events[-1]['type'])
        self.assertEqual(0, loop.argument_repairs)

    def test_navigation_keeps_public_acknowledgement_barrier(self):
        calls = []
        class C:
            def call_tool(self, name, args):
                calls.append((name, args))
                return {'requestId': 'owned'}
        codex_decisions.apply(C(), {'action': 'navigate', 'target_kind': 'coordinates',
                             'x': 1, 'y': 2, 'z': 3, 'message': 'moving'})
        self.assertEqual(['request_navigation', 'say', 'plan_navigation'], [n for n, _ in calls])
        self.assertEqual('owned', calls[1][1]['navigation_request_id'])
        self.assertEqual('owned', calls[2][1]['request_id'])

    def test_replacement_uses_one_validated_request_and_a_new_acknowledgement(self):
        calls=[]
        class C:
            def call_tool(self,name,args):
                calls.append((name,args));return {'requestId':'new'}
        codex_decisions.apply(C(),{'action':'navigate','message':'changing destination','pace':'walk','target_kind':'coordinates',
            'arguments_json':'{"x":1,"y":2,"z":3,"replace_request_id":"observed-old"}'})
        self.assertEqual(['request_navigation','say','plan_navigation'],[n for n,_ in calls])
        self.assertEqual('observed-old',calls[0][1]['replace_request_id'])
        self.assertEqual('new',calls[1][1]['navigation_request_id'])

    def test_failed_acknowledgement_releases_reserved_request(self):
        calls = []
        class C:
            def call_tool(self, name, args):
                calls.append(name)
                if name == 'say': raise minepilot.ClientError('rejected')
                return {'requestId': 'owned', 'phase': 'REQUESTED'}
        with self.assertRaises(minepilot.ClientError):
            codex_decisions.apply(C(), {'action': 'navigate', 'target_kind':'coordinates', 'message': 'moving'})
        self.assertEqual(['request_navigation', 'say', 'navigation_status', 'cancel_navigation'], calls)

class PreparationTests(unittest.TestCase):
    def test_direct_dropped_target_preparation_preserves_identity_and_ack_barrier(self):
        calls=[]
        class C:
            def call_tool(self,name,args):
                calls.append((name,args));return {'requestId':'owned','phase':'PLAN_READY'}
        args=minepilot.parser().parse_args(['prepare','--target-kind','dropped_item','--target-name','observed-uuid','--intent','collect','--message','coming'])
        minepilot.execute(C(),args)
        self.assertEqual(['request_navigation','say','plan_navigation'],[n for n,_ in calls])
        self.assertEqual('observed-uuid',calls[0][1]['target_name'])
        self.assertEqual('owned',calls[1][1]['navigation_request_id'])

    def test_direct_named_target_and_tool_arguments_fail_before_game_calls(self):
        client=Client()
        for command in [
            ['request','--target-kind','waypoint','--intent','home'],
            ['tool','--name','sense','--arguments','[]'],
            ['tool','--name','sense','--arguments','not json'],
            ['tool','--name','sense','--arguments',' '*4097],
        ]:
            with self.assertRaises(minepilot.ClientError):minepilot.execute(client,minepilot.parser().parse_args(command))
        self.assertEqual([],client.calls)

    def test_prepare_preserves_ack_barrier_and_does_not_choose(self):
        calls=[]
        class C:
            def call_tool(self,name,args):
                calls.append(name)
                return {'requestId':'one','phase':'PLAN_READY' if name=='navigation_status' else 'PLANNING'}
        args=minepilot.parser().parse_args(['prepare','--target-kind','coordinates','--x','1','--y','2','--z','3','--intent','walk','--message','hello'])
        with patch.object(minepilot.time,'sleep'): result=minepilot.execute(C(),args)
        self.assertEqual(['request_navigation','say','plan_navigation','navigation_status'],calls)
        self.assertEqual('PLAN_READY',result['phase'])
    def test_wait_returns_on_chat_while_body_still_executes(self):
        c=Client(); c.state={'phase':'EXECUTING'}; c.messages=[{'sequence':1,'text':'change goal'}]
        args=minepilot.parser().parse_args(['wait','--after-chat','0'])
        result=minepilot.execute(c,args)
        self.assertEqual('EXECUTING',result['phase']); self.assertEqual(1,len(result['newChat']['messages']))

if __name__ == '__main__': unittest.main()


class ResponsiveDecisionTests(unittest.TestCase):
    def test_cancel_never_blocks_receiving_new_chat(self):
        c=Client();old=Worker()
        old.wait=lambda **kwargs: self.fail('Listener blocked waiting for old model')
        old.terminate=lambda: setattr(old,'terminated',True)
        launched=[]
        loop=SessionLoop(c,lambda event,seq: launched.append(event) or Worker())
        loop.worker=old
        c.messages=[{'sequence':1,'text':'先别走，听我说'}]
        loop.tick()
        self.assertTrue(old.terminated)
        self.assertEqual('player_chat',launched[0]['type'])
        self.assertFalse(any(n=='request_navigation' for n,_ in c.calls))

    def test_compact_navigation_preserves_target_and_visible_ack(self):
        calls=[]
        class C:
            def call_tool(self,name,args):
                calls.append((name,args));return {'requestId':'one'}
        codex_decisions.apply(C(),{'action':'navigate','message':'我来了','tool_name':None,
            'arguments_json':'{"target_kind":"player","target_name":"human-uuid","continuous_follow":true,"pace":"walk"}'})
        self.assertEqual(['request_navigation','say','plan_navigation'],[n for n,_ in calls])
        self.assertEqual('human-uuid',calls[0][1]['target_name'])
        self.assertTrue(calls[0][1]['continuous_follow'])
        self.assertFalse(calls[0][1]['allow_partial'])

    def test_nested_arguments_cannot_change_action_or_smuggle_unsupported_parameters(self):
        for raw in ['{"action":"navigate"}','{"message":"fake"}','{"teleport":true}','[]']:
            c=Client()
            with self.assertRaises(ValueError):codex_decisions.apply(c,{'action':'say','message':'hello','arguments_json':raw})
            self.assertEqual([],c.calls)


class NavigationParameterValidationTests(unittest.TestCase):
    def test_invalid_speed_or_follow_mode_never_reserves_a_request(self):
        for patch in [{'pace':'normal'},{'continuous_follow':'false'},{'target_kind':'position'}]:
            c=Client()
            with self.assertRaises(codex_decisions.InvalidNavigationArguments):
                codex_decisions.apply(c,{'action':'navigate','target_kind':'coordinates','x':1,'y':2,'z':3,**patch})
            self.assertEqual([],c.calls)
