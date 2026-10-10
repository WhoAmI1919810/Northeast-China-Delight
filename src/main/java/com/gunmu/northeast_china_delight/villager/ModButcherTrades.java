package com.gunmu.northeast_china_delight.villager;

import com.gunmu.northeast_china_delight.block.ModBlocks;
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

import static com.gunmu.northeast_china_delight.villager.TradeHelpers.buy;
import static com.gunmu.northeast_china_delight.villager.TradeHelpers.buyBlock;
import static com.gunmu.northeast_china_delight.villager.TradeHelpers.sell;

public final class ModButcherTrades
{
    private ModButcherTrades()
    {
    }

    public static void addTrades(VillagerTradesEvent event)
    {
        if (event.getType() != VillagerProfession.BUTCHER)
        {
            return;
        }
        Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();

        trades.get(1).add(buy(ModItems.PIG_LIVER.get(), 6, 16, 2));
        trades.get(1).add(buy(ModItems.PORK_HOCK.get(), 4, 16, 2));

        trades.get(2).add(sell(6, ModItems.PORK_RIBS.get(), 1, 16, 5));
        trades.get(2).add(sell(5, ModItems.OIL_EDGE.get(), 1, 16, 5));

        trades.get(3).add(buy(ModItems.PORK_INTESTINE.get(), 10, 16, 10));
        trades.get(3).add(buy(ModItems.PIG_BLOOD.get(), 12, 16, 10));

        trades.get(4).add(sell(8, ModItems.GRILLED_CHICKEN_FRAME.get(), 1, 12, 15));
        trades.get(4).add(sell(9, ModItems.GRILLED_OIL_EDGE.get(), 1, 12, 15));
        trades.get(4).add(sell(7, ModItems.RED_SAUSAGE.get(), 1, 12, 15));
        trades.get(4).add(sell(6, ModItems.BLOOD_SAUSAGE.get(), 1, 12, 15));
        trades.get(4).add(sell(6, ModItems.RICE_SAUSAGE.get(), 1, 12, 15));

        trades.get(5).add(buyBlock(ModBlocks.ANIMAL_OIL_BLOCK.get(), 1, 12, 30));
        trades.get(5).add(sell(14, ModItems.HONG_SHAO_PAI_GU.get(), 1, 12, 30));
        trades.get(5).add(sell(16, ModItems.XIAO_JI_DUN_MO_GU_BOWL.get(), 1, 8, 30));
        trades.get(5).add(sell(12, ModItems.ZHU_ROU_DUN_FEN_TIAO_BOWL.get(), 1, 8, 30));
        trades.get(5).add(sell(13, ModItems.XUN_JIANG_PIN_PAN.get(), 1, 8, 30));
        trades.get(5).add(sell(15, ModItems.OLD_STYLE_GUO_BAO_ROU.get(), 1, 8, 30));
        trades.get(5).add(sell(16, ModItems.NEW_STYLE_GUO_BAO_ROU.get(), 1, 8, 30));
        trades.get(5).add(sell(17, ModItems.ORANGE_GUO_BAO_ROU.get(), 1, 8, 30));
        trades.get(5).add(sell(13, ModItems.LIU_ROU_DUAN.get(), 1, 8, 30));
    }
}
