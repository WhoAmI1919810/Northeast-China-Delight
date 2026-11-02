package com.gunmu.northeast_china_delight.mixin;

import com.gunmu.northeast_china_delight.item.LoomCurtains;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 织布机的旗帜格（{@code LoomMenu} 构造里第一个匿名 Slot，1.20.1 / 1.21.1 都是 {@code LoomMenu$3}）
 * 原版只收旗帜；让它也收普通门帘，好在织布机里染色（见 {@link LoomCurtains}）。
 */
@Mixin(targets = "net.minecraft.world.inventory.LoomMenu$3")
public abstract class LoomBannerSlotMixin {

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void northeast$acceptPlainCurtain(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (LoomCurtains.isPlainCurtain(stack)) {
            cir.setReturnValue(true);
        }
    }
}
