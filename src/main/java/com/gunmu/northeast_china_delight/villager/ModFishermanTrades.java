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

import static com.gunmu.northeast_china_delight.villager.TradeHelpers.buy;
import static com.gunmu.northeast_china_delight.villager.TradeHelpers.sell;

public final class ModFishermanTrades
{
    private ModFishermanTrades()
    {
    }

    public static void addTrades(VillagerTradesEvent event)
    {
        if (event.getType() != VillagerProfession.FISHERMAN)
        {
            return;
        }
        Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();

        trades.get(2).add(sell(5, ModItems.SHRIMP.get(), 1, 16, 5));

        trades.get(3).add(buy(ModItems.SALTED_FISH.get(), 6, 16, 10));
        trades.get(3).add(sell(6, ModItems.OYSTER.get(), 1, 16, 10));

        trades.get(4).add(buy(ModItems.SEA_CUCUMBER.get(), 4, 16, 15));
        trades.get(4).add(sell(7, ModItems.HAIRTAIL.get(), 1, 12, 15));

        trades.get(5).add(sell(8, ModItems.XIAN_YU_BING_ZI.get(), 1, 12, 30));
        trades.get(5).add(sell(18, ModItems.CONG_SHAO_HAI_SHEN.get(), 1, 8, 30));
        trades.get(5).add(sell(15, ModItems.SUAN_CAI_HAI_XIAN_GUO.get(), 1, 8, 30));
    }
}
