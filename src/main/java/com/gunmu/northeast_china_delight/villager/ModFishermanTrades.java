package com.gunmu.northeast_china_delight.villager;

import com.gunmu.northeast_china_delight.item.ModItems;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

import java.util.List;

import static com.gunmu.northeast_china_delight.villager.TradeHelpers.buy;
import static com.gunmu.northeast_china_delight.villager.TradeHelpers.sell;

/**
 * 渔夫的交易表。
 *
 * <p>需求 1 级保持原版不动；2/3/4 级各占原版 2/3/4 级；
 * 需求 5 级并进原版 4 级；需求 6/7/8 级并进原版 5 级（大师）。</p>
 */
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

        // ===== 原版 1 级：不动 =====

        // ===== 原版 2 级：卖大虾 =====
        trades.get(2).add(sell(5, ModItems.SHRIMP.get(), 1, 16, 5));       // 大虾：5 绿宝石 1 只

        // ===== 原版 3 级：收咸鱼、卖生蚝 =====
        trades.get(3).add(buy(ModItems.SALTED_FISH.get(), 6, 16, 10));     // 收咸鱼 6 换 1
        trades.get(3).add(sell(6, ModItems.OYSTER.get(), 1, 16, 10));      // 生蚝：6 绿宝石 1 只

        // ===== 原版 4 级：收海参、卖带鱼 =====
        trades.get(4).add(buy(ModItems.SEA_CUCUMBER.get(), 4, 16, 15));    // 收海参 4 换 1
        trades.get(4).add(sell(7, ModItems.HAIRTAIL.get(), 1, 12, 15));    // 带鱼：7 绿宝石 1 条

        // ===== 原版 5 级（大师）：卖海味硬菜 =====
        // 咸鱼饼子=咸鱼+玉米面+油，葱烧海参=海参4+葱+酱油，酸菜海鲜锅=大虾+海参+酸菜+调料
        trades.get(5).add(sell(8, ModItems.XIAN_YU_BING_ZI.get(), 1, 12, 30));         // 咸鱼饼子：8 绿宝石 1 份
        trades.get(5).add(sell(18, ModItems.CONG_SHAO_HAI_SHEN.get(), 1, 8, 30));      // 葱烧海参：18 绿宝石 1 份
        trades.get(5).add(sell(15, ModItems.SUAN_CAI_HAI_XIAN_GUO.get(), 1, 8, 30));   // 酸菜海鲜锅：15 绿宝石 1 份
    }
}
