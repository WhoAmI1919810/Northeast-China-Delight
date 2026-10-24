package com.gunmu.northeast_china_delight.block;

import com.gunmu.northeast_china_delight.compat.displaydelight.DisplayDelightCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 无器具菜肴的方块：摆在展示乐事的餐盘 / 小餐盘上（模型里自带那块盘子）。
 * 空手右键把菜拿回来、盘子留在原地；拆掉时菜和盘子一起掉。
 */
public class PlatedDishBlock extends DishBlock
{
    private final ResourceLocation plate;

    public PlatedDishBlock(Properties properties, VoxelShape shape, ResourceLocation plate)
    {
        super(properties, shape);
        this.plate = plate;
    }

    /** 这道菜要摆的那种盘子（{@link DisplayDelightCompat#PLATE} 或 {@link DisplayDelightCompat#SMALL_PLATE}） */
    public ResourceLocation plate()
    {
        return this.plate;
    }

    //? if <1.20.5 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit)
    {
        return hand == InteractionHand.MAIN_HAND ? takeBack(level, pos, player) : InteractionResult.PASS;
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit)
    {
        return takeBack(level, pos, player);
    }
    *///?}

    private InteractionResult takeBack(Level level, BlockPos pos, Player player)
    {
        Block plateBlock = DisplayDelightCompat.plateBlock(this.plate);
        if (!player.getMainHandItem().isEmpty() || plateBlock == null)
        {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide)
        {
            // 菜肴物品和方块同名（料理物品大多不是方块物品，asItem 拿不到）
            ItemStack dish = new ItemStack(BuiltInRegistries.ITEM.get(BuiltInRegistries.BLOCK.getKey(this)));
            level.setBlock(pos, Block.updateFromNeighbourShapes(plateBlock.defaultBlockState(), level, pos), 3);
            if (!player.addItem(dish))
            {
                player.drop(dish, false);
            }
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6F, 1.2F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving)
    {
        // 拿回菜时方块换回盘子，盘子不用另掉；其它拆法（挖掉、炸掉、活塞推）把盘子还出来，菜按掉落表掉
        if (!state.is(newState.getBlock()) && !level.isClientSide
                && !BuiltInRegistries.BLOCK.getKey(newState.getBlock()).equals(this.plate))
        {
            ItemStack plateItem = DisplayDelightCompat.plateItem(this.plate);
            if (!plateItem.isEmpty())
            {
                popResource(level, pos, plateItem);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
