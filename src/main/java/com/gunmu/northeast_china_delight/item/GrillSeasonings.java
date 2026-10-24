package com.gunmu.northeast_china_delight.item;

import com.gunmu.northeast_china_delight.ModTags;
import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.crafting.GrillVisual;
import com.gunmu.northeast_china_delight.util.DdIds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 烧烤时"怎么抹调料"的规则。
 */
public final class GrillSeasonings {

    private GrillSeasonings() {
    }

    public static final TagKey<Item> SAUCES = TagKey.create(Registries.ITEM,
            DdIds.of(NortheastChinaDelight.MODID, "grill_sauces"));

    public static boolean isSauce(ItemStack stack) {
        return !stack.isEmpty() && stack.is(SAUCES);
    }

    public static boolean isSauce(Item item) {
        return item.builtInRegistryHolder().is(SAUCES);
    }

    /** 这份调料是不是必须用刷子抹（瓶装调料 + 厚酱） */
    public static boolean needsBrush(ItemStack stack) {
        return SeasoningBottleItem.isBottle(stack) || isSauce(stack);
    }

    public static boolean isEgg(Item item) {
        return item.builtInRegistryHolder().is(ModTags.EGGS);
    }

    /** 厚酱（辣酱）刷上去的颜色 */
    public static final int SAUCE_COLOR = 0xB5321C;
    /** 番茄酱（农夫乐事）比辣酱鲜亮些 */
    public static final int TOMATO_SAUCE_COLOR = 0xD8442A;
    private static final ResourceLocation TOMATO_SAUCE = DdIds.of("farmersdelight", "tomato_sauce");
    /** 配方写了 brush 却没写颜色、调料又不是调料瓶 / 厚酱时的酱色 */
    private static final int GLAZE_COLOR = 0xA0521E;
    /** 撒料默认的颜色（盐、糖） */
    private static final int POWDER_COLOR = 0xF6F3EA;

    /** 默认稠度（不透明度）：瓶装调料稀，刷上去薄薄一层；厚酱实；蛋液几乎盖住底下；撒的颗粒不透明 */
    public static final float THIN_OPACITY = 0.40F;
    public static final float THICK_OPACITY = 0.78F;
    private static final float EGG_OPACITY = 0.96F;
    private static final float POWDER_OPACITY = 1.0F;

    /**
     * 把配方里写的视觉落实到这份调料上：AUTO 按调料判断（调料瓶和厚酱刷、鸡蛋打、其余撒），
     * 没写颜色、不透明度就按调料取。返回的 style 不会是 AUTO，color、opacity 不会是 -1。
     */
    public static GrillVisual resolve(GrillVisual declared, Item item) {
        GrillVisual.Style style = declared.style();
        if (style == GrillVisual.Style.AUTO) {
            style = item instanceof SeasoningBottleItem || isSauce(item) ? GrillVisual.Style.BRUSH
                    : isEgg(item) ? GrillVisual.Style.EGG
                    : GrillVisual.Style.SPRINKLE;
        }
        int color = declared.hasColor() ? declared.color()
                : item instanceof SeasoningBottleItem ? BottleColors.of(item) & 0xFFFFFF
                : isSauce(item) ? (BuiltInRegistries.ITEM.getKey(item).equals(TOMATO_SAUCE) ? TOMATO_SAUCE_COLOR : SAUCE_COLOR)
                : style == GrillVisual.Style.BRUSH ? GLAZE_COLOR
                : POWDER_COLOR;
        float opacity = declared.hasOpacity() ? declared.opacity()
                : style == GrillVisual.Style.EGG ? EGG_OPACITY
                : style == GrillVisual.Style.SPRINKLE ? POWDER_OPACITY
                : item instanceof SeasoningBottleItem ? THIN_OPACITY
                : THICK_OPACITY;
        return new GrillVisual(style, color, opacity);
    }
}
