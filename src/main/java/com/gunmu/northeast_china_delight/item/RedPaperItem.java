package com.gunmu.northeast_china_delight.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
//? if >=1.20.5 {
import net.minecraft.world.entity.EquipmentSlot;
//?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.level.Level;

/**
 * 红纸：剪窗花的料。一只手拿红纸、另一只手拿剪刀，右键剪出一张窗花（剪刀掉 1 点耐久）。
 *
 * <p>剪刀在主手、红纸在副手也行：剪刀自己没有右键动作，原版接着会用副手的红纸，同样走到这里。
 * 认剪刀按 {@link ShearsItem} 的子类算，别的模组的剪刀只要继承原版剪刀也能用。</p>
 */
public class RedPaperItem extends Item {

    public RedPaperItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack paper = player.getItemInHand(hand);
        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack shears = player.getItemInHand(otherHand);
        if (!(shears.getItem() instanceof ShearsItem)) {
            return InteractionResultHolder.pass(paper);
        }
        if (!level.isClientSide) {
            //? if <1.20.5 {
            /*shears.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(otherHand));
            *///?} else {
            shears.hurtAndBreak(1, player,
                    otherHand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            //?}
            if (!player.getAbilities().instabuild) {
                paper.shrink(1);
            }
            ItemStack cut = new ItemStack(ModItems.PAPER_CUT.get());
            if (!player.getInventory().add(cut)) {
                player.drop(cut, false);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHEEP_SHEAR,
                    SoundSource.PLAYERS, 0.8F, 1.3F);
        }
        return InteractionResultHolder.sidedSuccess(paper, level.isClientSide());
    }
}
