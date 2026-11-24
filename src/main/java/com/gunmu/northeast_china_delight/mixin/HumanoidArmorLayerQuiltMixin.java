package com.gunmu.northeast_china_delight.mixin;

import com.gunmu.northeast_china_delight.client.QuiltSleepLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 玩家盖着花被用枕头睡觉时，隐藏被窝里的胸甲、护腿和靴子渲染，保留头盔露在枕头上。
 * 避免原版盔甲（与被面 Z=-3.0F 重合且带 VIEW_OFFSET_Z_LAYERING 深度前移）画在被子上方。
 */
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerQuiltMixin {

    //? if <1.20.5 {
    @Inject(
            method = "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void northeast_china_delight$hideArmorUnderQuilt(
            PoseStack poseStack, MultiBufferSource buffer, LivingEntity entity,
            EquipmentSlot slot, int packedLight, HumanoidModel<?> model, CallbackInfo ci) {
        if (northeast_china_delight$isCoveredByQuilt(entity, slot)) {
            ci.cancel();
        }
    }
    //?} else {
    /*@Inject(
            method = "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;FFFFFF)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void northeast_china_delight$hideArmorUnderQuilt(
            PoseStack poseStack, MultiBufferSource buffer, LivingEntity entity,
            EquipmentSlot slot, int packedLight, HumanoidModel<?> model,
            float limbSwing, float limbSwingAmount, float partialTick,
            float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (northeast_china_delight$isCoveredByQuilt(entity, slot)) {
            ci.cancel();
        }
    }*/
    //?}

    @Unique
    private static boolean northeast_china_delight$isCoveredByQuilt(LivingEntity entity, EquipmentSlot slot) {
        return (slot == EquipmentSlot.CHEST || slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET)
                && entity instanceof AbstractClientPlayer player
                && QuiltSleepLayer.hasVisibleQuilt(player);
    }
}
