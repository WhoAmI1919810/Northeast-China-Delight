package com.gunmu.northeast_china_delight.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Supplier;

/**
 * 玉米作物：比其它作物更高，长到后期会在上方多出一节茎秆（两个方块高）。
 */
public class CornCropBlock extends DdCropBlock {

    public static final int STALK_AGE = 5;

    private static final VoxelShape TALL_SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 16.0D, 13.0D);

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (this.getAge(state) >= STALK_AGE) {
            return TALL_SHAPE;
        }
        return super.getShape(state, level, pos, context);
    }

    public CornCropBlock(BlockBehaviour.Properties properties, Supplier<? extends ItemLike> seed) {
        super(properties, seed);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.randomTick(state, level, pos, random);
        refreshStalk(level, pos);
    }

    @Override
    public void growCrops(Level level, BlockPos pos, BlockState state) {
        super.growCrops(level, pos, state);
        if (level instanceof ServerLevel serverLevel) {
            refreshStalk(serverLevel, pos);
        }
    }

    private void refreshStalk(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof CornCropBlock)) {
            return;
        }

        BlockPos abovePos = pos.above();
        BlockState aboveState = level.getBlockState(abovePos);
        Block stalk = ModBlocks.CORN_STALK.get();

        if (this.getAge(state) >= STALK_AGE) {
            int want = Math.min(CornStalkBlock.STAGE_RIPE,
                    Math.max(CornStalkBlock.STAGE_YOUNG, this.getAge(state) - STALK_AGE));
            BlockState wantState = stalk.defaultBlockState().setValue(CornStalkBlock.STAGE, want);
            if (aboveState.isAir()) {
                level.setBlock(abovePos, wantState, 3);
            } else if (aboveState.is(stalk) && aboveState.getValue(CornStalkBlock.STAGE) != want) {
                level.setBlock(abovePos, wantState, 3);
            }
        } else if (aboveState.is(stalk)) {
            level.removeBlock(abovePos, false);
        }
    }
}
