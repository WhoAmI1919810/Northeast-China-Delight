package com.gunmu.northeast_china_delight.block;

import com.gunmu.northeast_china_delight.item.ModItems;
import com.gunmu.northeast_china_delight.item.SeasoningBottleItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
//? if >=1.20.5 {
import net.minecraft.world.ItemInteractionResult;
//?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
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
 *
 * <p>手里拿的不是盛东西的家伙（空手、木棍、方块……）就光压一下：出水嘴喷一股水花（只有粒子和声音，不放水方块）。
 * 和别的方块的空手右键一样只认主手。</p>
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
        InteractionResult result = pump(player.getItemInHand(hand), level, pos, player, hand);
        return result == InteractionResult.PASS && hand == InteractionHand.MAIN_HAND
                ? splash(state, level, pos, player)
                : result;
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

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        return splash(state, level, pos, player);
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

    /**
     * 光压不接水：出水嘴（朝北放时在 8, 7, 2）往下喷一股水花。
     * 哪只手里有盛东西的家伙就让开 —— 主手的桶自己去倒水，副手拿着空桶的轮到副手时再压水。
     */
    private static InteractionResult splash(BlockState state, Level level, BlockPos pos, Player player) {
        if (isContainer(player.getMainHandItem()) || isContainer(player.getOffhandItem())) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            Direction facing = state.getValue(FACING);
            double x = pos.getX() + 0.5 + facing.getStepX() * 0.375;
            double y = pos.getY() + 0.42;
            double z = pos.getZ() + 0.5 + facing.getStepZ() * 0.375;
            server.sendParticles(ParticleTypes.SPLASH, x, y - 0.05, z, 18, 0.05, 0.03, 0.05, 0.12);
            server.sendParticles(ParticleTypes.FALLING_WATER, x, y, z, 6, 0.03, 0.0, 0.03, 0.0);
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.5F, 1.2F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** 盛东西的家伙：各种桶、玻璃瓶 / 药水瓶、大脸盆 / 水盆、调料瓶 */
    private static boolean isContainer(ItemStack stack) {
        return stack.getItem() instanceof BucketItem || stack.is(Items.MILK_BUCKET)
                || stack.is(Items.GLASS_BOTTLE) || stack.is(Items.POTION)
                || stack.is(ModItems.LARGE_BASIN.get()) || stack.is(ModItems.WATER_BASIN.get())
                || SeasoningBottleItem.isBottle(stack);
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
