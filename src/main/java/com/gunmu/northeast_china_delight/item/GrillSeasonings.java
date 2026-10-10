package com.gunmu.northeast_china_delight.item;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.util.DdIds;
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
}
