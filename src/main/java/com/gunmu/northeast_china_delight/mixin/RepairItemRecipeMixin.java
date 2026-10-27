package com.gunmu.northeast_china_delight.mixin;

import com.gunmu.northeast_china_delight.item.SeasoningBottleItem;
import net.minecraft.world.item.crafting.RepairItemRecipe;
import net.minecraft.world.level.Level;
//? if <1.20.5 {
/*import net.minecraft.world.inventory.CraftingContainer;
*///?} else {
import net.minecraft.world.item.crafting.CraftingInput;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 原版「修理」配方（工作台里两件同种耐久物品合成一件）不接瓶装调料。
 *
 * <p>瓶装调料的耐久就是剩余毫升：原版修理会多送 5% 耐久（凭空多 12 mB），还把其中一个玻璃瓶吞掉。
 * 并瓶改走 {@code SeasoningBottleMergeRecipe}。方法写全描述符，免得连同泛型桥接方法一起注入。</p>
 */
@Mixin(RepairItemRecipe.class)
public abstract class RepairItemRecipeMixin {

    //? if <1.20.5 {
    /*@Inject(method = "matches(Lnet/minecraft/world/inventory/CraftingContainer;Lnet/minecraft/world/level/Level;)Z",
            at = @At("HEAD"), cancellable = true)
    private void northeast$skipSeasoningBottles(CraftingContainer input, Level level,
                                                CallbackInfoReturnable<Boolean> cir) {
        for (int slot = 0; slot < input.getContainerSize(); slot++) {
    *///?} else {
    @Inject(method = "matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z",
            at = @At("HEAD"), cancellable = true)
    private void northeast$skipSeasoningBottles(CraftingInput input, Level level,
                                                CallbackInfoReturnable<Boolean> cir) {
        for (int slot = 0; slot < input.size(); slot++) {
    //?}
            if (SeasoningBottleItem.isBottle(input.getItem(slot))) {
                cir.setReturnValue(false);
                return;
            }
        }
    }
}
