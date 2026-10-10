package com.gunmu.northeast_china_delight.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.gunmu.northeast_china_delight.item.SeasoningBottleItem;
import com.gunmu.northeast_china_delight.item.ModItems;
//? if >=1.20.2 {
/*import net.minecraft.world.item.crafting.RecipeHolder;
*///?}
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
//? if <1.20.2 {
import net.minecraftforge.items.ItemStackHandler;
//?} else {
/*import net.neoforged.neoforge.items.ItemStackHandler;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;
import vectorwing.farmersdelight.common.crafting.CookingPotRecipe;

/**
 * 让瓶装调料留在厨锅里继续用。农夫乐事厨锅的 {@code processCooking} 写死了两件事：把食材的合成剩余物弹到锅外、然后把食材数量减 1。对瓶装调料来说这两件事
 * 都不对 —— 我们想要的是「留在锅里，每次扣 50 mB」。
 */
@Mixin(CookingPotBlockEntity.class)
public abstract class CookingPotBlockEntityMixin {

    @Unique
    private ItemStack northeast$cookingResult = ItemStack.EMPTY;

    @Inject(method = "processCooking", at = @At("HEAD"), remap = false)
    private void northeast$rememberCookingResult(
            //? if <1.20.2 {
            CookingPotRecipe recipe,
            //?} else {
            /*RecipeHolder<CookingPotRecipe> recipe,
            *///?}
                                               CookingPotBlockEntity pot,
                                               CallbackInfoReturnable<Boolean> cir) {
        Level level = pot.getLevel();
        if (level == null) {
            this.northeast$cookingResult = ItemStack.EMPTY;
            return;
        }
        //? if <1.20.2 {
        this.northeast$cookingResult = recipe.getResultItem(level.registryAccess()).copy();
        //?} else {
        /*this.northeast$cookingResult = recipe.value().getResultItem(level.registryAccess()).copy();
        *///?}
    }

    @WrapOperation(
            method = "processCooking",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Lvectorwing/farmersdelight/common/block/entity/CookingPotBlockEntity;ejectIngredientRemainder(Lnet/minecraft/world/item/ItemStack;)V"))
    private void northeast$keepBottleInPot(CookingPotBlockEntity pot, ItemStack remainder, Operation<Void> original) {
        if (SeasoningBottleItem.isBottle(remainder)) {
            return;
        }
        original.call(pot, remainder);
    }

    @WrapOperation(
            method = "processCooking",
            remap = false,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V", remap = true))
    private void northeast$useOneDose(ItemStack stack, int amount, Operation<Void> original) {
        if (!SeasoningBottleItem.isBottle(stack)) {
            original.call(stack, amount);
            return;
        }
        if (stack.is(ModItems.DEEP_FRY_OILS) && this.northeast$cookingResult.is(ModItems.DEEP_FRIED_DISHES)) {
            stack.shrink(amount);
            ((CookingPotBlockEntityInvoker) this)
                    .northeast$ejectIngredientRemainder(new ItemStack(Items.GLASS_BOTTLE));
            return;
        }
        ItemStack used = SeasoningBottleItem.useOnce(stack);
        if (used.is(Items.GLASS_BOTTLE)) {
            stack.setCount(0);
            ((CookingPotBlockEntityInvoker) this).northeast$ejectIngredientRemainder(used);
            return;
        }
        stack.setDamageValue(used.getDamageValue());
    }

    @Inject(method = "processCooking", at = @At("RETURN"), remap = false)
    private void northeast$leaveCracklings(
            //? if <1.20.2 {
            CookingPotRecipe recipe,
            //?} else {
            /*RecipeHolder<CookingPotRecipe> recipe,
            *///?}
                                         CookingPotBlockEntity pot,
                                         CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            return;
        }
        Level level = pot.getLevel();
        //? if <1.20.2 {
        if (level == null || !recipe.getResultItem(level.registryAccess()).is(ModItems.ANIMAL_OIL.get())) {
            return;
        }
        //?} else {
        /*if (level == null || !recipe.value().getResultItem(level.registryAccess()).is(ModItems.ANIMAL_OIL.get())) {
            return;
        }
        *///?}

        ItemStack cracklings = new ItemStack(ModItems.CRACKLINGS.get());
        ItemStackHandler inventory = pot.getInventory();
        for (int slot = 0; slot < CookingPotBlockEntity.MEAL_DISPLAY_SLOT && !cracklings.isEmpty(); slot++) {
            cracklings = inventory.insertItem(slot, cracklings, false);
        }
        if (!cracklings.isEmpty()) {
            ((CookingPotBlockEntityInvoker) pot).northeast$ejectIngredientRemainder(cracklings);
        }
    }
}
