package dev.mcai.companion.vendor.numen;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import com.google.gson.JsonObject;
import dev.mcai.companion.vendor.numen.task.RecoveryLadder;
import dev.mcai.companion.vendor.numen.event.EventQueue;
import dev.mcai.companion.vendor.numen.memory.CompactSplit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RecoveryAndEventTest {
    @Test void alternateApproachesRebuildButDoNotAcquireMissingPrerequisites(){
        var created=new AtomicInteger();var ladder=new RecoveryLadder<Integer,String>(List.of(
            new RecoveryLadder.Rung<>(created::incrementAndGet,Set.of("NO_PATH"),2),
            new RecoveryLadder.Rung<>(created::incrementAndGet,Set.of("NO_PATH"),1)));
        assertEquals(1,ladder.current());assertEquals(1,ladder.current());assertTrue(ladder.advance("NO_PATH"));assertEquals(2,ladder.current());
        assertTrue(ladder.advance("NO_PATH"));assertEquals(1,ladder.currentRung());assertEquals(3,ladder.current());
        assertFalse(ladder.advance("MISSING_TOOL"));assertNull(ladder.current());assertTrue(ladder.exhausted());
    }
    @Test void journalIsBoundedAndUrgentEventsCanWakeWithoutWaitingForOrdinaryBatch(){
        class Journal implements EventQueue.Journal {List<EventQueue.Entry> saved=List.of();public List<EventQueue.Entry> load(){return saved;}public void save(List<EventQueue.Entry> entries){saved=List.copyOf(entries);}}
        var journal=new Journal();var q=new EventQueue(journal,3);var wakes=new AtomicInteger();q.addUrgentListener(wakes::incrementAndGet);
        for(int i=0;i<5;i++)q.push("idle","event "+i,1000+i,false);
        assertEquals(3,journal.saved.size());q.push("needs","Hungry",1006,true);assertEquals(1,wakes.get());assertTrue(q.shouldDrain(1006,5));
        var restored=new EventQueue(journal,3);var events=restored.takeEntries(1007);assertTrue(events.stream().anyMatch(e->e.text().equals("Hungry")));assertTrue(journal.saved.isEmpty());
    }
    @Test void compactHistoryKeepsRecentConversationBoundaryAndAccountsForChinese(){
        var history=new ArrayList<JsonObject>();for(int i=0;i<12;i++){var row=new JsonObject();row.addProperty("role",i%2==0?"user":"assistant");row.addProperty("text","记住河边的营地".repeat(8));history.add(row);}
        var split=CompactSplit.byRecentBudget(history,200);assertFalse(split.toSummarize().isEmpty());assertFalse(split.kept().isEmpty());assertEquals("user",split.kept().getFirst().get("role").getAsString());assertEquals(history.getLast(),split.kept().getLast());
        assertTrue(CompactSplit.estimateTokens("村庄".repeat(10))>CompactSplit.estimateTokens("ab".repeat(10)));
    }
}
