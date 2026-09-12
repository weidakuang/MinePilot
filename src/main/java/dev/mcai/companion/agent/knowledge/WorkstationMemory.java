package dev.mcai.companion.agent.knowledge;

import com.google.gson.JsonObject;
import dev.mcai.companion.agent.AgentRuntime;
import dev.mcai.companion.vendor.numen.memory.WorkBlockMemory;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.storage.LevelResource;

/** Server adapter for the Numen station memory; distinct files per world/body/dimension. */
public final class WorkstationMemory {
    private final AgentRuntime runtime;
    private final Map<String,WorkBlockMemory> dimensions=new HashMap<>();
    public WorkstationMemory(AgentRuntime runtime){this.runtime=runtime;}
    private WorkBlockMemory current(){
        var dimension=runtime.player().level().dimension().identifier();
        return dimensions.computeIfAbsent(dimension.toString(),key->new WorkBlockMemory(
            runtime.server().getWorldPath(LevelResource.ROOT).resolve("data/minepilot-stations")
                .resolve(runtime.player().getUUID().toString()).resolve(dimension.getNamespace())
                .resolve(dimension.getPath()+".json")));
    }
    public void placed(BlockPos pos){
        var state=runtime.player().level().getBlockState(pos);
        current().record(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(),pos);
    }
    public JsonObject snapshot(){
        var memory=current();var out=new JsonObject();
        out.addProperty("dimension",runtime.player().level().dimension().identifier().toString());
        out.addProperty("knownBlocks",memory.formatXml(runtime.player().level(),pos -> runtime.player().level().isLoaded(pos) && net.minecraft.world.phys.Vec3.atCenterOf(pos).distanceToSqr(runtime.player().position())<=PerceptionRange.MAX*PerceptionRange.MAX));
        out.addProperty("memoryWritable",memory.writable());
        out.addProperty("source","remembered_placements_and_uses; loaded entries within sensing range revalidated");
        return out;
    }
}
