package com.gunmu.northeast_china_delight.crafting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.util.Locale;

/**
 * 烤架配方里一份调料加上去之后的样子：撒料、刷酱、打蛋或不显示。
 *
 * <p>{@code style} 为 {@link Style#AUTO} 时按调料本身判断（调料瓶和厚酱是刷，鸡蛋是打，其余是撒）；
 * {@code color} 为 -1 时按调料取色；{@code opacity}（0~1，越大越稠、越实）为 -1 时按调料取默认值。</p>
 */
public record GrillVisual(Style style, int color, float opacity) {

    public static final GrillVisual AUTO = new GrillVisual(Style.AUTO, -1, -1.0F);

    public enum Style {
        AUTO, SPRINKLE, BRUSH, EGG, NONE;

        public String id() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    public GrillVisual(Style style, int color) {
        this(style, color, -1.0F);
    }

    public boolean hasColor() {
        return this.color >= 0;
    }

    public boolean hasOpacity() {
        return this.opacity >= 0.0F;
    }

    /** 调料条目里写了 visual / color / opacity（或用 ingredient 包了一层）才算带视觉设置 */
    public static boolean declaredIn(JsonObject entry) {
        return entry.has("ingredient") || entry.has("visual") || entry.has("color") || entry.has("opacity");
    }

    /** 把这几个视觉字段从调料条目里去掉，剩下的才是原料写法 */
    public static void strip(JsonObject entry) {
        entry.remove("visual");
        entry.remove("color");
        entry.remove("opacity");
    }

    public static GrillVisual fromJson(JsonObject entry) {
        Style style = Style.AUTO;
        if (entry.has("visual")) {
            JsonElement value = entry.get("visual");
            String name = value.isJsonPrimitive() ? value.getAsString().toLowerCase(Locale.ROOT) : "";
            style = null;
            for (Style candidate : Style.values()) {
                if (candidate.id().equals(name)) {
                    style = candidate;
                }
            }
            if (style == null) {
                throw new JsonParseException("烤架调料 visual 只能是 sprinkle、brush、egg、none 或 auto：" + value);
            }
        }
        int color = -1;
        if (entry.has("color")) {
            color = parseColor(entry.get("color"));
        }
        float opacity = -1.0F;
        if (entry.has("opacity")) {
            opacity = parseOpacity(entry.get("opacity"));
        }
        return new GrillVisual(style, color, opacity);
    }

    private static int parseColor(JsonElement value) {
        String text = value.isJsonPrimitive() && value.getAsJsonPrimitive().isString() ? value.getAsString() : "";
        if (text.startsWith("#")) {
            text = text.substring(1);
        }
        if (!text.matches("[0-9a-fA-F]{6}")) {
            throw new JsonParseException("烤架调料 color 必须写成 \"#RRGGBB\"：" + value);
        }
        return Integer.parseInt(text, 16);
    }

    private static float parseOpacity(JsonElement value) {
        float opacity = value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber() ? value.getAsFloat() : -1.0F;
        if (!(opacity > 0.0F && opacity <= 1.0F)) {
            throw new JsonParseException("烤架调料 opacity 必须是大于 0、不超过 1 的数：" + value);
        }
        return opacity;
    }
}
