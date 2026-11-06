package com.gunmu.northeast_china_delight.crafting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 一条「大锅烹饪」配方（{@code northeast_china_delight:big_pot_cooking}）解出来的样子。格式见 docs/大锅烹饪配方格式.md：
 *
 * <pre>
 * {
 *   "type": "northeast_china_delight:big_pot_cooking",
 *   "config": {
 *     "liquid":      { "fluid": "minecraft:water", "amount": 1000 },      // 可省；"fluid" 写 "#标签" 也行；amount 见下
 *     "ingredients": [ { "item": "...", "count": 2 }, { "tag": "..." } ],  // 食材，最多 6 格
 *     "seasonings":  [ { "tag": "c:foods/seasonings/soy_sauce" } ],       // 调料，最多 3 格（瓶装调料一格 = 一剂）
 *     "container":   { "item": "northeast_china_delight:large_basin" },   // 盛菜用的容器，可省（空手盛）
 *     "result":      { "item": "...", "count": 1 },                       // count = 一锅能盛几份
 *     "cooking_time": 200,                                                 // 刻，默认 200（同农夫乐事厨锅）
 *     "lid_speedup": 40,                                                   // 盖上锅盖缩短的百分比，默认 40
 *     "experience": 1.0                                                    // 每盛一份给的经验，默认 0
 *   }
 * }
 * </pre>
 *
 * <p>{@code amount} 写数字是正好这么多毫升（默认 1000）；写对象是一个范围（2026-11-06 用户：「液体的量允许有范围」），
 * {@code min} 大于等于、{@code max} 小于等于、{@code more_than} 大于、{@code less_than} 小于，至少写一个，
 * 例如 {@code {"min": 1000, "max": 2000}}。以前数字是「至少这么多」，倒多了水也照样凑得上。</p>
 *
 * <p>{@code "disabled": true} 停用这条配方（数据包用同 ID 覆盖），{@link #decode} 返回 null。</p>
 */
public record BigPotRecipe(ResourceLocation id, @Nullable Liquid liquid, List<Entry> ingredients, List<Entry> seasonings,
                           ItemStack container, ItemStack result, int cookingTime, int lidSpeedup, float experience) {

    public static final int MAX_INGREDIENTS = 6;
    public static final int MAX_SEASONINGS = 3;
    /** 同农夫乐事厨锅的默认烹饪时间 */
    public static final int DEFAULT_COOKING_TIME = 200;
    public static final int DEFAULT_LID_SPEEDUP = 40;

    /** 食材 / 调料的一格：认哪些东西、要几个 */
    public record Entry(Ingredient ingredient, int count) {
    }

    /** 锅里要的液体：一种流体或一个流体标签，加上毫升数的范围（两头都算，max 不限时是 Integer.MAX_VALUE） */
    public record Liquid(@Nullable Fluid fluid, @Nullable TagKey<Fluid> tag, int min, int max) {

        /** 锅里这么多毫升行不行 */
        public boolean accepts(int mb) {
            return mb >= min && mb <= max;
        }

        public boolean test(Fluid other) {
            if (other == null || other == Fluids.EMPTY) {
                return false;
            }
            return fluid != null ? fluid.isSame(other) : tag != null && other.defaultFluidState().is(tag);
        }

        /** JEI 显示用：这格能填的所有流体（只取静止的那种） */
        public List<Fluid> fluids() {
            if (fluid != null) {
                return List.of(fluid);
            }
            List<Fluid> list = new ArrayList<>();
            for (Fluid candidate : BuiltInRegistries.FLUID) {
                if (candidate.isSource(candidate.defaultFluidState()) && candidate.defaultFluidState().is(tag)) {
                    list.add(candidate);
                }
            }
            return list;
        }
    }

    /** 盖着锅盖时要煮多久（刻），至少 1 刻 */
    public int lidCookingTime() {
        return Math.max(1, Math.round(cookingTime * (100 - lidSpeedup) / 100.0F));
    }

    public boolean needsContainer() {
        return !container.isEmpty();
    }

    @Nullable
    public static BigPotRecipe decode(JsonObject config, ResourceLocation id) {
        if (GsonHelper.getAsBoolean(config, "disabled", false)) {
            return null;
        }
        Liquid liquid = config.has("liquid") ? decodeLiquid(GsonHelper.getAsJsonObject(config, "liquid")) : null;
        List<Entry> ingredients = decodeEntries(config, "ingredients", MAX_INGREDIENTS);
        List<Entry> seasonings = decodeEntries(config, "seasonings", MAX_SEASONINGS);
        if (ingredients.isEmpty() && seasonings.isEmpty()) {
            throw new JsonParseException("大锅烹饪配方至少要有一样食材或调料");
        }
        ItemStack container = config.has("container")
                ? decodeItem(GsonHelper.getAsJsonObject(config, "container"), "container")
                : ItemStack.EMPTY;
        ItemStack result = decodeItem(GsonHelper.getAsJsonObject(config, "result"), "result");
        int cookingTime = integer(config, "cooking_time", DEFAULT_COOKING_TIME);
        if (cookingTime <= 0) {
            throw new JsonParseException("大锅烹饪 cooking_time 必须为正整数");
        }
        int lidSpeedup = integer(config, "lid_speedup", DEFAULT_LID_SPEEDUP);
        if (lidSpeedup < 0 || lidSpeedup > 100) {
            throw new JsonParseException("大锅烹饪 lid_speedup 必须在 0..100 之间");
        }
        float experience = config.has("experience") ? GsonHelper.getAsFloat(config, "experience") : 0.0F;
        if (experience < 0.0F) {
            throw new JsonParseException("大锅烹饪 experience 不能是负数");
        }
        return new BigPotRecipe(id, liquid, List.copyOf(ingredients), List.copyOf(seasonings), container, result,
                cookingTime, lidSpeedup, experience);
    }

    private static Liquid decodeLiquid(JsonObject json) {
        String name = GsonHelper.getAsString(json, "fluid");
        int min;
        int max;
        if (json.has("amount") && json.get("amount").isJsonObject()) {
            JsonObject range = json.getAsJsonObject("amount");
            if (range.has("min") && range.has("more_than") || range.has("max") && range.has("less_than")) {
                throw new JsonParseException("大锅烹饪 liquid.amount 里 min 和 more_than、max 和 less_than 只能各写一个");
            }
            if (!range.has("min") && !range.has("more_than") && !range.has("max") && !range.has("less_than")) {
                throw new JsonParseException("大锅烹饪 liquid.amount 范围至少要写 min、max、more_than、less_than 里的一个");
            }
            min = range.has("more_than") ? integer(range, "more_than", 0) + 1 : integer(range, "min", 1);
            max = range.has("less_than")
                    ? integer(range, "less_than", 0) - 1
                    : integer(range, "max", Integer.MAX_VALUE);
        } else {
            min = max = integer(json, "amount", 1000);
        }
        if (min <= 0 || max < min) {
            throw new JsonParseException("大锅烹饪 liquid.amount 必须为正整数，范围不能是空的");
        }
        if (name.startsWith("#")) {
            ResourceLocation tag = ResourceLocation.tryParse(name.substring(1));
            if (tag == null) {
                throw new JsonParseException("大锅烹饪流体标签无效：" + name);
            }
            return new Liquid(null, TagKey.create(Registries.FLUID, tag), min, max);
        }
        ResourceLocation fluidId = ResourceLocation.tryParse(name);
        if (fluidId == null || !BuiltInRegistries.FLUID.containsKey(fluidId)) {
            throw new JsonParseException("大锅烹饪流体不存在：" + name);
        }
        return new Liquid(BuiltInRegistries.FLUID.get(fluidId), null, min, max);
    }

    private static List<Entry> decodeEntries(JsonObject config, String field, int max) {
        List<Entry> entries = new ArrayList<>();
        if (!config.has(field)) {
            return entries;
        }
        JsonArray array = GsonHelper.getAsJsonArray(config, field);
        if (array.size() > max) {
            throw new JsonParseException("大锅烹饪 " + field + " 最多 " + max + " 格");
        }
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                throw new JsonParseException("大锅烹饪 " + field + " 的每一项都必须是对象");
            }
            JsonObject entry = element.getAsJsonObject().deepCopy();
            int count = integer(entry, "count", 1);
            if (count <= 0 || count > 64) {
                throw new JsonParseException("大锅烹饪 " + field + ".count 必须在 1..64 之间");
            }
            entry.remove("count");
            entries.add(new Entry(ingredient(entry), count));
        }
        return entries;
    }

    private static Ingredient ingredient(JsonElement element) {
        //? if <1.20.5 {
        /*return Ingredient.fromJson(element);
        *///?} else {
        return Ingredient.CODEC_NONEMPTY.parse(JsonOps.INSTANCE, element).getOrThrow();
        //?}
    }

    private static ItemStack decodeItem(JsonObject json, String field) {
        ResourceLocation itemId = ResourceLocation.tryParse(GsonHelper.getAsString(json, "item"));
        if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
            throw new JsonParseException("大锅烹饪 " + field + " 物品不存在：" + itemId);
        }
        int count = integer(json, "count", 1);
        if (count <= 0) {
            throw new JsonParseException("大锅烹饪 " + field + ".count 必须为正整数");
        }
        return new ItemStack(BuiltInRegistries.ITEM.get(itemId), count);
    }

    private static int integer(JsonObject json, String field, int fallback) {
        if (!json.has(field)) {
            return fallback;
        }
        JsonElement value = json.get(field);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new JsonParseException("大锅烹饪 " + field + " 必须是整数");
        }
        try {
            return value.getAsBigDecimal().intValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new JsonParseException("大锅烹饪 " + field + " 必须是整数");
        }
    }
}
