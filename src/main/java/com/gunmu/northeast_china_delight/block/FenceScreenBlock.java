package com.gunmu.northeast_china_delight.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * 障子（秫秸障子 / 板障子）：东北院子的篱笆。连法、碰撞箱都和铁栏杆 / 玻璃板一样，
 * 四个方向自己接上相邻的障子、墙和实心方块。
 *
 * <p>多一个 {@link #UP}：上面还摞着同一种障子时为 true，模型换成一通到顶的那套贴图（秆子不留梢、木板不削尖），
 * 两格摞起来看着是一整片。</p>
 *
 * <p>继承 {@link IronBarsBlock} 是为了它的连法（1.20.1 的构造器不公开，得有个子类）。不重写 codec()，理由同 {@link AnimalOilBlock}。</p>
 */
public class FenceScreenBlock extends IronBarsBlock {

    public static final BooleanProperty UP = BlockStateProperties.UP;

    public FenceScreenBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(UP, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }
        return state.setValue(UP, context.getLevel().getBlockState(context.getClickedPos().above()).is(this));
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BlockState updated = super.updateShape(state, direction, neighborState, level, pos, neighborPos);
        if (direction == Direction.UP && updated.is(this)) {
            updated = updated.setValue(UP, neighborState.is(this));
        }
        return updated;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(UP);
    }
}
