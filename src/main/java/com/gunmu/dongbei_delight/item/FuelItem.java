package com.gunmu.dongbei_delight.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.Nullable;

/** 可以当燃料烧的物品（燃烧时长由构造参数决定） */
public class FuelItem extends Item {

    private final int burnTime;

    public FuelItem(Item.Properties properties, int burnTime) {
        super(properties);
        this.burnTime = burnTime;
    }

    @Override
    public int getBurnTime(ItemStack stack, @Nullable RecipeType<?> recipeType) {
        return this.burnTime;
    }
}
