package dev.mcai.companion.agent.survival;

import com.google.gson.JsonObject;
import dev.mcai.companion.agent.AgentRuntime;
import dev.mcai.companion.agent.placement.HandController;
import dev.mcai.companion.vendor.numen.tools.MenuOps;
import dev.mcai.companion.vendor.numen.tools.NativeInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Native timed item use and furnace work; no tick acceleration or synthetic receipts. */
public final class SurvivalCoordinator implements AutoCloseable {
    private final AgentRuntime runtime;
    private String phase="IDLE", operation="", reason="";
    private int started, ended, wanted, collected, before, foodBefore;
    private Item input, output, burn;
    private int fuelCount, approachIndex;
    private String step="";
    private dev.mcai.companion.agent.navigation.NativeTravel travel;
    private java.util.List<net.minecraft.world.phys.Vec3> approaches=java.util.List.of();
    private BlockPos station;
    private java.util.UUID request;
    public SurvivalCoordinator(AgentRuntime runtime){this.runtime=runtime;}
    public boolean active(){return phase.equals("EXECUTING");}
    public JsonObject eat(String id){
        var p=runtime.player();if(!p.canEat(false))throw new IllegalStateException("Body is not hungry");
        int slot=-1;
        for(int i=0;i<36;i++){var s=p.getInventory().getItem(i);String name=BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
            if(!NativeInventory.usable(p,s) || !s.has(DataComponents.FOOD) || !s.has(DataComponents.CONSUMABLE))continue;
            if(!id.isBlank() && !s.is(NativeInventory.parseItem(id)))continue;
            if(id.isBlank() && (name.contains("rotten_flesh") || name.contains("spider_eye") || name.contains("pufferfish") || name.contains("poisonous") || name.contains("suspicious")))continue;
            slot=i;break;
        }
        if(slot<0)throw new IllegalStateException("No suitable carried food");
        p.closeContainer();HandController.equipSlot(p,slot,InteractionHand.MAIN_HAND);input=p.getMainHandItem().getItem();before=NativeInventory.count(p.getInventory(),input);foodBefore=p.getFoodData().getFoodLevel();
        var result=p.gameMode.useItem(p,p.level(),p.getMainHandItem(),InteractionHand.MAIN_HAND);
        if(!result.consumesAction() || !p.isUsingItem())throw new IllegalStateException("Native food use did not start");
        begin("eat",1);return status();
    }
    public JsonObject smelt(BlockPos pos,String item,String fuel,int count,int fuelCount){
        var p=runtime.player();input=NativeInventory.parseItem(item);burn=NativeInventory.parseItem(fuel);this.fuelCount=fuelCount;station=pos.immutable();output=null;
        if(!p.level().isLoaded(pos) || !(p.level().getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.AbstractFurnaceBlock))throw new IllegalArgumentException("Select a loaded furnace, smoker or blast furnace");
        validateMaterials(count);begin("smelt",count);
        try{SurvivalTools.useBlock(p,pos);}catch(IllegalArgumentException blocked){
            var candidates=new java.util.ArrayList<net.minecraft.world.phys.Vec3>();
            for(int x=-3;x<=3;x++)for(int y=-3;y<=2;y++)for(int z=-3;z<=3;z++){
                var cell=pos.offset(x,y,z);var feet=runtime.collection().standingFeet(cell);if(feet==null || feet.distanceToSqr(p.position())<.05)continue;
                if(SurvivalTools.canUseFrom(p,pos,feet))candidates.add(feet);
            }
            approaches=candidates.stream().distinct().sorted(java.util.Comparator.comparingDouble(p.position()::distanceToSqr)).limit(8).toList();approachIndex=0;
            if(approaches.isEmpty()){finish("BLOCKED","No checked native approach to the furnace; no materials transferred");return status();}
            if(travel==null)travel=new dev.mcai.companion.agent.navigation.NativeTravel(runtime);travel.start(approaches.get(0));step="APPROACH";return status();
        }
        loadFurnace();return status();
    }
    private void validateMaterials(int count){var p=runtime.player();
        if(!p.level().fuelValues().isFuel(new ItemStack(burn)))throw new IllegalArgumentException("Selected item is not native furnace fuel");
        if(NativeInventory.count(p.getInventory(),input)<count || NativeInventory.count(p.getInventory(),burn)<fuelCount || input==burn && NativeInventory.count(p.getInventory(),input)<count+fuelCount)
            throw new IllegalStateException("Insufficient carried input or fuel");
    }
    private void loadFurnace(){var p=runtime.player();
        try{
            validateMaterials(wanted);if(!(p.containerMenu instanceof AbstractFurnaceMenu))throw new IllegalArgumentException("Block did not open a furnace menu");
            var menu=p.containerMenu;
            if(!menu.slots.get(0).getItem().isEmpty() || !menu.slots.get(2).getItem().isEmpty())throw new IllegalStateException("Furnace contains previous input/output; inspect and handle it first");
            moveFromInventory(input,0,wanted);moveFromInventory(burn,1,fuelCount);menu.broadcastChanges();
            if(menu.slots.get(0).getItem().getCount()!=wanted)throw new IllegalStateException("Native furnace input transfer incomplete; inspect actual slots");
            step="COOK";
        }catch(RuntimeException failure){finish("BLOCKED",failure.getMessage());}
    }
    private void moveFromInventory(Item item,int destination,int count){
        var p=runtime.player();var menu=p.containerMenu;int remaining=count;
        for(var slot:menu.slots){if(slot.container!=p.getInventory() || !slot.getItem().is(item) || !NativeInventory.usable(p,slot.getItem()))continue;
            int before=menu.slots.get(destination).getItem().getCount();MenuOps.dripInto(menu,p,slot.index,destination,remaining);
            remaining-=Math.max(0,menu.slots.get(destination).getItem().getCount()-before);if(remaining==0)return;
        }
        if(remaining>0)throw new IllegalStateException("Native menu refused some materials; prior transfers remain in the menu");
    }
    private void begin(String op,int count){request=java.util.UUID.randomUUID();operation=op;phase="EXECUTING";reason="";started=runtime.server().getTickCount();ended=started;wanted=count;collected=0;step=op.equals("eat")?"EAT":"";}
    public void tick(){
        if(!active())return;var p=runtime.player();int elapsed=runtime.server().getTickCount()-started;
        if(!p.isAlive()){finish("BLOCKED","Body is not alive");return;}
        if(operation.equals("eat")){
            if(!p.isUsingItem()){
                collected=before-NativeInventory.count(p.getInventory(),input);
                finish(collected==1?"COMPLETED":"BLOCKED",collected==1?"Food consumed through native item use":"Food use stopped before consumption");
            }else if(elapsed>100){p.stopUsingItem();finish("BLOCKED","Food use timeout");}
            return;
        }
        if(step.equals("APPROACH")){
            travel.tick();if(travel.phase().equals("COMPLETED")){
                travel.cancel();try{SurvivalTools.useBlock(p,station);loadFurnace();return;}catch(IllegalArgumentException failure){reason=failure.getMessage();}
            }else if(!travel.phase().equals("FAILED")){if(elapsed>2400)finish("BLOCKED","Furnace approach exceeded its two-minute budget");return;}
            if(++approachIndex<approaches.size()){travel.start(approaches.get(approachIndex));return;}
            finish("BLOCKED","Checked furnace approaches exhausted; no new input was transferred");return;
        }
        var menu=p.containerMenu;
        if(!(menu instanceof AbstractFurnaceMenu furnace) || !menu.stillValid(p)){finish("BLOCKED","Furnace menu closed or body moved out of reach; loaded items remain in the furnace");return;}
        if(elapsed%5==0){
            var result=menu.slots.get(2).getItem();
            if(!result.isEmpty()){
                if(output!=null && output!=result.getItem()){finish("BLOCKED","Furnace output changed unexpectedly");return;}
                output=result.getItem();int beforeCount=NativeInventory.count(p.getInventory(),output);menu.clicked(2,0,ContainerInput.QUICK_MOVE,p);menu.broadcastChanges();
                int gained=NativeInventory.count(p.getInventory(),output)-beforeCount;collected+=Math.max(0,gained);
                if(gained<=0){finish("BLOCKED","Furnace output cannot fit in inventory");return;}
                if(collected>=wanted){p.closeContainer();finish("COMPLETED","Native furnace output collected into inventory");return;}
            }
            if(elapsed>40 && !furnace.isLit() && menu.slots.get(1).getItem().isEmpty()){finish("BLOCKED","Furnace ran out of fuel; partial results and input remain real");return;}
        }
        if(elapsed>Math.min(14400,400+wanted*300)){finish("BLOCKED","Furnace made no complete receipt before the bounded deadline");}
    }
    public void beforePhysics(){if(!active())return;if(step.equals("APPROACH") && travel!=null)travel.beforePhysics();else runtime.player().stopControlling();}
    private void finish(String state,String message){if(travel!=null)travel.cancel();ended=runtime.server().getTickCount();phase=state;reason=message==null?"Native survival action failed":message;}
    public JsonObject cancel(){if(active()){runtime.player().stopUsingItem();runtime.player().closeContainer();finish("CANCELLED","Stopped by player; already consumed or transferred items are preserved");}return status();}
    public JsonObject status(){var out=new JsonObject();out.addProperty("phase",phase);out.addProperty("operation",operation);out.addProperty("step",step);out.addProperty("elapsedTicks",request==null?0:(active()?runtime.server().getTickCount():ended)-started);out.addProperty("ownsBody",active());if(request!=null)out.addProperty("requestId",request.toString());out.addProperty("wanted",wanted);out.addProperty("collected",collected);out.addProperty("reason",reason);
        if(input!=null)out.addProperty("input",BuiltInRegistries.ITEM.getKey(input).toString());if(output!=null)out.addProperty("output",BuiltInRegistries.ITEM.getKey(output).toString());
        if(operation.equals("eat")){out.addProperty("foodBefore",foodBefore);out.addProperty("foodNow",runtime.player().getFoodData().getFoodLevel());}
        if(station!=null && operation.equals("smelt")){var pos=new JsonObject();pos.addProperty("x",station.getX());pos.addProperty("y",station.getY());pos.addProperty("z",station.getZ());out.add("station",pos);}return out;}
    @Override public void close(){cancel();if(travel!=null)travel.close();}
}
