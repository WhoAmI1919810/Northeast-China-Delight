package com.gunmu.northeast_china_delight.villager;

import com.gunmu.northeast_china_delight.item.ModItems;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
//? if <1.20.2 {
import net.minecraftforge.event.village.VillagerTradesEvent;
//?} else {
/*import net.neoforged.neoforge.event.village.VillagerTradesEvent;
*///?}

import java.util.List;

import static com.gunmu.northeast_china_delight.villager.TradeHelpers.sell;

/**
 * 工具匠：4 级出售黄铜碗，5 级出售大脸盆。两者也会在东北小院自然生成。
 * 机械动力是可选前置，未安装时无法制作黄铜板和铁板，因此保留这两条交易。
 * 定价：黄铜碗 12 绿宝石（荞麦冷面连碗带菜 14），大脸盆 18 绿宝石。
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
