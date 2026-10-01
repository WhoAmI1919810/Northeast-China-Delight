package com.gunmu.dongbei_delight.villager;

import com.gunmu.dongbei_delight.block.ModBlocks;
import com.gunmu.dongbei_delight.item.ModItems;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

import java.util.List;

import static com.gunmu.dongbei_delight.villager.TradeHelpers.buy;
import static com.gunmu.dongbei_delight.villager.TradeHelpers.buyBlock;
import static com.gunmu.dongbei_delight.villager.TradeHelpers.sell;
import static com.gunmu.dongbei_delight.villager.TradeHelpers.sellBlock;

/**
 * 农民的交易表。
 *
 * <p>原版村民只有 5 级，所以需求里的 8 档 + 大师级这样落位：
 * 需求 1 级 -> 原版 1 级；需求 2 级 -> 原版 2 级；需求 3 级 -> 原版 3 级；
 * 需求 4/5 级 -> 原版 4 级；需求 6/7/8 级和大师 -> 原版 5 级。
 * 每条交易都和同级的原版交易平分出现概率，所以数量不都取一样的整数。</p>
 */
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

        // ===== 原版 1 级（新手）：收购四样本地菜 =====
        trades.get(1).add(buy(ModItems.NAPA_CABBAGE.get(), 16, 16, 2));   // 大白菜 16 换 1
        trades.get(1).add(buy(ModItems.CUCUMBER.get(), 20, 16, 2));       // 黄瓜 20 换 1
        trades.get(1).add(buy(ModItems.GREEN_RADISH.get(), 18, 16, 2));   // 青萝卜 18 换 1
        trades.get(1).add(buy(ModItems.GREEN_ONION.get(), 22, 16, 2));    // 大葱 22 换 1

        // ===== 原版 2 级（学徒）：卖四样鲜货 =====
        trades.get(2).add(sell(1, ModItems.FROZEN_PEAR.get(), 4, 16, 5));  // 冻梨
        trades.get(2).add(sell(1, ModItems.EGGPLANT.get(), 4, 16, 5));     // 茄子
        trades.get(2).add(sell(1, ModItems.GREEN_PEPPER.get(), 4, 16, 5)); // 青椒
        trades.get(2).add(sell(1, ModItems.RED_CHILI.get(), 3, 16, 5));    // 红辣椒

        // ===== 原版 3 级（老手）：玉米 1 换 1、收玉米茎秆 =====
        trades.get(3).add(sell(1, ModItems.CORN.get(), 1, 16, 10));
        trades.get(3).add(buy(ModItems.CORN_STALK.get(), 16, 16, 10));

        // ===== 原版 4 级（专家）：袋装杂粮一进一出 =====
        trades.get(4).add(sellBlock(1, ModBlocks.SACKS.get("buckwheat_sack").get(), 1, 12, 15));
        trades.get(4).add(sellBlock(1, ModBlocks.SACKS.get("red_bean_sack").get(), 1, 12, 15));
        trades.get(4).add(buyBlock(ModBlocks.SACKS.get("corn_seeds_sack").get(), 1, 12, 15));

        // ===== 原版 5 级（大师）：收山货、卖熟食 =====
        trades.get(5).add(buyBlock(ModBlocks.CRATES.get("sweet_potato_crate").get(), 1, 12, 30));
        trades.get(5).add(buyBlock(ModBlocks.SACKS.get("soybean_sack").get(), 1, 12, 30));
        trades.get(5).add(buyBlock(ModBlocks.SACKS.get("peanut_sack").get(), 1, 12, 30));
        trades.get(5).add(sell(1, ModItems.GRILLED_CORN.get(), 1, 12, 30));
        trades.get(5).add(sell(1, ModItems.BAKED_SWEET_POTATO.get(), 1, 12, 30));
        trades.get(5).add(sell(1, ModItems.DI_SAN_XIAN.get(), 1, 12, 30));
        trades.get(5).add(sell(1, ModItems.ZHAN_JIANG_CAI.get(), 1, 12, 30));
        trades.get(5).add(sell(1, ModItems.TIGER_SALAD.get(), 1, 12, 30));
        trades.get(5).add(sell(1, ModItems.DA_FENG_SHOU_BOWL.get(), 1, 8, 30));
    }
}
