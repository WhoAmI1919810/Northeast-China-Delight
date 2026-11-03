package com.gunmu.northeast_china_delight.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/**
 * 有朝向的摆件（被褥、压水井……）：形状四个朝向一样（都是居中对称的），只有模型跟着朝向转；
 * 不对称的（贴墙的暖气片、炕围画）用 {@link #rotatedShapes} 给四个朝向各一份形状。
 *
 * <p>{@code towardPlayer} 为 true 时正面朝着放的人（压水井的出水嘴冲着人），
 * 否则顺着人看的方向（被褥：枕头在远处、被子朝着人）。</p>
 */
public class FacingDecorBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private final Map<Direction, VoxelShape> shapes;
    private final boolean towardPlayer;

    public FacingDecorBlock(BlockBehaviour.Properties properties, VoxelShape shape, boolean towardPlayer) {
        this(properties, Map.of(Direction.NORTH, shape, Direction.EAST, shape,
                Direction.SOUTH, shape, Direction.WEST, shape), towardPlayer);
    }

    public FacingDecorBlock(BlockBehaviour.Properties properties, Map<Direction, VoxelShape> shapes, boolean towardPlayer) {
        super(properties);
        this.shapes = shapes;
        this.towardPlayer = towardPlayer;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    /**
     * facing=north 时的盒子（像素；模型正面朝北）照方块状态里模型的 y 旋转转到四个朝向：
     * 东 y=90、南 y=180、西 y=270（俯看顺时针）。
     */
    public static Map<Direction, VoxelShape> rotatedShapes(double x0, double y0, double z0,
                                                           double x1, double y1, double z1) {
        Map<Direction, VoxelShape> map = new EnumMap<>(Direction.class);
        map.put(Direction.NORTH, Block.box(x0, y0, z0, x1, y1, z1));
        map.put(Direction.EAST, Block.box(16 - z1, y0, x0, 16 - z0, y1, x1));
        map.put(Direction.SOUTH, Block.box(16 - x1, y0, 16 - z1, 16 - x0, y1, 16 - z0));
        map.put(Direction.WEST, Block.box(z0, y0, 16 - x1, z1, y1, 16 - x0));
        return map;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.get(state.getValue(FACING));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction look = context.getHorizontalDirection();
        return defaultBlockState().setValue(FACING, towardPlayer ? look.getOpposite() : look);
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
        builder.add(FACING);
    }
}
