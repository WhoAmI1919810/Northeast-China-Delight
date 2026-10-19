package com.gunmu.northeast_china_delight.crafting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.gunmu.northeast_china_delight.util.DdIds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

public final class VatRecipeJson {

    private VatRecipeJson() {
    }

    @Nullable
    public static VatRecipe decode(@Nullable ResourceLocation id, JsonObject config) {
        if (config.has("disabled") && config.get("disabled").getAsBoolean()) {
            return null;
        }
        VatRecipes.Kind kind = enumValue(VatRecipes.Kind.class, GsonHelper.getAsString(config, "kind"));
        if (kind == VatRecipes.Kind.NONE) {
            throw new JsonParseException("大缸配方不能使用 Kind.NONE");
        }
        VatRecipe.Builder builder = VatRecipe.of(kind, id == null ? "unbound" : id.toString())
                .priority(GsonHelper.getAsInt(config, "priority", 0))
                .seal(enumValue(VatRecipe.Seal.class, GsonHelper.getAsString(config, "seal")))
                .seconds(GsonHelper.getAsInt(config, "seconds"));
        if (config.has("verb")) {
            builder.verb(verb(config.get("verb")));
        }
        // 进度文字的句式。不写就用模组自带的（中文「腌制中 50%」这种），
        // 写了就把整句的翻译键交给数据包，方便别的语言换语序
        if (config.has("progress")) {
            builder.progressText(translationKey(config.get("progress")));
        }
        if (config.has("done")) {
            builder.doneText(translationKey(config.get("done")));
        }
        if (config.has("time")) {
            builder.timeText(translationKey(config.get("time")));
        }
        if (config.has("hint")) {
            JsonObject hint = GsonHelper.getAsJsonObject(config, "hint");
            builder.hintText(translationKey(hint.get("long")), translationKey(hint.get("short")));
        }
        if (GsonHelper.getAsBoolean(config, "seconds_per_layer", false)) {
            builder.perLayerSeconds();
        }
        if (GsonHelper.getAsBoolean(config, "requires_snowy_biome", false)) {
            builder.requiresSnowyBiome();
        }
        if (config.has("required_seal_item")) {
            builder.requiredSealItem(ingredient(config.get("required_seal_item")));
        }
        if (config.has("biome_seconds")) {
            JsonObject multipliers = GsonHelper.getAsJsonObject(config, "biome_seconds");
            builder.biomeSeconds(map -> {
                for (String key : multipliers.keySet()) {
                    float multiplier = GsonHelper.getAsFloat(multipliers, key);
                    if (!Float.isFinite(multiplier) || multiplier <= 0) {
                        throw new JsonParseException("无效的群系时长系数：" + multiplier);
                    }
                    map.put(enumValue(VatRecipe.BiomeBand.class, key), multiplier);
                }
            });
        }

        // liquid 是可选的：不写就是无液体配方（辣白菜这类干腌）。
        // 1.21.1 的 GsonHelper 对缺失键会直接抛异常，不能依赖它返回空对象。
        JsonObject liquid = config.has("liquid")
                ? GsonHelper.getAsJsonObject(config, "liquid") : new JsonObject();
        int waterMin = GsonHelper.getAsInt(liquid, "water_min", 0);
        int waterMax = GsonHelper.getAsInt(liquid, "water_max", 0);
        if (waterMin < 0 || waterMax > 3 || waterMin > waterMax) {
            throw new JsonParseException("大缸水位必须在 0 到 3 层之间");
        }
        builder.water(waterMin, waterMax,
                enumValue(VatRecipe.LiquidAfter.class, GsonHelper.getAsString(liquid, "water_after", "KEEP")));
        if (liquid.has("sour_water_mb")) {
            int sourWaterMb = GsonHelper.getAsInt(liquid, "sour_water_mb");
            if (waterMax != 0 || sourWaterMb < 0 || sourWaterMb > 3000) {
                throw new JsonParseException("酸引水配方不能同时要求清水，且用量必须在 0 到 3000 mB 之间");
            }
            builder.sourWater(sourWaterMb,
                    enumValue(VatRecipe.LiquidAfter.class, GsonHelper.getAsString(liquid, "sour_water_after", "KEEP")));
        }
        if (liquid.has("product")) {
            net.minecraft.world.level.material.Fluid product = productFluid(GsonHelper.getAsString(liquid, "product"));
            int productMb = GsonHelper.getAsInt(liquid, "product_mb");
            // 缸本身装得下 3 层（3000 mB）。本模组自己的瓶子是 250 mB 一份、一缸 10 份，
            // 但数据包可以产出别的流体（比如 3000 mB 的茶），所以这里按缸的实际容量卡，不按瓶子份数卡
            if (productMb <= 0
                    || (long) productMb * (GsonHelper.getAsBoolean(liquid, "product_per_layer", false)
                    ? Math.max(1, waterMax) : 1) > VatRecipes.SOUR_WATER_MB) {
                throw new JsonParseException("大缸产物需是可存放的液体，且总量不得超过缸容量");
            }
            builder.product(product, productMb,
                    GsonHelper.getAsBoolean(liquid, "product_per_layer", false));
            // 用什么容器取、取出来是什么。两个都写才生效，只写一个算错。
            // 不写就按这种流体的默认装法（本模组自己的液体有默认瓶子，别的流体默认不能装）
            boolean hasContainer = liquid.has("serving_container");
            boolean hasItem = liquid.has("serving_item");
            if (hasContainer != hasItem) {
                throw new JsonParseException("装瓶需同时给出 serving_container 和 serving_item");
            }
            if (hasContainer) {
                builder.serving(item(GsonHelper.getAsString(liquid, "serving_container")),
                        item(GsonHelper.getAsString(liquid, "serving_item")));
            }
        }
        JsonArray slots = GsonHelper.getAsJsonArray(config, "slots");
        if (slots.isEmpty() || slots.size() > 12) {
            throw new JsonParseException("大缸配方必须有 1 到 12 个投料格");
        }
        for (int index = 0; index < slots.size(); index++) {
            builder.slot(slot(slots.get(index).getAsJsonObject(), index));
        }
        return builder.build();
    }

