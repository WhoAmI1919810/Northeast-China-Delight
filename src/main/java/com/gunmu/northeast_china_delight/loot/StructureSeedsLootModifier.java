package com.gunmu.northeast_china_delight.loot;

import com.gunmu.northeast_china_delight.item.ModItems;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
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

/**
 * 结构宝箱（沙漠神殿 / 掠夺者前哨站 / 废弃矿井 / 丛林神庙 / 沉船 / 雪屋 / 海底废墟，
 * 以及村庄里的非住宅箱）小概率塞东北乐事的种子与可种植谷物。
 *
 * <p>与村庄住宅用的 {@link VillageSeedsLootModifier} 不同：这一份概率刻意压低
 * （约 15% 出 1 份），并且把 15 种可种植物品全放进同一个池子——
 * 包括能直接当种子种的红薯、花生、大豆、红豆、荞麦。</p>
 */
public class StructureSeedsLootModifier extends LootModifier
{
    //? if <1.20.5 {
    /*public static final Codec<StructureSeedsLootModifier> CODEC = RecordCodecBuilder.create(
            instance -> codecStart(instance).apply(instance, StructureSeedsLootModifier::new));
    *///?} else {
    public static final MapCodec<StructureSeedsLootModifier> CODEC = RecordCodecBuilder.mapCodec(
            instance -> codecStart(instance).apply(instance, StructureSeedsLootModifier::new));
    //?}

    /** 全部可种植物品：种子类 + 能直接种的红薯/花生/大豆/红豆/荞麦 */
    private static final Entry[] SEEDS = {
            new Entry(() -> ModItems.CORN_SEEDS.get(), 8),
            new Entry(() -> ModItems.SOYBEAN.get(), 8),
            new Entry(() -> ModItems.NAPA_CABBAGE_SEEDS.get(), 7),
            new Entry(() -> ModItems.CUCUMBER_SEEDS.get(), 7),
            new Entry(() -> ModItems.GREEN_RADISH_SEEDS.get(), 7),
            new Entry(() -> ModItems.GREEN_ONION_SEEDS.get(), 7),
            new Entry(() -> ModItems.BUCKWHEAT.get(), 6),
            new Entry(() -> ModItems.RED_BEAN.get(), 6),
            new Entry(() -> ModItems.PEANUT.get(), 6),
            new Entry(() -> ModItems.SWEET_POTATO.get(), 6),
            new Entry(() -> ModItems.GREEN_PEPPER_SEEDS.get(), 6),
            new Entry(() -> ModItems.EGGPLANT_SEEDS.get(), 6),
            new Entry(() -> ModItems.GREEN_BEANS_SEEDS.get(), 6),
            new Entry(() -> ModItems.RED_CHILI_SEEDS.get(), 5),
            new Entry(() -> ModItems.GINSENG_SEEDS.get(), 2)   // 人参种：稀有
    };

    public StructureSeedsLootModifier(LootItemCondition[] conditions)
    {
        super(conditions);
    }

    @Override
    //? if <1.20.5 {
    /*public Codec<? extends IGlobalLootModifier> codec()
    {
        return CODEC;
    }*/
    //?} else {
    public MapCodec<? extends IGlobalLootModifier> codec()
    {
        return CODEC;
    }
    //?}

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context)
    {
        var random = context.getRandom();
        // 小概率：15% 塞 1 份种子（村庄住宅的 40% 是"大概率"，这里是"小概率"）
        if (random.nextFloat() < 0.15F)
        {
            generatedLoot.add(roll(random, SEEDS));
        }
        return generatedLoot;
    }

    private static ItemStack roll(net.minecraft.util.RandomSource random, Entry[] table)
    {
        int total = 0;
        for (Entry entry : table) { total += entry.weight; }
        int roll = random.nextInt(total);
        for (Entry entry : table)
        {
            roll -= entry.weight;
            if (roll < 0)
            {
                int count = 1 + random.nextInt(2);  // 1~2 个
                return new ItemStack(entry.item.get(), count);
            }
        }
        return ItemStack.EMPTY;
    }

    private record Entry(java.util.function.Supplier<Item> item, int weight) {}
}
