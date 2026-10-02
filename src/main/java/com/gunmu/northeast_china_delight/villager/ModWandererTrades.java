package com.gunmu.northeast_china_delight.villager;

import com.gunmu.northeast_china_delight.item.ModItems;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.neoforge.common.BasicItemListing;
import net.neoforged.neoforge.event.village.WandererTradesEvent;

import java.util.List;
import java.util.Optional;

/**
 * 流浪商人：特殊货里掺人参，普通货里掺山珍和本模组所有作物的种子。
 * 每样都只挂一条，和原版那些摊位平分出现概率。
 */
public final class ModWandererTrades
{
    /** 山参的起步价：48 绿宝石 */
    public static final int GINSENG_MIN_PRICE = 48;
    /** 出摊时在这个基础上随机再加 0~16，也就是 48~64 之间浮动 */
    public static final int GINSENG_PRICE_SPREAD = 16;

    private ModWandererTrades()
    {
    }

    /** 本模组能种出东西的种子（含「自己就是种子」的那几种作物）。 */
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

        // 普通货：山珍三件套 + 各种作物的种子
        generic.add(new BasicItemListing(1, new ItemStack(ModItems.HAZELNUT.get()), 12, 1));
        generic.add(new BasicItemListing(1, new ItemStack(ModItems.HAZEL_MUSHROOM.get()), 12, 1));
        generic.add(new BasicItemListing(1, new ItemStack(ModItems.WOOD_EAR.get()), 12, 1));
        for (Item seed : SEEDS)
        {
            generic.add(new BasicItemListing(1, new ItemStack(seed), 12, 1));
        }
    }

    /**
     * 山参这条交易：每来一个流浪商人现掷一次价（48 + 0~16），而且一次只卖一根。
     *
     * <p>原版那套固定价写法给不了随机价，所以这里直接写一条 {@link VillagerTrades.ItemListing}，
     * 在 {@code getOffer} 里掷骰子。</p>
     */
    private static VillagerTrades.ItemListing ginsengListing()
    {
        return (trader, random) -> {
            int price = GINSENG_MIN_PRICE + random.nextInt(GINSENG_PRICE_SPREAD + 1);
            return new MerchantOffer(
                    new ItemCost(Items.EMERALD, price),
                    Optional.empty(),
                    new ItemStack(ModItems.GINSENG.get()),
                    1,
                    1,
                    0.0F);
        };
    }
}
