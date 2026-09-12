package dev.mcai.companion.agent.knowledge;
import net.minecraft.sounds.*;

/** Native 26.2 LevelEventHandler sound/volume table, excluding global and data-dependent effects. */
final class LevelEventSounds {
    record Event(SoundEvent sound,SoundSource category,float volume) {}
    static Event resolve(int id,int data){
        if(id==1009)return data==0?new Event(SoundEvents.FIRE_EXTINGUISH,SoundSource.BLOCKS,.5F):data==1?new Event(SoundEvents.GENERIC_EXTINGUISH_FIRE,SoundSource.BLOCKS,.7F):null;
        if(id==1500)return new Event(data>0?SoundEvents.COMPOSTER_FILL_SUCCESS:SoundEvents.COMPOSTER_FILL,SoundSource.BLOCKS,1);
        return switch(id){
            case 1000 -> new Event(SoundEvents.DISPENSER_DISPENSE,SoundSource.BLOCKS,1.0F);
            case 1001 -> new Event(SoundEvents.DISPENSER_FAIL,SoundSource.BLOCKS,1.0F);
            case 1002 -> new Event(SoundEvents.DISPENSER_LAUNCH,SoundSource.BLOCKS,1.0F);
            case 1004 -> new Event(SoundEvents.FIREWORK_ROCKET_SHOOT,SoundSource.NEUTRAL,1.0F);
            case 1015 -> new Event(SoundEvents.GHAST_WARN,SoundSource.HOSTILE,10.0F);
            case 1016 -> new Event(SoundEvents.GHAST_SHOOT,SoundSource.HOSTILE,10.0F);
            case 1017 -> new Event(SoundEvents.ENDER_DRAGON_SHOOT,SoundSource.HOSTILE,10.0F);
            case 1018 -> new Event(SoundEvents.BLAZE_SHOOT,SoundSource.HOSTILE,2.0F);
            case 1019 -> new Event(SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR,SoundSource.HOSTILE,2.0F);
            case 1020 -> new Event(SoundEvents.ZOMBIE_ATTACK_IRON_DOOR,SoundSource.HOSTILE,2.0F);
            case 1021 -> new Event(SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,SoundSource.HOSTILE,2.0F);
            case 1022 -> new Event(SoundEvents.WITHER_BREAK_BLOCK,SoundSource.HOSTILE,2.0F);
            case 1024 -> new Event(SoundEvents.WITHER_SHOOT,SoundSource.HOSTILE,2.0F);
            case 1025 -> new Event(SoundEvents.BAT_TAKEOFF,SoundSource.NEUTRAL,0.05F);
            case 1026 -> new Event(SoundEvents.ZOMBIE_INFECT,SoundSource.HOSTILE,2.0F);
            case 1027 -> new Event(SoundEvents.ZOMBIE_VILLAGER_CONVERTED,SoundSource.HOSTILE,2.0F);
            case 1029 -> new Event(SoundEvents.ANVIL_DESTROY,SoundSource.BLOCKS,1.0F);
            case 1030 -> new Event(SoundEvents.ANVIL_USE,SoundSource.BLOCKS,1.0F);
            case 1031 -> new Event(SoundEvents.ANVIL_LAND,SoundSource.BLOCKS,0.3F);
            case 1033 -> new Event(SoundEvents.CHORUS_FLOWER_GROW,SoundSource.BLOCKS,1.0F);
            case 1034 -> new Event(SoundEvents.CHORUS_FLOWER_DEATH,SoundSource.BLOCKS,1.0F);
            case 1035 -> new Event(SoundEvents.BREWING_STAND_BREW,SoundSource.BLOCKS,1.0F);
            case 1039 -> new Event(SoundEvents.PHANTOM_BITE,SoundSource.HOSTILE,0.3F);
            case 1040 -> new Event(SoundEvents.ZOMBIE_CONVERTED_TO_DROWNED,SoundSource.HOSTILE,2.0F);
            case 1041 -> new Event(SoundEvents.HUSK_CONVERTED_TO_ZOMBIE,SoundSource.HOSTILE,2.0F);
            case 1042 -> new Event(SoundEvents.GRINDSTONE_USE,SoundSource.BLOCKS,1.0F);
            case 1043 -> new Event(SoundEvents.BOOK_PAGE_TURN,SoundSource.BLOCKS,1.0F);
            case 1044 -> new Event(SoundEvents.SMITHING_TABLE_USE,SoundSource.BLOCKS,1.0F);
            case 1045 -> new Event(SoundEvents.POINTED_DRIPSTONE_LAND,SoundSource.BLOCKS,2.0F);
            case 1046 -> new Event(SoundEvents.POINTED_DRIPSTONE_DRIP_LAVA_INTO_CAULDRON,SoundSource.BLOCKS,2.0F);
            case 1047 -> new Event(SoundEvents.POINTED_DRIPSTONE_DRIP_WATER_INTO_CAULDRON,SoundSource.BLOCKS,2.0F);
            case 1048 -> new Event(SoundEvents.SKELETON_CONVERTED_TO_STRAY,SoundSource.HOSTILE,2.0F);
            case 1049 -> new Event(SoundEvents.CRAFTER_CRAFT,SoundSource.BLOCKS,1.0F);
            case 1050 -> new Event(SoundEvents.CRAFTER_FAIL,SoundSource.BLOCKS,1.0F);
            case 1051 -> new Event(SoundEvents.WIND_CHARGE_THROW,SoundSource.BLOCKS,0.5F);
            case 1052 -> new Event(SoundEvents.SULFUR_SPIKE_LAND,SoundSource.BLOCKS,2.0F);
            case 1501 -> new Event(SoundEvents.LAVA_EXTINGUISH,SoundSource.BLOCKS,0.5F);
            case 1502 -> new Event(SoundEvents.REDSTONE_TORCH_BURNOUT,SoundSource.BLOCKS,0.5F);
            case 1503 -> new Event(SoundEvents.END_PORTAL_FRAME_FILL,SoundSource.BLOCKS,1.0F);
            case 1505 -> new Event(SoundEvents.BONE_MEAL_USE,SoundSource.BLOCKS,1.0F);
            default -> null;
        };
    }
}
