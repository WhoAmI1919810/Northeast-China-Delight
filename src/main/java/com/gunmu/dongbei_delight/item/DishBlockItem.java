package com.gunmu.dongbei_delight.item;

import com.gunmu.dongbei_delight.DongbeiConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

/**
 * 菜肴的物品形态：既是食物，也是能摆的方块。
 *
 * <ul>
 *   <li><b>普通右键</b>：直接吃（对准方块也是吃，不会误放）；</li>
 *   <li><b>潜行右键</b>：把这道菜摆成方块形态；</li>
 *   <li>摆放功能由 {@link DongbeiConfig#DISH_PLACEMENT_ENABLED} 控制，关掉之后潜行右键既不放置、
 *       也不会有任何提示文字，直接当普通右键吃。</li>
 * </ul>
 */
public class DishBlockItem extends ItemNameBlockItem
{
    public DishBlockItem(Block block, Properties properties)
    {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        Player player = context.getPlayer();
        boolean wantPlace = player != null && player.isSecondaryUseActive()
                && DongbeiConfig.DISH_PLACEMENT_ENABLED.get();

        if (wantPlace)
        {
            InteractionResult placed = this.place(new BlockPlaceContext(context));
            if (placed.consumesAction())
            {
                return placed;
            }
        }

        // 普通右键（或摆放功能被关掉时）：直接吃，什么都不提示
        if (context.getItemInHand().has(DataComponents.FOOD) && player != null)
        {
            InteractionResult eaten = this.use(context.getLevel(), player, context.getHand()).getResult();
            return eaten == InteractionResult.CONSUME ? InteractionResult.CONSUME_PARTIAL : eaten;
        }
        return InteractionResult.PASS;
    }
}
