package com.gunmu.dongbei_delight.block;

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

    /**
     * 从这个年龄（0 起算）开始变成两格高 —— 也就是美术上的「第 6 阶段」。
     *
     * <p>阶段号 = 年龄 + 1，对应关系：
     * <pre>
     * 阶段 1~5：单格，下格用 下1~下5
     * 阶段 6  ：两格，下格**复用下5** + 上格 上1
     * 阶段 7  ：两格，下格 下7       + 上格 上2
     * 阶段 8  ：两格，下格 下8       + 上格 上3（结玉米）
     * </pre>
     */
    public static final int STALK_AGE = 5;

    /**
     * 两格高阶段的选中/碰撞形状：下半格也做成一整根茎秆。
     *
     * <p>原来用的是作物默认的小矮盒，玩家指着两格高的玉米下半截时，
     * 黑线只有小小一坨，和模型对不上。
     */
    private static final VoxelShape TALL_SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 16.0D, 13.0D);

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (this.getAge(state) >= STALK_AGE) {
            return TALL_SHAPE;
        }
        return super.getShape(state, level, pos, context);
    }

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

    /**
     * 在上方维护那一格茎秆（上下同步长：年龄 5 / 6 / 7 → 上格的 3 个阶段）：
     * <ul>
     *     <li>年龄 5（第 6 阶段）→ 上格 {@link CornStalkBlock#STAGE_YOUNG}（上1）；</li>
     *     <li>年龄 6（第 7 阶段）→ 上格 {@link CornStalkBlock#STAGE_GROWN}（上2，长成未结穗）；</li>
     *     <li>年龄 7（第 8 阶段）→ 上格 {@link CornStalkBlock#STAGE_RIPE}（上3，结穗可掰）；</li>
     *     <li>年龄不到 5（比如上格被打掉后退回第 5 阶段）→ 清掉多余的茎秆。</li>
     * </ul>
     */
    private void refreshStalk(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof CornCropBlock)) {
            return;
        }

        BlockPos abovePos = pos.above();
        BlockState aboveState = level.getBlockState(abovePos);
        Block stalk = ModBlocks.CORN_STALK.get();

        if (this.getAge(state) >= STALK_AGE) {
            // 上下一起长：上格阶段 = 年龄 - STALK_AGE（正好 0/1/2 三档）
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
