package com.gunmu.northeast_china_delight.loot;

import com.gunmu.northeast_china_delight.item.ModItems;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
//? if <1.20.2 {
/*import net.minecraftforge.common.loot.IGlobalLootModifier;
*///?} else {
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
//?}
//? if <1.20.2 {
/*import net.minecraftforge.common.loot.LootModifier;
*///?} else {
import net.neoforged.neoforge.common.loot.LootModifier;
//?}

import java.util.ArrayList;
import java.util.List;

/**
 * 猪副产物掉落：用小刀宰杀猪时，每种副产物各按基础概率判定，
 * 有抢夺附魔时每级加概率；最多掉 2 种。着火的猪掉熏制版本（若有）。
 */
public class PigOffalLootModifier extends LootModifier {

    /** 基础概率（无抢夺） */
    public static final float BASE_CHANCE = 0.1F;
    /** 每级抢夺增加的概率 */
    public static final float LOOTING_BONUS = 0.05F;
    public static final int MAX_KINDS = 2;

    //? if <1.20.5 {
    /*public static final Codec<PigOffalLootModifier> CODEC = RecordCodecBuilder.create(
            instance -> codecStart(instance).apply(instance, PigOffalLootModifier::new));
    *///?} else {
    public static final MapCodec<PigOffalLootModifier> CODEC = RecordCodecBuilder.mapCodec(
            instance -> codecStart(instance).apply(instance, PigOffalLootModifier::new));
    //?}

    /** 普通 → 着火时的熏制替换（没有熏制版的不换），惰性初始化防止注册表未绑定 */
    private static java.util.Map<Item, Item> smokedMap = null;

    private static java.util.Map<Item, Item> getSmokedMap() {
        if (smokedMap == null) {
            smokedMap = new java.util.HashMap<>();
            smokedMap.put(ModItems.PIG_LIVER.get(), ModItems.SMOKED_PIG_LIVER.get());
            smokedMap.put(ModItems.PORK_HOCK.get(), ModItems.SMOKED_PORK_HOCK.get());
        }
        return smokedMap;
    }

    public PigOffalLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        List<Item> candidates = new ArrayList<>(List.of(
                ModItems.PORK_RIBS.get(),
                ModItems.PORK_INTESTINE.get(),
                ModItems.PORK_HOCK.get(),
                ModItems.PIG_BLOOD.get(),
                ModItems.PIG_LIVER.get(),
                ModItems.PORK_FAT.get(),
                ModItems.OIL_EDGE.get()));

        RandomSource random = context.getRandom();
        // 抢夺附魔加成：每级 +5%
        int lootingLevel = getLootingLevel(context);
        float chance = BASE_CHANCE + lootingLevel * LOOTING_BONUS;

        // 着火的猪：换成熏制版本
        var thisEntity = context.getParamOrNull(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY);
        boolean onFire = thisEntity != null && thisEntity.isOnFire();

        // 打乱顺序，保证不同副产物随机出
        for (int i = candidates.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Item tmp = candidates.get(i);
            candidates.set(i, candidates.get(j));
            candidates.set(j, tmp);
        }

        int kinds = 0;
        for (Item item : candidates) {
            if (kinds >= MAX_KINDS) {
                break;
            }
            if (random.nextFloat() < chance) {
                Item drop = onFire ? getSmokedMap().getOrDefault(item, item) : item;
                generatedLoot.add(new ItemStack(drop));
                kinds++;
            }
        }
        return generatedLoot;
    }

    /** 获取攻击者的抢夺附魔等级 */
    private static int getLootingLevel(LootContext context) {
        //? if <1.20.5 {
        /*var attacker = context.getParamOrNull(net.minecraft.world.level.storage.loot.parameters.LootContextParams.KILLER_ENTITY);
        *///?} else {
        var attacker = context.getParamOrNull(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ATTACKING_ENTITY);
        //?}
        if (!(attacker instanceof net.minecraft.world.entity.LivingEntity living)) {
            return 0;
        }
        //? if <1.20.5 {
        /*return net.minecraft.world.item.enchantment.EnchantmentHelper.getMobLooting(living);
        *///?} else {
        var registry = context.getLevel().registryAccess()
                .lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        var looting = registry.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING);
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(looting, living);
        //?}
    }    @Override
    //? if <1.20.5 {
    /*public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }*/
    //?} else {
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
    //?}
}
