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
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/**
 * 无器具菜肴的方块：摆在展示乐事的餐盘 / 小餐盘上（模型里自带那块盘子）。
 * 潜行空手右键把菜拿回来、盘子留在原地；拆掉时菜和盘子一起掉。
 */
public class PlatedDishBlock extends PlacedDishBlock
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
    /*@Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit)
    {
        return hand == InteractionHand.MAIN_HAND ? takeBack(level, pos, player) : InteractionResult.PASS;
    }
    *///?} else {
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit)
    {
        return takeBack(level, pos, player);
    }
    //?}

    private InteractionResult takeBack(Level level, BlockPos pos, Player player)
    {
        Block plateBlock = DisplayDelightCompat.plateBlock(this.plate);
        if (!player.isSecondaryUseActive() || !player.getMainHandItem().isEmpty() || plateBlock == null)
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
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params)
    {
        // 仅在真正产生掉落时（生存破坏、爆炸、活塞推碎）一并掉回展示餐盘；
        // 创造拆除、/setblock 和潜行空手拿回菜（takeBack）都不走 getDrops，不会多掉餐盘。
        List<ItemStack> drops = new ArrayList<>(super.getDrops(state, params));
        ItemStack plateItem = DisplayDelightCompat.plateItem(this.plate);
        if (!plateItem.isEmpty())
        {
            drops.add(plateItem);
        }
        return drops;
    }
}
