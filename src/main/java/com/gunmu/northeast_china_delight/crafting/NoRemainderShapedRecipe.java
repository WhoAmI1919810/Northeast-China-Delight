package com.gunmu.northeast_china_delight.crafting;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
//? if <1.20.5 {
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
//?} else {
/*import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

import java.util.Optional;
*///?}

/**
 * 不退剩余物的有序合成（{@code northeast_china_delight:no_remainder_shaped}），写法和原版
 * {@code minecraft:crafting_shaped} 一模一样。
 *
 * <p>原版水桶合成完会退回空桶；压水井要把一桶水整个用掉（灌进井里引水），所以格子里什么都不退。</p>
 */
public class NoRemainderShapedRecipe extends ShapedRecipe {

    public NoRemainderShapedRecipe(ShapedRecipe base) {
        //? if <1.20.5 {
        super(base.getId(), base.getGroup(), base.category(), base.getWidth(), base.getHeight(),
                base.getIngredients(), base.getResultItem(null), base.showNotification());
        //?} else {
        /*// 原版的 pattern 字段不公开，用宽、高、原料重建一份（data 只在写回 JSON 时用，和客户端收到的配方一样留空）
        super(base.getGroup(), base.category(),
                new ShapedRecipePattern(base.getWidth(), base.getHeight(), base.getIngredients(), Optional.empty()),
                base.getResultItem(null), base.showNotification());
        *///?}
    }

    @Override
    //? if <1.20.5 {
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer input) {
    //?} else {
    /*public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
    *///?}
        NonNullList<ItemStack> remaining = super.getRemainingItems(input);
        for (int slot = 0; slot < remaining.size(); slot++) {
            remaining.set(slot, ItemStack.EMPTY);
        }
        return remaining;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModCraftingRecipes.NO_REMAINDER_SHAPED.get();
    }

    /** 读写都交给原版有序合成，只是读出来以后包一层。 */
    public static final class Serializer implements RecipeSerializer<NoRemainderShapedRecipe> {
        //? if <1.20.5 {
        @Override
        public NoRemainderShapedRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new NoRemainderShapedRecipe(RecipeSerializer.SHAPED_RECIPE.fromJson(id, json));
        }

        @Override
        public NoRemainderShapedRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new NoRemainderShapedRecipe(RecipeSerializer.SHAPED_RECIPE.fromNetwork(id, buffer));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, NoRemainderShapedRecipe recipe) {
            RecipeSerializer.SHAPED_RECIPE.toNetwork(buffer, recipe);
        }
        //?} else {
        /*private static final MapCodec<NoRemainderShapedRecipe> CODEC = RecipeSerializer.SHAPED_RECIPE.codec()
                .xmap(NoRemainderShapedRecipe::new, recipe -> recipe);
        private static final StreamCodec<RegistryFriendlyByteBuf, NoRemainderShapedRecipe> STREAM_CODEC =
                RecipeSerializer.SHAPED_RECIPE.streamCodec().map(NoRemainderShapedRecipe::new, recipe -> recipe);

        @Override
        public MapCodec<NoRemainderShapedRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, NoRemainderShapedRecipe> streamCodec() {
            return STREAM_CODEC;
        }
        *///?}
    }
}
