package com.gunmu.dongbei_delight.loot;

import com.gunmu.dongbei_delight.item.ModItems;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * 带鱼的获取方式：在「冷水深海 / 冰冻深海」的广阔水面上钓鱼，鱼类别按下面的权重重新结算
 * （鳕鱼 50%、鲑鱼 25%、带鱼 25%，热带鱼和河豚不再出现）。
 *
 * 需要同时满足三个条件（先用 {@code data/dongbei_delight/loot_modifiers/hairtail_fishing.json}
 * 里的浮漂实体条件过一遍，再在下面逐条检查）：
 *
 * <ol>
 *   <li>浮漂所在生物群系是冷水深海或冰冻深海；</li>
 *   <li>浮漂所在高度上，前后左右各延伸 200 格都是水；</li>
 *   <li>浮漂往下 20 格都是水。</li>
 * </ol>
 *
 * 「水」包含海带、海草这类含水的水生植物（它们的水流体状态依然是水），
 * 但冰块、睡莲一类会直接判定失败。
 *
 * 只改写「鱼」这一类，宝藏和杂物照旧：判定方式是看这一次钓上来的东西是不是原版鱼类池里的东西
 * （鳕鱼、鲑鱼、热带鱼、河豚）——那四种恰好只会从鱼类池里出来，不会和别的池子撞车。
 *
 * 注意权重是在「鱼类别内部」结算的：也就是说在这个水域里钓到「鱼」时，
 * 有一半是鳕鱼、四分之一是鲑鱼、四分之一是带鱼。
 */
public class HairtailFishingLootModifier extends LootModifier {

    /** 浮漂同一高度上、四个方向各延伸多少格 */
    public static final int HORIZONTAL_RANGE = 200;
    /** 浮漂往下多少格 */
    public static final int VERTICAL_RANGE = 20;

    /** 符合条件的水域里，鱼类别内部的权重（总和 100） */
    public static final int WEIGHT_COD = 50;
    public static final int WEIGHT_SALMON = 25;
    public static final int WEIGHT_HAIRTAIL = 25;
    private static final int TOTAL_WEIGHT = WEIGHT_COD + WEIGHT_SALMON + WEIGHT_HAIRTAIL;

    public static final MapCodec<HairtailFishingLootModifier> CODEC = RecordCodecBuilder.mapCodec(
            instance -> codecStart(instance).apply(instance, HairtailFishingLootModifier::new));

    public HairtailFishingLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        // 先看这一杆钓上来的类别是不是「鱼」（这一步很便宜，不是鱼就直接放行）
        int fishIndex = indexOfFish(generatedLoot);
        if (fishIndex < 0) {
            return generatedLoot;
        }

        Vec3 origin = context.getParamOrNull(LootContextParams.ORIGIN);
        if (origin == null) {
            return generatedLoot;
        }
        ServerLevel level = context.getLevel();
        BlockPos bobber = BlockPos.containing(origin.x, origin.y, origin.z);

        // 浮漂贴着水面飘，可能刚好落在水面上方那一格里，所以往下找一格才是水面
        BlockPos surface = waterSurface(level, bobber);
        if (surface == null) {
            return generatedLoot;
        }

        if (!isColdDeepOcean(level, surface) || !isOpenWater(level, surface)) {
            return generatedLoot;
        }

        generatedLoot.set(fishIndex, rollFish(context));
        return generatedLoot;
    }

    /**
     * 重掷鱼类别：鳕鱼 50%、鲑鱼 25%、带鱼 25%。
     * 原版热带鱼和河豚的权重在这里被吃掉，不再出现。
     */
    private static ItemStack rollFish(LootContext context) {
        int roll = context.getRandom().nextInt(TOTAL_WEIGHT);
        if (roll < WEIGHT_COD) {
            return new ItemStack(Items.COD);
        }
        if (roll < WEIGHT_COD + WEIGHT_SALMON) {
            return new ItemStack(Items.SALMON);
        }
        return new ItemStack(ModItems.HAIRTAIL.get());
    }

    /** 浮漂所在的那一格（或者它下面一格）里的水面方块；都不是水就返回 null */
    private static BlockPos waterSurface(ServerLevel level, BlockPos bobber) {
        if (isWater(level, bobber)) {
            return bobber;
        }
        BlockPos below = bobber.below();
        return isWater(level, below) ? below : null;
    }

    /** 冷水深海 / 冰冻深海 */
    private static boolean isColdDeepOcean(ServerLevel level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        return biome.is(Biomes.DEEP_COLD_OCEAN) || biome.is(Biomes.DEEP_FROZEN_OCEAN);
    }

    /** 水面同一高度上四个方向各 200 格、以及往下 20 格，必须全都是水 */
    private static boolean isOpenWater(ServerLevel level, BlockPos surface) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos.MutableBlockPos cursor = surface.mutable();
            for (int i = 0; i < HORIZONTAL_RANGE; i++) {
                if (!isWater(level, cursor.move(direction))) {
                    return false;
                }
            }
        }

        BlockPos.MutableBlockPos cursor = surface.mutable();
        for (int i = 0; i < VERTICAL_RANGE; i++) {
            if (!isWater(level, cursor.move(Direction.DOWN))) {
                return false;
            }
        }
        return true;
    }

    /** 水，或者含水的水生植物（海带、海草……） */
    private static boolean isWater(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getFluidState().is(FluidTags.WATER);
    }

    private static int indexOfFish(ObjectArrayList<ItemStack> loot) {
        for (int i = 0; i < loot.size(); i++) {
            if (isFish(loot.get(i))) {
                return i;
            }
        }
        return -1;
    }

    /** 原版鱼类池：鳕鱼、鲑鱼、热带鱼、河豚 */
    private static boolean isFish(ItemStack stack) {
        return stack.is(Items.COD) || stack.is(Items.SALMON)
                || stack.is(Items.TROPICAL_FISH) || stack.is(Items.PUFFERFISH);
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
