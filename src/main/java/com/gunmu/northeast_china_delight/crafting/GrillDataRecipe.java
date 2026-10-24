package com.gunmu.northeast_china_delight.crafting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.util.DdIds;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
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

import java.util.ArrayList;
import java.util.List;

public final class GrillDataRecipe implements Recipe<
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
    private final GrillRecipes.Recipe rule;
    @Nullable
    private GrillRecipes.Recipe boundRule;

    private GrillDataRecipe(JsonObject config, @Nullable ResourceLocation id) {
        this.config = config.deepCopy();
        if (this.config.toString().length() > 32767) {
            throw new JsonParseException("烤架配方 config 超过网络同步长度上限，请使用标签精简食材列表");
        }
        this.id = id;
        this.rule = decode(config, id);
    }

    @Nullable
    private static GrillRecipes.Recipe decode(JsonObject config, @Nullable ResourceLocation id) {
        if (GsonHelper.getAsBoolean(config, "disabled", false)) {
            return null;
        }
        Ingredient input = ingredient(config.get("input"));
        List<Ingredient> seasonings = new ArrayList<>();
        List<GrillVisual> visuals = new ArrayList<>();
        JsonArray seasoningJson = GsonHelper.getAsJsonArray(config, "seasonings", new JsonArray());
        if (seasoningJson.size() > GrillRecipes.MAX_SEASONINGS) {
            throw new JsonParseException("烤架配方最多需要 4 份调料");
        }
        for (JsonElement entry : seasoningJson) {
            // 调料条目可以直接写 visual / color：{"tag": "...", "visual": "egg"}；
            // 任选数组没法再加字段，就包一层：{"ingredient": [...], "visual": "brush", "color": "#RRGGBB", "opacity": 0.5}
            if (entry.isJsonObject() && GrillVisual.declaredIn(entry.getAsJsonObject())) {
                JsonObject object = entry.getAsJsonObject();
                JsonElement ingredientJson = object.get("ingredient");
                if (ingredientJson == null) {
                    JsonObject stripped = object.deepCopy();
                    GrillVisual.strip(stripped);
                    ingredientJson = stripped;
                }
                seasonings.add(ingredient(ingredientJson));
                visuals.add(GrillVisual.fromJson(object));
            } else {
                seasonings.add(ingredient(entry));
                visuals.add(GrillVisual.AUTO);
            }
        }
        JsonObject output = GsonHelper.getAsJsonObject(config, "result");
        ResourceLocation outputId = ResourceLocation.tryParse(GsonHelper.getAsString(output, "item"));
        if (outputId == null || !BuiltInRegistries.ITEM.containsKey(outputId)) {
            throw new JsonParseException("烤架配方成品物品不存在：" + outputId);
        }
        Item resultItem = BuiltInRegistries.ITEM.get(outputId);
        int count = integer(output, "count", 1);
        ItemStack result = new ItemStack(resultItem, count);
        if (result.isEmpty() || count <= 0 || count > result.getMaxStackSize()) {
            throw new JsonParseException("烤架配方成品数量必须为 1 到该物品堆叠上限");
        }
        int seconds = integer(config, "seconds", 0);
        if (seconds <= 0 || seconds > Integer.MAX_VALUE / 20) {
            throw new JsonParseException("烤架配方 seconds 必须为正整数，且不能溢出刻数");
        }
        return new GrillRecipes.Recipe(id == null ? DdIds.of(NortheastChinaDelight.MODID, "grill/unbound") : id,
                integer(config, "priority", 0), input, seasonings, result, seconds, visuals);
    }

    private static int integer(JsonObject config, String field, int fallback) {
        if (!config.has(field)) {
            return fallback;
        }
        JsonElement value = config.get(field);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new JsonParseException(field + " 必须是整数");
        }
        try {
            return value.getAsBigDecimal().intValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new JsonParseException(field + " 必须是有效的 32 位整数", exception);
        }
    }

    private static Ingredient ingredient(@Nullable JsonElement json) {
        if (json == null) {
            throw new JsonParseException("烤架配方缺少 input");
        }
        //? if <1.20.5 {
        Ingredient ingredient = Ingredient.fromJson(json);
        //?} else {
        /*Ingredient ingredient = Ingredient.CODEC_NONEMPTY.parse(JsonOps.INSTANCE, json).getOrThrow();
        *///?}
        if (ingredient.isEmpty()) {
            throw new JsonParseException("烤架配方的食材或调料不能为空");
        }
        return ingredient;
    }

    @Nullable
    public GrillRecipes.Recipe rule(ResourceLocation id) {
        if (this.rule == null) {
            return null;
        }
        if (this.boundRule == null || !this.boundRule.id().equals(id)) {
            this.boundRule = this.rule.withId(id);
        }
        return this.boundRule;
    }

    @Override
    public boolean matches(
            //? if <1.20.5 {
            Container input,
            //?} else {
            /*RecipeInput input,
            *///?}
            Level level) {
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
        return getResultItem(registries);
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
        if (this.rule != null) {
            ingredients.add(this.rule.input());
            ingredients.addAll(this.rule.seasonings());
        }
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
        return ModGrillRecipes.SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModGrillRecipes.TYPE.get();
    }

    public static final class Serializer implements RecipeSerializer<GrillDataRecipe> {
        //? if <1.20.5 {
        @Override
        public GrillDataRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new GrillDataRecipe(GsonHelper.getAsJsonObject(json, "config"), id);
        }

        @Override
        public GrillDataRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new GrillDataRecipe(JsonParser.parseString(buffer.readUtf()).getAsJsonObject(), id);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, GrillDataRecipe recipe) {
            buffer.writeUtf(recipe.config.toString());
        }
        //?} else {
        /*private static final MapCodec<GrillDataRecipe> CODEC = Codec.PASSTHROUGH.fieldOf("config")
                .flatXmap(dynamic -> {
                    try {
                        JsonElement config = dynamic.convert(JsonOps.INSTANCE).getValue();
                        if (!config.isJsonObject()) {
                            return DataResult.error(() -> "烤架配方 config 必须是对象");
                        }
                        return DataResult.success(new GrillDataRecipe(config.getAsJsonObject(), null));
                    } catch (RuntimeException exception) {
                        return DataResult.error(() -> "无效的烤架配方：" + exception.getMessage());
                    }
                }, recipe -> DataResult.success(new Dynamic<>(JsonOps.INSTANCE, recipe.config)));

        private static final StreamCodec<RegistryFriendlyByteBuf, GrillDataRecipe> STREAM_CODEC = StreamCodec.of(
                (buffer, recipe) -> buffer.writeUtf(recipe.config.toString()),
                buffer -> new GrillDataRecipe(JsonParser.parseString(buffer.readUtf()).getAsJsonObject(), null));

        @Override
        public MapCodec<GrillDataRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, GrillDataRecipe> streamCodec() {
            return STREAM_CODEC;
        }
        *///?}
    }
}
