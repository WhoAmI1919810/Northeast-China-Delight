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
 * 让瓶装调料**留在厨锅里继续用**。
 *
 * <p>农夫乐事厨锅的 {@code processCooking} 写死了两件事：把食材的合成剩余物弹到锅外、然后把食材数量减 1。
 * 对瓶装调料来说这两件事都不对 —— 我们想要的是「留在锅里，每次扣 50 mB」。
 * 所以这里只给这个方法打两个小补丁：
 *
 * <ul>
 *     <li>弹剩余物那一步：是调料瓶就直接不放出去；</li>
 *     <li>减数量那一步：是调料瓶就改成扣 50 mB，瓶里还有剩就原地留着，正好用空才腾出格子并弹一个空玻璃瓶还给玩家。</li>
 * </ul>
 *
 * 其余食材（牛奶桶之类）完全按农夫乐事原本的行为走。
 *
 * <p>这两处都用 {@code @WrapOperation} 而不是 {@code @Redirect}：Redirect 是「抢占」那条指令，
 * 同一个指令上再出现一个 Redirect（别的模组也炖锅调味的话）会直接冲突崩溃；
 * WrapOperation 是后置包装，多个模组可以一层层套着，各自拿到 {@code original} 继续往下调。
 */
@Mixin(CookingPotBlockEntity.class)
public abstract class CookingPotBlockEntityMixin {

    /** 这一次下锅做的菜（在 {@code processCooking} 开头记下来，扣调料时要用它判断"是不是油炸菜"） */
    @Unique
    private ItemStack northeast$cookingResult = ItemStack.EMPTY;

    /** 记住这一锅做的是什么（油要用它判断是不是油炸菜） */
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

    /** 调料瓶不弹出锅外，留在锅里下一锅接着用 */
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

    /** 调料瓶不整瓶消耗，改成扣 50 mB 后留在锅里 */
    @WrapOperation(
            method = "processCooking",
            remap = false,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V", remap = true))
    private void northeast$useOneDose(ItemStack stack, int amount, Operation<Void> original) {
        if (!SeasoningBottleItem.isBottle(stack)) {
            original.call(stack, amount);
            return;
        }
        // 油炸菜里的油一次用光：整瓶扣掉，只把空玻璃瓶还给玩家
        if (stack.is(ModItems.DEEP_FRY_OILS) && this.northeast$cookingResult.is(ModItems.DEEP_FRIED_DISHES)) {
            stack.shrink(amount);
            ((CookingPotBlockEntityInvoker) this)
                    .northeast$ejectIngredientRemainder(new ItemStack(Items.GLASS_BOTTLE));
            return;
        }
        ItemStack used = SeasoningBottleItem.useOnce(stack);
        if (used.is(Items.GLASS_BOTTLE)) {
            // 用空了：腾出锅里的格子，并把空玻璃瓶弹出来还给玩家
            stack.setCount(0);
            ((CookingPotBlockEntityInvoker) this).northeast$ejectIngredientRemainder(used);
            return;
        }
        stack.setDamageValue(used.getDamageValue());
    }

    /**
     * 熬动物油：2 份肥肉 + 1 个玻璃瓶熬出 1 瓶动物油，**同时锅里还会留下 1 份油滋了**。
     *
     * <p>农夫乐事一条厨锅配方只有一个产物，所以在 {@code processCooking} 收尾之后补一份：
     * 先试着塞回锅里的空槽位（玩家开锅就能拿到），塞不下就按「食材剩余物」弹到锅外。
     */
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
            // 没真正做出这一锅就别给
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
