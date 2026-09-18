package com.gunmu.dongbei_delight.block;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
//? if <1.21.4 {
import net.minecraft.world.ItemInteractionResult;
//?}
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

public class Vat extends Block {
    public static final IntegerProperty WATER_LEVEL = ModBlockStateProperties.WATER_LEVEL;

    public Vat(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(WATER_LEVEL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WATER_LEVEL);
    }

    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        return this.defaultBlockState().setValue(WATER_LEVEL, 0);
    }

    // 手持水桶右键：往空缸里倒水。返回本次交互是否真的装了水。
    private boolean tryFillWithWater(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player) {
        if (!stack.is(Items.WATER_BUCKET) || state.getValue(WATER_LEVEL) != 0) {
            return false;
        }
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(WATER_LEVEL, 1), 3);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
                player.addItem(new ItemStack(Items.BUCKET));
            }
        }
        return true;
    }

    // 1.21.4 起 useItemOn 的返回类型由 ItemInteractionResult 改回了 InteractionResult，
    // 对应的「交给默认方块交互」也从 PASS_TO_DEFAULT_BLOCK_INTERACTION 变为 TRY_WITH_EMPTY_HAND。
    //? if >=1.21.4 {
    /*@Override
    protected @NotNull InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return tryFillWithWater(stack, state, level, pos, player)
                ? InteractionResult.SUCCESS
                : InteractionResult.TRY_WITH_EMPTY_HAND;
    }*/
    //?} else {
    @Override
    protected @NotNull ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return tryFillWithWater(stack, state, level, pos, player)
                ? ItemInteractionResult.sidedSuccess(level.isClientSide())
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    //?}

    // 未持有水桶且缸空时右键
    @Override
    protected @NotNull InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        int waterLevel = state.getValue(WATER_LEVEL);
        /*if (waterLevel == 0) {
            if (!level.isClientSide) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof MenuProvider provider) {
                    player.openMenu(provider);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }*/
        return InteractionResult.PASS;
    }
}
