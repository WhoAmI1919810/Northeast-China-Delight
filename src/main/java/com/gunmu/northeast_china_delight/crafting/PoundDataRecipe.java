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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
//? if <1.20.5 {
/*import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
*///?} else {
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeInput;
//?}

import java.util.ArrayList;
import java.util.List;

/**
 * 「舂捣」数据包配方（{@code northeast_china_delight:pounding}）。
 *
 * <p>新格式的输入、产物都支持多项：{@code inputs} 中每一项有自己的 {@code count} 和可选
 * {@code return_container}；{@code results} 中每一项可以有固定/随机数量、出现概率和副产物标记。
 * 旧的单输入 {@code input}/{@code return_container} 与单产物 {@code result} 仍然原样可用。
 */
public final class PoundDataRecipe implements Recipe<
        //? if <1.20.5 {
        /*Container
        *///?} else {
        RecipeInput
        //?}
        > {

    private final JsonObject config;
    @Nullable
    private final ResourceLocation id;
    @Nullable
    private final PoundingRecipes.Recipe rule;

    private PoundDataRecipe(JsonObject config, @Nullable ResourceLocation id) {
        this.config = config.deepCopy();
        this.id = id;
        this.rule = decode(config, id);
    }

    @Nullable
    public PoundingRecipes.Recipe rule(@Nullable ResourceLocation recipeId) {
        if (this.rule == null) {
            return null;
        }
        ResourceLocation effective = this.id != null ? this.id : recipeId;
        return effective == null || effective.equals(this.rule.id())
                ? this.rule
                : new PoundingRecipes.Recipe(effective, this.rule.priority(), this.rule.inputs(),
                        this.rule.tool(), this.rule.poundTimes(), this.rule.chargeThreshold(),
                        this.rule.stageModels(), this.rule.results(), this.rule.transition());
    }

    @Nullable
    private static PoundingRecipes.Recipe decode(JsonObject config, @Nullable ResourceLocation id) {
        if (GsonHelper.getAsBoolean(config, "disabled", false)) {
            return null;
        }
        ResourceLocation recipeId = id == null ? DdIds.of(NortheastChinaDelight.MODID, "pounding/unbound") : id;

        List<PoundingRecipes.Input> inputs = decodeInputs(config);
        if (inputs.isEmpty()) {
            throw new JsonParseException("舂捣配方的投入物不能为空");
        }

        Ingredient tool = config.has("tool")
                ? decodeIngredient(config, "tool")
                : Ingredient.of(net.minecraft.world.item.Items.STICK);
        if (tool.isEmpty()) {
            throw new JsonParseException("舂捣配方的工具不能为空");
        }

        int poundTimes = integer(config, "pound_times", 2);
        if (poundTimes <= 0) {
            throw new JsonParseException("舂捣配方 pound_times 必须为正整数");
        }

        float chargeThreshold = decimal(config, "charge_threshold", 0.9F);
        if (chargeThreshold < 0.0F || chargeThreshold > 1.0F) {
            throw new JsonParseException("舂捣配方 charge_threshold 必须在 0..1 之间");
        }

        List<ResourceLocation> stageModels = new ArrayList<>();
        if (config.has("stage_models")) {
            JsonArray arr = GsonHelper.getAsJsonArray(config, "stage_models");
            for (JsonElement element : arr) {
                ResourceLocation model = ResourceLocation.tryParse(element.getAsString());
                if (model == null) {
                    throw new JsonParseException("舂捣配方模型 id 无效：" + element);
                }
                stageModels.add(model);
            }
        }
        if (stageModels.isEmpty()) {
            stageModels.add(DdIds.of(NortheastChinaDelight.MODID, "block/wooden_mortar_1"));
        }
        if (stageModels.size() + 1 > com.gunmu.northeast_china_delight.block.WoodenMortarBlock.MAX_STATE) {
            throw new JsonParseException("舂捣配方模型数超出木臼支持的 stage 上限（最多 "
                    + com.gunmu.northeast_china_delight.block.WoodenMortarBlock.MAX_STATE + " 个加工/成品模型）");
        }

        List<PoundingRecipes.Result> results = decodeResults(config);
        if (results.isEmpty()) {
            throw new JsonParseException("舂捣配方的产物不能为空");
        }

        PoundingRecipes.Transition transition = PoundingRecipes.Transition.parse(
                config.has("transition") ? config.get("transition").getAsString() : null);

        return new PoundingRecipes.Recipe(recipeId, integer(config, "priority", 0), inputs, tool,
                poundTimes, chargeThreshold, stageModels, results, transition);
    }

    private static List<PoundingRecipes.Input> decodeInputs(JsonObject config) {
        List<PoundingRecipes.Input> inputs = new ArrayList<>();
        if (config.has("inputs")) {
            JsonArray array = GsonHelper.getAsJsonArray(config, "inputs");
            for (JsonElement element : array) {
                if (!element.isJsonObject()) {
                    throw new JsonParseException("舂捣 inputs 的每一项都必须是对象");
                }
                JsonObject entry = element.getAsJsonObject();
                Ingredient ingredient = decodeIngredient(entry, "ingredient");
                int count = integer(entry, "count", 1);
                if (count <= 0) {
                    throw new JsonParseException("舂捣 inputs.count 必须为正整数");
                }
                ItemStack container = entry.has("return_container")
                        ? decodeNestedItem(entry, "return_container")
                        : null;
                inputs.add(new PoundingRecipes.Input(ingredient, count, container));
            }
            return inputs;
        }

        Ingredient ingredient = decodeIngredient(config, "input");
        ItemStack container = config.has("return_container")
                ? decodeNestedItem(config, "return_container")
                : null;
        inputs.add(new PoundingRecipes.Input(ingredient, 1, container));
        return inputs;
    }

    private static List<PoundingRecipes.Result> decodeResults(JsonObject config) {
        List<PoundingRecipes.Result> results = new ArrayList<>();
        boolean modernFormat = config.has("results") || config.has("random_results");
        if (config.has("results")) {
            JsonArray array = GsonHelper.getAsJsonArray(config, "results");
            for (JsonElement element : array) {
                if (!element.isJsonObject()) {
                    throw new JsonParseException("舂捣 results 的每一项都必须是对象");
                }
                JsonObject entry = element.getAsJsonObject();
                ItemStack stack = decodeItem(entry);
                boolean byproduct = GsonHelper.getAsBoolean(entry, "byproduct", false);
                float chance = decimal(entry, "chance", 1.0F);
                if (chance < 0.0F || chance > 1.0F) {
                    throw new JsonParseException("舂捣 results.chance 必须在 0..1 之间");
                }

                int minCount = entry.has("min_count")
                        ? integer(entry, "min_count", 1)
                        : integer(entry, "count", 1);
                int maxCount = entry.has("max_count")
                        ? integer(entry, "max_count", minCount)
                        : minCount;
                if (minCount <= 0 || maxCount < minCount) {
                    throw new JsonParseException("舂捣 results 的数量范围无效：" + minCount + ".." + maxCount);
                }
                results.add(new PoundingRecipes.Result(stack, minCount, maxCount, chance, byproduct));
            }
        }

        if (config.has("random_results")) {
            JsonArray pools = GsonHelper.getAsJsonArray(config, "random_results");
            for (int poolIndex = 0; poolIndex < pools.size(); poolIndex++) {
                JsonElement poolElement = pools.get(poolIndex);
                if (!poolElement.isJsonObject()) {
                    throw new JsonParseException("舂捣 random_results 的每一项都必须是对象");
                }
                JsonObject pool = poolElement.getAsJsonObject();
                String poolId = pool.has("id")
                        ? GsonHelper.getAsString(pool, "id")
                        : "random_" + poolIndex;
                if (poolId.isBlank()) {
                    throw new JsonParseException("舂捣 random_results.id 不能为空");
                }
                float poolChance = decimal(pool, "chance", 1.0F);
                if (poolChance < 0.0F || poolChance > 1.0F) {
                    throw new JsonParseException("舂捣 random_results.chance 必须在 0..1 之间");
                }
                boolean poolByproduct = GsonHelper.getAsBoolean(pool, "byproduct", false);
                JsonArray entries = GsonHelper.getAsJsonArray(pool, "entries");
                if (entries.isEmpty()) {
                    throw new JsonParseException("舂捣 random_results.entries 不能为空");
                }
                for (JsonElement entryElement : entries) {
                    if (!entryElement.isJsonObject()) {
                        throw new JsonParseException("舂捣 random_results.entries 的每一项都必须是对象");
                    }
                    JsonObject entry = entryElement.getAsJsonObject();
                    ItemStack stack = decodeItem(entry);
                    int weight = integer(entry, "weight", 1);
                    if (weight <= 0) {
                        throw new JsonParseException("舂捣 random_results.entries.weight 必须为正整数");
                    }
                    int minCount = entry.has("min_count")
                            ? integer(entry, "min_count", 1)
                            : integer(entry, "count", 1);
                    int maxCount = entry.has("max_count")
                            ? integer(entry, "max_count", minCount)
                            : minCount;
                    if (minCount <= 0 || maxCount < minCount) {
                        throw new JsonParseException("舂捣 random_results 的数量范围无效："
                                + minCount + ".." + maxCount);
                    }
                    boolean byproduct = poolByproduct
                            || GsonHelper.getAsBoolean(entry, "byproduct", false);
                    results.add(new PoundingRecipes.Result(stack, minCount, maxCount, 1.0F,
                            byproduct, poolId, weight, poolChance));
                }
            }
        }

        if (modernFormat) {
            return results;
        }

        JsonObject resultJson = GsonHelper.getAsJsonObject(config, "result");
        ItemStack stack = decodeItem(resultJson);
        int count = integer(resultJson, "count", 1);
        if (count <= 0) {
            throw new JsonParseException("舂捣 result.count 必须为正整数");
        }
        results.add(new PoundingRecipes.Result(stack, count, count, 1.0F, false));
        return results;
    }

    private static ItemStack decodeNestedItem(JsonObject owner, String field) {
        return decodeItem(GsonHelper.getAsJsonObject(owner, field));
    }

    private static ItemStack decodeItem(JsonObject json) {
        if (!json.has("item")) {
            throw new JsonParseException("舂捣配方缺少字段：item");
        }
        ResourceLocation itemId = ResourceLocation.tryParse(GsonHelper.getAsString(json, "item"));
        if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
            throw new JsonParseException("舂捣配方物品不存在：" + itemId);
        }
        int count = integer(json, "count", 1);
        if (count <= 0) {
            throw new JsonParseException("舂捣配方物品数量必须为正整数");
        }
        return new ItemStack(BuiltInRegistries.ITEM.get(itemId), count);
    }

    private static Ingredient decodeIngredient(JsonObject config, String field) {
        JsonElement element = config.get(field);
        if (element == null) {
            throw new JsonParseException("舂捣配方缺少字段：" + field);
        }
        //? if <1.20.5 {
        /*return Ingredient.fromJson(element);
        *///?} else {
        return Ingredient.CODEC_NONEMPTY.parse(JsonOps.INSTANCE, element).getOrThrow();
        //?}
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
            throw new JsonParseException(field + " 必须是整数");
        }
    }

    private static float decimal(JsonObject config, String field, float fallback) {
        if (!config.has(field)) {
            return fallback;
        }
        JsonElement value = config.get(field);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new JsonParseException(field + " 必须是数字");
        }
        return value.getAsFloat();
    }

    @Override
    public boolean matches(
            //? if <1.20.5 {
            /*Container input, Level level
            *///?} else {
            RecipeInput input, Level level
            //?}
    ) {
        return false;
    }

    @Override
    public ItemStack assemble(
            //? if <1.20.5 {
            /*Container input, RegistryAccess registries
            *///?} else {
            RecipeInput input, HolderLookup.Provider registries
            //?}
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
            /*RegistryAccess registries
            *///?} else {
            HolderLookup.Provider registries
            //?}
    ) {
        return this.rule == null ? ItemStack.EMPTY : this.rule.previewResult();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        if (this.rule != null) {
            for (PoundingRecipes.Input input : this.rule.inputs()) {
                ingredients.add(input.ingredient());
            }
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
    /*@Override
    public ResourceLocation getId() {
        return this.id;
    }
    *///?}

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModPoundRecipes.SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModPoundRecipes.TYPE.get();
    }

    public static final class Serializer implements RecipeSerializer<PoundDataRecipe> {
        //? if <1.20.5 {
        /*@Override
        public PoundDataRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new PoundDataRecipe(GsonHelper.getAsJsonObject(json, "config"), id);
        }

        @Override
        public PoundDataRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new PoundDataRecipe(JsonParser.parseString(buffer.readUtf()).getAsJsonObject(), id);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, PoundDataRecipe recipe) {
            buffer.writeUtf(recipe.config.toString());
        }
        *///?} else {
        private static final MapCodec<PoundDataRecipe> CODEC = Codec.PASSTHROUGH.fieldOf("config")
                .flatXmap(dynamic -> {
                    try {
                        JsonElement config = dynamic.convert(JsonOps.INSTANCE).getValue();
                        if (!config.isJsonObject()) {
                            return DataResult.error(() -> "舂捣配方 config 必须是对象");
                        }
                        return DataResult.success(new PoundDataRecipe(config.getAsJsonObject(), null));
                    } catch (RuntimeException exception) {
                        return DataResult.error(() -> "无效的舂捣配方：" + exception.getMessage());
                    }
                }, recipe -> DataResult.success(new Dynamic<>(JsonOps.INSTANCE, recipe.config)));

        private static final StreamCodec<RegistryFriendlyByteBuf, PoundDataRecipe> STREAM_CODEC = StreamCodec.of(
                (buffer, recipe) -> buffer.writeUtf(recipe.config.toString()),
                buffer -> new PoundDataRecipe(JsonParser.parseString(buffer.readUtf()).getAsJsonObject(), null));

        @Override
        public MapCodec<PoundDataRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, PoundDataRecipe> streamCodec() {
            return STREAM_CODEC;
        }
        //?}
    }
}
