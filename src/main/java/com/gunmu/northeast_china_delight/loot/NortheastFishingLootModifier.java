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

/**
 * 钓鱼的「鱼」这一类在本模组里有三张表，按浮漂所在的水域来挑：
 *
 * <ol>
 *   <li><b>带鱼表</b>（最优先）：冷水深海 / 冰冻深海，而且水面四个方向各 150 格内是水/冰/浮冰/蓝冰/含水方块、往下 10 格是水 ——
 *       鳕鱼 25%、鲑鱼 25%、带鱼 25%、<b>海参 25%</b>；</li>
 *   <li><b>生蚝表</b>：浮漂 30 格内同时有河流和任意海洋生物群系 ——
 *       鳕鱼 30%、鲑鱼 25%、河豚 13%、热带鱼 2%、<b>大虾 15%、生蚝 15%</b>；</li>
 *   <li><b>通用表</b>：除此之外的所有水域 —— 鳕鱼 45%、鲑鱼 25%、河豚 13%、热带鱼 2%、<b>大虾 15%</b>。</li>
 * </ol>
 *
 * <p>大虾的 15% 是从鳕鱼身上扣的（60% → 45%），生蚝表同理；带鱼表里海参和带鱼平分，
 * 鳕鱼降到 25%。宝藏和杂物两类照旧不动，只改「鱼」。</p>
 */
public class NortheastFishingLootModifier extends LootModifier
{
    /** 带鱼表的水面延伸范围 */
    public static final int HAIRTAIL_HORIZONTAL_RANGE = 150;
    /** 带鱼表的水深 */
    public static final int HAIRTAIL_VERTICAL_RANGE = 20;
    /** 生蚝表的检测半径 */
    public static final int OYSTER_BIOME_RANGE = 30;

    /** 通用表：鳕鱼 45%、鲑鱼 25%、河豚 13%、热带鱼 2%、大虾 15% */
    private static final Entry[] GENERAL = {
            new Entry(() -> Items.COD, 45), new Entry(() -> Items.SALMON, 25),
            new Entry(() -> Items.PUFFERFISH, 13), new Entry(() -> Items.TROPICAL_FISH, 2),
            new Entry(ModItems.SHRIMP, 15)
    };

    /** 生蚝表：鳕鱼 30%、鲑鱼 25%、河豚 13%、热带鱼 2%、大虾 15%、生蚝 15% */
    private static final Entry[] OYSTER = {
            new Entry(() -> Items.COD, 30), new Entry(() -> Items.SALMON, 25),
            new Entry(() -> Items.PUFFERFISH, 13), new Entry(() -> Items.TROPICAL_FISH, 2),
            new Entry(ModItems.SHRIMP, 15), new Entry(ModItems.OYSTER, 15)
    };

    /** 带鱼表：鳕鱼 25%、鲑鱼 25%、带鱼 25%、海参 25% */
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

        // 调试：每次抛竿都在物品栏上方显示当前水域判定详情
        boolean deepOcean = isColdDeepOcean(level, surface);
        boolean openWater = isOpenWater(level, surface);
        int depth = waterDepth(level, surface);
        String biomeName = level.getBiome(surface).unwrapKey().map(k -> k.location().toString()).orElse("unknown");
        // 浮漂是 FishingHook 不是玩家，从钓点位置找最近的玩家发消息
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

    /** 30 格之内既碰得到河流、又碰得到海洋 */
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

    /** 水平方向放宽：水、冰、浮冰、蓝冰、或其他含水方块都算"开阔水面" */
    private static boolean isWaterOrIce(ServerLevel level, BlockPos pos)
    {
        BlockState state = level.getBlockState(pos);
        // 流体是水 → 通过
        if (state.getFluidState().is(FluidTags.WATER))
        {
            return true;
        }
        // 实体冰块（冰、浮冰、蓝冰）→ 通过
        if (state.is(net.minecraft.world.level.block.Blocks.ICE)
                || state.is(net.minecraft.world.level.block.Blocks.PACKED_ICE)
                || state.is(net.minecraft.world.level.block.Blocks.BLUE_ICE)
                || state.is(net.minecraft.world.level.block.Blocks.FROSTED_ICE))
        {
            return true;
        }
        // 含水方块（半砖、楼梯、栅栏、墙、树叶等 waterlogged=true）→ 通过
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)
                && state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED))
        {
            return true;
        }
        return false;
    }

    /** 垂直方向只看是不是水（冰层在水面上，底下还是水就行） */
    /** 水面以下连续有多少格是水（最多查 30 格） */
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

    /** 原版鱼类池：鳕鱼、鲑鱼、热带鱼、河豚 */
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

    /** 表里的一项：掉落物 + 权重（用 Supplier 是为了不在注册表冻结前取物品实例） */
    private record Entry(java.util.function.Supplier<? extends net.minecraft.world.level.ItemLike> item, int weight)
    {
    }
}
