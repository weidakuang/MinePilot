package dev.mcai.companion.agent.knowledge;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import dev.mcai.companion.agent.body.MinePilotServerPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;

/** World-scoped item policy and an actual-count acquisition journal. All access is server-thread owned. */
public final class InventoryLedger {
    private final MinePilotServerPlayer player;
    private final Path file;
    private JsonObject memory = new JsonObject();
    private final Map<String, Integer> previous = new HashMap<>();
    private final List<JsonObject> pickups = new ArrayList<>();
    private final Deque<JsonObject> events = new ArrayDeque<>();
    private final LinkedHashMap<String, ItemStack> observedVariants = new LinkedHashMap<>();
    private final Map<String,String> variantKeys = new HashMap<>();
    private final LinkedHashMap<String, ItemStack> identities = new LinkedHashMap<>();
    private long sequence;
    private long revision;
    private String lastEquipmentState = "";
    private boolean writable = true;

    public InventoryLedger(MinePilotServerPlayer player) {
        this.player = player;
        file = player.level().getServer().getWorldPath(LevelResource.ROOT).resolve("data/minepilot-memory.json");
        try {
            if (Files.exists(file)) {
                if (Files.size(file) > 2_000_000) throw new IllegalStateException("Memory size limit exceeded");
                memory = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            }
            if (!memory.has("policies")) memory.add("policies", new JsonObject());
            if (!memory.has("waypoints")) memory.add("waypoints", new JsonObject());
            if(memory.has("treeFarms") && !memory.get("treeFarms").isJsonObject())throw new IllegalStateException("Invalid farm memory");
            if (!memory.get("policies").isJsonObject() || !memory.get("waypoints").isJsonObject()) throw new IllegalStateException("Invalid memory schema");
        } catch (Exception failure) {
            // Preserve the invalid file and fail closed: nothing becomes expendable.
            writable = false; memory = new JsonObject();
            memory.add("policies", new JsonObject()); memory.add("waypoints", new JsonObject());
        }
        String beforeMigration=memory.toString();
        previous.putAll(counts());
        if(writable && !beforeMigration.equals(memory.toString())) {
            try {save(memory.deepCopy());}catch(IllegalStateException unavailable){writable=false;}
        }
    }

