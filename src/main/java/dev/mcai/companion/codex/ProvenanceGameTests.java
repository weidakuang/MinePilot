package dev.mcai.companion.codex;
import com.google.gson.*;
import dev.mcai.companion.agent.AgentRuntime;
import dev.mcai.companion.agent.knowledge.DropProvenance;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;

/** Source-informed native origin/merge/pickup gate; setup actions are not attributed to model play. */
@GameTestNamespace("mcai_companion") @GameTestDontPrefix
public final class ProvenanceGameTests {
    @GameTest(name="provenance_regressions",structure="forge:empty48x32x48",maxTicks=2000,padding=8)
    public static void run(GameTestHelper h){if(!Boolean.getBoolean("minepilot.provenanceTest")){h.fail("Explicit provenance gate required");return;}new Gate(h).start();}
    private static final class Gate {
        final GameTestHelper h;final AgentRuntime r;final BlockPos origin;ItemEntity first,second,merged;int phase;long cursor;net.minecraft.world.level.block.entity.ChestBlockEntity outputChest;JsonArray evidence=new JsonArray();
        Gate(GameTestHelper h){this.h=h;r=AgentRuntime.active(h.getLevel().getServer());origin=h.absolutePos(new BlockPos(16,4,16));}
        void start(){
            var p=r.player();for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++){h.getLevel().setBlock(origin.offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);for(int y=0;y<4;y++)h.getLevel().setBlock(origin.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);}
            p.setPos(Vec3.atBottomCenterOf(origin));p.setDeltaMovement(Vec3.ZERO);p.setOnGround(true);p.stopControlling();p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();p.level().getChunkSource().move(p);p.inventoryLedger.tick();
            var point=Vec3.atBottomCenterOf(origin.offset(4,0,0));first=p.drop(new ItemStack(Items.APPLE,2),false);h.assertTrue(first!=null,"Native player toss denied");first.setPos(point);first.setDeltaMovement(Vec3.ZERO);first.setNoPickUpDelay();
            second=new ItemEntity(h.getLevel(),point.x,point.y,point.z,new ItemStack(Items.APPLE,3));second.setNoPickUpDelay();second.setDeltaMovement(Vec3.ZERO);h.getLevel().addFreshEntity(second);
            h.onEachTick(()->{try{tick();}catch(RuntimeException failure){throw new IllegalStateException("Provenance phase "+phase+" evidence="+evidence+" events="+p.inventoryLedger.events(cursor,32),failure);}});
        }
        void record(String name,JsonObject result){var row=result.deepCopy();row.addProperty("case",name);evidence.add(row);}
        void tick(){
            var p=r.player();h.assertTrue(h.getTick()<1500,"Provenance phase timeout "+phase);
            if(phase==0){
                merged=first.isAlive() && first.getItem().getCount()==5?first:second.isAlive() && second.getItem().getCount()==5?second:null;if(merged==null)return;
                var source=DropProvenance.source(merged,p);h.assertTrue(source.get("category").getAsString().equals("mixed") && source.get("lineageCount").getAsInt()==5,"Native merge lost origin quantities: "+source);
                int tossed=0,unknown=0;for(var value:source.getAsJsonArray("lineage")){var row=value.getAsJsonObject();String c=row.getAsJsonObject("source").get("category").getAsString();if(c.equals("player_toss"))tossed+=row.get("count").getAsInt();if(c.equals("unknown"))unknown+=row.get("count").getAsInt();}
                h.assertTrue(tossed==2 && unknown==3,"Merged attribution guessed an unknown giver");record("native merge preserves 2 known + 3 unknown",source);
                // Fill slots to allow a real partial pickup of one item.
                for(int slot=0;slot<36;slot++)p.getInventory().setItem(slot,new ItemStack(Items.STONE,64));p.getInventory().setItem(0,new ItemStack(Items.APPLE,63));p.inventoryLedger.tick();cursor=p.inventoryLedger.events(0,32).get("latestSequence").getAsLong();
                p.setPos(merged.position());p.setDeltaMovement(Vec3.ZERO);p.setOnGround(true);p.stopControlling();phase=1;
            }else if(phase==1 && p.getInventory().getItem(0).getCount()==64){
                h.assertTrue(merged.isAlive() && merged.getItem().getCount()==4,"Partial pickup altered total count");var events=p.inventoryLedger.events(cursor,32);int count=0;
                for(var ev:events.getAsJsonArray("events"))for(var value:ev.getAsJsonObject().getAsJsonArray("acquired")){var row=value.getAsJsonObject();if(row.get("item").getAsString().equals("minecraft:apple")){count+=row.get("count").getAsInt();h.assertTrue(row.getAsJsonObject("source").has("lineageCount") && row.getAsJsonObject("source").get("lineageCount").getAsInt()==1,"Partial receipt lacks counted attribution: "+row);}}
                if(count==0)return;h.assertTrue(count==1,"Partial pickup receipt double counted");h.assertTrue(DropProvenance.source(merged,p).get("lineageCount").getAsInt()==4,"Remaining lineage count changed");record("partial pickup and remaining entity",events);
                merged.discard();p.getInventory().clearContent();p.inventoryLedger.tick();cursor=p.inventoryLedger.events(0,32).get("latestSequence").getAsLong();
                r.server().getCommands().performPrefixedCommand(r.server().createCommandSourceStack(),"give MinePilot minecraft:diamond 3");phase=2;
            }else if(phase==2){
                var events=p.inventoryLedger.events(cursor,32);boolean found=false;for(var ev:events.getAsJsonArray("events"))for(var value:ev.getAsJsonObject().getAsJsonArray("acquired")){var row=value.getAsJsonObject();if(row.get("item").getAsString().equals("minecraft:diamond")){h.assertTrue(row.get("count").getAsInt()==3 && row.getAsJsonObject("source").get("category").getAsString().equals("system_grant"),"Give command source lost: "+row);found=true;}}
                if(!found)return;record("native give command",events);
                var carried=p.inventoryLedger.inventory().getAsJsonArray("entries").get(0).getAsJsonObject().getAsJsonObject("origins");h.assertTrue(carried.get("lineageCount").getAsInt()==3 && carried.get("allOriginsKnown").getAsBoolean(),"Carried grant attribution missing");
                p.getInventory().setSelectedSlot(0);p.drop(false);
                var tossed=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(p.blockPosition()).inflate(8),e->e.getItem().is(Items.DIAMOND) && e.getItem().getCount()==1);h.assertTrue(tossed.size()==1,"Native toss did not create one physical diamond");
                var transfer=DropProvenance.source(tossed.getFirst(),p);h.assertTrue(transfer.get("category").getAsString().equals("system_grant") && transfer.has("lastTransfer"),"Toss lost original grant: "+transfer);record("carried origin survives native toss",transfer);
                dev.mcai.companion.agent.knowledge.CarriedProvenance.forgetCache(p);
                p.inventoryLedger.tick();var saved=p.inventoryLedger.inventory().getAsJsonArray("entries").get(0).getAsJsonObject().getAsJsonObject("origins");h.assertTrue(saved.get("lineageCount").getAsInt()==2 && saved.get("allOriginsKnown").getAsBoolean(),"Persistent carried remainder lost origin");
                var chest=origin.offset(-3,0,0);h.getLevel().setBlock(chest,Blocks.CHEST.defaultBlockState(),3);var container=(net.minecraft.world.level.block.entity.ChestBlockEntity)h.getLevel().getBlockEntity(chest);container.setItem(0,new ItemStack(Items.EMERALD,7));
                var secondChest=chest.south();h.getLevel().setBlock(secondChest,Blocks.CHEST.defaultBlockState(),3);var other=(net.minecraft.world.level.block.entity.ChestBlockEntity)h.getLevel().getBlockEntity(secondChest);for(int slot=1;slot<27;slot++)container.setItem(slot,new ItemStack(Items.STONE,64));
                var doubleChest=new net.minecraft.world.CompoundContainer(container,other);p.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inventory,player)->net.minecraft.world.inventory.ChestMenu.sixRows(id,inventory,doubleChest),net.minecraft.network.chat.Component.literal("Double provenance chest")));int diamondSlot=-1;for(int i=0;i<p.containerMenu.slots.size();i++)if(p.containerMenu.slots.get(i).container==p.getInventory() && p.containerMenu.slots.get(i).getItem().is(Items.DIAMOND)){diamondSlot=i;break;}
                h.assertTrue(diamondSlot>=0,"Native menu could not find carried diamonds");p.containerMenu.clicked(diamondSlot,0,net.minecraft.world.inventory.ContainerInput.QUICK_MOVE,p);p.closeContainer();h.assertTrue(p.getInventory().countItem(Items.DIAMOND)==0 && other.countItem(Items.DIAMOND)==2,"Native menu did not move two diamonds into the chest");
                p.gameMode.destroyBlock(chest);p.gameMode.destroyBlock(secondChest);phase=3;
            }else if(phase==3){
                var items=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(origin).inflate(8),e->e.getItem().is(Items.EMERALD));if(items.isEmpty())return;int n=0;
                for(var item:items){var source=DropProvenance.source(item,p);h.assertTrue(source.get("category").getAsString().equals("broken_container_contents") && source.get("actorId").getAsString().equals(p.getUUID().toString()),"Container cause lost: "+source);n+=item.getItem().getCount();record("native broken container",source);}
                h.assertTrue(n==7,"Container actual drop count differs");
                var stored=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(origin).inflate(8),e->e.getItem().is(Items.DIAMOND) && e.getItem().getCount()==2);h.assertTrue(stored.size()==1,"Previously stored diamonds did not physically drop");
                var chain=DropProvenance.source(stored.getFirst(),p);h.assertTrue(chain.get("category").getAsString().equals("broken_container_contents") && chain.get("originCategory").getAsString().equals("system_grant") && chain.get("allOriginsKnown").getAsBoolean(),"Grant -> inventory -> chest -> broken-container chain lost: "+chain);record("native menu transfer and container break retain original grant",chain);
                var hopperPos=origin.offset(-4,0,3);var out=hopperPos.east();h.getLevel().setBlock(out,Blocks.CHEST.defaultBlockState(),3);outputChest=(net.minecraft.world.level.block.entity.ChestBlockEntity)h.getLevel().getBlockEntity(out);
                h.getLevel().setBlock(hopperPos,Blocks.HOPPER.defaultBlockState().setValue(net.minecraft.world.level.block.HopperBlock.FACING,net.minecraft.core.Direction.EAST),3);
                var item=stored.getFirst();item.setPos(Vec3.atBottomCenterOf(hopperPos.above()));item.setDeltaMovement(Vec3.ZERO);item.setNoPickUpDelay();phase=4;
            }else if(phase==4 && outputChest.countItem(Items.DIAMOND)==2){
                p.gameMode.destroyBlock(outputChest.getBlockPos());phase=5;
            }else if(phase==5){
                var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(origin.offset(-3,0,3)).inflate(1.5),e->e.getItem().is(Items.DIAMOND));if(drops.isEmpty())return;
                int total=0;for(var item:drops){var chain=DropProvenance.source(item,p);total+=item.getItem().getCount();h.assertTrue(chain.get("allOriginsKnown").getAsBoolean() && chain.getAsJsonArray("lineage").asList().stream().anyMatch(v->v.toString().contains("native_hopper_transfer")),"Native hopper lost origin: "+chain);record("physical hopper pickup and output retain original grant",chain);}h.assertTrue(total==2,"Hopper changed diamond count");
                var chicken=net.minecraft.world.entity.EntityTypes.CHICKEN.create(h.getLevel(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);h.assertTrue(chicken!=null,"Native chicken unavailable");chicken.setPos(Vec3.atBottomCenterOf(origin.offset(4,0,4)));h.getLevel().addFreshEntity(chicken);var egg=chicken.spawnAtLocation(h.getLevel(),new ItemStack(Items.EGG));
                var eggSource=DropProvenance.source(egg,p);h.assertTrue(eggSource.get("category").getAsString().equals("entity_drop") && eggSource.get("sourceEntityType").getAsString().equals("minecraft:chicken") && eggSource.get("actorId").getAsString().equals(chicken.getUUID().toString()),"Natural emitter source missing: "+eggSource);record("native entity drop emitter",eggSource);chicken.discard();
                try{var file=r.server().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("provenance-physical-evidence.json");java.nio.file.Files.writeString(file,new GsonBuilder().setPrettyPrinting().create().toJson(evidence));}catch(Exception e){throw new IllegalStateException(e);}phase=6;h.succeed();
            }
        }
    }
}
