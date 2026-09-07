package dev.mcai.companion.agent.knowledge;

import com.google.gson.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.util.RandomSource;

/** Vanilla 26.2 metadata only; does not load client classes or audio on a dedicated server. */
public final class VanillaSoundCatalog {
    private final JsonObject events;
    private final JsonObject translations;
    public record Caption(String key, String text, double range) {}
    private record Sample(float volume, int distance) {}

    public VanillaSoundCatalog() {
        try (var in=VanillaSoundCatalog.class.getResourceAsStream("/data/mcai_companion/sound_catalog.json")) {
            if(in==null)throw new IllegalStateException("Native sound metadata is missing");
            var data=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
            events=data.getAsJsonObject("events");translations=data.getAsJsonObject("zh_cn");
        } catch(java.io.IOException failure){throw new IllegalStateException("Cannot read native sound metadata",failure);}
    }
    public String label(String key,String fallback) {
        return translations.has(key)?translations.get(key).getAsString():fallback;
    }
    public Caption resolve(String id,float volume,long seed) {
        if(!id.startsWith("minecraft:") || !Float.isFinite(volume))return null;
        String name=id.substring(10);var definition=events.getAsJsonObject(name);
        if(definition==null || !definition.has("subtitle"))return null;
        Sample sample=sample(name,RandomSource.create(seed),0);
        if(sample==null)return null;
        String key=definition.get("subtitle").getAsString();
        // SoundEngine notifies SubtitleOverlay before user category volume is applied.
        double range=Math.max(volume*sample.volume(),1.0F)*sample.distance();
        return new Caption(key,label(key,key),range);
    }
    private int weight(String name,int depth) {
        if(depth>16 || !events.has(name))return 0;
        int total=0;
        for(var v:events.getAsJsonObject(name).getAsJsonArray("variants")) {
            var row=v.getAsJsonObject();total+=row.has("event")?weight(row.get("event").getAsString(),depth+1):row.get("weight").getAsInt();
        }
        return total;
    }
    private Sample sample(String name,RandomSource random,int depth) {
        int total=weight(name,depth);if(total==0)return null;
        int selected=random.nextInt(total);
        for(var v:events.getAsJsonObject(name).getAsJsonArray("variants")) {
            var row=v.getAsJsonObject();int w=row.has("event")?weight(row.get("event").getAsString(),depth+1):row.get("weight").getAsInt();
            selected-=w;if(selected>=0)continue;
            float volume=row.get("volume").getAsFloat();
            if(!row.has("event"))return new Sample(volume,row.get("distance").getAsInt());
            var child=sample(row.get("event").getAsString(),random,depth+1);
            return child==null?null:new Sample(child.volume()*volume,child.distance());
        }
        return null;
    }
}
