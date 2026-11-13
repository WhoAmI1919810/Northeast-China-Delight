package com.gunmu.northeast_china_delight.mixin;

import com.gunmu.northeast_china_delight.compat.RecipeAvailability;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.crafting.Recipe;
//? if >=1.20.5 {
import net.minecraft.world.item.crafting.RecipeHolder;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

@Mixin(ClientRecipeBook.class)
public abstract class ClientRecipeBookFilterMixin {
    //? if >=1.20.5 {
    @ModifyVariable(method = "setupCollections", at = @At("HEAD"), argsOnly = true)
    private Iterable<RecipeHolder<?>> northeast$filterRecipes(Iterable<RecipeHolder<?>> recipes,
                                                               Iterable<RecipeHolder<?>> targetRecipes,
                                                               RegistryAccess registries) {
        List<RecipeHolder<?>> visible = new ArrayList<>();
        for (RecipeHolder<?> holder : recipes) {
            if (RecipeAvailability.visible(holder.value(), registries)) {
                visible.add(holder);
            }
        }
        return visible;
    }
    //?} else {
    /*@ModifyVariable(method = "setupCollections", at = @At("HEAD"), argsOnly = true)
    private Iterable<Recipe<?>> northeast$filterRecipes(Iterable<Recipe<?>> recipes,
                                                        RegistryAccess registries) {
        List<Recipe<?>> visible = new ArrayList<>();
        for (Recipe<?> recipe : recipes) {
            if (RecipeAvailability.visible(recipe, registries)) {
                visible.add(recipe);
            }
        }
        return visible;
    }
    *///?}
}
