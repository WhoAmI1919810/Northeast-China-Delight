package com.gunmu.northeast_china_delight.mixin;

import com.gunmu.northeast_china_delight.item.LoomCurtains;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * 织布机算结果：旗帜格里是普通门帘时不走原版的旗帜图案逻辑，结果格直接放染好的门帘
 * （染料不对就空着），图案列表清空，界面上不会出图案按钮。服务端、客户端两边的菜单都会跑这里。
 * 同时支持从背包 Shift+左键把普通门帘快速放进旗帜格。
 */
@Mixin(LoomMenu.class)
public abstract class LoomMenuMixin extends AbstractContainerMenu {

    @Shadow @Final Slot bannerSlot;
    @Shadow @Final Slot dyeSlot;
    @Shadow @Final private Slot resultSlot;
    @Shadow @Final DataSlot selectedBannerPatternIndex;
    @Shadow private List<?> selectablePatterns;

    protected LoomMenuMixin(@Nullable MenuType<?> menuType, int containerId) {
        super(menuType, containerId);
    }

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

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void northeast$quickMoveCurtain(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (index < 4 || index >= this.slots.size()) {
            return;
        }
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return;
        }
        ItemStack stack = slot.getItem();
        if (!LoomCurtains.isPlainCurtain(stack)) {
            return;
        }
        ItemStack original = stack.copy();
        if (!this.moveItemStackTo(stack, this.bannerSlot.index, this.bannerSlot.index + 1, false)) {
            return;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        cir.setReturnValue(original);
    }
}
