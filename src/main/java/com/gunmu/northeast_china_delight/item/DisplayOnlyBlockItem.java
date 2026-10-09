package com.gunmu.northeast_china_delight.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

/**
 * 只当物品用的方块。
 *
 * <p>本体得是个方块，物品栏里才能像大缸一样按方块模型立体渲染；
 * 但永远放不到世界里 —— 玩家真去摆它时，在物品栏上方提示一句为什么。缸坯用它。</p>
 */
public class DisplayOnlyBlockItem extends BlockItem
{
    /** 摆放被拦下时显示的提示语键 */
    private final String placeHintKey;

    public DisplayOnlyBlockItem(Block block, String placeHintKey, Properties properties)
    {
        super(block, properties);
        this.placeHintKey = placeHintKey;
    }

    /** 右键不摆放：提示一句原因，手感照原版「这里放不下」返回 FAIL */
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
