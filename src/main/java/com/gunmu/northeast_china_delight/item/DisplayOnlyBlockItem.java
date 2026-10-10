package com.gunmu.northeast_china_delight.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

public class DisplayOnlyBlockItem extends BlockItem
{
    private final String placeHintKey;

    public DisplayOnlyBlockItem(Block block, String placeHintKey, Properties properties)
    {
        super(block, properties);
        this.placeHintKey = placeHintKey;
    }

    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        Player player = context.getPlayer();
        if (player != null && !context.getLevel().isClientSide)
        {
            player.displayClientMessage(Component.translatable(this.placeHintKey), true);
        }
        return InteractionResult.FAIL;
    }
}
