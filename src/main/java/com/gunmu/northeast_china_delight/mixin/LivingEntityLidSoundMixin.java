package com.gunmu.northeast_china_delight.mixin;

import com.gunmu.northeast_china_delight.item.PotLidItem;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 举着木锅盖时不放原版盾牌的声音（格挡换成木头破坏中的声音、碎裂换成木头被破坏的声音，
 * 木头的声音由服务端放，见 {@link PotLidItem}）。
 *
 * <p>原版这两声是服务端发实体事件、客户端在 LivingEntity#handleEntityEvent 里放的：29 = 举盾挡住（shield.block），
 * 30 = 被斧子打掉（shield.break）。这两个事件除了放这一声什么都不做（1.20.1、1.21.1 字节码看过），
 * 所以是锅盖的就整个跳过。</p>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityLidSoundMixin
{
    @Inject(method = "handleEntityEvent", at = @At("HEAD"), cancellable = true)
    private void northeast_china_delight$skipShieldSounds(byte id, CallbackInfo callback)
    {
        // 29：举盾挡住；30：盾被斧子打掉（不在 mixin 里加常量字段，免得并进 LivingEntity）
        if ((id == 29 || id == 30) && PotLidItem.blocksWithLid((LivingEntity) (Object) this))
        {
            callback.cancel();
        }
    }
}
