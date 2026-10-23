package com.gunmu.northeast_china_delight.item;

import com.gunmu.northeast_china_delight.block.BellflowerCropBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

/** 桔梗种子：只允许播在耕地上，野外桔梗由世界生成直接放置。 */
public class BellflowerSeedItem extends ItemNameBlockItem
{
    public BellflowerSeedItem(Block block, Properties properties)
    {
        super(block, properties);
    }

    @Override
    public BlockPlaceContext updatePlacementContext(BlockPlaceContext context)
    {
        BlockPlaceContext adjusted = super.updatePlacementContext(context);
        if (adjusted == null)
        {
            return null;
        }
        BlockPos placementPos = adjusted.getClickedPos();
        if (!BellflowerCropBlock.isPlayerPlantableGround(
                adjusted.getLevel().getBlockState(placementPos.below())))
        {
            return null;
        }
        return adjusted;
    }
}
