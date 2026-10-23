package com.gunmu.northeast_china_delight.loot;

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

public class VillageSeedsLootModifier extends LootModifier
{
    //? if <1.20.5 {
    public static final Codec<VillageSeedsLootModifier> CODEC = RecordCodecBuilder.create(
            instance -> codecStart(instance).apply(instance, VillageSeedsLootModifier::new));
    //?} else {
    /*public static final MapCodec<VillageSeedsLootModifier> CODEC = RecordCodecBuilder.mapCodec(
            instance -> codecStart(instance).apply(instance, VillageSeedsLootModifier::new));
    *///?}

    private static final Entry[] TEMPERATE = {
            new Entry(() -> ModItems.CORN_SEEDS.get(), 8),
            new Entry(() -> ModItems.SOYBEAN.get(), 8),
            new Entry(() -> ModItems.NAPA_CABBAGE_SEEDS.get(), 7),
            new Entry(() -> ModItems.SNOW_MUSTARD_SEEDS.get(), 5),
            new Entry(() -> ModItems.SNOW_MUSTARD.get(), 4),
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
            new Entry(() -> ModItems.GINSENG_SEEDS.get(), 2),
            new Entry(() -> ModItems.BELLFLOWER_ROOT.get(), 4),
            new Entry(() -> ModItems.BELLFLOWER_SEEDS.get(), 4)
    };

    public VillageSeedsLootModifier(LootItemCondition[] conditions)
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
        if (random.nextFloat() < 0.40F)
        {
            ChestLootConfigModifier.addIfAllowed(generatedLoot, roll(random, TEMPERATE));
        }
        if (random.nextFloat() < 0.15F)
        {
            ChestLootConfigModifier.addIfAllowed(generatedLoot, roll(random, TEMPERATE));
        }
        if (random.nextFloat() < 0.15F)
        {
            ChestLootConfigModifier.addIfAllowed(generatedLoot, rollProcessed(random));
        }
        return generatedLoot;
    }

    /** 权重 5/4/3/2。玉米面和水面团 1~3 个，花生酱固定 1 瓶，腌雪里蕻 1~2 份。不替换种子那一抽。 */
    private static ItemStack rollProcessed(net.minecraft.util.RandomSource random)
    {
        int roll = random.nextInt(14);
        if (roll < 5)
        {
            return new ItemStack(ModItems.CORN_FLOUR.get(), 1 + random.nextInt(3));
        }
        if (roll < 9)
        {
            return new ItemStack(ModItems.WATER_DOUGH.get(), 1 + random.nextInt(3));
        }
        if (roll < 12)
        {
            return new ItemStack(ModItems.PEANUT_BUTTER.get());
        }
        return new ItemStack(ModItems.PICKLED_SNOW_MUSTARD.get(), 1 + random.nextInt(2));
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
