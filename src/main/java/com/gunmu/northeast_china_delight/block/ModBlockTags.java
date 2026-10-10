package com.gunmu.northeast_china_delight.block;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.util.DdIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ModBlockTags
{
    public static final TagKey<Block> HAZEL_MUSHROOM_COLONY_GROWABLE_ON =
            TagKey.create(Registries.BLOCK,
                    DdIds.of(NortheastChinaDelight.MODID, "hazel_mushroom_colony_growable_on"));

    public static final TagKey<Item> KNIVES =
            TagKey.create(Registries.ITEM, DdIds.of("farmersdelight", "tools/knives"));

    /** 踩在这种方块上的榛蘑会随机刻长成菌落（对齐农夫乐事的沃土设定） */
    public static final TagKey<Block> RICH_SOIL_CONVERTS_TO_COLONY =
            TagKey.create(Registries.BLOCK,
                    DdIds.of(NortheastChinaDelight.MODID, "rich_soil_converts_to_colony"));

    public static final TagKey<Block> GINSENG_PLANTABLE_ON =
            TagKey.create(Registries.BLOCK,
                    DdIds.of(NortheastChinaDelight.MODID, "ginseng_plantable_on"));

    public static final TagKey<Block> GINSENG_SHADE =
            TagKey.create(Registries.BLOCK,
                    DdIds.of(NortheastChinaDelight.MODID, "ginseng_shade"));

    public static final TagKey<Block> GINSENG_SNOW_NEARBY =
            TagKey.create(Registries.BLOCK,
                    DdIds.of(NortheastChinaDelight.MODID, "ginseng_snow_nearby"));

    private ModBlockTags()
    {
    }
}
