package dev.mcai.companion.agent.knowledge;

import com.google.gson.*;
import java.util.*;
import dev.mcai.companion.agent.body.MinePilotServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Observes balanced native menu transfers, including the carried cursor. No slot mutation. */
public final class ContainerOrigins {
    private static final ThreadLocal<Boolean> AUTOMATED=ThreadLocal.withInitial(()->false);
    private static final Map<Object,JsonObject> VOLATILE=new WeakHashMap<>();
    private static final ThreadLocal<Container> DROPPING=new ThreadLocal<>();
    private static String key(Player p,ItemStack stack){var copy=stack.copyWithCount(1);if(copy.isDamageableItem())copy.setDamageValue(0);return InventoryLedger.fingerprint(p,copy);}
    private static JsonArray unknown(int n){var rows=new JsonArray();if(n>0){var row=new JsonObject();row.addProperty("count",n);var source=new JsonObject();source.addProperty("category","unknown");source.addProperty("reason","No recorded container transfer covers this quantity");row.add("source",source);rows.add(row);}return rows;}
    private static int count(JsonArray rows){return rows.asList().stream().mapToInt(v->v.getAsJsonObject().get("count").getAsInt()).sum();}
    private static JsonObject data(Object owner){
        if(owner instanceof BlockEntity block){String text=((StoredOrigins)block).minepilot$getOrigins();if(!text.isEmpty() && text.length()<=500_000)try{return JsonParser.parseString(text).getAsJsonObject();}catch(RuntimeException ignored){}return new JsonObject();}
        return VOLATILE.getOrDefault(owner,new JsonObject()).deepCopy();
    }
    private static void save(Object owner,JsonObject data){
        String value=data.toString();if(value.length()>500_000)data=new JsonObject();
        if(owner instanceof BlockEntity block){((StoredOrigins)block).minepilot$setOrigins(data.toString());block.setChanged();}
        else VOLATILE.put(owner,data);
    }
    private static JsonArray bounded(JsonArray rows,int amount){
        JsonArray result=new JsonArray();int left=amount;
        try{for(var value:rows){var r=value.getAsJsonObject();if(!r.getAsJsonObject("source").has("category"))throw new IllegalArgumentException();int n=Math.min(left,Math.max(0,r.get("count").getAsInt()));if(n>0 && result.size()<64){var copy=r.deepCopy();copy.addProperty("count",n);result.add(copy);left-=n;}}}catch(RuntimeException invalid){result=new JsonArray();left=amount;}
        for(var row:unknown(left))result.add(row);return result;
    }
    private static final class Store {
        final Object owner;final Player p;final Map<String,ItemStack> prototypes=new LinkedHashMap<>();
        final Map<String,Integer> before;final Map<String,JsonArray> origins=new HashMap<>();
        Store(Object owner,Player p){this.owner=owner;this.p=p;before=counts();var saved=owner==p.getInventory()?null:data(owner);
            for(var e:before.entrySet()){
                JsonArray rows=owner instanceof net.minecraft.world.entity.item.ItemEntity item?DropProvenance.segments(item,e.getValue()):owner==p.getInventory()?CarriedProvenance.describe(p,e.getKey(),e.getValue()).getAsJsonArray("lineage"):
                    saved.has(e.getKey()) && saved.get(e.getKey()).isJsonArray()?saved.getAsJsonArray(e.getKey()):new JsonArray();
                origins.put(e.getKey(),bounded(rows,e.getValue()));
            }
        }
        Map<String,Integer> counts(){var result=new LinkedHashMap<String,Integer>();
            if(owner instanceof net.minecraft.world.entity.item.ItemEntity item){var stack=item.getItem();if(item.isAlive() && !stack.isEmpty()){String id=key(p,stack);result.put(id,stack.getCount());prototypes.put(id,stack.copyWithCount(1));}return result;}
            int size=owner instanceof Container c?Math.min(c.getContainerSize(),256):1;
            for(int i=0;i<size;i++){var s=owner instanceof Container c?c.getItem(i):((AbstractContainerMenu)owner).getCarried();if(!s.isEmpty()){String id=key(p,s);result.merge(id,s.getCount(),Integer::sum);prototypes.putIfAbsent(id,s.copyWithCount(1));}}return result;
        }
        void saveAll(Map<String,Integer> now){var saved=new JsonObject();for(var e:now.entrySet()){
            var rows=bounded(origins.getOrDefault(e.getKey(),new JsonArray()),e.getValue());
            if(owner==p.getInventory())CarriedProvenance.assign(p,e.getKey(),rows);else saved.add(e.getKey(),rows);
        }if(owner instanceof net.minecraft.world.entity.item.ItemEntity item){var rows=new JsonArray();for(var value:saved.entrySet())for(var row:value.getValue().getAsJsonArray())rows.add(row);DropProvenance.save(item,rows);}else if(owner!=p.getInventory())save(owner,saved);}
        JsonObject transferCause(){
            var source=new JsonObject();source.addProperty("kind",AUTOMATED.get()?"native_hopper_transfer":"native_menu_transfer");if(!AUTOMATED.get()){source.addProperty("actorId",p.getUUID().toString());source.addProperty("actorName",InventoryLedger.bounded(p.getName().getString(),128));}var level=owner instanceof BlockEntity b && b.getLevel()!=null?b.getLevel():owner instanceof net.minecraft.world.entity.Entity e?e.level():p.level();source.addProperty("gameTick",level.getGameTime());source.addProperty("dimension",level.dimension().identifier().toString());
            if(owner instanceof BlockEntity block){source.addProperty("containerBlock",net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block.getBlockState().getBlock()).toString());source.addProperty("x",block.getBlockPos().getX());source.addProperty("y",block.getBlockPos().getY());source.addProperty("z",block.getBlockPos().getZ());}
            else if(owner instanceof net.minecraft.world.entity.item.ItemEntity item)source.addProperty("entityId",item.getUUID().toString());
            else source.addProperty("container",owner==p.getInventory()?"player_inventory":owner instanceof AbstractContainerMenu?"menu_cursor":"transient_or_custom_container");return source;
        }
    }
    public static final class Transaction {
        final List<Store> stores;final Player p;
        Transaction(AbstractContainerMenu menu,Player p){this.p=p;CarriedProvenance.sync(p);var containers=new ArrayList<Object>();for(var slot:menu.slots)addPhysical(containers,slot.container);addPhysical(containers,menu);stores=containers.stream().map(c->new Store(c,p)).toList();}
        Transaction(List<Object> owners,Player p){this.p=p;stores=owners.stream().map(c->new Store(c,p)).toList();}
        public void finish(){
            if(stores.stream().allMatch(store->store.before.equals(store.counts())))return;
            var now=new IdentityHashMap<Store,Map<String,Integer>>();var removed=new LinkedHashMap<String,JsonArray>();
            for(var store:stores){var current=store.counts();now.put(store,current);for(var e:store.before.entrySet()){
                int n=Math.max(0,e.getValue()-current.getOrDefault(e.getKey(),0));if(n==0)continue;
                var outgoing=DropProvenance.take(store.origins.get(e.getKey()),n);var cause=store.transferCause();
                for(var v:outgoing){var source=v.getAsJsonObject().getAsJsonObject("source");var history=source.has("transfers")?source.getAsJsonArray("transfers"):new JsonArray();if(history.size()<8)history.add(cause.deepCopy());else source.addProperty("transferHistoryTruncated",true);source.add("transfers",history);source.add("lastTransfer",cause.deepCopy());removed.computeIfAbsent(e.getKey(),k->new JsonArray()).add(v);}
            }}
            for(var store:stores){for(var e:now.get(store).entrySet()){
                int n=e.getValue()-store.before.getOrDefault(e.getKey(),0);if(n<=0)continue;
                var moved=DropProvenance.take(removed.computeIfAbsent(e.getKey(),k->new JsonArray()),n);int missing=n-count(moved);for(var v:unknown(missing))moved.add(v);
                for(var v:moved)store.origins.computeIfAbsent(e.getKey(),k->new JsonArray()).add(v.deepCopy());
                if(store.owner==p.getInventory() && p instanceof MinePilotServerPlayer body && body.inventoryLedger!=null){
                    var source=DropProvenance.summarize(moved,p);body.inventoryLedger.granted(store.prototypes.get(e.getKey()).copyWithCount(n),source);
                }
            }store.saveAll(now.get(store));}
            if(!AUTOMATED.get())CarriedProvenance.sync(p);
        }
    }
    private static void addPhysical(List<Object> list,Object owner){
        if(owner instanceof dev.mcai.companion.mixin.CompoundContainerAccess compound){addPhysical(list,compound.minepilot$first());addPhysical(list,compound.minepilot$second());}
        else if(owner!=null && list.stream().noneMatch(v->v==owner))list.add(owner);
    }
    private static void addAt(List<Object> owners,net.minecraft.server.level.ServerLevel level,net.minecraft.core.BlockPos pos){
        if(!level.hasChunkAt(pos))return;var block=level.getBlockEntity(pos);if(block instanceof Container)addPhysical(owners,block);
        // Include physical halves without selecting a random native inventory or creating chunks.
        if(block instanceof net.minecraft.world.level.block.entity.ChestBlockEntity)for(var d:net.minecraft.core.Direction.Plane.HORIZONTAL){var other=level.getBlockEntity(pos.relative(d));if(other instanceof net.minecraft.world.level.block.entity.ChestBlockEntity)addPhysical(owners,other);}
        if(block==null)for(var entity:level.getEntities((net.minecraft.world.entity.Entity)null,new net.minecraft.world.phys.AABB(pos),e->e instanceof Container))addPhysical(owners,entity);
    }
    public static AutoCloseable hopper(net.minecraft.world.level.Level world,net.minecraft.core.BlockPos pos,net.minecraft.world.level.block.state.BlockState state,Container hopper){
        if(AUTOMATED.get() || !(world instanceof net.minecraft.server.level.ServerLevel level))return ()->{};
        var runtime=dev.mcai.companion.agent.AgentRuntime.active(level.getServer());if(runtime==null)return ()->{};
        var owners=new ArrayList<Object>();addPhysical(owners,hopper);addAt(owners,level,pos.above());addAt(owners,level,pos.relative(state.getValue(net.minecraft.world.level.block.HopperBlock.FACING)));
        for(var item:level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).expandTowards(0,1,0)))if(owners.size()<72)addPhysical(owners,item);
        return automate(owners,runtime.player());
    }
    public static AutoCloseable hopperPickup(Container container,net.minecraft.world.entity.item.ItemEntity item){
        if(AUTOMATED.get() || !(item.level() instanceof net.minecraft.server.level.ServerLevel level))return ()->{};
        var runtime=dev.mcai.companion.agent.AgentRuntime.active(level.getServer());if(runtime==null)return ()->{};
        var owners=new ArrayList<Object>();addPhysical(owners,container);addPhysical(owners,item);return automate(owners,runtime.player());
    }
    private static AutoCloseable automate(List<Object> owners,Player context){
        boolean tracked=owners.stream().anyMatch(o->o instanceof BlockEntity b && !((StoredOrigins)b).minepilot$getOrigins().isEmpty() || o instanceof net.minecraft.world.entity.item.ItemEntity i && (!i.getPersistentData().getString("minepilot.source").orElse("unknown").equals("unknown") || i.getPersistentData().contains("minepilot.lineage.v1")) || VOLATILE.containsKey(o));
        if(!tracked)return ()->{};
        AUTOMATED.set(true);try{var tx=new Transaction(owners,context);return ()->{try{tx.finish();}finally{AUTOMATED.remove();}};}catch(RuntimeException failure){AUTOMATED.remove();throw failure;}
    }
    public static Transaction before(AbstractContainerMenu menu,Player p){return new Transaction(menu,p);}
    public static AutoCloseable dropping(Container container){var previous=DROPPING.get();DROPPING.set(container);return ()->{if(previous==null)DROPPING.remove();else DROPPING.set(previous);};}
    public static JsonArray dropOrigins(Player p,ItemStack stack){
        var container=DROPPING.get();if(container==null || stack.isEmpty())return null;
        var store=new Store(container,p);String id=key(p,stack);var rows=store.origins.getOrDefault(id,unknown(stack.getCount()));var result=DropProvenance.take(rows,stack.getCount());
        var saved=data(container);saved.add(id,rows);save(container,saved);return result;
    }
}
