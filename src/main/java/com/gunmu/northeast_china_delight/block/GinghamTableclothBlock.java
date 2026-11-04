package com.gunmu.northeast_china_delight.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 格子桌布（2026-11-04；村庄副食店柜台上铺的，替掉机械动力的桌布，16 种染料色）。
 *
 * <p>像地毯一样铺在方块顶上（碰撞箱就是地毯那 1 像素），哪边没挨着别的格子桌布，模型就在那边往下垂一截布边、
 * 盖住下面那块的上沿；挨着的桌布连成一整块，中间不垂边（{@link #NORTH} 等四个属性记那一边挨着没有，
 * 模型见 blockstates 的 multipart，生成脚本 work/tools/ne_building/tablecloth.py）。</p>
 *
 * <p>结构里摆的桌布会跟着结构转：{@link #rotate} / {@link #mirror} 照原版栅栏的写法换四个方向。
 * 不重写 codec()，理由同 {@link AnimalOilBlock}。</p>
 */
public class GinghamTableclothBlock extends CarpetBlock {

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    /** 和模型一样 0.5 像素厚（2026-11-04 用户：桌布太厚，调到和炕画一样）；原版地毯是 1 像素 */
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 0.5, 16.0);

    public GinghamTableclothBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false));
    }

    private static BooleanProperty side(Direction direction) {
        return switch (direction) {
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            default -> NORTH;
        };
    }

    private static boolean joins(BlockState neighbor) {
        return neighbor.getBlock() instanceof GinghamTableclothBlock;
    }

    private BlockState connect(BlockState state, BlockGetter level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            state = state.setValue(side(direction), joins(level.getBlockState(pos.relative(direction))));
        }
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return connect(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** 下面的方块没了就掉（地毯的规矩）；旁边换了方块就重新看那一边挨不挨着桌布 */
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                  BlockPos pos, BlockPos neighborPos) {
        BlockState updated = super.updateShape(state, direction, neighbor, level, pos, neighborPos);
        if (!updated.is(this) || !direction.getAxis().isHorizontal()) {
            return updated;
        }
        return updated.setValue(side(direction), joins(neighbor));
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_180 -> state.setValue(NORTH, state.getValue(SOUTH)).setValue(EAST, state.getValue(WEST))
                    .setValue(SOUTH, state.getValue(NORTH)).setValue(WEST, state.getValue(EAST));
            case COUNTERCLOCKWISE_90 -> state.setValue(NORTH, state.getValue(EAST)).setValue(EAST, state.getValue(SOUTH))
                    .setValue(SOUTH, state.getValue(WEST)).setValue(WEST, state.getValue(NORTH));
            case CLOCKWISE_90 -> state.setValue(NORTH, state.getValue(WEST)).setValue(EAST, state.getValue(NORTH))
                    .setValue(SOUTH, state.getValue(EAST)).setValue(WEST, state.getValue(SOUTH));
            default -> state;
        };
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return switch (mirror) {
            case LEFT_RIGHT -> state.setValue(NORTH, state.getValue(SOUTH)).setValue(SOUTH, state.getValue(NORTH));
            case FRONT_BACK -> state.setValue(EAST, state.getValue(WEST)).setValue(WEST, state.getValue(EAST));
            default -> state;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(NORTH, EAST, SOUTH, WEST);
    }
}
