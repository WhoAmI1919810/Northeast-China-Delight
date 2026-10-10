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
import net.minecraftforge.common.loot.IGlobalLootModifier;
//?} else {
/*import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
*///?}
//? if <1.20.2 {
import net.minecraftforge.common.loot.LootModifier;
//?} else {
/*import net.neoforged.neoforge.common.loot.LootModifier;
*///?}

public class ButcherLootModifier extends LootModifier
{
    //? if <1.20.5 {
    public static final Codec<ButcherLootModifier> CODEC = RecordCodecBuilder.create(
            instance -> codecStart(instance).apply(instance, ButcherLootModifier::new));
    //?} else {
    /*public static final MapCodec<ButcherLootModifier> CODEC = RecordCodecBuilder.mapCodec(
            instance -> codecStart(instance).apply(instance, ButcherLootModifier::new));
    *///?}

    private static final Entry[] RAW = {
            new Entry(() -> ModItems.PORK_RIBS.get(), 8),
            new Entry(() -> ModItems.OIL_EDGE.get(), 7),
            new Entry(() -> ModItems.PORK_HOCK.get(), 7),
            new Entry(() -> ModItems.PORK_INTESTINE.get(), 6),
            new Entry(() -> ModItems.PIG_LIVER.get(), 6),
            new Entry(() -> ModItems.PIG_BLOOD.get(), 5),
            new Entry(() -> ModItems.CHICKEN_FRAME.get(), 5)
    };

    private static final Entry[] DISH = {
            new Entry(() -> ModItems.RED_SAUSAGE.get(), 8),
            new Entry(() -> ModItems.BLOOD_SAUSAGE.get(), 7),
            new Entry(() -> ModItems.RICE_SAUSAGE.get(), 7),
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
    public Codec<? extends IGlobalLootModifier> codec()
    {
        return CODEC;
    }
    //?} else {
    /*public MapCodec<? extends IGlobalLootModifier> codec()
    {
        return CODEC;
    }
    *///?}

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
