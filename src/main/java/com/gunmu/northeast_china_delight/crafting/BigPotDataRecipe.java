package com.gunmu.northeast_china_delight.crafting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.util.DdIds;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
//? if <1.20.5 {
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
//?} else {
/*import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeInput;
*///?}

/**
 * 「大锅烹饪」数据包配方（{@code northeast_china_delight:big_pot_cooking}）：JSON 原样包在 {@code config} 里，
 * 解出来的规则见 {@link BigPotRecipe}。和舂捣一样不走原版合成匹配，大锅自己拿规则对锅里的东西（{@link BigPotRecipes}）。
 */
public final class BigPotDataRecipe implements Recipe<
        //? if <1.20.5 {
        Container
        //?} else {
        /*RecipeInput
        *///?}
        > {

    private final JsonObject config;
    @Nullable
    private final ResourceLocation id;
    @Nullable
    private final BigPotRecipe rule;

    private BigPotDataRecipe(JsonObject config, @Nullable ResourceLocation id) {
        this.config = config.deepCopy();
        this.id = id;
        this.rule = BigPotRecipe.decode(this.config,
                id != null ? id : DdIds.of(NortheastChinaDelight.MODID, "big_pot_cooking/unbound"));
    }

    /** 带上配方 id 的规则（1.21.1 的配方对象里没有 id，得从 RecipeHolder 拿）；停用的配方是 null */
    @Nullable
    public BigPotRecipe rule(@Nullable ResourceLocation recipeId) {
        if (this.rule == null) {
            return null;
        }
        ResourceLocation effective = this.id != null ? this.id : recipeId;
        if (effective == null || effective.equals(this.rule.id())) {
            return this.rule;
        }
        return new BigPotRecipe(effective, rule.liquid(), rule.ingredients(), rule.seasonings(), rule.container(),
                rule.result(), rule.cookingTime(), rule.lidSpeedup(), rule.experience());
    }

    @Override
    public boolean matches(
            //? if <1.20.5 {
            Container input, Level level
            //?} else {
            /*RecipeInput input, Level level
            *///?}
    ) {
        return false;
    }

    @Override
    public ItemStack assemble(
            //? if <1.20.5 {
            Container input, RegistryAccess registries
            //?} else {
            /*RecipeInput input, HolderLookup.Provider registries
            *///?}
    ) {
        return this.rule == null ? ItemStack.EMPTY : this.rule.result().copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public ItemStack getResultItem(
            //? if <1.20.5 {
            RegistryAccess registries
            //?} else {
            /*HolderLookup.Provider registries
            *///?}
    ) {
        return this.rule == null ? ItemStack.EMPTY : this.rule.result().copy();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        if (this.rule == null) {
            return ingredients;
        }
        this.rule.ingredients().forEach(entry -> ingredients.add(entry.ingredient()));
        this.rule.seasonings().forEach(entry -> ingredients.add(entry.ingredient()));
        return ingredients;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    //? if <1.20.5 {
    @Override
    public ResourceLocation getId() {
        return this.id;
    }
    //?}

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModBigPotRecipes.SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModBigPotRecipes.TYPE.get();
    }

    public static final class Serializer implements RecipeSerializer<BigPotDataRecipe> {
        //? if <1.20.5 {
        @Override
        public BigPotDataRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new BigPotDataRecipe(GsonHelper.getAsJsonObject(json, "config"), id);
        }

        @Override
        public BigPotDataRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new BigPotDataRecipe(JsonParser.parseString(buffer.readUtf()).getAsJsonObject(), id);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, BigPotDataRecipe recipe) {
            buffer.writeUtf(recipe.config.toString());
        }
        //?} else {
        /*private static final MapCodec<BigPotDataRecipe> CODEC = Codec.PASSTHROUGH.fieldOf("config")
                .flatXmap(dynamic -> {
                    try {
                        JsonElement config = dynamic.convert(JsonOps.INSTANCE).getValue();
                        if (!config.isJsonObject()) {
                            return DataResult.error(() -> "大锅烹饪配方 config 必须是对象");
                        }
                        return DataResult.success(new BigPotDataRecipe(config.getAsJsonObject(), null));
                    } catch (RuntimeException exception) {
                        return DataResult.error(() -> "无效的大锅烹饪配方：" + exception.getMessage());
                    }
                }, recipe -> DataResult.success(new Dynamic<>(JsonOps.INSTANCE, recipe.config)));

        private static final StreamCodec<RegistryFriendlyByteBuf, BigPotDataRecipe> STREAM_CODEC = StreamCodec.of(
                (buffer, recipe) -> buffer.writeUtf(recipe.config.toString()),
                buffer -> new BigPotDataRecipe(JsonParser.parseString(buffer.readUtf()).getAsJsonObject(), null));

        @Override
        public MapCodec<BigPotDataRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BigPotDataRecipe> streamCodec() {
            return STREAM_CODEC;
        }
        *///?}
    }
}