    private static Component verb(JsonElement json) {
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
            String text = json.getAsString();
            if (text.isBlank()) {
                throw new JsonParseException("大缸工序动词不能为空");
            }
            return Component.literal(text);
        }
        if (json.isJsonObject()) {
            JsonObject object = json.getAsJsonObject();
            if (object.has("text") == object.has("translate")) {
                throw new JsonParseException("大缸工序动词需提供 text 或 translate，不能同时提供");
            }
            boolean translated = object.has("translate");
            String value = GsonHelper.getAsString(object, translated ? "translate" : "text");
            if (value.isBlank()) {
                throw new JsonParseException("大缸工序动词不能为空");
            }
            return translated ? Component.translatable(value) : Component.literal(value);
        }
        throw new JsonParseException("大缸工序动词需是字符串或 text/translate 对象");
    }

    /** 句式翻译键：写成字符串，或者 {"translate": "..."}，跟 verb 的写法对齐 */
    private static String translationKey(JsonElement json) {
        if (json == null || json.isJsonNull()) {
            throw new JsonParseException("大缸进度文字需提供翻译键");
        }
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
            String key = json.getAsString();
            if (key.isBlank()) {
                throw new JsonParseException("大缸进度文字的翻译键不能为空");
            }
            return key;
        }
        if (json.isJsonObject() && json.getAsJsonObject().has("translate")) {
            return translationKey(json.getAsJsonObject().get("translate"));
        }
        throw new JsonParseException("大缸进度文字需是翻译键字符串");
    }

    private static VatRecipe.Slot slot(JsonObject json, int index) {
        Predicate<ItemStack> matcher = ingredient(json.get("ingredient"));
        VatRecipe.BoundsRule bounds = bounds(GsonHelper.getAsJsonObject(json, "bounds"), index);
        VatRecipe.Fate fate = enumValue(VatRecipe.Fate.class, GsonHelper.getAsString(json, "fate"));
        VatRecipe.Slot slot = switch (fate) {
            case KEEP -> VatRecipe.Slot.keep(matcher, bounds, converter(json));
            case CONVERT -> VatRecipe.Slot.convert(matcher, bounds, item(GsonHelper.getAsString(json, "result")));
            case CONSUME -> VatRecipe.Slot.consume(matcher, bounds);
            case ABSORB -> VatRecipe.Slot.absorb(matcher, bounds);
        };
        if (json.has("refund")) {
            slot = slot.refund(item(GsonHelper.getAsString(json, "refund")));
        }
        if (GsonHelper.getAsBoolean(json, "per_dose", false)) {
            slot = slot.dosed();
        }
        if (enumValue(VatRecipe.Layer.class, GsonHelper.getAsString(json, "layer", "INGREDIENT"))
                == VatRecipe.Layer.SEASONING) {
            slot = slot.seasoning();
        }
        return slot;
    }

    private static VatRecipe.Converter converter(JsonObject json) {
        JsonElement value = json.get("converter");
        if (value != null) {
            if (value.isJsonObject()) {
                return mappedConverter(value.getAsJsonObject());
            }
            if (!value.isJsonPrimitive()) {
                throw new JsonParseException("大缸换算必须是字符串或映射对象");
            }
            return switch (value.getAsString()) {
                case "pickle" -> VatRecipes::convertPickle;
                case "spicy_pickle" -> VatRecipes::convertSpicyPickle;
                default -> throw new JsonParseException("未知的大缸换算：" + value);
            };
        }
        return json.has("result") ? VatRecipe.Converter.to(item(GsonHelper.getAsString(json, "result")))
                : VatRecipe.Converter.same();
    }

    /**
     * 按输入分别指定取货结果。第一个匹配的条目生效，没有匹配时原样返还。
     * 泡菜 / 辣白菜这类“同一槽位放不同蔬菜、取出不同成品”的配方用它扩展，不用再改 Java。
     */
    private static VatRecipe.Converter mappedConverter(JsonObject json) {
        String type = GsonHelper.getAsString(json, "type", "mapped");
        if (!type.equals("mapped")) {
            throw new JsonParseException("未知的大缸映射换算类型：" + type);
        }
        JsonArray entries = GsonHelper.getAsJsonArray(json, "entries");
        if (entries.isEmpty()) {
            throw new JsonParseException("大缸映射换算至少要有一个 entries 条目");
        }
        List<Mapping> mappings = new ArrayList<>(entries.size());
        for (JsonElement entry : entries) {
            if (!entry.isJsonObject()) {
                throw new JsonParseException("大缸映射换算条目必须是对象");
            }
            JsonObject mapping = entry.getAsJsonObject();
            mappings.add(new Mapping(ingredient(mapping.get("ingredient")),
                    item(GsonHelper.getAsString(mapping, "result"))));
        }
        return stack -> {
            for (Mapping mapping : mappings) {
                if (mapping.input().test(stack)) {
                    return new ItemStack(mapping.result());
                }
            }
            return stack;
        };
    }

    private record Mapping(Predicate<ItemStack> input, Item result) {
    }

    private static VatRecipe.BoundsRule bounds(JsonObject json, int index) {
        String type = GsonHelper.getAsString(json, "type");
        if ((type.equals("same_as") || type.equals("half_of"))
                && (GsonHelper.getAsInt(json, "slot") < 0 || GsonHelper.getAsInt(json, "slot") >= index)) {
            throw new JsonParseException("数量规则只能引用前面的格子");
        }
        return switch (type) {
            case "exact" -> VatRecipe.BoundsRule.exact(GsonHelper.getAsInt(json, "count"));
            case "between" -> VatRecipe.BoundsRule.between(GsonHelper.getAsInt(json, "min"),
                    GsonHelper.getAsInt(json, "max"));
            case "per_layer" -> VatRecipe.BoundsRule.perLayer(GsonHelper.getAsInt(json, "min"),
                    GsonHelper.getAsInt(json, "max"));
            case "per_layer_exact" -> VatRecipe.BoundsRule.perLayerExact(GsonHelper.getAsInt(json, "count"));
            case "min_with_per_layer_max" -> VatRecipe.BoundsRule.minWithPerLayerMax(
                    GsonHelper.getAsInt(json, "min"), GsonHelper.getAsInt(json, "max_per_layer"));
            case "min_by_water" -> {
                JsonArray levels = GsonHelper.getAsJsonArray(json, "min_by_water");
                if (levels.size() != 4) {
                    throw new JsonParseException("min_by_water 必须列出 0、1、2、3 层水的下限");
                }
                int[] minimums = new int[4];
                for (int waterLevel = 0; waterLevel < minimums.length; waterLevel++) {
                    minimums[waterLevel] = levels.get(waterLevel).getAsInt();
                }
                yield VatRecipe.BoundsRule.minByWater(minimums, GsonHelper.getAsInt(json, "max"));
            }
            case "same_as" -> VatRecipe.BoundsRule.sameAs(GsonHelper.getAsInt(json, "slot"),
                    GsonHelper.getAsInt(json, "ratio"));
            case "half_of" -> VatRecipe.BoundsRule.halfOf(GsonHelper.getAsInt(json, "slot"),
                    GsonHelper.getAsInt(json, "max"));
            default -> throw new JsonParseException("未知的大缸数量规则：" + type);
        };
    }

    private static Predicate<ItemStack> ingredient(JsonElement json) {
        if (json == null) {
            throw new JsonParseException("大缸原料不能为空");
        }
        if (json.isJsonArray()) {
            JsonArray choices = json.getAsJsonArray();
            if (choices.isEmpty()) {
                throw new JsonParseException("大缸原料选项不能为空");
            }
            Predicate<ItemStack> match = stack -> false;
            for (JsonElement choice : choices) {
                match = match.or(ingredient(choice));
            }
            return match;
        }
        JsonObject object = json.getAsJsonObject();
        if (object.has("item")) {
            Item item = item(GsonHelper.getAsString(object, "item"));
            return stack -> stack.is(item);
        }
        if (object.has("tag")) {
            TagKey<Item> tag = TagKey.create(Registries.ITEM, DdIds.parse(GsonHelper.getAsString(object, "tag")));
            return stack -> stack.is(tag);
        }
        if (object.has("matcher")) {
            return switch (GsonHelper.getAsString(object, "matcher")) {
                case "pickle_ingredient" -> VatRecipes::isPickleIngredient;
                case "raw_pork" -> VatRecipes::isMeatInput;
                case "raw_fish" -> VatRecipes::isRawFish;
                case "soybean" -> VatRecipes::isSoybean;
                case "wheat" -> VatRecipes::isWheat;
                case "shrimp" -> VatRecipes::isShrimp;
                case "vinegar_grain" -> VatRecipes::isVinegarGrain;
                case "corn_kernels" -> VatRecipes::isCornKernels;
                case "bread" -> VatRecipes::isBread;
                case "pear" -> VatRecipes::isPear;
                default -> throw new JsonParseException("未知的大缸原料匹配器：" + object.get("matcher"));
            };
        }
        throw new JsonParseException("大缸原料需提供 item、tag 或 matcher");
    }

    private static Item item(String id) {
        ResourceLocation key = DdIds.parse(id);
        return BuiltInRegistries.ITEM.getOptional(key)
                .orElseThrow(() -> new JsonParseException("不存在的物品：" + key));
    }

    /**
     * 产物液体。新配方直接写流体 id（{@code "northeast_china_delight:soy_sauce"}），
     * 旧数据包里写的是枚举名（{@code "SOY_SAUCE"}），两种都认：没有冒号的按旧枚举名翻译，
     * 带冒号的按流体 id 去注册表里找。清水不是成品液体，写了也算错。
     */
    private static net.minecraft.world.level.material.Fluid productFluid(String value) {
        if (!value.contains(":")) {
            net.minecraft.world.level.material.Fluid legacy = enumValue(VatRecipe.Fluid.class, value).resolve();
            if (legacy == null) {
                throw new JsonParseException("大缸产物不能是清水：" + value);
            }
            return legacy;
        }
        ResourceLocation key = DdIds.parse(value);
        net.minecraft.world.level.material.Fluid fluid = BuiltInRegistries.FLUID.getOptional(key)
                .orElseThrow(() -> new JsonParseException("不存在的流体：" + key));
        if (fluid.isSame(net.minecraft.world.level.material.Fluids.EMPTY)
                || fluid.isSame(net.minecraft.world.level.material.Fluids.WATER)) {
            throw new JsonParseException("大缸产物需是可存放的液体：" + key);
        }
        return fluid;
    }

    private static <T extends Enum<T>> T enumValue(Class<T> type, String value) {
        try {
            return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new JsonParseException("未知的 " + type.getSimpleName() + "：" + value, exception);
        }
    }
}
