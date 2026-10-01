package com.gunmu.dongbei_delight.item;

import com.gunmu.dongbei_delight.block.HazelMushroomColonyBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * 榛蘑簇的物品形态。
 *
 * <p>和农夫乐事的蘑菇菌簇一样：玩家自己放下去的时候直接是**长满的 3 阶**，
 * 而不是放下一丛幼苗 —— 放下去还要等它长，手感很差。</p>
 */
public class HazelMushroomColonyItem extends BlockItem
{
    public HazelMushroomColonyItem(Block block, Properties properties)
    {
        super(block, properties);
    }

    @Nullable
    @Override
    protected BlockState getPlacementState(BlockPlaceContext context)
    {
        BlockState placed = super.getPlacementState(context);
        if (placed == null)
        {
            return null;
        }
        BlockState mature = placed.setValue(HazelMushroomColonyBlock.COLONY_AGE, HazelMushroomColonyBlock.MAX_AGE);
        return this.canPlace(context, mature) ? mature : null;
    }
}
