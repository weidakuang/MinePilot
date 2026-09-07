import unittest
from test_session import Client, Worker
from companion_session import SessionLoop, check_mod_tools, IncompatibleMod
import codex_decisions, fast_commands

class InventoryClient(Client):
    def __init__(self): super().__init__(); self.gains=[]
    def call_tool(self,name,args):
        if name=='inventory_events':return {'events':[e for e in self.gains if e['sequence']>args['after_sequence']]}
        return super().call_tool(name,args)

class InventorySessionTests(unittest.TestCase):
    def test_missing_target_is_not_correlated_with_unknown_acquisition(self):
        loop=SessionLoop(InventoryClient(),None)
        event={'type':'navigation_event','state':{'phase':'FAILED'},'recentAcquisitions':[{'sequence':1,'acquired':[{'item':'stone'}]}]}
        self.assertEqual([],loop.acquisition_context(event))
        loop.worker_event=event
        decision=loop.quiet_inventory_decision({'action':'say','message':''})
        self.assertEqual('say',decision['action']);self.assertTrue(decision['message'])
    def test_empty_reply_is_silence_and_quiet_cancel_still_ends_navigation(self):
        c=InventoryClient()
        codex_decisions.apply(c,{'action':'say','message':'  '})
        codex_decisions.apply(c,{'action':'organize','annotations':[],'message':'  '})
        codex_decisions.apply(c,{'action':'cancel','request_id':'done','message':''})
        self.assertEqual([('cancel_navigation',{'request_id':'done','reason':'Request ended without additional chat'})],c.calls)

    def test_whitespace_reply_does_not_mark_an_acquisition_reported(self):
        c=InventoryClient();workers=[]
        def launch(e,s):w=Worker();workers.append(w);return w
        loop=SessionLoop(c,launch);c.gains=[{'sequence':1,'acquired':[]}];loop.tick()
        workers[-1].returncode=0;workers[-1].decision=lambda:{'action':'say','message':'  ','speech_reason':'player_gift_or_loan'}
        loop.tick()
        self.assertFalse(loop.reported_acquisitions)
    def test_ordinary_gain_and_idle_organization_do_not_emit_unnecessary_chat(self):
        c=InventoryClient();workers=[]
        def launch(e,s):w=Worker();workers.append(w);return w
        loop=SessionLoop(c,launch);c.gains=[{'sequence':1,'acquired':[{'item':'stone','count':1}]}];loop.tick()
        workers[-1].returncode=0;workers[-1].decision=lambda:{'action':'say','message':'I picked up stone','speech_reason':'none'}
        loop.tick()
        self.assertEqual('inventory_review',loop.worker_event['type'])
        workers[-1].returncode=0;workers[-1].decision=lambda:{'action':'organize','annotations':[{'entry_id':'stone','importance':4,'note':'spare'}],'message':'Organized','speech_reason':'direct_player_relevance'}
        loop.tick()
        self.assertTrue(any(n=='annotate_item' for n,_ in c.calls))
        self.assertFalse(any(n=='say' for n,_ in c.calls))

    def test_gift_acknowledgement_is_not_repeated_by_target_loss_or_idle_review(self):
        c=InventoryClient();workers=[];events=[]
        def launch(e,s):events.append(e);w=Worker();workers.append(w);return w
        loop=SessionLoop(c,launch);c.state={'phase':'EXECUTING','requestId':'collect','destination':{'targetIdentity':'apple-uuid'}}
        c.gains=[{'sequence':1,'acquired':[{'entityId':'apple-uuid','item':'apple','count':3,'source':{'category':'player_toss'}}]}]
        loop.tick()
        c.state.update(phase='REPLAN_REQUIRED',lastEventMessage='DROPPED_ITEM_UNAVAILABLE')
        workers[-1].returncode=0;workers[-1].decision=lambda:{'action':'say','message':'Thanks for the apples','speech_reason':'player_gift_or_loan'}
        loop.tick()
        self.assertTrue(events[-1]['acquisitionAlreadyReported'])
        self.assertEqual(1,len(events[-1]['recentAcquisitions']))
        workers[-1].returncode=0;workers[-1].decision=lambda:{'action':'cancel','request_id':'collect','message':'Collected three apples'}
        loop.tick()
        self.assertEqual(1,sum(n=='say' for n,_ in c.calls))
        self.assertTrue(any(n=='cancel_navigation' for n,_ in c.calls))

    def test_explicit_report_request_can_make_idle_review_speak(self):
        c=InventoryClient();workers=[]
        def launch(e,s):w=Worker();workers.append(w);return w
        loop=SessionLoop(c,launch);loop.inventory_review=True;loop.tick()
        workers[-1].returncode=0;workers[-1].decision=lambda:{'action':'organize','annotations':[],'message':'As requested, inventory reviewed','speech_reason':'requested_report'}
        loop.tick()
        self.assertEqual(1,sum(n=='say' for n,_ in c.calls))

    def test_navigation_failure_requires_visible_outcome(self):
        schema=codex_decisions.event_schema({'type':'navigation_event','state':{'phase':'FAILED'}})
        self.assertEqual(['say'],schema['properties']['action']['enum'])
    def test_old_mod_reports_missing_tools_instead_of_waiting_for_a_running_world(self):
        class OldMod:
            def request(self,method):return {'tools':[{'name':'observe'},{'name':'read_chat'}]}
        with self.assertRaisesRegex(IncompatibleMod,'inventory_events'):check_mod_tools(OldMod())
    def test_pickup_can_be_silent_then_reviewed_at_idle_without_replay(self):
        c=InventoryClient();events=[];workers=[]
        def launch(e,s):events.append(e);w=Worker();workers.append(w);return w
        loop=SessionLoop(c,launch);c.gains=[{'sequence':1,'acquired':[{'item':'stone','count':3}]}]
        c.state={'phase':'EXECUTING','requestId':'walk'};loop.tick()
        self.assertEqual('inventory_event',events[-1]['type'])
        workers[-1].returncode=0;workers[-1].decision=lambda:{'action':'wait'};loop.tick()
        self.assertEqual(1,len(events))
        c.state={'phase':'IDLE'};loop.tick();self.assertEqual('inventory_review',events[-1]['type'])
        workers[-1].returncode=0;workers[-1].decision=lambda:{'action':'wait'};loop.tick();loop.tick()
        self.assertEqual(2,len(events));self.assertFalse(any(n=='say' for n,_ in c.calls))
    def test_new_player_chat_takes_priority_without_losing_pickup(self):
        c=InventoryClient();events=[];workers=[]
        def launch(e,s):events.append(e);w=Worker();workers.append(w);return w
        loop=SessionLoop(c,launch);c.gains=[{'sequence':1,'acquired':[]}];c.messages=[{'sequence':1,'text':'你好'}]
        loop.tick();self.assertEqual('player_chat',events[-1]['type'])
        workers[-1].returncode=0;workers[-1].decision=lambda:{'action':'wait'};loop.tick()
        self.assertEqual('inventory_event',events[-1]['type'])
    def test_tools_cannot_escape_game_allowlist(self):
        c=Client()
        with self.assertRaises(ValueError):codex_decisions.apply(c,{'action':'tool','tool_name':'shell','arguments_json':'{}'})
        self.assertEqual([],c.calls)
    def test_short_command_fast_path_does_not_match_negation_or_compound_requests(self):
        self.assertEqual('jump',fast_commands.parse('跳一下')['action'])
        self.assertEqual(-2,fast_commands.parse('往后走两格')['forward_blocks'])
        for text in ['不要跳一下','跳一下然后前进','你刚才跳一下了吗','转向361度']:
            self.assertIsNone(fast_commands.parse(text))

    def test_listen_decision_returns_public_data_without_chat_or_movement(self):
        c=Client()
        result=codex_decisions.apply(c,{'action':'tool','tool_name':'listen','arguments_json':'{"limit":8}'})
        self.assertEqual([('listen',{'limit':8})],c.calls)
        self.assertEqual({'tool':'listen','result':{}},result)
        args=__import__('minepilot').parser().parse_args(['tool','--name','listen','--arguments','{}'])
        self.assertEqual('listen',args.name)
        self.assertIn('listen',codex_decisions.SCHEMA['properties']['tool_name']['enum'])
