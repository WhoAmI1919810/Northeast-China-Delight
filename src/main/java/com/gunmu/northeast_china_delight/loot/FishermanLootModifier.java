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
import net.minecraftforge.common.loot.IGlobalLootModifier;
//?} else {
/*import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
*///?}
//? if <1.20.2 {
import net.minecraftforge.common.loot.LootModifier;
//?} else {
/*import net.neoforged.neoforge.common.loot.LootModifier;
*///?}

public class FishermanLootModifier extends LootModifier
{
    //? if <1.20.5 {
    public static final Codec<FishermanLootModifier> CODEC = RecordCodecBuilder.create(
            instance -> codecStart(instance).apply(instance, FishermanLootModifier::new));
    //?} else {
    /*public static final MapCodec<FishermanLootModifier> CODEC = RecordCodecBuilder.mapCodec(
            instance -> codecStart(instance).apply(instance, FishermanLootModifier::new));
    *///?}

    private static final Entry[] RAW = {
            new Entry(() -> CrciItems.SHRIMP.get(), 9),
            new Entry(() -> CrciItems.OYSTER.get(), 8),
            new Entry(() -> CrciItems.HAIRTAIL.get(), 6),
            new Entry(() -> CrciItems.SEA_CUCUMBER.get(), 5)
    };

    private static final Entry[] DISH = {
            new Entry(() -> CrciItems.SALTED_FISH.get(), 8),
            new Entry(() -> ModItems.XIAN_YU_BING_ZI.get(), 6),
            new Entry(() -> ModItems.CONG_SHAO_HAI_SHEN.get(), 4),
            new Entry(() -> ModItems.SUAN_CAI_HAI_XIAN_GUO.get(), 4)
    };

    public FishermanLootModifier(LootItemCondition[] conditions)
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
