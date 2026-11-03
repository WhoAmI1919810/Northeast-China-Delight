package com.gunmu.northeast_china_delight.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 障子（秫秸障子 / 板障子）：东北院子的篱笆。连法、碰撞箱都和铁栏杆 / 玻璃板一样，
 * 四个方向自己接上相邻的障子、墙和实心方块。
 *
 * <p>多一个 {@link #UP}：上面还摞着同一种障子时为 true，模型换成一通到顶的那套贴图（秆子不留梢、木板不削尖），
 * 两格摞起来看着是一整片。</p>
 *
 * <p>碰撞箱和原版栅栏一样高（1.5 格，跳不过去）；样子和选框还是铁栏杆那套。</p>
 *
 * <p>还会接到旁边顺着同一条线的栅栏门上（障子柴门 {@link ScreenGateBlock}，原版栅栏门也算）——
 * 原版铁栏杆不认栅栏门，门边上会空一截。</p>
 *
 * <p>继承 {@link IronBarsBlock} 是为了它的连法（1.20.1 的构造器不公开，得有个子类）。不重写 codec()，理由同 {@link AnimalOilBlock}。</p>
 */
public class FenceScreenBlock extends IronBarsBlock {

    public static final BooleanProperty UP = BlockStateProperties.UP;

    /** 按 北 1 / 东 2 / 南 4 / 西 8 连着哪几边取碰撞箱：中间一根桩加连过去的那几截，都是 24 像素高 */
    private static final VoxelShape[] COLLISION_SHAPES = makeCollisionShapes();

    private static VoxelShape[] makeCollisionShapes() {
        VoxelShape post = Block.box(7, 0, 7, 9, 24, 9);
        VoxelShape[] arms = {
                Block.box(7, 0, 0, 9, 24, 9), Block.box(7, 0, 7, 16, 24, 9),
                Block.box(7, 0, 7, 9, 24, 16), Block.box(0, 0, 7, 9, 24, 9)
        };
        VoxelShape[] shapes = new VoxelShape[16];
        for (int i = 0; i < shapes.length; i++) {
            VoxelShape shape = post;
            for (int side = 0; side < arms.length; side++) {
                if ((i & (1 << side)) != 0) {
                    shape = Shapes.or(shape, arms[side]);
                }
            }
            shapes[i] = shape;
        }
        return shapes;
    }

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
        BlockGetter level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (isGateAlong(level.getBlockState(pos.relative(direction)), direction)) {
                state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), true);
            }
        }
        return state.setValue(UP, level.getBlockState(pos.above()).is(this));
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BlockState updated = super.updateShape(state, direction, neighborState, level, pos, neighborPos);
        if (direction == Direction.UP && updated.is(this)) {
            updated = updated.setValue(UP, neighborState.is(this));
        }
        if (direction.getAxis().isHorizontal() && updated.is(this) && isGateAlong(neighborState, direction)) {
            updated = updated.setValue(PROPERTY_BY_DIRECTION.get(direction), true);
        }
        return updated;
    }

    /** 那边是栅栏门、而且门是顺着这条线放的（门扇横在 direction 的垂直方向上） */
    private static boolean isGateAlong(BlockState neighbor, Direction direction) {
        return neighbor.getBlock() instanceof FenceGateBlock && FenceGateBlock.connectsToDirection(neighbor, direction);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int index = (state.getValue(NORTH) ? 1 : 0) | (state.getValue(EAST) ? 2 : 0)
                | (state.getValue(SOUTH) ? 4 : 0) | (state.getValue(WEST) ? 8 : 0);
        return COLLISION_SHAPES[index];
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(UP);
    }
}
