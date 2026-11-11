package com.gunmu.northeast_china_delight.villager;

import com.gunmu.chinese_regional_culinary_ingredients.block.CrciBlocks;

import com.gunmu.chinese_regional_culinary_ingredients.item.CrciItems;

import com.gunmu.northeast_china_delight.block.ModBlocks;
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
import static com.gunmu.northeast_china_delight.villager.TradeHelpers.buyBlock;
import static com.gunmu.northeast_china_delight.villager.TradeHelpers.sell;
import static com.gunmu.northeast_china_delight.villager.TradeHelpers.sellBlock;

public final class ModFarmerTrades
{
    private ModFarmerTrades()
    {
    }

    public static void addTrades(VillagerTradesEvent event)
    {
        if (event.getType() != VillagerProfession.FARMER)
        {
            return;
        }
        Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();

        trades.get(1).add(buy(CrciItems.NAPA_CABBAGE.get(), 16, 16, 2));
        trades.get(1).add(buy(CrciItems.CUCUMBER.get(), 20, 16, 2));
        trades.get(1).add(buy(CrciItems.GREEN_RADISH.get(), 18, 16, 2));
        trades.get(1).add(buy(CrciItems.GREEN_ONION.get(), 22, 16, 2));

        trades.get(2).add(sell(4, CrciItems.FROZEN_PEAR.get(), 1, 16, 5));
        trades.get(2).add(sell(3, CrciItems.EGGPLANT.get(), 1, 16, 5));
        trades.get(2).add(sell(3, CrciItems.GREEN_PEPPER.get(), 1, 16, 5));
        trades.get(2).add(sell(4, CrciItems.RED_CHILI.get(), 1, 16, 5));
        trades.get(2).add(sell(3, CrciItems.SNOW_MUSTARD.get(), 1, 16, 5));
        trades.get(2).add(buy(CrciItems.BELLFLOWER_ROOT.get(), 20, 16, 5));

        trades.get(3).add(sell(4, CrciItems.CORN.get(), 1, 16, 10));
        trades.get(3).add(buy(CrciItems.CORN_STALK.get(), 18, 16, 10));

        trades.get(4).add(sellBlock(12, CrciBlocks.SACKS.get("buckwheat_sack").get(), 1, 12, 15));
        trades.get(4).add(sellBlock(14, CrciBlocks.SACKS.get("red_bean_sack").get(), 1, 12, 15));
        trades.get(4).add(buyBlock(CrciBlocks.SACKS.get("corn_seeds_sack").get(), 1, 12, 15));

        trades.get(5).add(buyBlock(CrciBlocks.CRATES.get("sweet_potato_crate").get(), 1, 12, 30));
        trades.get(5).add(buyBlock(CrciBlocks.SACKS.get("soybean_sack").get(), 1, 12, 30));
        trades.get(5).add(buyBlock(CrciBlocks.SACKS.get("peanut_sack").get(), 1, 12, 30));
        trades.get(5).add(sell(6, ModItems.GRILLED_CORN.get(), 1, 12, 30));
        trades.get(5).add(sell(3, ModItems.BAKED_SWEET_POTATO.get(), 1, 12, 30));
        trades.get(5).add(sell(6, ModItems.DI_SAN_XIAN.get(), 1, 12, 30));
        trades.get(5).add(sell(4, ModItems.ZHAN_JIANG_CAI.get(), 1, 12, 30));
        trades.get(5).add(sell(5, ModItems.TIGER_SALAD.get(), 1, 12, 30));
        trades.get(5).add(sell(10, ModItems.DA_FENG_SHOU_BOWL.get(), 1, 8, 30));
    }
}
