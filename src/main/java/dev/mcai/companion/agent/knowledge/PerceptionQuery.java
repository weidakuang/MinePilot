package dev.mcai.companion.agent.knowledge;

import java.util.Locale;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.state.BlockState;

/** Exact noun aliases; unknown text remains a filter, never an invented world result. */
public final class PerceptionQuery {
    private PerceptionQuery() {}
    private static final Map<String,String> ALIASES=Map.ofEntries(
        Map.entry("石头","stone"),Map.entry("石块","stone"),Map.entry("圆石","cobblestone"),Map.entry("煤矿","coal_ore"),Map.entry("煤炭","coal"),
        Map.entry("铁矿","iron_ore"),Map.entry("铜矿","copper_ore"),Map.entry("金矿","gold_ore"),Map.entry("钻石矿","diamond_ore"),Map.entry("红石矿","redstone_ore"),
        Map.entry("绿宝石","emerald"),Map.entry("铁锭","iron_ingot"),Map.entry("鸡蛋","egg"),Map.entry("工作台","crafting_table"),
        Map.entry("床","_bed"),Map.entry("钟","bell"),Map.entry("门","_door"),Map.entry("箱子","chest"),Map.entry("草","grass"),
        Map.entry("村民","villager"),Map.entry("普通村民","villager"),Map.entry("铁傀儡","iron_golem"),Map.entry("铁巨人","iron_golem"),
        Map.entry("农民","farmer"),Map.entry("傻子","nitwit"),Map.entry("傻子村民","nitwit"),Map.entry("石匠","mason"),Map.entry("牧羊人","shepherd"),
        Map.entry("玩家","player"),Map.entry("僵尸","zombie"),Map.entry("羊","sheep"),Map.entry("牛","cow"),Map.entry("猪","pig"),Map.entry("鸡","chicken"));
    public static String normalize(String text){String key=text.strip().toLowerCase(Locale.ROOT);return ALIASES.getOrDefault(key,key);}
    public static boolean block(BlockState state,String query){
        String id=BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(),term=normalize(query);
        // Generic stone is not redstone, sandstone or every decorative stone variant.
        if(query.equals("石头") || query.equals("石块"))return id.equals("minecraft:stone") || id.equals("minecraft:deepslate");
        return id.contains(term);
    }
    public static boolean entity(Entity entity,String query){
        if(query.equals("普通村民") && entity instanceof Villager v && v.getVillagerData().profession().unwrapKey().map(k->k.identifier().getPath().equals("nitwit")).orElse(false))return false;
        String text=BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())+" "+entity.getName().getString();
        if(entity instanceof ItemEntity item)text+=" "+BuiltInRegistries.ITEM.getKey(item.getItem().getItem());
        if(entity instanceof Villager v)text+=" "+v.getVillagerData().profession().unwrapKey().map(k->k.identifier().toString()).orElse("");
        return text.toLowerCase(Locale.ROOT).contains(normalize(query));
    }
}
