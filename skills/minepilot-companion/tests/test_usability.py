import unittest
from unittest.mock import patch
import minepilot
from test_session import Client, Worker
from companion_session import SessionLoop

class UsabilityTests(unittest.TestCase):
    def setUp(self):
        self.client = Client(); self.events = []; self.workers = []
        def launch(event, sequence):
            self.events.append(event); w = Worker(); self.workers.append(w); return w
        self.loop = SessionLoop(self.client, launch)
    def test_new_navigation_does_not_dispatch_previous_completion(self):
        self.client.state = {'phase':'COMPLETED','requestId':'old'}
        self.loop.initial_message = 'new destination'; self.loop.tick()
        self.workers[-1].returncode = 0
        self.workers[-1].decision = lambda:{'action':'navigate','message':'coming'}
        def apply(client, decision): client.state = {'phase':'PLANNING','requestId':'new'}
        with patch('codex_decisions.apply', side_effect=apply): self.loop.tick()
        self.assertEqual(1,len(self.events))
        self.assertEqual('new',self.loop.authorized_request)
    def test_tool_failure_retains_reason_without_retrying(self):
        self.loop.initial_message = 'jump'; self.loop.tick()
        self.workers[-1].returncode = 0
        self.workers[-1].decision = lambda:{'action':'jump'}
        with patch('codex_decisions.apply', side_effect=minepilot.ToolError('Jump requires stable ground')):
            self.loop.tick()
        self.assertEqual('operation_failed',self.events[-1]['type'])
        self.assertEqual('Jump requires stable ground',self.events[-1]['reason'])
        self.workers[-1].returncode = 0
        self.workers[-1].decision = lambda:{'action':'jump'}
        self.loop.tick()
        self.assertFalse(any(n=='jump_once' for n,_ in self.client.calls))
    def test_chat_during_selection_does_not_consume_ready_event(self):
        self.client.state = {'phase':'PLAN_READY','requestId':'one'}
        self.loop.tick()
        self.client.messages = [{'sequence':1,'text':'hello'}]
        self.loop.tick(); self.workers[-1].returncode = 0
        self.workers[-1].decision = lambda:{'action':'say','message':'hello'}
        self.loop.tick()
        self.assertEqual('navigation_event',self.events[-1]['type'])
    def test_stale_choice_cannot_execute_new_request(self):
        self.client.state = {'phase':'PLAN_READY','requestId':'old'}
        self.loop.tick(); self.workers[-1].returncode = 0
        self.workers[-1].decision = lambda:{'action':'choose','request_id':'old','option_id':'A'}
        self.client.state = {'phase':'EXECUTING','requestId':'new'}
        self.loop.tick()
        self.assertFalse(any(n == 'choose_navigation' for n,_ in self.client.calls))
    def test_local_stop_discards_pending_model_response(self):
        self.loop.initial_message = 'go'; self.loop.tick()
        self.client.messages = [{'sequence':1,'text':'停下','handledLocally':True}]
        self.loop.tick()
        self.assertTrue(self.workers[0].terminated)
        self.assertEqual(1,len(self.workers))
    def test_unique_safe_route_starts_but_partial_route_needs_model_choice(self):
        for partial in [False, True]:
            with self.subTest(partial=partial):
                self.setUp(); self.loop.authorized_request = 'one'
                self.client.state = {'phase':'PLAN_READY','requestId':'one','routeOptions':[{
                    'optionId':'A','feasibleNow':True,'estimatedHealthLost':0,'supportBlocksRequired':0,
                    'hazards':[],'suggestedPace':'walk','supportedPaces':['walk']}]}
                if partial:self.client.state['partialDestination']={'x':1}
                self.loop.tick()
                self.assertEqual(not partial,any(n=='choose_navigation' for n,_ in self.client.calls))
    def test_navigation_event_cannot_replay_old_move_command(self):
        self.client.state = {'phase':'PLAN_READY','requestId':'one'}
        self.loop.tick(); self.workers[-1].returncode = 0
        self.workers[-1].decision = lambda:{'action':'navigate','message':'coming again'}
        self.loop.tick()
        self.assertFalse(any(n=='request_navigation' for n,_ in self.client.calls))
