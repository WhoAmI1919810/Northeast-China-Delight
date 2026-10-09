package com.gunmu.northeast_china_delight.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** A seawater bucket can only be emptied back into a clean ocean water column. */
public class SeaWaterBucketItem extends BucketItem {
    public SeaWaterBucketItem(Properties properties) {
        super(Fluids.WATER, properties);
    }

    @Override
    public boolean emptyContents(Player player, Level level, BlockPos pos,
                                 BlockHitResult result, ItemStack container) {
        return isValidOceanColumn(level, pos) && super.emptyContents(player, level, pos, result, container);
    }

    /**
     * 原版倒水只认「实心方块」的准星命中，而水流没有命中框：所以站在海里对着水右键时，
     * 要么得瞄到海底方块，要么什么都不发生。这里补一条「瞄着水也能倒」的路 ——
     * 先走原版（瞄到方块时的行为不变），原版什么都没瞄到（MISS）时再用 SOURCE_ONLY
     * 瞄一次水源方块：干净海洋水柱里就把海水倒回去（水本来就还在，不需要再放），换回空桶。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        InteractionResultHolder<ItemStack> vanilla = super.use(level, player, hand);
        if (vanilla.getResult() != InteractionResult.PASS) {
            return vanilla;
        }
        BlockHitResult waterHit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (waterHit.getType() != HitResult.Type.BLOCK) {
            return vanilla;
        }
        BlockPos pos = waterHit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!state.getFluidState().is(FluidTags.WATER) || !state.getFluidState().isSource()
                || !isValidOceanColumn(level, pos)) {
            return vanilla;
        }
        Direction direction = waterHit.getDirection();
        if (!level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos.relative(direction), direction, stack)) {
            return InteractionResultHolder.fail(stack);
        }
        playEmptySound(player, level, pos);
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(getEmptySuccessItem(stack, player), level.isClientSide());
    }

    public static boolean isValidOceanColumn(Level level, BlockPos waterPos) {
        if (!level.getBiome(waterPos).is(BiomeTags.IS_OCEAN)) {
            return false;
        }
        for (int y = waterPos.getY() + 1; y <= level.getSeaLevel(); y++) {
            BlockState state = level.getBlockState(new BlockPos(waterPos.getX(), y, waterPos.getZ()));
            if (state.getFluidState().is(FluidTags.WATER) || state.isAir()
                    || state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE)
                    || state.is(Blocks.BLUE_ICE) || state.is(Blocks.FROSTED_ICE)
                    || state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK)) {
                continue;
            }
            return false;
        }
        return true;
    }

    /**
     * 取海水的视线检测：和原版空桶取水走同一条路 —— 只认水源方块（流动的水不算），
     * 长度用玩家自己的方块交互距离。1.21.1 那边就是靠它才瞄得准水面的。
     */
    public static BlockHitResult raycastSourceFluid(Level level, Player player) {
        return getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
    }
}
