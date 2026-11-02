package com.gunmu.northeast_china_delight.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.item.BannerItem;
//? if <1.20.5 {
/*import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.injection.Redirect;
*///?} else {
import net.minecraft.world.level.block.entity.BannerPatternLayers;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 织布机界面（客户端）：结果格里是染好的门帘（不是旗帜）时，不画右边那面旗帜的预览。
 *
 * <p>原版两处会把结果格的东西硬转成旗帜：1.20.1 在 {@code containerChanged} 里一拿到结果就转
 * （这里把那一次 {@code getItem()} 换成「不是旗帜就当白旗帜」，免得崩），1.21.1 在 {@code renderBg} 里
 * 只要 {@code resultBannerPatterns} 不为空就转。所以 {@code containerChanged} 结尾统一把预览清空。</p>
 */
@Mixin(LoomScreen.class)
public abstract class LoomScreenMixin extends AbstractContainerScreen<LoomMenu> {

    //? if <1.20.5 {
    /*@Shadow private java.util.List<?> resultBannerPatterns;
    *///?} else {
    @Shadow private BannerPatternLayers resultBannerPatterns;
    //?}

    protected LoomScreenMixin(LoomMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    //? if <1.20.5 {
    /*@Redirect(method = "containerChanged", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;getItem()Lnet/minecraft/world/item/Item;", ordinal = 0))
    private Item northeast$bannerOrPlaceholder(ItemStack result) {
        Item item = result.getItem();
        return item instanceof BannerItem ? item : Items.WHITE_BANNER;
    }
    *///?}

    @Inject(method = "containerChanged", at = @At("TAIL"))
    private void northeast$noPreviewForCurtain(CallbackInfo ci) {
        if (!(this.menu.getResultSlot().getItem().getItem() instanceof BannerItem)) {
            this.resultBannerPatterns = null;
        }
    }
}
