import json
import unittest
from test_session import Worker
from test_inventory_session import InventoryClient
from companion_session import SessionLoop
from pickup_notifications import summarize
import codex_decisions

class PickupTests(unittest.TestCase):
    def test_merged_batch_keeps_each_source_quantity_and_latest_dropper(self):
        rows=[{'sequence':1,'acquired':[{'entityId':'drop','item':'minecraft:emerald','count':7,'source':{'category':'mixed','lineage':[
            {'count':2,'source':{'category':'mined_block','actorName':'Miner','lastTransfer':{'kind':'player_toss','actorId':'alice','actorName':'Alice'}}},
            {'count':3,'source':{'category':'player_toss','actorId':'bob','actorName':'Bob'}}]}}]}]
        notice=summarize(rows);parts=notice['items'][0]['sources']
        self.assertEqual(7,notice['totalCount']);self.assertEqual([2,3,2],[p['count'] for p in parts])
        self.assertEqual('Alice',parts[0]['actor']['actorName']);self.assertEqual('mined_block',parts[0]['originCause'])
        self.assertEqual('unknown',parts[-1]['cause']);self.assertIsNone(parts[-1]['actor'])

    def test_counts_are_acquisitions_not_stack_size(self):
        notice=summarize([{'sequence':1,'acquired':[{'item':'minecraft:stone','count':1}]},
                          {'sequence':2,'acquired':[{'item':'minecraft:stone','count':3}]}])
        self.assertEqual(4,notice['items'][0]['count']);self.assertEqual([1,2],notice['eventSequences'])

    def test_death_owner_is_separate_from_killer(self):
        notice=summarize([{'sequence':1,'acquired':[{'item':'minecraft:iron_ingot','count':2,'source':{
            'category':'death_drop','actorName':'Alice','killerName':'Zombie','damageType':'mob'}}]}])
        part=notice['items'][0]['sources'][0]
        self.assertEqual('Alice',part['actor']['actorName']);self.assertEqual('Zombie',part['killerName'])
        self.assertEqual('mob',part['damageType'])

    def make_loop(self):
        class Game(InventoryClient):
            def poll_events(self,chat,system,inventory):
                return {'chat':{'messages':[m for m in self.messages if m['sequence']>chat]},
                        'inventoryEvents':{'events':[g for g in self.gains if g['sequence']>inventory]},
                        'navigation':self.state,'gather':{'requestId':'job','phase':'EXECUTING','ownsBody':True}}
        game=Game();events=[];workers=[]
        def launch(e,s):events.append(e);w=Worker();workers.append(w);return w
        loop=SessionLoop(game,launch)
        game.gains=[{'sequence':1,'acquired':[{'entityId':'gift','item':'minecraft:apple','count':3,'source':{'category':'player_toss','actorName':'Alice'}}]}]
        return game,loop,events,workers

    def test_pickup_can_thank_immediately_while_gathering_without_stopping(self):
        game,loop,events,workers=self.make_loop();loop.tick()
        self.assertTrue(events[0]['speechFirst']);self.assertEqual(3,events[0]['pickupNotice']['totalCount'])
        workers[0].returncode=0;workers[0].decision=lambda:{'action':'say','message':'谢谢 Alice 的苹果！'}
        loop.tick();self.assertEqual([('say',{'message':'谢谢 Alice 的苹果！'})],[(n,a) for n,a in game.calls if n=='say'])
        self.assertFalse(any(n.startswith('cancel_') for n,a in game.calls))

    def test_pickup_can_be_silent_and_is_not_replayed(self):
        game,loop,events,workers=self.make_loop();loop.tick()
        workers[0].returncode=0;workers[0].decision=lambda:{'action':'wait','message':''}
        loop.tick();loop.tick();self.assertEqual(1,len(events));self.assertFalse(any(n=='say' for n,a in game.calls))

    def test_chat_interrupt_preserves_unspoken_pickup(self):
        game,loop,events,workers=self.make_loop();loop.tick()
        game.messages=[{'sequence':1,'text':'你好'}];loop.tick();self.assertTrue(workers[0].terminated)
        self.assertEqual('player_chat',events[-1]['type']);workers[-1].returncode=0;workers[-1].decision=lambda:{'action':'wait'}
        loop.tick();self.assertTrue(events[-1]['speechFirst']);self.assertEqual([1],events[-1]['pickupNotice']['eventSequences'])

    def test_speech_decision_has_no_body_tools(self):
        schema=codex_decisions.event_schema({'type':'inventory_event','speechFirst':True})
        self.assertEqual(['say','wait'],schema['properties']['action']['enum']);self.assertNotIn('tool_name',schema['properties'])
