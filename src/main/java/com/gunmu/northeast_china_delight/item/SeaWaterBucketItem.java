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

/**
 * A seawater bucket can only be emptied back into a clean ocean water column.
 */
public class SeaWaterBucketItem extends BucketItem {
    public SeaWaterBucketItem(Properties properties) {
        super(Fluids.WATER, properties);
    }

    @Override
    public boolean emptyContents(Player player, Level level, BlockPos pos,
                                 BlockHitResult result, ItemStack container) {
        return isValidOceanColumn(level, pos) && super.emptyContents(player, level, pos, result, container);
    }

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

    public static BlockHitResult raycastSourceFluid(Level level, Player player) {
        return getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
    }
}
