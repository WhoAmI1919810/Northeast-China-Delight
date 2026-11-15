package com.gunmu.northeast_china_delight.block;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.util.DdIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class ModBlockTags
{
    /** 能作为枕头睡眠条件中“被子方块”的方块。 */
    public static final TagKey<Block> QUILTS =
            TagKey.create(Registries.BLOCK,
                    DdIds.of(NortheastChinaDelight.MODID, "quilts"));

    private ModBlockTags()
    {
    }
}