    public String key(ItemStack stack) {
        for(var variant:observedVariants.entrySet())
            if(ItemStack.isSameItemSameComponents(variant.getValue(),stack)) {
                String found=variantKeys.get(variant.getKey());
                if(!identities.containsKey(found)){
                    var normalized=stack.copyWithCount(1);if(normalized.isDamageableItem())normalized.setDamageValue(0);
                    if(identities.size()>=256)identities.remove(identities.keySet().iterator().next());
                    identities.put(found,normalized);
                }
                return found;
            }
        ItemStack normalized = stack.copyWithCount(1);
        if (normalized.isDamageableItem()) normalized.setDamageValue(0);
        String legacy = digest(stack.copyWithCount(1));
        String stable = digest(normalized);
        // Preserve older damage-specific policies; identical tools share the most protective grade.
        var policies = memory.getAsJsonObject("policies");
        if (!legacy.equals(stable) && policies.has(legacy)) {
            try {
                var old = policies.getAsJsonObject(legacy);
                var current = policies.has(stable) ? policies.getAsJsonObject(stable) : null;
                int grade=old.get("importance").getAsInt();
                if(grade<0 || grade>5)throw new IllegalArgumentException("Invalid legacy policy grade");
                if (current == null || grade < current.get("importance").getAsInt())policies.add(stable, old.deepCopy());
            } catch(RuntimeException invalidPolicy){writable=false;}
        }
        if(observedVariants.size()>=256){String oldest=observedVariants.keySet().iterator().next();observedVariants.remove(oldest);variantKeys.remove(oldest);}
        observedVariants.put(legacy,stack.copyWithCount(1));variantKeys.put(legacy,stable);
        stack = normalized;
        for (var entry : identities.entrySet()) if (ItemStack.isSameItemSameComponents(entry.getValue(), stack)) return entry.getKey();
        if (identities.size() >= 256) identities.remove(identities.keySet().iterator().next());
        identities.put(stable, stack.copyWithCount(1)); return stable;
    }
    private String digest(ItemStack stack) {
        var value = ItemStack.CODEC.encodeStart(player.registryAccess().createSerializationContext(JsonOps.INSTANCE), stack).getOrThrow();
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical(value).getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    private static String canonical(JsonElement value) {
        if (value.isJsonObject()) {
            var map = new TreeMap<String,String>(); value.getAsJsonObject().entrySet().forEach(e -> map.put(e.getKey(), canonical(e.getValue())));
            return map.entrySet().stream().map(e -> new Gson().toJson(e.getKey()) + ":" + e.getValue()).collect(java.util.stream.Collectors.joining(",", "{", "}"));
        }
        if (value.isJsonArray()) { var a = new ArrayList<String>(); value.getAsJsonArray().forEach(e -> a.add(canonical(e))); return "[" + String.join(",",a) + "]"; }
        return value.toString();
    }

    public int importance(ItemStack stack) {
        JsonElement policy = memory.getAsJsonObject("policies").get(key(stack));
        try { int n = policy.getAsJsonObject().get("importance").getAsInt(); return n >= 0 && n <= 5 ? n : 2; }
        catch (RuntimeException absent) { return 2; }
    }
    public boolean expendable(ItemStack stack) { return !stack.isEmpty() && writable && importance(stack) >= 3; }
    public JsonObject annotate(String id, int importance, String note) {
        if (importance < 0 || importance > 5 || note.length() > 256) throw new IllegalArgumentException("Importance must be 0..5 and note <=256 characters");
        if (!counts().containsKey(id)) throw new IllegalArgumentException("That item identity is no longer in the inventory");
        var policies = memory.getAsJsonObject("policies");
        if (!policies.has(id) && policies.size() >= 2048) throw new IllegalStateException("Item policy capacity reached");
        JsonObject p = new JsonObject(); p.addProperty("importance",importance); p.addProperty("note",note);
        JsonObject changed = memory.deepCopy(); changed.getAsJsonObject("policies").add(id,p); save(changed); revision++;
        return inventory();
    }
    private void save(JsonObject changed) {
        if (!writable) throw new IllegalStateException("World memory is unreadable; its original file was preserved. Items remain protected");
        try {
            Files.createDirectories(file.getParent()); Path temp = file.resolveSibling(file.getFileName()+".tmp");
            String text = changed.toString(); if (text.length() > 2_000_000) throw new IllegalStateException("Memory size limit exceeded");
            Files.writeString(temp,text); Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE); memory = changed;
        } catch (java.io.IOException failure) {
            writable = false;
            throw new IllegalStateException("Could not save world memory; all carried items remain protected until memory is recovered",failure);
        }
    }
    public JsonObject waypoint(String operation, String name, String dimension, double x, double y, double z, String note) {
        if (operation.equals("list")) return waypoints(0,32);
        if (name.isBlank() || name.length() > 64 || note.length() > 256 || !Double.isFinite(x+y+z)) throw new IllegalArgumentException("Invalid waypoint");
        String id = dimension + "|" + name;
        var next = memory.deepCopy(); var points = next.getAsJsonObject("waypoints");
        if (operation.equals("remove")) points.remove(id);
        else if (operation.equals("save")) {
            if (!points.has(id) && points.size() >= 256) throw new IllegalStateException("Waypoint capacity reached");
            JsonObject point = new JsonObject(); point.addProperty("name",name); point.addProperty("dimension",dimension);
            point.addProperty("x",x);point.addProperty("y",y);point.addProperty("z",z);point.addProperty("note",note);
            point.addProperty("savedAtGameTick",player.level().getGameTime());point.addProperty("source","remembered_coordinate");points.add(id,point);
        } else throw new IllegalArgumentException("Unknown waypoint operation");
        save(next); return waypoint("list","",dimension,0,0,0,"");
    }
    public JsonObject remembered(String name, String dimension) {
        var p=memory.getAsJsonObject("waypoints").get(dimension+"|"+name);
        if (p==null) throw new IllegalArgumentException("No remembered waypoint in this dimension: "+name);
        return p.getAsJsonObject().deepCopy();
    }
    /** Explicit controller-declared farm bounds, not an ownership inference from appearance. */
    public JsonObject treeFarms() {
        var out=new JsonObject();out.addProperty("memoryWritable",writable);
        out.add("farms",memory.has("treeFarms")?memory.getAsJsonObject("treeFarms").deepCopy():new JsonObject());return out;
    }
    public JsonObject treeFarm(String operation,String name,JsonObject farm) {
        if(operation.equals("list"))return treeFarms();
        if(name.isBlank() || name.length()>64)throw new IllegalArgumentException("Invalid farm name");
        var next=memory.deepCopy();if(!next.has("treeFarms"))next.add("treeFarms",new JsonObject());
        var farms=next.getAsJsonObject("treeFarms");String key=player.level().dimension().identifier()+"|"+name;
        if(operation.equals("remove"))farms.remove(key);
        else if(operation.equals("save")) {
            if(farms.size()>=32 && !farms.has(key))throw new IllegalArgumentException("Farm memory capacity reached");
            var row=farm.deepCopy();row.addProperty("name",name);row.addProperty("dimension",player.level().dimension().identifier().toString());
            row.addProperty("source","controller_declared_area");farms.add(key,row);
        }else throw new IllegalArgumentException("Unknown farm operation");
        save(next);return treeFarms();
    }
    public JsonObject waypoints(int offset,int limit) {
        if(offset<0 || limit<1 || limit>32)throw new IllegalArgumentException("Invalid waypoint page");
        var all=memory.getAsJsonObject("waypoints");var selected=new JsonObject();int i=0;
        for(var entry:all.entrySet()){if(i++<offset)continue;if(selected.size()==limit)break;selected.add(entry.getKey(),entry.getValue().deepCopy());}
        var out=new JsonObject();out.add("waypoints",selected);out.addProperty("total",all.size());out.addProperty("nextOffset",offset+selected.size());out.addProperty("truncated",offset+selected.size()<all.size());return out;
    }
    public void pickedUp(ItemEntity entity, ItemStack actuallyPickedUp) {
        if (actuallyPickedUp.isEmpty()) return;
        JsonObject row = describe(actuallyPickedUp); row.addProperty("count",actuallyPickedUp.getCount());
        JsonObject source = DropProvenance.source(entity,player);
        row.add("source",source); row.addProperty("entityId",entity.getUUID().toString());
        if (pickups.size() < 128) pickups.add(row);
    }
    public void tick() {
        var now = counts(); JsonArray changes = new JsonArray(); var credited = new HashMap<String,Integer>();
        for (JsonObject pickup : pickups) {
            String id=pickup.get("entryId").getAsString(); int n=pickup.get("count").getAsInt();
            // The post-pickup event is authoritative even if another action consumes items in the same tick.
            changes.add(pickup); credited.merge(id,n,Integer::sum);
        }
        pickups.clear();
        for (var e:now.entrySet()) {
            int gained=e.getValue()-previous.getOrDefault(e.getKey(),0)-credited.getOrDefault(e.getKey(),0);
            if(gained>0) { var row=describe(identities.get(e.getKey())); row.addProperty("count",gained);
                JsonObject source=new JsonObject();source.addProperty("category","unknown");source.addProperty("confidence","unknown");row.add("source",source);changes.add(row); }
        }
        String equipment = equipment().toString();
        if(!now.equals(previous) || !equipment.equals(lastEquipmentState))revision++;
        lastEquipmentState = equipment;
        previous.clear();previous.putAll(now);
        if (!changes.isEmpty()) {
            JsonObject event=new JsonObject();event.addProperty("sequence",++sequence);event.addProperty("gameTick",player.level().getGameTime());
            event.add("acquired",changes);events.addLast(event);while(events.size()>128)events.removeFirst();
        }
    }
    private Map<String,Integer> counts() {
        var counts=new LinkedHashMap<String,Integer>();
        for(int i=0;i<player.getInventory().getContainerSize();i++){var s=player.getInventory().getItem(i);if(!s.isEmpty())counts.merge(key(s),s.getCount(),Integer::sum);}
        return counts;
    }
    public JsonObject describe(ItemStack stack) {
        JsonObject row=new JsonObject();row.addProperty("entryId",key(stack));row.addProperty("item",BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        row.addProperty("name",bounded(stack.getHoverName().getString(),128));row.addProperty("count",stack.getCount());row.addProperty("importance",importance(stack));
        row.addProperty("protected",!expendable(stack));JsonElement policy=memory.getAsJsonObject("policies").get(key(stack));
        row.addProperty("classified",policy!=null);if(policy!=null && policy.isJsonObject() && policy.getAsJsonObject().has("note"))row.add("note",policy.getAsJsonObject().get("note"));return row;
    }
    public JsonObject inventory() {
        JsonObject out=new JsonObject();JsonArray entries=new JsonArray();var rows=new LinkedHashMap<String,JsonObject>();
        for(int i=0;i<player.getInventory().getContainerSize();i++) {
            var stack=player.getInventory().getItem(i);if(stack.isEmpty())continue;String id=key(stack);JsonObject row=rows.get(id);
            if(row==null){row=describe(stack);row.addProperty("count",0);row.add("slots",new JsonArray());rows.put(id,row);}
            row.addProperty("count",row.get("count").getAsInt()+stack.getCount());row.getAsJsonArray("slots").add(i);
        }
        rows.values().forEach(entries::add);out.add("entries",entries);out.add("equipment",equipment());out.addProperty("revision",revision);out.addProperty("latestEventSequence",sequence);out.addProperty("memoryWritable",writable);return out;
    }
    /** Exact slot observations remain distinct even when policy identity ignores wear. */
    public JsonArray equipment() {
        var result = new JsonArray();
        for (int slot=0;slot<player.getInventory().getContainerSize();slot++) {
            var stack=player.getInventory().getItem(slot);
            if (stack.isEmpty() || !stack.isDamageableItem()) continue;
            var row=new JsonObject();row.addProperty("slot",slot);row.addProperty("entryId",key(stack));
            row.addProperty("item",BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            row.addProperty("damage",stack.getDamageValue());row.addProperty("maxDurability",stack.getMaxDamage());
            row.addProperty("remainingDurability",Math.max(0,stack.getMaxDamage()-stack.getDamageValue()));
            result.add(row);
        }
        return result;
    }
    public JsonObject events(long after,int limit) {
        if(after<0 || limit<1 || limit>32)throw new IllegalArgumentException("Invalid event cursor/limit");
        JsonArray selected=new JsonArray();for(var e:events)if(e.get("sequence").getAsLong()>after && selected.size()<limit)selected.add(e.deepCopy());
        JsonObject out=new JsonObject();out.add("events",selected);out.addProperty("latestSequence",sequence);
        out.addProperty("historyLost",!events.isEmpty() && after<events.getFirst().get("sequence").getAsLong()-1);
        if(!selected.isEmpty())out.add("inventory",inventory());return out;
    }
    public static String bounded(String s,int n){return s.length()>n?s.substring(0,n):s;}
}
