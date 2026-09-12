import json
import sys
import unittest
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'scripts'))
import codex_decisions
from companion_session import SessionLoop
from test_inventory_session import InventoryClient
from test_session import Worker

class DropSessionTests(unittest.TestCase):
    def test_inventory_event_can_drop_without_speech_or_cancelling_movement(self):
        client=InventoryClient();workers=[]
        def launch(event,state):
            w=Worker();workers.append(w);return w
        loop=SessionLoop(client,launch)
        client.state={'phase':'EXECUTING','requestId':'walk'}
        client.gains=[{'sequence':1,'acquired':[{'item':'stone','count':3}]}]
        loop.tick()
        self.assertEqual('inventory_event',loop.worker_event['type'])
        args={'request_key':'drop-1','items':[{'item':'minecraft:stone','count':2}],'reason':'capacity'}
        workers[-1].returncode=0
        workers[-1].decision=lambda:{'action':'tool','tool_name':'drop_items','arguments_json':json.dumps(args),'message':'Unnecessary announcement','speech_reason':'none'}
        loop.tick()
        self.assertIn(('drop_items',args),client.calls)
        self.assertFalse(any(name in {'say','cancel_navigation','request_navigation'} for name,_ in client.calls))
        self.assertEqual('walk',client.state['requestId'])

    def test_inventory_schema_limits_actions_to_inventory_tools(self):
        for kind in ('inventory_event','inventory_review'):
            schema=codex_decisions.event_schema({'type':kind})['properties']
            self.assertIn('tool',schema['action']['enum'])
            self.assertIn('drop_items',schema['tool_name']['enum'])
            self.assertIn('reclaim_drop',schema['tool_name']['enum'])
            self.assertNotIn('plan_excavation',schema['tool_name']['enum'])

    def test_reclaim_does_not_fabricate_navigation_or_receipt(self):
        client=InventoryClient()
        result=codex_decisions.apply(client,{'action':'tool','tool_name':'reclaim_drop','arguments_json':'{"entity_id":"seen-entity"}'})
        self.assertEqual([('reclaim_drop',{'entity_id':'seen-entity'})],client.calls)
        self.assertEqual('reclaim_drop',result['tool'])
