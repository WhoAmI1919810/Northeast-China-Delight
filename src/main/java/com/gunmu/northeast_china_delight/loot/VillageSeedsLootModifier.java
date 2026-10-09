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
 * 村庄住宅箱子：按群系塞东北乐事的种子和谷物。
 *
 * <p>不是「替换」而是「追加」——往已经roll好的战利品里再塞 1~2 个种子，
 * 概率按群系调（平原/热带草原/针叶林才出东北作物，雪原/沙漠不出）。</p>
 *
 * <p>JSON 写法：
 * <pre>
 * {
 *   "type": "northeast_china_delight:village_seeds",
 *   "conditions": [
 *     { "condition": "neoforge:loot_table_id", "loot_table_id": "minecraft:chests/village/village_plains_house" }
 *   ]
 * }
 * </pre>
 * </p>
 */
public class VillageSeedsLootModifier extends LootModifier
{
    //? if <1.20.5 {
    /*public static final Codec<VillageSeedsLootModifier> CODEC = RecordCodecBuilder.create(
            instance -> codecStart(instance).apply(instance, VillageSeedsLootModifier::new));
    *///?} else {
    public static final MapCodec<VillageSeedsLootModifier> CODEC = RecordCodecBuilder.mapCodec(
            instance -> codecStart(instance).apply(instance, VillageSeedsLootModifier::new));
    //?}

    /** 平原/热带草原/针叶林村庄的种子池 */
    private static final Entry[] TEMPERATE = {
            new Entry(() -> ModItems.CORN_SEEDS.get(), 8),
            new Entry(() -> ModItems.SOYBEAN.get(), 8),
            new Entry(() -> ModItems.NAPA_CABBAGE_SEEDS.get(), 7),
            new Entry(() -> ModItems.CUCUMBER_SEEDS.get(), 7),
            new Entry(() -> ModItems.GREEN_RADISH_SEEDS.get(), 7),
            new Entry(() -> ModItems.GREEN_ONION_SEEDS.get(), 7),
            new Entry(() -> ModItems.BUCKWHEAT.get(), 6),
            new Entry(() -> ModItems.RED_BEAN.get(), 6),
            new Entry(() -> ModItems.PEANUT.get(), 6),
            new Entry(() -> ModItems.SWEET_POTATO.get(), 5),
            new Entry(() -> ModItems.GREEN_PEPPER_SEEDS.get(), 6),
            new Entry(() -> ModItems.EGGPLANT_SEEDS.get(), 6),
            new Entry(() -> ModItems.GREEN_BEANS_SEEDS.get(), 6),
            new Entry(() -> ModItems.RED_CHILI_SEEDS.get(), 5),
            new Entry(() -> ModItems.GINSENG_SEEDS.get(), 2)   // 人参：稀有，只有 2 权重
    };

    public VillageSeedsLootModifier(LootItemCondition[] conditions)
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
        // 40% 概率塞一个种子，15% 概率再塞第二个
        if (random.nextFloat() < 0.40F)
        {
            generatedLoot.add(roll(random, TEMPERATE));
        }
        if (random.nextFloat() < 0.15F)
        {
            generatedLoot.add(roll(random, TEMPERATE));
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
