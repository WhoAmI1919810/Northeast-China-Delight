package com.gunmu.northeast_china_delight.mixin;

import com.gunmu.northeast_china_delight.item.LoomCurtains;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * 织布机算结果：旗帜格里是普通门帘时不走原版的旗帜图案逻辑，结果格直接放染好的门帘
 * （染料不对就空着），图案列表清空，界面上不会出图案按钮。服务端、客户端两边的菜单都会跑这里。
 */
@Mixin(LoomMenu.class)
public abstract class LoomMenuMixin {

    @Shadow @Final Slot bannerSlot;
    @Shadow @Final Slot dyeSlot;
    @Shadow @Final private Slot resultSlot;
    @Shadow @Final DataSlot selectedBannerPatternIndex;
    @Shadow private List<?> selectablePatterns;

    @Inject(method = "slotsChanged", at = @At("HEAD"), cancellable = true)
    private void northeast$dyeCurtain(Container container, CallbackInfo ci) {
        ItemStack banner = this.bannerSlot.getItem();
        if (!LoomCurtains.isPlainCurtain(banner)) {
            return;
        }
        this.resultSlot.set(LoomCurtains.dyedCurtain(this.dyeSlot.getItem()));
        this.selectablePatterns = List.of();
        this.selectedBannerPatternIndex.set(-1);
        ci.cancel();
    }
}
