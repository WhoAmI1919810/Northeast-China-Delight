package com.gunmu.northeast_china_delight.item;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;

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
}
