package com.gunmu.northeast_china_delight.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
//? if <1.20.5 {
import net.minecraft.world.InteractionResult;
//?} else {
/*import net.minecraft.world.ItemInteractionResult;
*///?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * 水盆 / 空盆和原版炼药锅，照水桶的规矩来（一盆水就是一桶水）：
 * <ul>
 *   <li>水盆右键空炼药锅、或者没满的水炼药锅：倒满，手里剩空盆；</li>
 *   <li>空盆右键水炼药锅：只认满的（和空桶一样），舀走得水盆，炼药锅空了。</li>
 * </ul>
 * 在 commonSetup 的 enqueueWork 里注册（原版这几张交互表不是线程安全的）。
 */
public final class BasinCauldronInteractions {

    private BasinCauldronInteractions() {
    }

    public static void register() {
        CauldronInteraction pour = BasinCauldronInteractions::pour;
        CauldronInteraction scoop = (state, level, pos, player, hand, stack) -> CauldronInteraction.fillBucket(
                state, level, pos, player, hand, stack, new ItemStack(ModItems.WATER_BASIN.get()),
                s -> s.getValue(LayeredCauldronBlock.LEVEL) == 3, SoundEvents.BUCKET_FILL);
        //? if <1.20.5 {
        CauldronInteraction.EMPTY.put(ModItems.WATER_BASIN.get(), pour);
        CauldronInteraction.WATER.put(ModItems.WATER_BASIN.get(), pour);
        CauldronInteraction.WATER.put(ModItems.LARGE_BASIN.get(), scoop);
        //?} else {
        /*CauldronInteraction.EMPTY.map().put(ModItems.WATER_BASIN.get(), pour);
        CauldronInteraction.WATER.map().put(ModItems.WATER_BASIN.get(), pour);
        CauldronInteraction.WATER.map().put(ModItems.LARGE_BASIN.get(), scoop);
        *///?}
    }

    /** 照原版 emptyBucket 写，只是还回来的是空盆不是空桶 */
    //? if <1.20.5 {
    private static InteractionResult pour(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack stack) {
    //?} else {
    /*private static ItemInteractionResult pour(BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, ItemStack stack) {
    *///?}
        if (!level.isClientSide) {
            Item used = stack.getItem();
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(ModItems.LARGE_BASIN.get())));
            player.awardStat(Stats.FILL_CAULDRON);
            player.awardStat(Stats.ITEM_USED.get(used));
            level.setBlockAndUpdate(pos, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
        }
        //? if <1.20.5 {
        return InteractionResult.sidedSuccess(level.isClientSide);
        //?} else {
        /*return ItemInteractionResult.sidedSuccess(level.isClientSide);
        *///?}
    }
}
