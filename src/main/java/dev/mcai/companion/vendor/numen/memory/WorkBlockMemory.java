/*
 * SPDX-License-Identifier: LGPL-3.0-only
 * Adapted from Numen, Copyright (c) 2026 Dwinovo.
 * Upstream commit: 34ef004dac3095fbbd928a897927e277c69d02fa
 * Modifications 2026-09-12 by weida / MinePilot contributors:
 * server/world-scoped persistence, observation-bounded validation, atomic writes,
 * corrupt-file preservation, package relocation and English documentation.
 * See THIRD_PARTY_NOTICES.md and META-INF/licenses/numen/.
 */
package dev.mcai.companion.vendor.numen.memory;

import dev.mcai.companion.MinecraftAiCompanion;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Remember stations placed or used by this body. The host supplies a distinct
 * file per world, body and dimension, and limits revalidation to current perception.
 * Unobserved entries are remembered coordinates, never proof of current existence.
 */
public final class WorkBlockMemory {

    /** Block id paths worth remembering — interaction infrastructure, not decoration. */
    private static final Set<String> TRACKED_TYPES = Set.of(
            "crafting_table", "furnace", "blast_furnace", "smoker",
            "chest", "barrel", "ender_chest",
            "anvil", "chipped_anvil", "damaged_anvil",
            "grindstone", "stonecutter", "smithing_table",
            "enchanting_table", "brewing_stand", "lodestone");

    /** Cap on remembered blocks: oldest-touched entries fall off first. */
    private static final int MAX_ENTRIES = 16;

    private final Path file;
    private boolean writable=true;
    public boolean writable(){return writable;}
    /** packed BlockPos → block id path; insertion order = recency (refreshed on record). */
    private final LinkedHashMap<Long, String> blocks = new LinkedHashMap<>();

    public WorkBlockMemory(Path file) {
        this.file = file;
        load();
    }


    /** Normalize station names, including mod-wrapped vanilla registry paths. */
    static String stationType(String blockId) {
        int colon = blockId.indexOf(':');
        String path = colon >= 0 ? blockId.substring(colon + 1) : blockId;
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    /** Is this block id a type we remember at all? Any id form works — see {@link #stationType}. */
    public static boolean isTracked(String blockId) {
        return TRACKED_TYPES.contains(stationType(blockId));
    }

    /** Remember (or refresh the recency of) a tracked block. Untracked types are ignored. */
    public void record(String blockId, BlockPos pos) {
        if (!isTracked(blockId)) return;
        String blockPath = stationType(blockId);
        long key = pos.asLong();
        String prev = blocks.remove(key);      // re-insert → newest
        blocks.put(key, blockPath);
        while (blocks.size() > MAX_ENTRIES) {
            Iterator<Long> it = blocks.keySet().iterator();
            it.next();
            it.remove();
        }
        if (!blockPath.equals(prev)) {
            MinecraftAiCompanion.LOGGER.info("[numen-memory] remembered {} at {},{},{}",
                    blockPath, pos.getX(), pos.getY(), pos.getZ());
        }
        save();
    }

    /**
     * Build the {@code <known_blocks>} system-prompt section, validating entries
     * against loaded chunks first (stale → evicted). Returns the empty string
     * when nothing is known, so callers can append unconditionally.
     *
     * @param level the entity's level for validation, or {@code null} when the
     *              body isn't resolvable this turn (entries kept on faith)
     */
    public String formatXml(Level level, java.util.function.Predicate<BlockPos> observable) {
        boolean changed = false;
        if (level != null) {
            Iterator<Map.Entry<Long, String>> it = blocks.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Long, String> e = it.next();
                BlockPos pos = BlockPos.of(e.getKey());
                if (!level.hasChunkAt(pos) || !observable.test(pos)) continue;   // unloaded — can't verify, keep
                String actual = stationType(BuiltInRegistries.BLOCK
                        .getKey(level.getBlockState(pos).getBlock()).getPath());
                if (!actual.equals(e.getValue())) {
                    MinecraftAiCompanion.LOGGER.info("[numen-memory] forgot {} at {},{},{} (now {})",
                            e.getValue(), pos.getX(), pos.getY(), pos.getZ(), actual);
                    it.remove();
                    changed = true;
                }
            }
        }
        if (changed) save();
        if (blocks.isEmpty()) return "";

        StringBuilder sb = new StringBuilder(128);
        sb.append("<known_blocks>\n");
        sb.append("  Remembered functional blocks placed or used in this dimension; recheck before using. Return to these ")
          .append("instead of crafting/placing duplicates:\n");
        for (Map.Entry<Long, String> e : blocks.entrySet()) {
            BlockPos pos = BlockPos.of(e.getKey());
            sb.append("  <block type=\"").append(e.getValue())
              .append("\" x=\"").append(pos.getX())
              .append("\" y=\"").append(pos.getY())
              .append("\" z=\"").append(pos.getZ()).append("\"/>\n");
        }
        sb.append("</known_blocks>");
        return sb.toString();
    }

    // ---- persistence (tiny write-through JSON file) ----

    private void load() {
        if (!Files.isRegularFile(file)) return;
        try {
            JsonArray arr = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8))
                    .getAsJsonArray();
            if(arr.size()>MAX_ENTRIES)throw new IllegalArgumentException("Memory entry limit exceeded");
            for (JsonElement el : arr) {
                JsonObject o = el.getAsJsonObject();
                BlockPos pos = new BlockPos(o.get("x").getAsInt(),
                        o.get("y").getAsInt(), o.get("z").getAsInt());
                String type=o.get("type").getAsString();
                if(!isTracked(type))throw new IllegalArgumentException("Invalid station type");
                blocks.put(pos.asLong(), type);
            }
        } catch (IOException | RuntimeException ex) {
            blocks.clear();writable=false;
            MinecraftAiCompanion.LOGGER.warn("[numen-memory] failed to load {}: {}", file, ex.toString());
        }
    }

    private void save() {
        if(!writable)return;
        JsonArray arr = new JsonArray();
        for (Map.Entry<Long, String> e : blocks.entrySet()) {
            BlockPos pos = BlockPos.of(e.getKey());
            JsonObject o = new JsonObject();
            o.addProperty("type", e.getValue());
            o.addProperty("x", pos.getX());
            o.addProperty("y", pos.getY());
            o.addProperty("z", pos.getZ());
            arr.add(o);
        }
        try {
            Files.createDirectories(file.getParent());
            Path temp=file.resolveSibling(file.getFileName()+".tmp");
            Files.writeString(temp, arr.toString(), StandardCharsets.UTF_8);
            Files.move(temp,file,java.nio.file.StandardCopyOption.REPLACE_EXISTING,java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            writable=false;
            MinecraftAiCompanion.LOGGER.warn("[numen-memory] failed to save {}: {}", file, ex.toString());
        }
    }

    /** Visible for the GUI / debug: current entries as readable lines. */
    public List<String> describeAll() {
        List<String> out = new ArrayList<>(blocks.size());
        for (Map.Entry<Long, String> e : blocks.entrySet()) {
            BlockPos pos = BlockPos.of(e.getKey());
            out.add(e.getValue() + " @ " + pos.getX() + "," + pos.getY() + "," + pos.getZ());
        }
        return out;
    }
}
