package com.gunmu.northeast_china_delight.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * 黄桃罐头：玻璃瓶装的甜食，右键方块放置，右键空气照常食用并返还玻璃瓶。
 *
 * <p>物品栏画 16×16 贴图，拿在手上 / 掉落 / 展示框画方块模型，
 * 分视角见 {@code models/item/canned_yellow_peach.json}（separate_transforms）。</p>
 */
public class CannedYellowPeachItem extends BlockItem
{
    public CannedYellowPeachItem(Block block, Properties properties)
    {
        super(block, properties);
    }

    //? if <1.20.5 {
    /*@Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity consumer)
    {
        ItemStack container = stack.getCraftingRemainingItem();
        super.finishUsingItem(stack, level, consumer);
        if (container.isEmpty())
        {
            return stack;
        }
        if (stack.isEmpty())
        {
            return container;
        }
        if (consumer instanceof Player player && !player.getAbilities().instabuild)
        {
            if (!player.getInventory().add(container))
            {
                player.drop(container, false);
            }
        }
        return stack;
    }
    *///?}
}