package com.gunmu.northeast_china_delight.mixin;

import com.gunmu.northeast_china_delight.entity.UseDurationData;
//? if <1.20.5 {
import net.minecraft.client.Minecraft;
//?}
import net.minecraft.client.renderer.ItemInHandRenderer;
//? if >=1.20.5 {
/*import net.minecraft.world.entity.LivingEntity;*/
//?}
import net.minecraft.world.item.ItemStack;
//? if <1.20.5 {
import org.spongepowered.asm.mixin.Final;
//?}
import org.spongepowered.asm.mixin.Mixin;
//? if <1.20.5 {
import org.spongepowered.asm.mixin.Shadow;
//?}
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 第一人称进食动画读取实际起始总时长（见 {@link LivingEntityUseDurationMixin}），
 * 避免油腻/爽口状态延长进食时间后 {@code f1 = remaining / getUseDuration > 1} 造成 {@code Math.pow(f1, 27)} 溢出、
 * 物品前几秒静止或飞出屏幕。
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererEatMixin {

    //? if <1.20.5 {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Redirect(
            method = "applyEatTransform",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getUseDuration()I")
    )
    private int northeast_china_delight$effectiveEatDuration(ItemStack stack) {
        int base = stack.getUseDuration();
        if (this.minecraft.player instanceof UseDurationData data) {
            int active = data.ncd$getActiveUseDuration();
            int remaining = this.minecraft.player.getUseItemRemainingTicks();
            return Math.max(base, Math.max(active, remaining));
        }
        return base;
    }
    //?} else {
    /*@Redirect(
            method = "applyEatTransform",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getUseDuration(Lnet/minecraft/world/entity/LivingEntity;)I")
    )
    private int northeast_china_delight$effectiveEatDuration(ItemStack stack, LivingEntity entity) {
        int base = stack.getUseDuration(entity);
        if (entity instanceof UseDurationData data) {
            int active = data.ncd$getActiveUseDuration();
            int remaining = entity.getUseItemRemainingTicks();
            return Math.max(base, Math.max(active, remaining));
        }
        return base;
    }*/
    //?}
}
