package com.gunmu.northeast_china_delight.villager;

import com.gunmu.northeast_china_delight.item.ModItems;
import com.gunmu.northeast_china_delight.item.SeasoningBottleItem;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if <1.20.2 {
/*import net.minecraftforge.common.BasicItemListing;
*///?} else {
import net.neoforged.neoforge.common.BasicItemListing;
//?}
//? if <1.20.2 {
/*import net.minecraftforge.event.village.VillagerTradesEvent;
*///?} else {
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
//?}

import java.util.List;

import static com.gunmu.northeast_china_delight.villager.TradeHelpers.buy;
import static com.gunmu.northeast_china_delight.villager.TradeHelpers.sell;

public final class SideDishMerchantTrades
{
    private SideDishMerchantTrades()
    {
    }

    public static void addTrades(VillagerTradesEvent event)
    {
        if (event.getType() != ModVillagerProfessions.SIDE_DISH_MERCHANT.get())
        {
            return;
        }
        Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();

        trades.get(1).add(sell(2, ModItems.SALT.get(), 1, 16, 2));
        trades.get(1).add(sell(3, Items.SUGAR, 1, 16, 2));
        trades.get(1).add(sell(4, ModItems.TOFU.get(), 1, 16, 2));
        trades.get(1).add(sellWithUses(ModItems.SOY_SAUCE.get(), 1, 3, 16, 2));
        trades.get(1).add(buy(ModItems.SOYBEAN.get(), 24, 16, 2));

        trades.get(2).add(sell(6, ModItems.DRIED_TOFU.get(), 1, 16, 5));
        trades.get(2).add(sell(8, ModItems.VERMICELLI.get(), 1, 16, 5));
        trades.get(2).add(sell(5, ModItems.SOY_PASTE_CHUNK.get(), 1, 16, 5));
        trades.get(2).add(coldOnly(sellWithUses(ModItems.SOY_PASTE.get(), 2, 4, 12, 5)));
        trades.get(2).add(buy(ModItems.NAPA_CABBAGE.get(), 18, 16, 5));

        trades.get(3).add(sellWithUses(ModItems.VINEGAR.get(), 2, 4, 12, 10));
        trades.get(3).add(sellWithUses(ModItems.WHITE_VINEGAR.get(), 2, 4, 12, 10));
        trades.get(3).add(sellWithUses(ModItems.CHILI_SAUCE.get(), 2, 5, 12, 10));
        trades.get(3).add(coldOnly(sell(5, ModItems.PICKLED_GREEN_RADISH.get(), 1, 12, 10)));
        trades.get(3).add(coldOnly(sell(6, ModItems.SOUR_CABBAGE.get(), 1, 12, 10)));
        trades.get(3).add(coldOnly(sell(9, ModItems.SPICY_CABBAGE.get(), 1, 12, 10)));
        trades.get(3).add(buy(ModItems.RED_CHILI.get(), 18, 16, 10));

        trades.get(4).add(sellFull(ModItems.SOY_SAUCE.get(), 8, 12, 15));
        trades.get(4).add(sellFull(ModItems.CHILI_OIL.get(), 10, 12, 15));
        trades.get(4).add(sellFull(ModItems.FISH_SAUCE.get(), 9, 12, 15));
        trades.get(4).add(sell(12, ModItems.LA_PI.get(), 1, 12, 15));
        trades.get(4).add(sell(10, ModItems.COLD_NOODLE_SHEET.get(), 1, 12, 15));
        trades.get(4).add(buy(ModItems.CORN.get(), 20, 16, 15));

        trades.get(5).add(sellFull(ModItems.SHRIMP_PASTE.get(), 12, 12, 30));
        trades.get(5).add(sellFull(ModItems.ANIMAL_OIL.get(), 10, 12, 30));
        trades.get(5).add(sell(6, ModItems.SWEET_POTATO_STARCH.get(), 1, 12, 30));
        trades.get(5).add(sell(4, ModItems.ROASTED_PEANUTS.get(), 1, 12, 30));
        trades.get(5).add(sell(5, ModItems.BEAN_SPROUTS.get(), 1, 12, 30));
        trades.get(5).add(sell(14, ModItems.BUCKWHEAT_COLD_NOODLES.get(), 1, 8, 30));
        trades.get(5).add(sell(7, ModItems.CANDIED_PEANUTS.get(), 1, 12, 30));
        trades.get(5).add(sell(9, ModItems.NIAN_DOU_BAO.get(), 1, 12, 30));
        trades.get(5).add(sell(6, ModItems.SOY_MILK.get(), 1, 12, 30));
        trades.get(5).add(sell(8, ModItems.KVASS.get(), 1, 12, 30));
        trades.get(5).add(buy(ModItems.PEANUT.get(), 22, 16, 30));
    }

    private static VillagerTrades.ItemListing sellWithUses(Item item, int uses, int emeralds, int maxUses, int xp)
    {
        ItemStack stack = new ItemStack(item);
        stack.setDamageValue(SeasoningBottleItem.CAPACITY_MB - uses * SeasoningBottleItem.DOSE_MB);
        return new BasicItemListing(emeralds, stack, maxUses, xp);
    }

    private static VillagerTrades.ItemListing sellFull(Item item, int price, int maxUses, int xp)
    {
        return new BasicItemListing(price, new ItemStack(item), maxUses, xp);
    }

    private static VillagerTrades.ItemListing coldOnly(VillagerTrades.ItemListing listing)
    {
        return (trader, random) -> {
            if (trader instanceof Villager villager)
            {
                VillagerType type = villager.getVillagerData().getType();
                if (type == VillagerType.SNOW || type == VillagerType.TAIGA)
                {
                    return listing.getOffer(trader, random);
                }
            }
            return null;
        };
    }
}
