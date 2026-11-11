package com.gunmu.northeast_china_delight.item;


import com.gunmu.chinese_regional_culinary_ingredients.item.PotLidItem;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
//? if <1.20.2 {
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
//?} else {
/*import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
*///?}

import java.util.Map;
import java.util.WeakHashMap;

/**
 * 「东北行头」四件套：头戴大脸盆、身穿大花袄、主手木槌、副手木锅盖（2026-11-04 用户定的三条套装效果）。
 * 穿齐了才有，少一件都不算：
 * <ul>
 *   <li>锅盖真挡事：举锅盖挡下的伤害从三成变成全挡，和盾牌一样（{@link #LID_BLOCKED_FRACTION}，2026-11-05 用户），
 *       见 {@link PotLidItem#onShieldBlock}。以前挡住时还「铛」一声（农夫乐事平底锅的金属声），同日用户要锅盖格挡的声音换成
 *       木头的，「铛」去掉了，挡住的声音见 PotLidItem；</li>
 *   <li>一棒子敲懵：木槌近身打中的东西多被击退一截（相当于击退 I）、缓慢 II 两秒，见 {@link #onLivingDamage} / {@link #onKnockBack}；</li>
 *   <li>自家人：找村民换东西便宜一点（每条交易的第一样要价少 15%，至少少 1 个；村庄英雄 I 是 30%），
 *       见 {@link #applyVillagerDiscount}，由 mixin/VillagerOutfitDiscountMixin 在原版算完声望、村庄英雄折扣之后调用。
 *       流浪商人不算村民，没有这个折扣。</li>
 * </ul>
 * 「你瞅啥？」进度也认这一身（ModGameplayEvents#onLivingChangeTarget）。
 */
public final class NortheastOutfit {

    /** 穿齐时锅盖挡下的伤害比例：全挡，和盾牌一样（单拿锅盖见 {@link PotLidItem#BLOCKED_FRACTION}） */
    public static final float LID_BLOCKED_FRACTION = 1.0F;
    /** 木槌敲懵：缓慢 II、2 秒 */
    private static final int DAZE_TICKS = 40;
    private static final int DAZE_AMPLIFIER = 1;
    /** 多加的击退力度：同击退 I 附魔（原版每级 0.5） */
    private static final float EXTRA_KNOCKBACK = 0.5F;
    /** 找村民换东西少要的比例 */
    private static final float VILLAGER_DISCOUNT = 0.15F;

    /**
     * 这一下木槌刚打中、还没轮到原版击退的东西（记的是打中时的游戏刻）。原版在扣血之后才击退（LivingEntity#hurt），
     * 而且无敌帧里挨打不击退，所以不在扣血时直接推，而是等原版自己击退时（{@link #onKnockBack}）把力度加上去。
     */
    private static final Map<LivingEntity, Long> PENDING_KNOCKBACK = new WeakHashMap<>();

    private NortheastOutfit() {
    }

    public static boolean isComplete(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.LARGE_BASIN.get())
                && entity.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.FLORAL_JACKET.get())
                && entity.getMainHandItem().is(ModItems.WOODEN_MALLET.get())
                && entity.getOffhandItem().is(ModItems.BIG_POT_LID.get());
    }

    /** 一棒子敲懵：穿齐了用木槌近身打中，缓慢两秒，并记下来等原版击退时加力度 */
    //? if <1.20.2 {
    public static void onLivingDamage(LivingDamageEvent event) {
        malletHit(event.getEntity(), event.getSource());
    }
    //?} else {
    /*public static void onLivingDamage(LivingDamageEvent.Post event) {
        malletHit(event.getEntity(), event.getSource());
    }
    *///?}

    private static void malletHit(LivingEntity target, DamageSource source) {
        if (target.level().isClientSide() || !source.is(DamageTypes.PLAYER_ATTACK)
                || !(source.getEntity() instanceof Player player) || source.getDirectEntity() != player
                || !isComplete(player)) {
            return;
        }
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DAZE_TICKS, DAZE_AMPLIFIER), player);
        PENDING_KNOCKBACK.put(target, target.level().getGameTime());
    }

    public static void onKnockBack(LivingKnockBackEvent event) {
        LivingEntity target = event.getEntity();
        Long tick = PENDING_KNOCKBACK.remove(target);
        if (tick != null && tick == target.level().getGameTime()) {
            event.setStrength(event.getStrength() + EXTRA_KNOCKBACK);
        }
    }

    /** 自家人：穿齐了找村民换东西，每条交易的第一样要价少 15%（至少 1 个；原版最少要价 1，不会降到 0） */
    public static void applyVillagerDiscount(Villager villager, Player player) {
        if (!isComplete(player)) {
            return;
        }
        for (MerchantOffer offer : villager.getOffers()) {
            offer.addToSpecialPriceDiff(-Math.max(1, Mth.floor(VILLAGER_DISCOUNT * offer.getBaseCostA().getCount())));
        }
    }
}
