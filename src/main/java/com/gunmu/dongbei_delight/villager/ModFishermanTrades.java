package com.gunmu.dongbei_delight.villager;

import com.gunmu.dongbei_delight.item.ModItems;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

import java.util.List;

import static com.gunmu.dongbei_delight.villager.TradeHelpers.buy;
import static com.gunmu.dongbei_delight.villager.TradeHelpers.sell;

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
        trades.get(2).add(sell(1, ModItems.SHRIMP.get(), 4, 16, 5));       // 大虾 4 个换 1

        // ===== 原版 3 级：收咸鱼、卖生蚝 =====
        trades.get(3).add(buy(ModItems.SALTED_FISH.get(), 4, 16, 10));     // 咸鱼 4 换 1
        trades.get(3).add(sell(1, ModItems.OYSTER.get(), 4, 16, 10));      // 生蚝

        // ===== 原版 4 级：收海参、卖带鱼 =====
        trades.get(4).add(buy(ModItems.SEA_CUCUMBER.get(), 3, 16, 15));    // 海参 3 换 1
        trades.get(4).add(sell(1, ModItems.HAIRTAIL.get(), 3, 12, 15));    // 带鱼

        // ===== 原版 5 级（大师）：卖海味硬菜 =====
        trades.get(5).add(sell(1, ModItems.XIAN_YU_BING_ZI.get(), 2, 12, 30));         // 咸鱼饼子
        trades.get(5).add(sell(1, ModItems.CONG_SHAO_HAI_SHEN.get(), 1, 8, 30));       // 葱烧海参
        trades.get(5).add(sell(1, ModItems.SUAN_CAI_HAI_XIAN_GUO.get(), 1, 8, 30));    // 酸菜海鲜锅
    }
}
