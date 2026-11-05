package com.gunmu.northeast_china_delight.item;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * 盆装菜（大锅烹饪、大脸盆盛的那几道，含盆装米饭、盆装谜之炖菜）的方块物品：放下就是一大盆（见 {@code block/DdFeastBlock}），
 * 拿碗右键盛一份。
 *
 * <p>右键方块直接放下（BlockItem 的默认；2026-11-05 用户先要求和盘装菜一样潜行才放，当天又改回直接右键放下）。</p>
 *
 * <p>单独立这个类只为挂客户端渲染器：物品栏是 2D 贴图、手 / 地面是方块模型，
 * 分流见 {@code client/DdFeastItemRenderer} —— 渲染器得注册在具体的物品类上，
 * 所以不能继续用裸 {@code BlockItem}。</p>
 */
public class DdFeastItem extends BlockItem
{
    public DdFeastItem(Block block, Item.Properties properties)
    {
        super(block, properties);
    }

    /*
     * 1.20.1 没有 RegisterClientExtensionsEvent，客户端渲染器只能从物品这里挂；
     * 1.21.1 那份在 NortheastChinaDelight.ClientModEvents#onRegisterClientExtensions。
     */
    //? if <1.20.2 {
    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer)
    {
        consumer.accept(com.gunmu.northeast_china_delight.client.DdFeastItemRenderer.CLIENT_EXTENSION);
    }
    //?}
}
