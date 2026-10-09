package com.gunmu.northeast_china_delight.mixin;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;

/**
 * 把农夫乐事厨锅里那个 protected 的 {@code ejectIngredientRemainder} 借出来用 ——
 * 我们在 {@link CookingPotBlockEntityMixin} 里要替它决定"什么该弹、什么不该弹"。
 */
@Mixin(CookingPotBlockEntity.class)
public interface CookingPotBlockEntityInvoker {

    @Invoker(value = "ejectIngredientRemainder", remap = false)
    void northeast$ejectIngredientRemainder(ItemStack stack);
}
