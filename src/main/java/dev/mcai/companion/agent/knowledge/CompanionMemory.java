package dev.mcai.companion.agent.knowledge;

import com.google.gson.*;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import dev.mcai.companion.agent.AgentRuntime;
import dev.mcai.companion.vendor.numen.memory.CompactSplit;
import net.minecraft.world.level.storage.LevelResource;

/** World/body-scoped dialogue and current goals; live observations never enter this history. */
public final class CompanionMemory {
    private final AgentRuntime runtime;private Path path;private JsonObject data;private boolean writable=true;
    public CompanionMemory(AgentRuntime runtime){this.runtime=runtime;}
    private void load(){if(data!=null)return;path=runtime.server().getWorldPath(LevelResource.ROOT).resolve("data/minepilot-companion").resolve(runtime.player().getUUID()+".json");
        try{data=Files.exists(path)?JsonParser.parseString(Files.readString(path)).getAsJsonObject():new JsonObject();if(data.has("recent") && !data.get("recent").isJsonArray())throw new IllegalStateException("Malformed recent history");}
        catch(RuntimeException|IOException failure){data=new JsonObject();writable=false;dev.mcai.companion.MinecraftAiCompanion.LOGGER.warn("Companion memory could not be loaded; original file preserved: {}",path);}
        if(!data.has("recent"))data.add("recent",new JsonArray());if(!data.has("summary"))data.addProperty("summary","");if(!data.has("paused"))data.addProperty("paused",false);
    }
    public void chat(String role,String speaker,String text){load();var row=new JsonObject();row.addProperty("role",role);row.addProperty("speaker",speaker);row.addProperty("text",text);row.addProperty("at",System.currentTimeMillis());row.addProperty("dimension",runtime.player().level().dimension().identifier().toString());
        var recent=data.getAsJsonArray("recent");recent.add(row);var list=recent.asList().stream().map(JsonElement::getAsJsonObject).toList();int tokens=list.stream().mapToInt(m->CompactSplit.estimateTokens(m.get("text").getAsString())).sum();
        if(tokens>1800 || list.size()>32){var split=CompactSplit.byRecentBudget(list,1000);var kept=new JsonArray();split.kept().stream().skip(Math.max(0,split.kept().size()-24)).forEach(kept::add);data.add("recent",kept);
            var old=new StringBuilder(data.has("earlierExcerpts")?data.get("earlierExcerpts").getAsString():"");
            for(var message:split.toSummarize()){String line=message.get("speaker").getAsString()+": "+message.get("text").getAsString();old.append(line.length()>120?line.substring(0,120)+"…":line).append('\n');}
            String bounded=old.toString();data.addProperty("earlierExcerpts",bounded.substring(Math.max(0,bounded.length()-1600)));
        }save();
    }
    public void pause(boolean pause){load();data.addProperty("paused",pause);if(pause && data.has("goal"))data.getAsJsonObject("goal").addProperty("status","PAUSED");save();}
    public boolean paused(){load();return data.get("paused").getAsBoolean();}
    public JsonObject update(String summary,String objective,String status,boolean autonomous){load();
        if(summary!=null){if(summary.length()>2400)throw new IllegalArgumentException("Memory summary exceeds 2400 characters");data.addProperty("summary",summary);}
        if(objective!=null){if(objective.length()>512 || !Set.of("ACTIVE","COMPLETED","BLOCKED","PAUSED").contains(status))throw new IllegalArgumentException("Invalid goal memory");var goal=new JsonObject();goal.addProperty("objective",objective);goal.addProperty("status",status);goal.addProperty("autonomous",autonomous);goal.addProperty("dimension",runtime.player().level().dimension().identifier().toString());goal.addProperty("updatedAt",System.currentTimeMillis());data.add("goal",goal);}
        save();return snapshot();
    }
    public JsonObject snapshot(){load();var out=data.deepCopy();out.remove("pendingEvents");out.remove("camp");out.addProperty("memoryWritable",writable);out.addProperty("source","persisted_conversation_and_goal; remembered statements are not current world observations");return out;}
    public JsonArray pendingEvents(){load();return data.has("pendingEvents")?data.getAsJsonArray("pendingEvents").deepCopy():new JsonArray();}
    public void pendingEvents(JsonArray entries){load();data.add("pendingEvents",entries);save();}
    public JsonObject camp(){load();return data.has("camp")?data.getAsJsonObject("camp").deepCopy():new JsonObject();}
    public void camp(JsonObject record){load();data.add("camp",record.deepCopy());save();}
    private void save(){if(!writable)return;try{Files.createDirectories(path.getParent());var temp=Files.createTempFile(path.getParent(),"memory-",".tmp");Files.writeString(temp,data.toString()+"\n");try{Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException failure){Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING);}}
        catch(IOException failure){writable=false;dev.mcai.companion.MinecraftAiCompanion.LOGGER.warn("Companion memory save failed; previous file retained: {}",path);}}
}
