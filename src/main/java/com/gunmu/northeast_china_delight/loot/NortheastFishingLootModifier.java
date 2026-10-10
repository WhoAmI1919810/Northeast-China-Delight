package com.gunmu.northeast_china_delight.loot;

import com.gunmu.northeast_china_delight.item.ModItems;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
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

public class NortheastFishingLootModifier extends LootModifier
{
    public static final int HAIRTAIL_HORIZONTAL_RANGE = 150;
    public static final int HAIRTAIL_VERTICAL_RANGE = 20;
    public static final int OYSTER_BIOME_RANGE = 30;

    private static final Entry[] GENERAL = {
            new Entry(() -> Items.COD, 45), new Entry(() -> Items.SALMON, 25),
            new Entry(() -> Items.PUFFERFISH, 13), new Entry(() -> Items.TROPICAL_FISH, 2),
            new Entry(ModItems.SHRIMP, 15)
    };

    private static final Entry[] OYSTER = {
            new Entry(() -> Items.COD, 30), new Entry(() -> Items.SALMON, 25),
            new Entry(() -> Items.PUFFERFISH, 13), new Entry(() -> Items.TROPICAL_FISH, 2),
            new Entry(ModItems.SHRIMP, 15), new Entry(ModItems.OYSTER, 15)
    };

    private static final Entry[] HAIRTAIL = {
            new Entry(() -> Items.COD, 25), new Entry(() -> Items.SALMON, 25),
            new Entry(ModItems.HAIRTAIL, 25), new Entry(ModItems.SEA_CUCUMBER, 25)
    };

    //? if <1.20.5 {
    public static final Codec<NortheastFishingLootModifier> CODEC = RecordCodecBuilder.create(
            instance -> codecStart(instance).apply(instance, NortheastFishingLootModifier::new));
    //?} else {
    /*public static final MapCodec<NortheastFishingLootModifier> CODEC = RecordCodecBuilder.mapCodec(
            instance -> codecStart(instance).apply(instance, NortheastFishingLootModifier::new));
    *///?}

    public NortheastFishingLootModifier(LootItemCondition[] conditions)
    {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context)
    {
        int fishIndex = indexOfFish(generatedLoot);
        if (fishIndex < 0)
        {
            return generatedLoot;
        }
        Vec3 origin = context.getParamOrNull(LootContextParams.ORIGIN);
        if (origin == null)
        {
            return generatedLoot;
        }
        ServerLevel level = context.getLevel();
        BlockPos bobber = BlockPos.containing(origin.x, origin.y, origin.z);
        BlockPos surface = waterSurface(level, bobber);
        if (surface == null)
        {
            return generatedLoot;
        }

        boolean deepOcean = isColdDeepOcean(level, surface);
        boolean openWater = isOpenWater(level, surface);
        int depth = waterDepth(level, surface);
        String biomeName = level.getBiome(surface).unwrapKey().map(k -> k.location().toString()).orElse("unknown");
        net.minecraft.world.entity.player.Player player = level.getNearestPlayer(
                bobber.getX(), bobber.getY(), bobber.getZ(), 8.0D, false);
        if (player != null)
        {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(String.format(
                            "§7[东北乐事] §f开阔水面: %s §f(150格内水/冰/含水方块) §f水深: %d 群系: %s",
                            openWater ? "§a是" : "§c否", depth, biomeName)), true);
        }

