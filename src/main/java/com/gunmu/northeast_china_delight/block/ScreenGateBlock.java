package com.gunmu.northeast_china_delight.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
//? if <1.20.5 {
import net.minecraft.world.InteractionHand;
//?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

/**
 * 障子柴门（秫秸障子柴门、板障子柴门）：和障子一样高的一扇门，障子会自己接到门两边（见 {@link FenceScreenBlock}）。
 * 关着时的碰撞箱照原版栅栏门，1.5 格高跳不过去；开着时没有碰撞箱。
 *
 * <p>原版栅栏门从另一面推开时会把朝向翻过来（门总往人推的方向开）。这扇门的合页只在一边（朝北放时在西边），
 * 开着时门扇贴在合页那边，朝向一翻合页就跳到对面去了，所以开关门只切 open、不动朝向。
 * 开关门的声音照原版同木材的栅栏门（原版的 type 字段是私有的，这里自己存一份）。</p>
 *
 * <p>选框：关着时照原版（不随 in_wall 变矮，模型也不变矮）；开着时贴着合页那边。
 * 不重写 codec()，理由同 {@link AnimalOilBlock}。</p>
 */
public class ScreenGateBlock extends FenceGateBlock {

    private static final VoxelShape CLOSED_Z = Block.box(0, 0, 6, 16, 16, 10);
    private static final VoxelShape CLOSED_X = Block.box(6, 0, 0, 10, 16, 16);
    /** 开着时门扇在合页那边 x 0..3.5（朝北放时）；其余朝向照模型的 y 旋转转过去 */
    private static final Map<Direction, VoxelShape> OPEN_SHAPES = FacingDecorBlock.rotatedShapes(0, 0, 0, 3.5, 16, 16);

    private final WoodType woodType;

    public ScreenGateBlock(BlockBehaviour.Properties properties, WoodType woodType) {
        //? if <1.20.5 {
        super(properties, woodType);
        //?} else {
        /*super(woodType, properties);
        *///?}
        this.woodType = woodType;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        if (state.getValue(OPEN)) {
            return OPEN_SHAPES.get(facing);
        }
        return facing.getAxis() == Direction.Axis.Z ? CLOSED_Z : CLOSED_X;
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
