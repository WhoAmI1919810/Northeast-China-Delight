package com.gunmu.northeast_china_delight.block;

import com.gunmu.northeast_china_delight.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
//? if >=1.20.5 {
/*import net.minecraft.world.ItemInteractionResult;
*///?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 放在地上的大脸盆（空盆 / 水盆）。
 *
 * <p>照原版炼药锅倒水、舀水的写法：拿水桶（或水盆）右键空盆，倒进去变成水盆、手里剩空桶（空盆）；
 * 拿空桶（或空盆）右键水盆，舀走一盆水、盆变回空盆。一盆水就是一桶水（1000 mB），舀进舀出不多不少。
 * 地上的盆（空盆、水盆）潜行空手右键，直接收回物品栏。</p>
 */
public class BasinBlock extends PlacedDishBlock
{
    private final boolean filled;

    public BasinBlock(BlockBehaviour.Properties properties, boolean filled)
    {
        super(properties, BASIN_SHAPE);
        this.filled = filled;
    }

    @Override
    protected void spawnDestroyParticles(Level level, Player player, BlockPos pos, BlockState state)
    {
        if (this.filled)
        {
            level.levelEvent(player, 2001, pos,
                    Block.getId(ModBlocks.LARGE_BASIN.get().defaultBlockState()));
            return;
        }
        super.spawnDestroyParticles(level, player, pos, state);
    }

    //? if <1.20.5 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hitResult)
    {
        ItemStack held = player.getItemInHand(hand);
        return held.isEmpty() ? pickUp(level, pos, player) : pourOrScoop(held, level, pos, player, hand);
    }
    //?} else {
    /*@Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult)
    {
        InteractionResult result = pourOrScoop(stack, level, pos, player, hand);
        return result == InteractionResult.PASS
                ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                : ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult)
    {
        return pickUp(level, pos, player);
    }
    *///?}

    /** 地上的盆：潜行空手右键收回物品栏（背包满了就掉在脚下） */
    private InteractionResult pickUp(Level level, BlockPos pos, Player player)
    {
        if (!player.isSecondaryUseActive())
        {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide)
        {
            level.removeBlock(pos, false);
            level.gameEvent(player, GameEvent.BLOCK_DESTROY, pos);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, 1.0F);
            ItemStack item = new ItemStack(filled ? ModItems.WATER_BASIN.get() : ModItems.LARGE_BASIN.get());
            if (!player.getInventory().add(item))
            {
                player.drop(item, false);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private InteractionResult pourOrScoop(ItemStack held, Level level, BlockPos pos, Player player, InteractionHand hand)
    {
        // 桶和盆都行：倒进去剩空桶 / 空盆，舀出来得水桶 / 水盆
        boolean basin = held.is(ModItems.WATER_BASIN.get()) || held.is(ModItems.LARGE_BASIN.get());
        boolean pour = !filled && (held.is(Items.WATER_BUCKET) || held.is(ModItems.WATER_BASIN.get()));
        boolean scoop = filled && (held.is(Items.BUCKET) || held.is(ModItems.LARGE_BASIN.get()));
        if (!pour && !scoop)
        {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide)
        {
            player.awardStat(Stats.ITEM_USED.get(held.getItem()));
            ItemStack result = new ItemStack(basin
                    ? (pour ? ModItems.LARGE_BASIN.get() : ModItems.WATER_BASIN.get())
                    : (pour ? Items.BUCKET : Items.WATER_BUCKET));
            player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, result));
            Block next = pour ? ModBlocks.WATER_BASIN.get() : ModBlocks.LARGE_BASIN.get();
            level.setBlock(pos, PlacedDishBlock.withFacing(next.defaultBlockState(), level.getBlockState(pos)),
                    Block.UPDATE_ALL);
            level.playSound(null, pos, pour ? SoundEvents.BUCKET_EMPTY : SoundEvents.BUCKET_FILL,
                    SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(player, pour ? GameEvent.FLUID_PLACE : GameEvent.FLUID_PICKUP, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
