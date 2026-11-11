package com.gunmu.northeast_china_delight.block;


import com.gunmu.chinese_regional_culinary_ingredients.block.DishBlock;
import com.gunmu.northeast_china_delight.entity.ModEntities;
import com.gunmu.northeast_china_delight.entity.SeatEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
//? if <1.20.5 {
/*import net.minecraft.world.InteractionHand;
*///?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 小板凳：右键坐上去（潜行右键不坐，留给放方块），潜行起身。一张板凳只能坐一个人。
 * 坐的时候骑的是一个看不见的 {@link SeatEntity}。
 *
 * <p>{@link #FACING} 是放的人面朝的方向：长的那条边横在人面前（2026-11-03 用户：放置所对的方向是长边）。
 * 模型朝北放时长边沿 x（x 3..13、z 4..12），东西向放时转 90°。</p>
 */
public class StoolBlock extends DishBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    /** 朝南北放（长边沿 x） */
    public static final VoxelShape SHAPE = Block.box(3, 0, 4, 13, 6.5, 12);
    /** 朝东西放（长边沿 z） */
    private static final VoxelShape SHAPE_Z = Block.box(4, 0, 3, 12, 6.5, 13);

    public StoolBlock(BlockBehaviour.Properties properties) {
        super(properties, SHAPE);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    private static VoxelShape shapeOf(BlockState state) {
        return state.getValue(FACING).getAxis() == Direction.Axis.X ? SHAPE_Z : SHAPE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeOf(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeOf(state);
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
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    //? if <1.20.5 {
    /*@Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hitResult) {
        return sit(level, pos, player);
    }*/
    //?} else {
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        return sit(level, pos, player);
    }
    //?}

    private static InteractionResult sit(Level level, BlockPos pos, Player player) {
        if (player.isSecondaryUseActive() || player.isPassenger()
                || !level.getEntitiesOfClass(SeatEntity.class, new AABB(pos)).isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            SeatEntity seat = new SeatEntity(ModEntities.SEAT.get(), level);
            seat.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            level.addFreshEntity(seat);
            player.startRiding(seat);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
