package com.gunmu.dongbei_delight.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/**
 * 玉米作物：比其它作物更高，长到后期会在上方多出一节茎秆（两个方块高）。
 */
public class CornCropBlock extends DdCropBlock {

    /** 长到这个年龄后，上方开始出现茎秆 */
    public static final int STALK_AGE = 6;

    public CornCropBlock(BlockBehaviour.Properties properties, Supplier<? extends ItemLike> seed) {
        super(properties, seed);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
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

    /** 成熟后在上方补出茎秆（年龄 7 时上半格结穗），未成熟则清理掉多余的茎秆 */
    private void refreshStalk(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof CornCropBlock)) {
            return;
        }

        BlockPos abovePos = pos.above();
        BlockState aboveState = level.getBlockState(abovePos);
        Block stalk = ModBlocks.CORN_STALK.get();

        if (this.getAge(state) >= STALK_AGE) {
            boolean ripe = this.getAge(state) >= this.getMaxAge();
            BlockState want = stalk.defaultBlockState().setValue(CornStalkBlock.HAS_CORN, ripe);
            if (aboveState.isAir()) {
                level.setBlock(abovePos, want, 3);
            } else if (aboveState.is(stalk) && aboveState.getValue(CornStalkBlock.HAS_CORN) != ripe) {
                // 长熟（或者玉米被掰掉退回茎秆）时同步上半格
                level.setBlock(abovePos, want, 3);
            }
        } else if (aboveState.is(stalk)) {
            level.removeBlock(abovePos, false);
        }
    }
}
