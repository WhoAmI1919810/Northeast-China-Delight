package com.gunmu.northeast_china_delight.crafting;

import com.gunmu.northeast_china_delight.item.SeasoningBottleItem;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
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
*///?}

/**
 * 整瓶倒进去的无序合成（{@code northeast_china_delight:whole_bottle_shapeless}），写法和原版
 * {@code minecraft:crafting_shapeless} 一模一样。
 *
 * <p>瓶装调料的合成剩余物是「扣掉一剂（50 mB）的瓶子」，普通合成里放一瓶只算用了 50 mB。
 * 4 瓶动物油凝成一块动物油块（1000 mB）这种要整瓶用掉的配方就得用这个类型：瓶装调料一律退回空玻璃瓶，
 * 否则每次合成会退回 4 个还剩 200 mB 的瓶子，凭空多出 800 mB。</p>
 */
public class WholeBottleShapelessRecipe extends ShapelessRecipe {

    public WholeBottleShapelessRecipe(ShapelessRecipe base) {
        //? if <1.20.5 {
        super(base.getId(), base.getGroup(), base.category(), base.getResultItem(null), base.getIngredients());
        //?} else {
        /*super(base.getGroup(), base.category(), base.getResultItem(null), base.getIngredients());
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
            if (SeasoningBottleItem.isBottle(input.getItem(slot))) {
                remaining.set(slot, new ItemStack(Items.GLASS_BOTTLE));
            }
        }
        return remaining;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModCraftingRecipes.WHOLE_BOTTLE_SHAPELESS.get();
    }

    /** 读写都交给原版无序合成，只是读出来以后包一层。 */
    public static final class Serializer implements RecipeSerializer<WholeBottleShapelessRecipe> {
        //? if <1.20.5 {
        @Override
        public WholeBottleShapelessRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new WholeBottleShapelessRecipe(RecipeSerializer.SHAPELESS_RECIPE.fromJson(id, json));
        }

        @Override
        public WholeBottleShapelessRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new WholeBottleShapelessRecipe(RecipeSerializer.SHAPELESS_RECIPE.fromNetwork(id, buffer));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, WholeBottleShapelessRecipe recipe) {
            RecipeSerializer.SHAPELESS_RECIPE.toNetwork(buffer, recipe);
        }
        //?} else {
        /*private static final MapCodec<WholeBottleShapelessRecipe> CODEC = RecipeSerializer.SHAPELESS_RECIPE.codec()
                .xmap(WholeBottleShapelessRecipe::new, recipe -> recipe);
        private static final StreamCodec<RegistryFriendlyByteBuf, WholeBottleShapelessRecipe> STREAM_CODEC =
                RecipeSerializer.SHAPELESS_RECIPE.streamCodec().map(WholeBottleShapelessRecipe::new, recipe -> recipe);

        @Override
        public MapCodec<WholeBottleShapelessRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, WholeBottleShapelessRecipe> streamCodec() {
            return STREAM_CODEC;
        }
        *///?}
    }
}
