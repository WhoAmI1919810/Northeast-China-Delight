package com.gunmu.northeast_china_delight.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
//? if <1.20.2 {
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
//?} else {
/*import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
*///?}

/**
 * 木锅盖：盖在大灶的大铁锅上（见 BigStoveBlock），也能拿在手里当盾牌（2026-11-04 用户：锅盖起到盾牌的作用，
 * 没有耐久条，但只能格挡 10% 的伤害）。
 *
 * <p>右键举起来，和原版盾牌一样：举满 5 刻才算挡着、只挡正面来的攻击、斧子能把它打下来；
 * 但挡住的只有一成伤害（{@link #BLOCKED_FRACTION}），其余九成照样挨（{@link #onShieldBlock}）。
 * 没有耐久，挨多少下都不坏，所以也进不了附魔台。潜行点物品栏里的锅盖会进副手（{@link Equipable}，同原版盾牌）。</p>
 *
 * <p>对着大灶右键时先走大灶的交互（盖到锅上），大灶不接（PASS）才举起来，和拿盾牌点方块一样。
 * 「举着」的手里模型见 models/item/big_pot_lid_blocking.json，由客户端的 blocking 属性切换
 * （NortheastChinaDelight.ClientModEvents#onClientSetup）。</p>
 */
public class PotLidItem extends Item implements Equipable {

    /** 挡下的伤害比例：只挡一成 */
    public static final float BLOCKED_FRACTION = 0.1F;
    /** 举着不放就一直举着（同原版盾牌） */
    private static final int USE_DURATION = 72000;

    public PotLidItem(Properties properties) {
        super(properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    //? if <1.20.5 {
    @Override
    public int getUseDuration(ItemStack stack) {
        return USE_DURATION;
    }
    //?} else {
    /*@Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION;
    }
    *///?}

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.OFFHAND;
    }

    /** 让（Neo）Forge 把它当盾牌：LivingEntity#isBlocking 认的是这个，不是物品类 */
    //? if <1.20.2 {
    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction toolAction) {
        return ToolActions.DEFAULT_SHIELD_ACTIONS.contains(toolAction);
    }
    //?} else {
    /*@Override
    public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
        return ItemAbilities.DEFAULT_SHIELD_ACTIONS.contains(itemAbility);
    }
    *///?}

    /**
     * 举着锅盖挡住了一下：挡下的伤害改成原本的一成，锅盖也不扣耐久（本来就没有）。
     * 没挡满，原版就当这一下没挡住（照样受伤、红一下、出受伤的声音），只是伤害少了一成。
     */
    //? if <1.20.2 {
    public static void onShieldBlock(ShieldBlockEvent event) {
        if (!event.getEntity().getUseItem().is(ModItems.BIG_POT_LID.get())) {
            return;
        }
        event.setBlockedDamage(event.getOriginalBlockedDamage() * BLOCKED_FRACTION);
        event.setShieldTakesDamage(false);
    }
    //?} else {
    /*public static void onShieldBlock(LivingShieldBlockEvent event) {
        if (!event.getBlocked() || !event.getEntity().getUseItem().is(ModItems.BIG_POT_LID.get())) {
            return;
        }
        event.setBlockedDamage(event.getOriginalBlockedDamage() * BLOCKED_FRACTION);
        event.setShieldDamage(0.0F);
    }
    *///?}
}
