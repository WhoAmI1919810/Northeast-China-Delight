package com.gunmu.northeast_china_delight.item;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 烧烤时"怎么抹调料"的规则。
 *
 * <p>调料分两类：
 * <ul>
 *     <li><b>瓶装调料</b>（{@link SeasoningBottleItem}：酱油、醋、辣椒油…）—— 左手拿瓶、右手拿刷子抹，
 *         每份食材只花 {@link SeasoningBottleItem#GRILL_DOSE_MB} mB；</li>
 *     <li><b>厚酱</b>（标签 {@link #SAUCES}：辣椒酱…）—— 同样要用刷子抹，但一份酱就抹一份食材
 *         （它不按 mB 记账，也没有耐久条）；</li>
 *     <li>其余（盐、糖…）直接撒，一次消耗一份。</li>
 * </ul>
 *
 * <p>想再让某种酱"必须刷、别撒"，只要把它加进
 * {@code data/northeast_china_delight/tags/item/grill_sauces.json} 就行。
 */
public final class GrillSeasonings {

    private GrillSeasonings() {
    }

    /** 需要拿刷子抹的厚酱（辣椒酱等） */
    public static final TagKey<Item> SAUCES = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(NortheastChinaDelight.MODID, "grill_sauces"));

    /** 这份调料是不是"厚酱" */
    public static boolean isSauce(ItemStack stack) {
        return !stack.isEmpty() && stack.is(SAUCES);
    }

    /** 这份调料是不是"厚酱"（按物品判断，渲染 / JEI 用得到） */
    public static boolean isSauce(Item item) {
        return item.builtInRegistryHolder().is(SAUCES);
    }

    /** 这份调料是不是必须用刷子抹（瓶装调料 + 厚酱） */
    public static boolean needsBrush(ItemStack stack) {
        return SeasoningBottleItem.isBottle(stack) || isSauce(stack);
    }
}
