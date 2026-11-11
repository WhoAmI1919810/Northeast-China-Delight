package com.gunmu.northeast_china_delight.block;


import com.gunmu.chinese_regional_culinary_ingredients.block.AnimalOilBlock;
import com.gunmu.chinese_regional_culinary_ingredients.block.FacingDecorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
//? if <1.20.5 {
import net.minecraft.world.InteractionHand;
//?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

/**
 * 障子柴门（秫秸障子柴门、板障子柴门）：和障子一样高的一扇门，障子会自己接到门两边（见 {@link FenceScreenBlock}）。
 * 关着时的碰撞箱照原版栅栏门，1.5 格高跳不过去；开着时没有碰撞箱。
 *
 * <p>原版栅栏门从另一面推开时会把朝向翻过来（门总往人推的方向开）。这扇门的合页只在一边，
 * 开着时门扇贴在合页那边，朝向一翻合页就跳到对面去了，所以开关门只切 open、不动朝向。
 * 开关门的声音照原版同木材的栅栏门（原版的 type 字段是私有的，这里自己存一份）。</p>
 *
 * <p>合页在哪边（{@link #HINGE}，和门一样左 / 右，按放的人看过去）：默认右边。旁边已经有一扇顺着同一条线、
 * 合页在远离这边的柴门时，这扇放在另一边，两扇凑成一对从中间往两边开。模型是合页在左的一套（朝北放时门轴在西边）：
 * 关着的合页在右就是这套转 180°；开着的门轴原地不动、门扇绕门轴往朝向那一侧推开（2026-11-03 用户：固定一根轴、
 * 开关方向不变），合页在右用左右镜像的那份（{@code _open_right}），所以一对门往同一侧开。</p>
 *
 * <p>朝向顺着障子：放的时候旁边只在一条线上有障子（或顺着这条线的柴门），门扇就顺着那条线，
 * 不管人是从哪个方向看过来的；正面朝人站的那一侧。</p>
 *
 * <p>选框：关着时照原版（不随 in_wall 变矮，模型也不变矮）；开着时只剩门轴和门扇根上那截（伸到隔壁那格的门扇没有选框）。
 * 不重写 codec()，理由同 {@link AnimalOilBlock}。</p>
 */
public class ScreenGateBlock extends FenceGateBlock {

    private static final VoxelShape CLOSED_Z = Block.box(0, 0, 6, 16, 16, 10);
    private static final VoxelShape CLOSED_X = Block.box(6, 0, 0, 10, 16, 16);
    /**
     * 开着时（朝北放、合页在左）：门轴原地不动，门扇绕门轴往北推开，一半伸到北边那格；这一格里只剩门轴和门扇根上那截
     * x 0..1.75、z 0..9.75。合页在右是左右镜像。其余朝向照模型的 y 旋转转过去
     */
    private static final Map<Direction, VoxelShape> OPEN_LEFT = FacingDecorBlock.rotatedShapes(0, 0, 0, 1.75, 16, 9.75);
    private static final Map<Direction, VoxelShape> OPEN_RIGHT = FacingDecorBlock.rotatedShapes(14.25, 0, 0, 16, 16, 9.75);

    public static final EnumProperty<DoorHingeSide> HINGE = BlockStateProperties.DOOR_HINGE;

    private final WoodType woodType;

    public ScreenGateBlock(BlockBehaviour.Properties properties, WoodType woodType) {
        //? if <1.20.5 {
        super(properties, woodType);
        //?} else {
        /*super(woodType, properties);
        *///?}
        this.woodType = woodType;
        registerDefaultState(defaultBlockState().setValue(HINGE, DoorHingeSide.RIGHT));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        if (state.getValue(OPEN)) {
            return (state.getValue(HINGE) == DoorHingeSide.RIGHT ? OPEN_RIGHT : OPEN_LEFT).get(facing);
        }
        return facing.getAxis() == Direction.Axis.Z ? CLOSED_Z : CLOSED_X;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction facing = state.getValue(FACING);
        Vec3 from = context.getPlayer() != null ? context.getPlayer().position() : context.getClickLocation();
        // 顺着障子：只在东西一条线上有障子，门扇横着放（朝南 / 北，看人站在哪边）；南北同理
        boolean lineX = lineAt(level, pos, Direction.EAST) || lineAt(level, pos, Direction.WEST);
        boolean lineZ = lineAt(level, pos, Direction.NORTH) || lineAt(level, pos, Direction.SOUTH);
        if (lineX && !lineZ && facing.getAxis() == Direction.Axis.X) {
            facing = from.z < pos.getZ() + 0.5 ? Direction.SOUTH : Direction.NORTH;
        } else if (lineZ && !lineX && facing.getAxis() == Direction.Axis.Z) {
            facing = from.x < pos.getX() + 0.5 ? Direction.EAST : Direction.WEST;
        }
        boolean inWall = facing.getAxis() == Direction.Axis.Z
                ? isWall(level.getBlockState(pos.west())) || isWall(level.getBlockState(pos.east()))
                : isWall(level.getBlockState(pos.north())) || isWall(level.getBlockState(pos.south()));
        // 合页：默认右边；旁边那扇柴门合页在远离这边时，这扇合页放另一边，凑成一对
        Direction right = facing.getClockWise();
        Direction hinge = right;
        for (Direction side : new Direction[]{right, right.getOpposite()}) {
            BlockState neighbor = level.getBlockState(pos.relative(side));
            if (neighbor.getBlock() instanceof ScreenGateBlock
                    && neighbor.getValue(FACING).getAxis() == facing.getAxis() && hingeSide(neighbor) == side) {
                hinge = side.getOpposite();
                break;
            }
        }
        return state.setValue(FACING, facing).setValue(IN_WALL, inWall)
                .setValue(HINGE, hinge == right ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT);
    }

    /** 合页在世界里的哪一边 */
    private static Direction hingeSide(BlockState state) {
        Direction facing = state.getValue(FACING);
        return state.getValue(HINGE) == DoorHingeSide.RIGHT ? facing.getClockWise() : facing.getCounterClockWise();
    }

    /** 那边是障子，或者顺着这条线的柴门 */
    private static boolean lineAt(BlockGetter level, BlockPos pos, Direction direction) {
        BlockState neighbor = level.getBlockState(pos.relative(direction));
        return neighbor.getBlock() instanceof FenceScreenBlock
                || neighbor.getBlock() instanceof ScreenGateBlock && FenceGateBlock.connectsToDirection(neighbor, direction);
    }

    private static boolean isWall(BlockState state) {
        return state.is(BlockTags.WALLS);
    }

    /** 镜像：朝向照原版转，合页左右对调（同原版门） */
    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return mirror == Mirror.NONE ? state
                : state.rotate(mirror.getRotation(state.getValue(FACING))).cycle(HINGE);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HINGE);
    }

    //? if <1.20.5 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hitResult) {
        return toggle(state, level, pos, player);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        return toggle(state, level, pos, player);
    }
    *///?}

    /** 照原版栅栏门开关门（声音、游戏事件都一样），只是不改朝向 */
    private InteractionResult toggle(BlockState state, Level level, BlockPos pos, Player player) {
        boolean open = !state.getValue(OPEN);
        level.setBlock(pos, state.setValue(OPEN, open), Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);
        level.playSound(player, pos, open ? woodType.fenceGateOpen() : woodType.fenceGateClose(),
                SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);
        level.gameEvent(player, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
