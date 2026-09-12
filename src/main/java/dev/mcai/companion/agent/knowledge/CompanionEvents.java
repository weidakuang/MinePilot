package dev.mcai.companion.agent.knowledge;

import com.google.gson.*;
import java.util.*;
import dev.mcai.companion.agent.AgentRuntime;
import dev.mcai.companion.vendor.numen.event.EventQueue;

/** Persistent batched needs/events; one decision wake per batch, urgent needs first. */
public final class CompanionEvents {
    private final AgentRuntime runtime;private EventQueue queue;private long sequence;private JsonArray batch=new JsonArray();
    private int lastCheck,lastIdle,lastPeople;private String previousNeeds="";private boolean previousBreath;
    public CompanionEvents(AgentRuntime runtime){this.runtime=runtime;}
    private EventQueue queue(){if(queue==null)queue=new EventQueue(new EventQueue.Journal(){
        public List<EventQueue.Entry> load(){var rows=runtime.memory.pendingEvents();var result=new ArrayList<EventQueue.Entry>();for(var v:rows){try{result.add(new Gson().fromJson(v,EventQueue.Entry.class));}catch(RuntimeException ignored){}}return result;}
        public void save(List<EventQueue.Entry> rows){runtime.memory.pendingEvents(new Gson().toJsonTree(rows).getAsJsonArray());}
    },64);return queue;}
    public boolean idle(){var nav=runtime.navigation().status();return !runtime.camp().active() && !runtime.gather().active() && !runtime.survival().active() && !runtime.collection().ownsBody() && !runtime.mining().ownsBody() && !runtime.placement().ownsBody() && !runtime.excavation().ownsBody() && !runtime.turnActive() && !runtime.jumpActive() && (nav.phase().terminal() || nav.phase()==dev.mcai.companion.agent.navigation.NavigationToolCoordinator.Phase.IDLE);}
    public void tick(){int tick=runtime.server().getTickCount();if(tick-lastCheck<20)return;lastCheck=tick;
        int people=(int)runtime.server().getPlayerList().getPlayers().stream().filter(p->p!=runtime.player() && p.isAlive()).count();long now=System.currentTimeMillis();
        boolean breathing=runtime.breath.status().get("active").getAsBoolean();if(breathing && !previousBreath)queue().push("body","Surfacing to breathe; inspect current air and terrain before giving more work",now,true);previousBreath=breathing;
        if(people>0 && !runtime.memory.paused()){
            var p=runtime.player();int food=p.getFoodData().getFoodLevel();var needs=new JsonObject();needs.addProperty("foodLevel",food);boolean pick=false,axe=false,edible=false;int logs=0;
            for(int slot=0;slot<36;slot++){var stack=p.getInventory().getItem(slot);if(stack.isEmpty())continue;String id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();pick|=id.endsWith("_pickaxe");axe|=id.endsWith("_axe");edible|=stack.has(net.minecraft.core.component.DataComponents.FOOD);if(!dev.mcai.companion.agent.mining.TreeSurvey.species("minecraft:"+id).isEmpty())logs+=stack.getCount();}
            needs.addProperty("hasPickaxe",pick);needs.addProperty("hasAxe",axe);needs.addProperty("hasFood",edible);needs.addProperty("logs",logs);
            String current=needs.toString();boolean urgent=food<=8;
            if(!current.equals(previousNeeds)){queue().push("needs",current,now,urgent);previousNeeds=current;}
            if(idle() && (lastPeople==0 || tick-lastIdle>=600)){queue().push("idle","Player is online. Continue a remembered unfinished goal, address current survival needs or quietly keep company. Do not repeat a blocked plan without new evidence.",now,lastPeople==0);lastIdle=tick;}
            if(idle() && (queue().shouldDrain(now,3) || queue().oldestAgeMs(now)>=10000) || queue().hasUrgent()){
                batch=new Gson().toJsonTree(queue().takeEntries(now)).getAsJsonArray();sequence++;
            }
        }
        lastPeople=people;
    }
    public JsonObject snapshot(){var out=new JsonObject();out.addProperty("sequence",sequence);out.addProperty("paused",runtime.memory.paused());out.addProperty("onlinePlayers",lastPeople);out.addProperty("idle",idle());out.add("events",batch.deepCopy());return out;}
    public void interrupted(){queue().clearInterrupted();batch=new JsonArray();sequence++;}
}
