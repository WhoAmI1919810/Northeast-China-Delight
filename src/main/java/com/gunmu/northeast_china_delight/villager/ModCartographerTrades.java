package com.gunmu.northeast_china_delight.villager;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
//? if <1.20.2 {
import net.minecraftforge.event.village.VillagerTradesEvent;
//?} else {
/*import net.neoforged.neoforge.event.village.VillagerTradesEvent;
*///?}

import java.util.List;

/**
 * 制图师：在原有的「5/6 级」档位（原版只有 5 级，所以落在原版 4、5 级）里挂小院地图。
 *
 * <p>和原版卖探险家地图的条目一样：13 个绿宝石加一个指南针换一张地图，
 * 地图本身是原版的地图物品，价位、次数、经验都照着原版探险家地图来。</p>
 */
public final class ModCartographerTrades
{
    private ModCartographerTrades()
    {
    }

    public static void addTrades(VillagerTradesEvent event)
    {
        if (event.getType() != VillagerProfession.CARTOGRAPHER)
        {
            return;
        }
        Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();

        // 原版 4 级（专家）与 5 级（大师）
        trades.get(4).add(new YardMapForEmeralds(13, 12, 15));
        trades.get(5).add(new YardMapForEmeralds(13, 12, 30));
    }
}
