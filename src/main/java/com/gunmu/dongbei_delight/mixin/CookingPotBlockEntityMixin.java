package com.gunmu.dongbei_delight.mixin;

import com.gunmu.dongbei_delight.item.SeasoningBottleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;

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
 */
@Mixin(CookingPotBlockEntity.class)
public abstract class CookingPotBlockEntityMixin {

    /** 调料瓶不弹出锅外，留在锅里下一锅接着用 */
    @Redirect(
            method = "processCooking",
            at = @At(
                    value = "INVOKE",
                    target = "Lvectorwing/farmersdelight/common/block/entity/CookingPotBlockEntity;ejectIngredientRemainder(Lnet/minecraft/world/item/ItemStack;)V"))
    private void dongbei$keepBottleInPot(CookingPotBlockEntity pot, ItemStack remainder) {
        if (SeasoningBottleItem.isBottle(remainder)) {
            return;
        }
        ((CookingPotBlockEntityInvoker) pot).dongbei$ejectIngredientRemainder(remainder);
    }

    /** 调料瓶不整瓶消耗，改成扣 50 mB 后留在锅里 */
    @Redirect(
            method = "processCooking",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"))
    private void dongbei$useOneDose(ItemStack stack, int amount) {
        if (!SeasoningBottleItem.isBottle(stack)) {
            stack.shrink(amount);
            return;
        }
        ItemStack used = SeasoningBottleItem.useOnce(stack);
        if (used.is(Items.GLASS_BOTTLE)) {
            // 用空了：腾出锅里的格子，并把空玻璃瓶弹出来还给玩家
            stack.setCount(0);
            ((CookingPotBlockEntityInvoker) this).dongbei$ejectIngredientRemainder(used);
            return;
        }
        stack.setDamageValue(used.getDamageValue());
    }
}
