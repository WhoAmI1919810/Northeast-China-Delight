package com.gunmu.dongbei_delight.block;

import com.gunmu.dongbei_delight.DongbeiDelight;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** 本模组用到的方块／物品标签。 */
public final class ModBlockTags
{
    /** 榛蘑簇能往上长的地面（照农夫乐事的蘑菇菌簇，另外补上普通土壤） */
    public static final TagKey<Block> HAZEL_MUSHROOM_COLONY_GROWABLE_ON =
            TagKey.create(Registries.BLOCK,
                    ResourceLocation.fromNamespaceAndPath(DongbeiDelight.MODID, "hazel_mushroom_colony_growable_on"));

    /** 农夫乐事的刀（榛蘑簇可以一刀割完） */
    public static final TagKey<Item> KNIVES =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("farmersdelight", "tools/knives"));

    /** 人参能种的地面：灰化土、泥土、苔藓块 */
    public static final TagKey<Block> GINSENG_PLANTABLE_ON =
            TagKey.create(Registries.BLOCK,
                    ResourceLocation.fromNamespaceAndPath(DongbeiDelight.MODID, "ginseng_plantable_on"));

    /** 能给参苗遮阴的方块（树叶） */
    public static final TagKey<Block> GINSENG_SHADE =
            TagKey.create(Registries.BLOCK,
                    ResourceLocation.fromNamespaceAndPath(DongbeiDelight.MODID, "ginseng_shade"));

    /** 算作「附近有雪」的方块 */
    public static final TagKey<Block> GINSENG_SNOW_NEARBY =
            TagKey.create(Registries.BLOCK,
                    ResourceLocation.fromNamespaceAndPath(DongbeiDelight.MODID, "ginseng_snow_nearby"));

    private ModBlockTags()
    {
    }
}
