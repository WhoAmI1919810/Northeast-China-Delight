package com.gunmu.northeast_china_delight.mixin;

import com.gunmu.northeast_china_delight.effect.DishEffectEvents;
import com.gunmu.northeast_china_delight.entity.UseDurationData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 同步并暴露「本次使用物品的实际起始总时长」：
 * 油腻 / 爽口状态通过 {@code LivingEntityUseItemEvent.Start} 延长了 {@code useItemRemaining}，
 * 但原版 {@code shouldTriggerItemUseEffects} 与 {@code ItemInHandRenderer#applyEatTransform}
 * 仍读取物品默认的 {@code getUseDuration}（32 刻），导致延长的前几秒既无进食动画也无咀嚼音效/粒子。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityUseDurationMixin implements UseDurationData {

    @Shadow
    @Final
    protected static EntityDataAccessor<Byte> DATA_LIVING_ENTITY_FLAGS;

    @Shadow
    protected ItemStack useItem;

    @Shadow
    protected int useItemRemaining;

    @Unique
    private int ncd$activeUseDuration;

    @Override
    public int ncd$getActiveUseDuration() {
        return this.ncd$activeUseDuration;
    }

    @Inject(method = "startUsingItem", at = @At("TAIL"))
    private void northeast_china_delight$recordStartUseDuration(InteractionHand hand, CallbackInfo ci) {
        this.ncd$activeUseDuration = this.useItem.isEmpty() ? 0 : Math.max(0, this.useItemRemaining);
    }

    @Inject(method = "stopUsingItem", at = @At("TAIL"))
    private void northeast_china_delight$clearUseDuration(CallbackInfo ci) {
        this.ncd$activeUseDuration = 0;
    }

    @Inject(method = "onSyncedDataUpdated", at = @At("TAIL"))
    private void northeast_china_delight$syncRemoteUseDuration(EntityDataAccessor<?> key, CallbackInfo ci) {
        if (!DATA_LIVING_ENTITY_FLAGS.equals(key)) {
            return;
        }
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide) {
            return;
        }
        if (!self.isUsingItem()) {
            this.ncd$activeUseDuration = 0;
        } else if (this.ncd$activeUseDuration <= 0 && !this.useItem.isEmpty()) {
            if (self instanceof Player player) {
                this.useItemRemaining = DishEffectEvents.applyFlavorSlowdown(
                        player, this.useItem.getItem(), this.useItemRemaining);
            }
            this.ncd$activeUseDuration = Math.max(0, this.useItemRemaining);
        }
    }

    //? if <1.20.5 {
    /*@Redirect(
            method = "shouldTriggerItemUseEffects",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getUseDuration()I")
    )
    private int northeast_china_delight$effectiveUseEffectsDuration(ItemStack stack) {
        int base = stack.getUseDuration();
        return Math.max(base, Math.max(this.ncd$activeUseDuration, this.useItemRemaining));
    }*/
    //?} else {
    @Redirect(
            method = "shouldTriggerItemUseEffects",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getUseDuration(Lnet/minecraft/world/entity/LivingEntity;)I")
    )
    private int northeast_china_delight$effectiveUseEffectsDuration(ItemStack stack, LivingEntity entity) {
        int base = stack.getUseDuration(entity);
        return Math.max(base, Math.max(this.ncd$activeUseDuration, this.useItemRemaining));
    }
    //?}
}
