package com.gunmu.northeast_china_delight.loot;

import com.gunmu.northeast_china_delight.item.ModItems;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * 渔夫（village_fisher）箱子：小概率塞东北乐事的水产食材和海味菜。
 *
 * <p>先按 ~18% 概率出一份「生鲜」（大虾/生蚝/带鱼/海参），
 * 再按 ~10% 概率出一份「海味菜」（咸鱼/咸鱼饼子/葱烧海参/酸菜海鲜锅）。</p>
 */
public class FishermanLootModifier extends LootModifier
{
    public static final MapCodec<FishermanLootModifier> CODEC = RecordCodecBuilder.mapCodec(
            instance -> codecStart(instance).apply(instance, FishermanLootModifier::new));

    /** 生鲜水产 */
    private static final Entry[] RAW = {
            new Entry(() -> ModItems.SHRIMP.get(), 9),
            new Entry(() -> ModItems.OYSTER.get(), 8),
            new Entry(() -> ModItems.HAIRTAIL.get(), 6),
            new Entry(() -> ModItems.SEA_CUCUMBER.get(), 5)
    };

    /** 海味成品 */
    private static final Entry[] DISH = {
            new Entry(() -> ModItems.SALTED_FISH.get(), 8),
            new Entry(() -> ModItems.XIAN_YU_BING_ZI.get(), 6),
            new Entry(() -> ModItems.CONG_SHAO_HAI_SHEN.get(), 4),
            new Entry(() -> ModItems.SUAN_CAI_HAI_XIAN_GUO.get(), 4)
    };

    public FishermanLootModifier(LootItemCondition[] conditions)
    {
        super(conditions);
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec()
    {
        return CODEC;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context)
    {
        var random = context.getRandom();
        if (random.nextFloat() < 0.18F)
        {
            generatedLoot.add(roll(random, RAW, 1, 2));
        }
        if (random.nextFloat() < 0.10F)
        {
            generatedLoot.add(roll(random, DISH, 1, 1));
        }
        return generatedLoot;
    }

    private static ItemStack roll(net.minecraft.util.RandomSource random, Entry[] table, int min, int max)
    {
        int total = 0;
        for (Entry entry : table) { total += entry.weight; }
        int roll = random.nextInt(total);
        for (Entry entry : table)
        {
            roll -= entry.weight;
            if (roll < 0)
            {
                int count = min + (max > min ? random.nextInt(max - min + 1) : 0);
                return new ItemStack(entry.item.get(), count);
            }
        }
        return ItemStack.EMPTY;
    }

    private record Entry(java.util.function.Supplier<Item> item, int weight) {}
}
