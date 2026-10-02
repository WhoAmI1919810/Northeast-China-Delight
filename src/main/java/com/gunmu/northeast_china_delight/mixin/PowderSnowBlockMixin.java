package com.gunmu.northeast_china_delight.mixin;

import com.gunmu.northeast_china_delight.effect.ModEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.PowderSnowBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 带着「暖身」效果时不陷进细雪 —— 和穿皮革靴子一个待遇。
 */
@Mixin(PowderSnowBlock.class)
public class PowderSnowBlockMixin
{
    @Inject(method = "canEntityWalkOnPowderSnow", at = @At("HEAD"), cancellable = true)
    private static void northeast_china_delight$warmthWalksOnPowderSnow(Entity entity,
                                                                CallbackInfoReturnable<Boolean> callback)
    {
        if (entity instanceof LivingEntity living && living.hasEffect(ModEffects.WARMTH))
        {
            callback.setReturnValue(true);
        }
    }
}
