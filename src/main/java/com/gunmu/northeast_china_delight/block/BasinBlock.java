package com.gunmu.northeast_china_delight.block;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
//? if >=1.20.5 {
import net.minecraft.world.ItemInteractionResult;
//?}
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
 * <p>照原版炼药锅倒水、舀水的写法：拿水桶右键空盆，倒进去变成水盆、手里剩空桶；
 * 拿空桶右键水盆，舀出一桶水、盆变回空盆。一盆水就是一桶水（1000 mB），舀进舀出不多不少。</p>
 */
public class BasinBlock extends DishBlock
{
    private final boolean filled;

    public BasinBlock(BlockBehaviour.Properties properties, boolean filled)
    {
        super(properties, BASIN_SHAPE);
        this.filled = filled;
    }

    //? if <1.20.5 {
    /*@Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hitResult)
    {
        return pourOrScoop(player.getItemInHand(hand), level, pos, player, hand);
    }*/
    //?} else {
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult)
    {
        InteractionResult result = pourOrScoop(stack, level, pos, player, hand);
        return result == InteractionResult.PASS
                ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                : ItemInteractionResult.sidedSuccess(level.isClientSide());
    }
    //?}

    private InteractionResult pourOrScoop(ItemStack held, Level level, BlockPos pos, Player player, InteractionHand hand)
    {
        boolean pour = !filled && held.is(Items.WATER_BUCKET);
        boolean scoop = filled && held.is(Items.BUCKET);
        if (!pour && !scoop)
        {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide)
        {
            player.awardStat(Stats.ITEM_USED.get(held.getItem()));
            ItemStack result = new ItemStack(pour ? Items.BUCKET : Items.WATER_BUCKET);
            player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, result));
            Block next = pour ? ModBlocks.WATER_BASIN.get() : ModBlocks.LARGE_BASIN.get();
            level.setBlock(pos, next.defaultBlockState(), Block.UPDATE_ALL);
            level.playSound(null, pos, pour ? SoundEvents.BUCKET_EMPTY : SoundEvents.BUCKET_FILL,
                    SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(player, pour ? GameEvent.FLUID_PLACE : GameEvent.FLUID_PICKUP, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
