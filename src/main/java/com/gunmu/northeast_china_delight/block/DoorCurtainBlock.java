package com.gunmu.northeast_china_delight.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 门帘（棉门帘 / 大花布门帘）：上下两格，像门一样挂在门洞一侧。
 *
 * <p>不用开关：人和生物都能直接掀帘子走过去（没有碰撞箱）。只有上半格顶部的木条参与瞄准和破坏，
 * 正常视高看向帘面时射线会穿过去，可以直接与门帘后面的门互动。
 * 放置时点击位置就是上半格，整条门帘向下补出下半格；朝向取被点击的方块侧面。
 * 上下两半的配对照原版门：一半没了另一半跟着没；掉落表只让下半掉东西，
 * 生存模式敲上半时同步结算下半掉落，创造模式则直接抹掉下半，保证整条门帘只掉一份。</p>
 */
public class DoorCurtainBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    // 只有上半格顶部的木条参与瞄准：帘面本身没有碰撞箱，正常视高可以透过门帘直接点后面的门。
    private static final VoxelShape TOP_SHAPE_NORTH = Block.box(0, 14, 13, 16, 16, 15.5);
    private static final VoxelShape TOP_SHAPE_SOUTH = Block.box(0, 14, 0.5, 16, 16, 3);
    private static final VoxelShape TOP_SHAPE_WEST = Block.box(13, 14, 0, 15.5, 16, 16);
    private static final VoxelShape TOP_SHAPE_EAST = Block.box(0.5, 14, 0, 3, 16, 16);

    public DoorCurtainBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            return Shapes.empty();
        }
        return switch (state.getValue(FACING)) {
            case SOUTH -> TOP_SHAPE_SOUTH;
            case WEST -> TOP_SHAPE_WEST;
            case EAST -> TOP_SHAPE_EAST;
            default -> TOP_SHAPE_NORTH;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        BlockPos belowPos = pos.below();
        if (belowPos.getY() >= level.getMinBuildHeight() && level.getBlockState(belowPos).canBeReplaced(context)) {
            Direction clickedFace = context.getClickedFace();
            Direction facing = clickedFace.getAxis().isHorizontal()
                    ? clickedFace
                    : context.getHorizontalDirection();
            return defaultBlockState()
                    .setValue(FACING, facing)
                    .setValue(HALF, DoubleBlockHalf.UPPER);
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            level.setBlock(pos.below(), state.setValue(HALF, DoubleBlockHalf.LOWER), 3);
        } else {
            level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
        }
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        DoubleBlockHalf half = state.getValue(HALF);
        if (direction.getAxis() == Direction.Axis.Y && (half == DoubleBlockHalf.LOWER) == (direction == Direction.UP)) {
            return neighborState.is(this) && neighborState.getValue(HALF) != half
                    ? state.setValue(FACING, neighborState.getValue(FACING))
                    : Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    //? if <1.20.2 {
    /*@Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        removeLowerHalf(level, pos, state, player);
        super.playerWillDestroy(level, pos, state, player);
    }
    *///?} else {
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        removeLowerHalf(level, pos, state, player);
        return super.playerWillDestroy(level, pos, state, player);
    }
    //?}

    /**
     * 门帘只能从上半格顶部木条处瞄准。破坏上半格时把下半格一并处理：
     * 创造模式直接抹掉，生存模式正常破坏下半格并走掉落表，否则整条门帘会没有掉落物。
     */
    private static void removeLowerHalf(Level level, BlockPos pos, BlockState state, Player player) {
        if (level.isClientSide || state.getValue(HALF) != DoubleBlockHalf.UPPER) {
            return;
        }
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        if (!belowState.is(state.getBlock()) || belowState.getValue(HALF) != DoubleBlockHalf.LOWER) {
            return;
        }
        if (player.isCreative()) {
            level.setBlock(below, Blocks.AIR.defaultBlockState(), 35);
            level.levelEvent(player, 2001, below, Block.getId(belowState));
        } else {
            level.destroyBlock(below, true, player);
        }
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF);
    }
}
