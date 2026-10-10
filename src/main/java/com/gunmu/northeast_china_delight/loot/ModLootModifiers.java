package com.gunmu.northeast_china_delight.loot;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
//? if <1.20.2 {
import net.minecraftforge.eventbus.api.IEventBus;
//?} else {
/*import net.neoforged.bus.api.IEventBus;
*///?}
//? if <1.20.2 {
import net.minecraftforge.common.loot.IGlobalLootModifier;
//?} else {
/*import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
*///?}
//? if <1.20.2 {
import net.minecraftforge.registries.DeferredRegister;
//?} else {
/*import net.neoforged.neoforge.registries.DeferredRegister;
*///?}
//? if <1.20.2 {
import net.minecraftforge.registries.ForgeRegistries;
//?} else {
/*import net.neoforged.neoforge.registries.NeoForgeRegistries;
*///?}

/**
 * 本模组的全局掉落修改器。 1.20.1 的序列化注册表要的是 {@code Codec}，1.21.1 起改成 {@code MapCodec}，所以注册类型按版本分流；顺手把
 * 没人引用的 DeferredHolder 常量收成 private 注册调用。
 */
public class ModLootModifiers {

    //? if <1.20.2 {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, NortheastChinaDelight.MODID);

    private static void add(String name, Codec<? extends IGlobalLootModifier> codec) {
        LOOT_MODIFIERS.register(name, () -> codec);
    }
    //?} else if <1.20.5 {
    /*public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, NortheastChinaDelight.MODID);

    private static void add(String name, Codec<? extends IGlobalLootModifier> codec) {
        LOOT_MODIFIERS.register(name, () -> codec);
    }*/
    //?} else {
    /*public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, NortheastChinaDelight.MODID);

    private static void add(String name, MapCodec<? extends IGlobalLootModifier> codec) {
        LOOT_MODIFIERS.register(name, () -> codec);
    }
    *///?}

    public static void register(IEventBus eventBus) {
        add("pig_offal", PigOffalLootModifier.CODEC);
        add("fishing", NortheastFishingLootModifier.CODEC);
        add("structure_seeds", StructureSeedsLootModifier.CODEC);
        add("butcher_loot", ButcherLootModifier.CODEC);
        add("fisherman_loot", FishermanLootModifier.CODEC);
        add("village_seeds", VillageSeedsLootModifier.CODEC);

        LOOT_MODIFIERS.register(eventBus);
    }
}
