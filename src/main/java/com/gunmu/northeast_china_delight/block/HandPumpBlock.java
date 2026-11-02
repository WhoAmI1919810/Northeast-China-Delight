package com.gunmu.northeast_china_delight.block;

import com.gunmu.northeast_china_delight.item.ModItems;
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
import net.minecraft.world.item.Item;
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
 * 压水井：拿空桶、空的大脸盆、玻璃瓶右键，压出一桶水 / 一盆水 / 一瓶水。水不限量（地下水）。
 */
public class HandPumpBlock extends FacingDecorBlock {

    public static final net.minecraft.world.phys.shapes.VoxelShape SHAPE = Block.box(5, 0, 5, 11, 15, 11);

    public HandPumpBlock(BlockBehaviour.Properties properties) {
        super(properties, SHAPE, true);
    }

    //? if <1.20.5 {
    /*@Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hitResult) {
        return pump(player.getItemInHand(hand), level, pos, player, hand);
    }*/
    //?} else {
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        InteractionResult result = pump(stack, level, pos, player, hand);
        return result == InteractionResult.PASS
                ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                : ItemInteractionResult.sidedSuccess(level.isClientSide());
    }
    //?}

    private static InteractionResult pump(ItemStack held, Level level, BlockPos pos, Player player, InteractionHand hand) {
        ItemStack filled;
        if (held.is(Items.BUCKET)) {
            filled = new ItemStack(Items.WATER_BUCKET);
        } else if (held.is(ModItems.LARGE_BASIN.get())) {
            filled = new ItemStack(ModItems.WATER_BASIN.get());
        } else if (held.is(Items.GLASS_BOTTLE)) {
            filled = waterBottle();
        } else {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            Item used = held.getItem();
            player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, filled));
            player.awardStat(Stats.ITEM_USED.get(used));
            level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static ItemStack waterBottle() {
        //? if <1.20.5 {
        /*return net.minecraft.world.item.alchemy.PotionUtils.setPotion(new ItemStack(Items.POTION),
                net.minecraft.world.item.alchemy.Potions.WATER);
        *///?} else {
        return net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION,
                net.minecraft.world.item.alchemy.Potions.WATER);
        //?}
    }
}
