package com.gunmu.northeast_china_delight.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 木格窗（糊纸 / 玻璃）：一扇嵌在墙洞正中的窗（厚 4 像素），不能开。
 *
 * <p>{@link #FACING} 指屋外那一面：糊纸窗的纸糊在这一面（东北"窗户纸糊在外"），另一面露出窗棂。
 * 放的时候屋外那面朝着放窗的人，站在屋外砌墙装窗，纸就在外面。</p>
 *
 * <p>几扇同一种、同一朝向的窗拼在一起连成一整扇：{@link #LEFT} / {@link #RIGHT} /
 * {@link #UP} / {@link #DOWN} 记那一边挨着没有，挨着的那一边不画外框、补上窗纸 / 玻璃，窗棂接过去
 * （模型见 work/tools/ne_building/window_connect.py）。左右是朝北放时模型的西边 / 东边，也就是朝向逆时针 / 顺时针那一边。</p>
 */
public class LatticeWindowBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LEFT = BooleanProperty.create("left");
    public static final BooleanProperty RIGHT = BooleanProperty.create("right");
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    private static final VoxelShape ALONG_X = Block.box(0, 0, 6, 16, 16, 10);
    private static final VoxelShape ALONG_Z = Block.box(6, 0, 0, 10, 16, 16);

    public LatticeWindowBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(LEFT, false).setValue(RIGHT, false).setValue(UP, false).setValue(DOWN, false));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? ALONG_X : ALONG_Z;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return connected(state, context.getLevel(), context.getClickedPos());
    }

    /** 旁边换了东西（放了 / 拆了窗、窗转了向）就重新看四边连没连 */
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BlockState updated = super.updateShape(state, direction, neighborState, level, pos, neighborPos);
        return updated.is(this) ? connected(updated, level, pos) : updated;
    }

    /** 四边各挨没挨着同一种、同一朝向的窗 */
    private BlockState connected(BlockState state, BlockGetter level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        return state.setValue(LEFT, joins(state, level.getBlockState(pos.relative(facing.getCounterClockWise()))))
                .setValue(RIGHT, joins(state, level.getBlockState(pos.relative(facing.getClockWise()))))
                .setValue(UP, joins(state, level.getBlockState(pos.above())))
                .setValue(DOWN, joins(state, level.getBlockState(pos.below())));
    }

    private boolean joins(BlockState self, BlockState other) {
        return other.is(this) && other.getValue(FACING) == self.getValue(FACING);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    /** 镜像：朝向照原版转，左右对调 */
    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState rotated = state.rotate(mirror.getRotation(state.getValue(FACING)));
        return mirror == Mirror.NONE ? rotated
                : rotated.setValue(LEFT, state.getValue(RIGHT)).setValue(RIGHT, state.getValue(LEFT));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LEFT, RIGHT, UP, DOWN);
    }
}
