package com.gunmu.northeast_china_delight.loot;

import com.gunmu.northeast_china_delight.item.ModItems;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

import java.util.ArrayList;
import java.util.List;

/**
 * 猪副产物掉落：用小刀宰杀猪时，每种副产物各自 10% 概率，且一次最多掉 2 种。
 *
 * 以前是 4 条独立掉落（各 40%，可能一次全掉），现在合并成这一条统一处理，
 * 并且加上了猪肝。
 */
public class PigOffalLootModifier extends LootModifier {

    /** 每种副产物的掉落概率 */
    public static final float CHANCE = 0.1F;
    /** 一次最多掉几种 */
    public static final int MAX_KINDS = 2;

    public static final MapCodec<PigOffalLootModifier> CODEC = RecordCodecBuilder.mapCodec(
            instance -> codecStart(instance).apply(instance, PigOffalLootModifier::new));

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
        // 打乱顺序：保证"最多 2 种"不会总是偏向列表里靠前的几项
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
            if (random.nextFloat() < CHANCE) {
                generatedLoot.add(new ItemStack(item));
                kinds++;
            }
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
