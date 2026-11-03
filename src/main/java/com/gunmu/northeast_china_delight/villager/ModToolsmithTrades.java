package com.gunmu.northeast_china_delight.villager;

import com.gunmu.northeast_china_delight.item.ModItems;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
//? if <1.20.2 {
/*import net.minecraftforge.event.village.VillagerTradesEvent;
*///?} else {
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
//?}

import java.util.List;

import static com.gunmu.northeast_china_delight.villager.TradeHelpers.sell;

/**
 * 工具匠：4 级卖黄铜碗、5 级（满级）卖大脸盆（2026-11-04 用户：机械动力改成可选前置以后，不装它做不出黄铜板、铁板，
 * 这两样就从工具匠这儿买，也会在东北小院的餐桌、厕所等处自然生成）。定价往贵里定（用户：定价肯定不能便宜）：
 * 一个黄铜碗 12 绿宝石（荞麦冷面连碗带菜才 14），一个大脸盆 18 绿宝石。
 */
public final class ModToolsmithTrades
{
    private ModToolsmithTrades()
    {
    }

    public static void addTrades(VillagerTradesEvent event)
    {
        if (event.getType() != VillagerProfession.TOOLSMITH)
        {
            return;
        }
        Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();

        trades.get(4).add(sell(12, ModItems.BRASS_BOWL.get(), 1, 8, 15));

        trades.get(5).add(sell(18, ModItems.LARGE_BASIN.get(), 1, 4, 30));
    }
}
