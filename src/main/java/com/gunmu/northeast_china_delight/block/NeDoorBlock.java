package com.gunmu.northeast_china_delight.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
//? if <1.20.5 {
import net.minecraft.world.InteractionHand;
//?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 本模组的门：红漆铁大门、糊纸木格门。
 *
 * <p>红漆铁大门用原版铁门的声音（{@link BlockSetType#IRON}），但它是院门，得能用手推开 ——
 * 原版铁门的类型规定不能手开，这里 {@code openByHand} 为 true 时绕过去，直接调原版的 {@code setOpen}
 * （开关门、声音、游戏事件都照原版）。</p>
 *
 * <p>只是为了这一点继承；不重写 codec()，理由同 {@link AnimalOilBlock}。</p>
 */
public class NeDoorBlock extends DoorBlock {

    private final boolean openByHand;

    public NeDoorBlock(BlockBehaviour.Properties properties, BlockSetType type, boolean openByHand) {
        //? if <1.20.2 {
        super(properties, type);
        //?} else {
        /*super(type, properties);
        *///?}
        this.openByHand = openByHand;
    }

    //? if <1.20.5 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hitResult) {
        if (!openByHand || type().canOpenByHand()) {
            return super.use(state, level, pos, player, hand, hitResult);
        }
        setOpen(player, level, state, pos, !state.getValue(OPEN));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        if (!openByHand || type().canOpenByHand()) {
            return super.useWithoutItem(state, level, pos, player, hitResult);
        }
        setOpen(player, level, state, pos, !state.getValue(OPEN));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    *///?}
}
