package com.gunmu.northeast_china_delight.villager;

import com.gunmu.northeast_china_delight.item.ModItems;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if >=1.20.5 {
/*import net.minecraft.world.item.trading.ItemCost;
*///?}
import net.minecraft.world.item.trading.MerchantOffer;
//? if <1.20.2 {
import net.minecraftforge.common.BasicItemListing;
//?} else {
/*import net.neoforged.neoforge.common.BasicItemListing;
*///?}
//? if <1.20.2 {
import net.minecraftforge.event.village.WandererTradesEvent;
//?} else {
/*import net.neoforged.neoforge.event.village.WandererTradesEvent;
*///?}

import java.util.List;
import java.util.Optional;

public final class ModWandererTrades
{
    public static final int GINSENG_MIN_PRICE = 48;
    public static final int GINSENG_PRICE_SPREAD = 16;

    private ModWandererTrades()
    {
    }

    private static final List<Item> SEEDS = List.of(
            ModItems.NAPA_CABBAGE_SEEDS.get(), ModItems.CUCUMBER_SEEDS.get(),
            ModItems.GREEN_RADISH_SEEDS.get(), ModItems.GREEN_ONION_SEEDS.get(),
            ModItems.EGGPLANT_SEEDS.get(), ModItems.GREEN_PEPPER_SEEDS.get(),
            ModItems.RED_CHILI_SEEDS.get(), ModItems.GREEN_BEANS_SEEDS.get(),
            ModItems.CORN_SEEDS.get(), ModItems.SOYBEAN.get(),
            ModItems.BUCKWHEAT.get(), ModItems.SWEET_POTATO.get(),
            ModItems.PEANUT.get(), ModItems.RED_BEAN.get(),
            ModItems.HAZELNUT.get());

    public static void addTrades(WandererTradesEvent event)
    {
        List<VillagerTrades.ItemListing> generic = event.getGenericTrades();
        List<VillagerTrades.ItemListing> rare = event.getRareTrades();

        // 特殊货：山参（48~64 绿宝石 1 根，一次只卖一根 —— 雪地里刨出来的东西，不该白菜价）
        rare.add(ginsengListing());

        generic.add(new BasicItemListing(1, new ItemStack(ModItems.HAZELNUT.get()), 12, 1));
        generic.add(new BasicItemListing(1, new ItemStack(ModItems.HAZEL_MUSHROOM.get()), 12, 1));
        generic.add(new BasicItemListing(1, new ItemStack(ModItems.WOOD_EAR.get()), 12, 1));
        for (Item seed : SEEDS)
        {
            generic.add(new BasicItemListing(1, new ItemStack(seed), 12, 1));
        }
    }

    private static VillagerTrades.ItemListing ginsengListing()
    {
        return (trader, random) -> {
            int price = GINSENG_MIN_PRICE + random.nextInt(GINSENG_PRICE_SPREAD + 1);
            //? if <1.20.5 {
            return new MerchantOffer(
                    new ItemStack(Items.EMERALD, price),
                    ItemStack.EMPTY,
                    new ItemStack(ModItems.GINSENG.get()),
                    1,
                    1,
                    0.0F);
            //?} else {
            /*return new MerchantOffer(
                    new ItemCost(Items.EMERALD, price),
                    Optional.empty(),
                    new ItemStack(ModItems.GINSENG.get()),
                    1,
                    1,
                    0.0F);
            *///?}
        };
    }
}
