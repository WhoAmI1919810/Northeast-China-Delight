package com.gunmu.northeast_china_delight.villager;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
//? if <1.20.2 {
/*import net.minecraftforge.event.village.VillagerTradesEvent;
*///?} else {
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
//?}

import java.util.List;

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

        trades.get(4).add(new YardMapForEmeralds(13, 12, 15));
        trades.get(5).add(new YardMapForEmeralds(13, 12, 30));
    }
}