        Entry[] table;
        if (deepOcean && openWater)
        {
            table = HAIRTAIL;
        }
        else if (isRiverNearOcean(level, surface))
        {
            table = OYSTER;
        }
        else
        {
            table = GENERAL;
        }
        generatedLoot.set(fishIndex, roll(context, table));
        return generatedLoot;
    }

    private static ItemStack roll(LootContext context, Entry[] table)
    {
        int total = 0;
        for (Entry entry : table)
        {
            total += entry.weight;
        }
        int roll = context.getRandom().nextInt(total);
        for (Entry entry : table)
        {
            roll -= entry.weight;
            if (roll < 0)
            {
                return new ItemStack(entry.item.get());
            }
        }
        return new ItemStack(Items.COD);
    }

    private static boolean isRiverNearOcean(ServerLevel level, BlockPos surface)
    {
        boolean river = false;
        boolean ocean = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -OYSTER_BIOME_RANGE; dx <= OYSTER_BIOME_RANGE; dx += 5)
        {
            for (int dz = -OYSTER_BIOME_RANGE; dz <= OYSTER_BIOME_RANGE; dz += 5)
            {
                if (dx * dx + dz * dz > OYSTER_BIOME_RANGE * OYSTER_BIOME_RANGE)
                {
                    continue;
                }
                cursor.set(surface.getX() + dx, surface.getY(), surface.getZ() + dz);
                Holder<Biome> biome = level.getBiome(cursor);
                river |= biome.is(BiomeTags.IS_RIVER);
                ocean |= biome.is(BiomeTags.IS_OCEAN);
                if (river && ocean)
                {
                    return true;
                }
            }
        }
        return false;
    }

    private static BlockPos waterSurface(ServerLevel level, BlockPos bobber)
    {
        if (isWater(level, bobber))
        {
            return bobber;
        }
        BlockPos below = bobber.below();
        return isWater(level, below) ? below : null;
    }

    public static boolean isColdDeepOcean(ServerLevel level, BlockPos pos)
    {
        Holder<Biome> biome = level.getBiome(pos);
        return biome.is(Biomes.DEEP_COLD_OCEAN) || biome.is(Biomes.DEEP_FROZEN_OCEAN);
    }

    public static boolean isOpenWater(ServerLevel level, BlockPos surface)
    {
        for (Direction direction : Direction.Plane.HORIZONTAL)
        {
            BlockPos.MutableBlockPos cursor = surface.mutable();
            for (int i = 0; i < HAIRTAIL_HORIZONTAL_RANGE; i++)
            {
                if (!isWaterOrIce(level, cursor.move(direction)))
                {
                    return false;
                }
            }
        }
        BlockPos.MutableBlockPos cursor = surface.mutable();
        for (int i = 0; i < HAIRTAIL_VERTICAL_RANGE; i++)
        {
            if (!isWater(level, cursor.move(Direction.DOWN)))
            {
                return false;
            }
        }
        return true;
    }

    private static boolean isWaterOrIce(ServerLevel level, BlockPos pos)
    {
        BlockState state = level.getBlockState(pos);
        if (state.getFluidState().is(FluidTags.WATER))
        {
            return true;
        }
        if (state.is(net.minecraft.world.level.block.Blocks.ICE)
                || state.is(net.minecraft.world.level.block.Blocks.PACKED_ICE)
                || state.is(net.minecraft.world.level.block.Blocks.BLUE_ICE)
                || state.is(net.minecraft.world.level.block.Blocks.FROSTED_ICE))
        {
            return true;
        }
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)
                && state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED))
        {
            return true;
        }
        return false;
    }

    public static int waterDepth(ServerLevel level, BlockPos surface)
    {
        int depth = 0;
        BlockPos.MutableBlockPos cursor = surface.mutable();
        for (int i = 0; i < 30; i++)
        {
            if (!isWater(level, cursor.move(Direction.DOWN)))
            {
                break;
            }
            depth++;
        }
        return depth;
    }

    private static boolean isWater(ServerLevel level, BlockPos pos)
    {
        BlockState state = level.getBlockState(pos);
        return state.getFluidState().is(FluidTags.WATER)
                || (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)
                        && state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED));
    }

    private static int indexOfFish(ObjectArrayList<ItemStack> loot)
    {
        for (int i = 0; i < loot.size(); i++)
        {
            if (isFish(loot.get(i)))
            {
                return i;
            }
        }
        return -1;
    }

    private static boolean isFish(ItemStack stack)
    {
        return stack.is(Items.COD) || stack.is(Items.SALMON)
                || stack.is(Items.TROPICAL_FISH) || stack.is(Items.PUFFERFISH);
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

    private record Entry(java.util.function.Supplier<? extends net.minecraft.world.level.ItemLike> item, int weight)
    {
    }
}
