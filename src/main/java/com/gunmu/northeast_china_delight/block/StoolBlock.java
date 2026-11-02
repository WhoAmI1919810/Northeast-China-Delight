package com.gunmu.northeast_china_delight.block;

import com.gunmu.northeast_china_delight.entity.ModEntities;
import com.gunmu.northeast_china_delight.entity.SeatEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
//? if <1.20.5 {
import net.minecraft.world.InteractionHand;
//?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 小板凳：右键坐上去（潜行右键不坐，留给放方块），潜行起身。一张板凳只能坐一个人。
 * 坐的时候骑的是一个看不见的 {@link SeatEntity}。
 */
public class StoolBlock extends DishBlock {

    public static final net.minecraft.world.phys.shapes.VoxelShape SHAPE = Block.box(3, 0, 4, 13, 6.5, 12);

    public StoolBlock(BlockBehaviour.Properties properties) {
        super(properties, SHAPE);
    }

    //? if <1.20.5 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hitResult) {
        return sit(level, pos, player);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        return sit(level, pos, player);
    }
    *///?}

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
