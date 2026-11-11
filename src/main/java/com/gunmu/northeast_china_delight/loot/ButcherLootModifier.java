package com.gunmu.northeast_china_delight.loot;

import com.gunmu.chinese_regional_culinary_ingredients.item.CrciItems;

import com.gunmu.northeast_china_delight.NortheastChinaConfig;
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
 * 屠夫（village_butcher）箱子：小概率塞东北乐事的肉类食材和肉菜。
 *
 * <p>分两档roll：先按 ~18% 概率出一份「生料」（排骨/油边/下水这类屠夫卖的鲜肉），
 * 再按 ~10% 概率出一份「肉菜」（红肠/血肠/烤货/炖菜这类成品）。</p>
 */
public class ButcherLootModifier extends LootModifier
{
    //? if <1.20.5 {
    /*public static final Codec<ButcherLootModifier> CODEC = RecordCodecBuilder.create(
            instance -> codecStart(instance).apply(instance, ButcherLootModifier::new));
    *///?} else {
    public static final MapCodec<ButcherLootModifier> CODEC = RecordCodecBuilder.mapCodec(
            instance -> codecStart(instance).apply(instance, ButcherLootModifier::new));
    //?}

    /** 生肉 / 下水（屠夫卖的料） */
    private static final Entry[] RAW = {
            new Entry(() -> CrciItems.PORK_RIBS.get(), 8),
            new Entry(() -> CrciItems.OIL_EDGE.get(), 7),
            new Entry(() -> CrciItems.PORK_HOCK.get(), 7),
            new Entry(() -> CrciItems.PORK_INTESTINE.get(), 6),
            new Entry(() -> CrciItems.PIG_LIVER.get(), 6),
            new Entry(() -> CrciItems.PIG_BLOOD.get(), 5),
            new Entry(() -> CrciItems.CHICKEN_FRAME.get(), 5)
    };

    private static final Entry[] DISH = {
            new Entry(() -> CrciItems.RED_SAUSAGE.get(), 8),
            new Entry(() -> CrciItems.BLOOD_SAUSAGE.get(), 7),
            new Entry(() -> CrciItems.RICE_SAUSAGE.get(), 7),
            new Entry(() -> ModItems.GRILLED_CHICKEN_FRAME.get(), 6),
            new Entry(() -> ModItems.GRILLED_OIL_EDGE.get(), 6),
            new Entry(() -> ModItems.LIU_ROU_DUAN.get(), 4),
            new Entry(() -> ModItems.HONG_SHAO_PAI_GU.get(), 4),
            new Entry(() -> ModItems.XUN_JIANG_PIN_PAN.get(), 4),
            new Entry(() -> ModItems.ZHU_ROU_DUN_FEN_TIAO_BOWL.get(), 3),
            new Entry(() -> ModItems.OLD_STYLE_GUO_BAO_ROU.get(), 3)
    };

    public ButcherLootModifier(LootItemCondition[] conditions)
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
        if (!NortheastChinaConfig.isChestLootEnabled())
        {
            return generatedLoot;
        }
        var random = context.getRandom();
        if (random.nextFloat() < 0.18F)
        {
            ChestLootConfigModifier.addIfAllowed(generatedLoot, roll(random, RAW, 1, 2));
        }
        if (random.nextFloat() < 0.10F)
        {
            ChestLootConfigModifier.addIfAllowed(generatedLoot, roll(random, DISH, 1, 1));
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
