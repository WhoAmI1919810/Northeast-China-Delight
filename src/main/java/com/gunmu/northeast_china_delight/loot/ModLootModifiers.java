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
 * 本模组的全局掉落修改器。
 *
 * <p>1.20.4 的序列化注册表要的是 {@code Codec}，1.21.1 起改成 {@code MapCodec}，
 * 所以注册类型按版本分流；顺手把没人引用的 DeferredHolder 常量收成 private 注册调用。</p>
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
        // 猪副产物：小刀宰杀时每种 10%、最多 2 种
        add("pig_offal", PigOffalLootModifier.CODEC);
        // 三张钓鱼表：带鱼（冷水深海）、生蚝（河口近海）、大虾（其它任何水域）
        add("fishing", NortheastFishingLootModifier.CODEC);
        // 结构宝箱（神殿/前哨站/矿井/神庙/沉船/雪屋/海底废墟/村庄非住宅）小概率塞种子
        add("structure_seeds", StructureSeedsLootModifier.CODEC);
        // 屠夫箱：小概率塞肉类食材和肉菜
        add("butcher_loot", ButcherLootModifier.CODEC);
        // 渔夫箱：小概率塞水产食材和海味菜
        add("fisherman_loot", FishermanLootModifier.CODEC);
        // 村庄住宅箱子：按群系塞东北乐事的种子和谷物
        add("village_seeds", VillageSeedsLootModifier.CODEC);

        LOOT_MODIFIERS.register(eventBus);
    }
}
