package com.gunmu.northeast_china_delight.block;

import com.gunmu.northeast_china_delight.util.DdStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
//? if >=1.20.5 {
import net.minecraft.world.ItemInteractionResult;
//?}
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * 本模组的通用作物方块。
 *
 * 行为完全沿用原版 {@link CropBlock}（7 个生长阶段、随机刻生长、骨粉催熟），
 * 只是把「种子物品」抽出来做成可配置的，避免为每种作物各写一个类。
 *
 * 果菜类（黄瓜、茄子、青椒、豆角）额外支持用剪刀采摘：只摘走果实，
 * 植株退回 {@link #SHEARED_AGE} 阶段继续生长。
 *
 * 注意：NeoForge 把 {@code mayPlaceOn} 改成了 {@code instanceof FarmBlock}，
 * 因此本模组作物可以直接种在农夫乐事的富饶农田上，并享受其生长加成。
 */
public class DdCropBlock extends CropBlock {

    /** 剪刀采摘后植株退回的生长阶段 */
    public static final int SHEARED_AGE = 4;

    private final Supplier<? extends ItemLike> seed;

    /** 可以用剪刀采摘的果实；为 null 表示只能等成熟后整株收获 */
    @Nullable
    private final Supplier<? extends ItemLike> shearsHarvest;

    public DdCropBlock(BlockBehaviour.Properties properties, Supplier<? extends ItemLike> seed) {
        this(properties, seed, null);
    }

    public DdCropBlock(BlockBehaviour.Properties properties, Supplier<? extends ItemLike> seed,
                       @Nullable Supplier<? extends ItemLike> shearsHarvest) {
        super(properties);
        this.seed = seed;
        this.shearsHarvest = shearsHarvest;
    }

    /** 用于「选取方块」时给回种子 */
    @Override
    protected ItemLike getBaseSeedId() {
        return this.seed.get();
    }

    /**
     * 成熟后用剪刀右键：只摘走果实，植株退回幼株继续生长（消耗剪刀 1 点耐久）。
     */
    //? if <1.20.5 {
    /*@Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (this.shearsHarvest != null && stack.is(Items.SHEARS) && this.isMaxAge(state)) {
            if (!level.isClientSide) {
                popResource(level, pos, new ItemStack(this.shearsHarvest.get()));
                level.setBlock(pos, this.getStateForAge(SHEARED_AGE), 2);
                DdStacks.hurtAndBreak(stack, 1, player, hand);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.use(state, level, pos, player, hand, hit);
    }*/
    //?} else {
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (this.shearsHarvest != null && stack.is(Items.SHEARS) && this.isMaxAge(state)) {
            if (!level.isClientSide) {
                popResource(level, pos, new ItemStack(this.shearsHarvest.get()));
                level.setBlock(pos, this.getStateForAge(SHEARED_AGE), 2);
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }
    //?}
}
