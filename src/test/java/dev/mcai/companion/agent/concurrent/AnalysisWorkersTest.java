package dev.mcai.companion.agent.concurrent;

import org.junit.jupiter.api.Test;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

final class AnalysisWorkersTest {
    @Test void cancellationInterruptsRunningWorkWithoutClosingAnotherOwner() throws Exception {
        try(var owner=new AnalysisWorkers();var other=new AnalysisWorkers()) {
            var began=new CountDownLatch(1);var interrupted=new CountDownLatch(1);
            var task=owner.submit(()->{began.countDown();try{new CountDownLatch(1).await();}catch(InterruptedException cancelled){interrupted.countDown();}return 1;});
            assertTrue(began.await(2,TimeUnit.SECONDS));task.cancel(true);
            assertTrue(interrupted.await(2,TimeUnit.SECONDS));
            owner.close();assertEquals(7,other.submit(()->7).get(2,TimeUnit.SECONDS));
            assertThrows(ExecutionException.class,()->owner.submit(()->8).get());
        }
    }
    @Test void computationLeavesCallingThreadAndQueueIsBounded() throws Exception {
        try(var owner=new AnalysisWorkers()) {
            assertTrue(owner.submit(()->Thread.currentThread().getName()).get(2,TimeUnit.SECONDS).startsWith("minepilot-analysis-"));
            var hold=new CountDownLatch(1);
            try {
                for(int i=0;i<4;i++)owner.submit(()->{try{hold.await();}catch(InterruptedException cancelled){Thread.currentThread().interrupt();}return 0;});
                assertTrue(owner.submit(()->9).isCompletedExceptionally());
                assertTrue(AnalysisWorkers.workerCount()<=4);
            } finally {hold.countDown();}
        }
    }
}
