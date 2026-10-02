package com.gunmu.northeast_china_delight.loot;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** 本模组的全局掉落修改器 */
public class ModLootModifiers {

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, NortheastChinaDelight.MODID);

    /** 猪副产物：小刀宰杀时每种 10%、最多 2 种 */
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<PigOffalLootModifier>> PIG_OFFAL =
            LOOT_MODIFIERS.register("pig_offal", () -> PigOffalLootModifier.CODEC);

    /** 三张钓鱼表：带鱼（冷水深海）、生蚝（河口近海）、大虾（其它任何水域） */
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<NortheastFishingLootModifier>> FISHING =
            LOOT_MODIFIERS.register("fishing", () -> NortheastFishingLootModifier.CODEC);

    public static void register(IEventBus eventBus) {
        LOOT_MODIFIERS.register(eventBus);
    }
}
