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

public class StructureSeedsLootModifier extends LootModifier
{
    //? if <1.20.5 {
    public static final Codec<StructureSeedsLootModifier> CODEC = RecordCodecBuilder.create(
            instance -> codecStart(instance).apply(instance, StructureSeedsLootModifier::new));
    //?} else {
    /*public static final MapCodec<StructureSeedsLootModifier> CODEC = RecordCodecBuilder.mapCodec(
            instance -> codecStart(instance).apply(instance, StructureSeedsLootModifier::new));
    *///?}

    private static final Entry[] SEEDS = {
            new Entry(() -> CrciItems.CORN_SEEDS.get(), 8),
            new Entry(() -> CrciItems.SOYBEAN.get(), 8),
            new Entry(() -> CrciItems.NAPA_CABBAGE_SEEDS.get(), 7),
            new Entry(() -> CrciItems.SNOW_MUSTARD_SEEDS.get(), 5),
            new Entry(() -> CrciItems.SNOW_MUSTARD.get(), 4),
            new Entry(() -> CrciItems.CUCUMBER_SEEDS.get(), 7),
            new Entry(() -> CrciItems.GREEN_RADISH_SEEDS.get(), 7),
            new Entry(() -> CrciItems.GREEN_ONION_SEEDS.get(), 7),
            new Entry(() -> CrciItems.BUCKWHEAT.get(), 6),
            new Entry(() -> CrciItems.RED_BEAN.get(), 6),
            new Entry(() -> CrciItems.PEANUT.get(), 6),
            new Entry(() -> CrciItems.SWEET_POTATO.get(), 6),
            new Entry(() -> CrciItems.GREEN_PEPPER_SEEDS.get(), 6),
            new Entry(() -> CrciItems.EGGPLANT_SEEDS.get(), 6),
            new Entry(() -> CrciItems.GREEN_BEANS_SEEDS.get(), 6),
            new Entry(() -> CrciItems.RED_CHILI_SEEDS.get(), 5),
            new Entry(() -> CrciItems.GINSENG_SEEDS.get(), 2),
            new Entry(() -> CrciItems.BELLFLOWER_ROOT.get(), 4),
            new Entry(() -> CrciItems.BELLFLOWER_SEEDS.get(), 4)
    };

    public StructureSeedsLootModifier(LootItemCondition[] conditions)
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
        if (random.nextFloat() < 0.15F)
        {
            ChestLootConfigModifier.addIfAllowed(generatedLoot, roll(random, SEEDS));
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
                int count = 1 + random.nextInt(2);
                return new ItemStack(entry.item.get(), count);
            }
        }
        return ItemStack.EMPTY;
    }

    private record Entry(java.util.function.Supplier<Item> item, int weight) {}
}
