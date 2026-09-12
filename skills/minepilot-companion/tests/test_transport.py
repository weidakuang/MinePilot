import io,json,sys,unittest
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'scripts'))
from codex_transport import PersistentModel,ModelWorker

class TransportTests(unittest.TestCase):
    def test_fast_uses_confirmed_priority_with_low_effort(self):
        model=PersistentModel('unused',Path('/tmp'),'gpt-5.6-luna','fast')
        model._start=lambda:None;calls=[]
        def rpc(name,args):
            calls.append((name,args))
            return {'thread':{'id':'fast-thread'},'serviceTier':'priority','reasoningEffort':'low'} if name=='thread/start' else {'turn':{'id':'fast-turn'}}
        model.rpc=rpc;worker=ModelWorker(model);model._begin(worker,'instructions','hello',{})
        self.assertEqual('fast',calls[0][1]['config']['service_tier'])
        self.assertEqual('low',calls[-1][1]['effort'])
        self.assertEqual('priority',model.actual_service_tier)
        self.assertTrue(model.ready.is_set())

    def test_fast_does_not_silently_fall_back(self):
        model=PersistentModel('unused',Path('/tmp'),'gpt-5.6-luna','fast');model._start=lambda:None
        model.rpc=lambda *a:{'thread':{'id':'wrong'},'serviceTier':'default'}
        with self.assertRaisesRegex(RuntimeError,'not accepted'):model._ensure_thread('instructions')
        self.assertFalse(model.ready.is_set())

    def test_only_matching_turn_can_finish_current_worker(self):
        model=PersistentModel('unused',Path('/tmp'),'gpt-5.6-luna');worker=ModelWorker(model)
        worker.thread_id='thread';worker.turn_id='current';model.worker=worker
        events=[{'method':'turn/completed','params':{'threadId':'thread','turn':{'id':'old','status':'failed'}}},
                {'method':'item/completed','params':{'threadId':'thread','turnId':'old','item':{'type':'agentMessage','text':'{"action":"navigate"}'}}},
                {'method':'item/completed','params':{'threadId':'thread','turnId':'current','item':{'type':'agentMessage','text':'{"action":"say","message":"hello"}'}}},
                {'method':'turn/completed','params':{'threadId':'thread','turn':{'id':'current','status':'completed'}}}]
        class Process: stdout=io.StringIO('\n'.join(map(json.dumps,events)))
        model.process=Process();model._read(model.process)
        self.assertEqual(0,worker.poll());self.assertEqual('hello',worker.decision()['message'])
    def test_transport_failure_finishes_pending_worker(self):
        model=PersistentModel('unused',Path('/tmp'),'gpt-5.6-luna');model.worker=ModelWorker(model)
        class Process:stdout=io.StringIO('')
        model.process=Process();model._read(model.process)
        self.assertEqual(1,model.worker.poll())
    def test_invalid_action_and_oversize_decisions_are_rejected(self):
        worker=ModelWorker(None)
        for text in ['{"action":"teleport"}', 'x'*65537]:
            worker.text=text
            with self.assertRaises(ValueError):worker.decision()

class CancellationIsolationTests(unittest.TestCase):
    def test_replacement_uses_new_thread_while_previous_turn_is_cancelling(self):
        model=PersistentModel('unused',Path('/tmp'),'gpt-5.6-luna')
        old=ModelWorker(model);old.thread_id='old';old.cancelled=True;model.worker=old;model.thread_id='old'
        model._start=lambda:None
        calls=[]
        def rpc(name,args):
            calls.append((name,args))
            return {'thread':{'id':'new'}} if name=='thread/start' else {'turn':{'id':'new-turn'}}
        model.rpc=rpc
        new=ModelWorker(model);model._begin(new,'instructions','new chat',{})
        self.assertEqual('new',new.thread_id)
        self.assertEqual('new',calls[-1][1]['threadId'])
        self.assertFalse(old.done.is_set())

if __name__=='__main__':unittest.main()
