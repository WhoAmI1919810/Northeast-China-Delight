package com.gunmu.dongbei_delight.villager;

import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.BasicItemListing;

/**
 * 各类村民交易共用的几个小助手：
 * sell = 玩家给村民绿宝石、村民把货给玩家；buy = 玩家把货卖给村民、村民给绿宝石。
 */
final class TradeHelpers
{
    private TradeHelpers()
    {
    }

    /** 他卖给你：emeralds 个绿宝石 -> count 个货。 */
    static VillagerTrades.ItemListing sell(int emeralds, Item item, int count, int maxUses, int xp)
    {
        return new BasicItemListing(emeralds, new ItemStack(item, count), maxUses, xp);
    }

    /** 他卖给你方块化的存储货（箱装/袋装）。 */
    static VillagerTrades.ItemListing sellBlock(int emeralds, Block block, int count, int maxUses, int xp)
    {
        return new BasicItemListing(emeralds, new ItemStack(block, count), maxUses, xp);
    }

    /** 他收：count 个货 -> 1 个绿宝石。 */
    static VillagerTrades.ItemListing buy(Item item, int count, int maxUses, int xp)
    {
        return new BasicItemListing(new ItemStack(item, count), new ItemStack(Items.EMERALD), maxUses, xp, 0.05F);
    }

    /** 他收方块化的存储货：count 个方块 -> 1 个绿宝石。 */
    static VillagerTrades.ItemListing buyBlock(Block block, int count, int maxUses, int xp)
    {
        return new BasicItemListing(new ItemStack(block, count), new ItemStack(Items.EMERALD), maxUses, xp, 0.05F);
    }
}
