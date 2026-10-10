package com.gunmu.northeast_china_delight.item;

import com.gunmu.northeast_china_delight.NortheastChinaConfig;
import com.gunmu.northeast_china_delight.util.DdStacks;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;

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
                && NortheastChinaConfig.DISH_PLACEMENT_ENABLED.get();

        if (wantPlace)
        {
            InteractionResult placed = this.place(new BlockPlaceContext(context));
            if (placed.consumesAction())
            {
                return placed;
            }
        }

        if (DdStacks.isEdible(context.getItemInHand()) && player != null)
        {
            InteractionResult eaten = this.use(context.getLevel(), player, context.getHand()).getResult();
            return eaten == InteractionResult.CONSUME ? InteractionResult.CONSUME_PARTIAL : eaten;
        }
        return InteractionResult.PASS;
    }

    /**
     * 1.20.1 没有食物的「返还容器」组件（{@code usingConvertsTo}），
     * 只能自己在这里把空碗还给玩家（和 {@link DdConsumableItem} 同一套写法）。
     */
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
    }*/
    //?}
}
