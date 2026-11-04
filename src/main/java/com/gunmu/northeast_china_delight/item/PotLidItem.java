package com.gunmu.northeast_china_delight.item;

import com.gunmu.northeast_china_delight.util.DdStacks;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
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
/*import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
*///?}

/**
 * 木锅盖：盖在大灶的大铁锅上（见 BigStoveBlock），也能拿在手里当盾牌（2026-11-04 用户：锅盖起到盾牌的作用，
 * 只能格挡 10% 的伤害）。
 *
 * <p>右键举起来，和原版盾牌一样：举满 5 刻才算挡着、只挡正面来的攻击、斧子能把它打下来；
 * 但挡住的只有一成伤害（{@link #BLOCKED_FRACTION}），其余九成照样挨（{@link #onShieldBlock}）。
 * 潜行点物品栏里的锅盖会进副手（{@link Equipable}，同原版盾牌）。</p>
 *
 * <p>耐久（同日用户改的）：原版盾牌的一半 {@link #DURABILITY}；每挡一下掉 1 点，被斧子劈中（原版判定能打掉盾牌的那一下）
 * 掉最大耐久的六成（{@link #AXE_WEAR}），并且像盾牌一样 5 秒内举不起来。耐久、经验修补能附（附魔书上铁砧；
 * 同原版盾牌，附魔台上附不了），见 data/minecraft/tags/item/enchantable/durability.json。
 * 盖在锅上时大灶把这一个锅盖原样存着（耐久、附魔、改的名字都在），揭下来、拆灶还回来的就是它（BigPotBlockEntity#putLid）。</p>
 *
 * <p>对着大灶右键时先走大灶的交互（盖到锅上），大灶不接（PASS）才举起来，和拿盾牌点方块一样。
 * 「举着」的手里模型见 models/item/big_pot_lid_blocking.json，由客户端的 blocking 属性切换
 * （NortheastChinaDelight.ClientModEvents#onClientSetup）。</p>
 */
public class PotLidItem extends Item implements Equipable {

    /** 挡下的伤害比例：只挡一成（穿齐「东北行头」四件套时五成，见 {@link NortheastOutfit}） */
    public static final float BLOCKED_FRACTION = 0.1F;
    /** 耐久：原版盾牌（336）的一半 */
    public static final int DURABILITY = 168;
    /** 被斧子劈中掉的耐久：最大耐久的六成（四舍五入，168 → 101） */
    private static final float AXE_WEAR = 0.6F;
    /** 被斧子劈中后举不起来的时间（同原版盾牌被打掉，5 秒） */
    private static final int AXE_COOLDOWN = 100;
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
     * 举着锅盖挡住了一下：挡下的伤害改成原本的一成（穿齐四件套时五成，还「铛」一声），耐久自己扣（{@link #wear}）。
     * 原版扣盾牌耐久是按伤害算的（不到 3 点不扣、多了扣 1 + 伤害），这里不要，所以把原版那份关掉。
     * 没挡满，原版就当这一下没挡住（照样受伤、红一下、出受伤的声音），只是伤害少了一些。
     */
    //? if <1.20.2 {
    public static void onShieldBlock(ShieldBlockEvent event) {
        LivingEntity blocker = event.getEntity();
        ItemStack lid = blocker.getUseItem();
        if (!lid.is(ModItems.BIG_POT_LID.get())) {
            return;
        }
        event.setBlockedDamage(event.getOriginalBlockedDamage() * blockedFraction(blocker));
        event.setShieldTakesDamage(false);
        wear(blocker, lid, event.getDamageSource());
    }
    //?} else {
    /*public static void onShieldBlock(LivingShieldBlockEvent event) {
        LivingEntity blocker = event.getEntity();
        ItemStack lid = blocker.getUseItem();
        if (!event.getBlocked() || !lid.is(ModItems.BIG_POT_LID.get())) {
            return;
        }
        event.setBlockedDamage(event.getOriginalBlockedDamage() * blockedFraction(blocker));
        event.setShieldDamage(0.0F);
        wear(blocker, lid, event.getDamageSource());
    }
    *///?}

    /** 穿齐四件套挡五成、「铛」一声；不然只挡一成 */
    private static float blockedFraction(LivingEntity blocker) {
        if (!NortheastOutfit.isComplete(blocker)) {
            return BLOCKED_FRACTION;
        }
        if (!blocker.level().isClientSide()) {
            NortheastOutfit.playLidClang(blocker);
        }
        return NortheastOutfit.LID_BLOCKED_FRACTION;
    }

    /**
     * 扣耐久：挡一下 1 点；被斧子劈中掉最大耐久的六成、5 秒举不起来。扣耐久走原版 hurtAndBreak，
     * 所以耐久附魔照样有几率免掉（斧子那六成也一样）、创造模式不扣。碎了就放下、出盾牌碎掉的声音（同原版盾牌）。
     * 「斧子劈中」用原版判定能不能打掉盾牌的同一个条件：近身打（不是弹射物）、打的人 canDisableShield。
     */
    private static void wear(LivingEntity blocker, ItemStack lid, DamageSource source) {
        if (blocker.level().isClientSide() || !(blocker instanceof Player player)) {
            return;
        }
        boolean axe = !source.is(DamageTypeTags.IS_PROJECTILE)
                && source.getDirectEntity() instanceof LivingEntity attacker && attacker.canDisableShield();
        InteractionHand hand = player.getUsedItemHand();
        DdStacks.hurtAndBreak(lid, axe ? Math.round(lid.getMaxDamage() * AXE_WEAR) : 1, player, hand);
        if (lid.isEmpty()) {
            player.stopUsingItem();
            // Player#playSound 在服务端不发给自己（原版留给客户端自己放），这里是服务端算的，得发给所有人
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHIELD_BREAK,
                    player.getSoundSource(), 0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
        } else if (axe) {
            player.getCooldowns().addCooldown(lid.getItem(), AXE_COOLDOWN);
        }
    }
}
