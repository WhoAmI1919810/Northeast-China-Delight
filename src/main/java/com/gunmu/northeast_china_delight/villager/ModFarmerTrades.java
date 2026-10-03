package com.gunmu.northeast_china_delight.villager;

import com.gunmu.northeast_china_delight.block.ModBlocks;
import com.gunmu.northeast_china_delight.item.ModItems;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

import java.util.List;

import static com.gunmu.northeast_china_delight.villager.TradeHelpers.buy;
import static com.gunmu.northeast_china_delight.villager.TradeHelpers.buyBlock;
import static com.gunmu.northeast_china_delight.villager.TradeHelpers.sell;
import static com.gunmu.northeast_china_delight.villager.TradeHelpers.sellBlock;

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
        // 冻梨要冰雪环境，茄子/青椒/红辣椒都是现摘鲜菜，卖 3~4 绿宝石 1 个
        trades.get(2).add(sell(4, ModItems.FROZEN_PEAR.get(), 1, 16, 5));   // 冻梨：4 绿宝石 1 个
        trades.get(2).add(sell(3, ModItems.EGGPLANT.get(), 1, 16, 5));      // 茄子：3 绿宝石 1 个
        trades.get(2).add(sell(3, ModItems.GREEN_PEPPER.get(), 1, 16, 5));  // 青椒：3 绿宝石 1 个
        trades.get(2).add(sell(4, ModItems.RED_CHILI.get(), 1, 16, 5));     // 红辣椒：4 绿宝石 1 个

        // ===== 原版 3 级（老手）：玉米 4 绿宝石 1 个、收玉米茎秆 =====
        trades.get(3).add(sell(4, ModItems.CORN.get(), 1, 16, 10));         // 玉米：4 绿宝石 1 根
        trades.get(3).add(buy(ModItems.CORN_STALK.get(), 18, 16, 10));      // 收玉米茎秆 18 换 1

        // ===== 原版 4 级（专家）：袋装杂粮一进一出 =====
        // 袋装是压缩存储（9 份散货一袋），卖 12~14 绿宝石 1 袋
        trades.get(4).add(sellBlock(12, ModBlocks.SACKS.get("buckwheat_sack").get(), 1, 12, 15));   // 荞麦袋：12 绿宝石
        trades.get(4).add(sellBlock(14, ModBlocks.SACKS.get("red_bean_sack").get(), 1, 12, 15));    // 红豆袋：14 绿宝石
        trades.get(4).add(buyBlock(ModBlocks.SACKS.get("corn_seeds_sack").get(), 1, 12, 15));       // 收玉米种子袋 1 换 1

        // ===== 原版 5 级（大师）：收山货、卖熟食 =====
        trades.get(5).add(buyBlock(ModBlocks.CRATES.get("sweet_potato_crate").get(), 1, 12, 30));
        trades.get(5).add(buyBlock(ModBlocks.SACKS.get("soybean_sack").get(), 1, 12, 30));
        trades.get(5).add(buyBlock(ModBlocks.SACKS.get("peanut_sack").get(), 1, 12, 30));
        // 熟食按食材总价略低定价：烤玉米=玉米4+糖3≈7 卖 6；烤地瓜=地瓜+火≈4 卖 3；
        // 地三鲜=茄子3+青椒3+土豆≈7 卖 6；蘸酱菜=青菜+大酱≈5 卖 4；老虎菜=青椒+葱+香菜≈6 卖 5
        trades.get(5).add(sell(6, ModItems.GRILLED_CORN.get(), 1, 12, 30));          // 烤玉米：6 绿宝石
        trades.get(5).add(sell(3, ModItems.BAKED_SWEET_POTATO.get(), 1, 12, 30));    // 烤地瓜：3 绿宝石
        trades.get(5).add(sell(6, ModItems.DI_SAN_XIAN.get(), 1, 12, 30));           // 地三鲜：6 绿宝石
        trades.get(5).add(sell(4, ModItems.ZHAN_JIANG_CAI.get(), 1, 12, 30));        // 蘸酱菜：4 绿宝石
        trades.get(5).add(sell(5, ModItems.TIGER_SALAD.get(), 1, 12, 30));           // 老虎菜：5 绿宝石
        trades.get(5).add(sell(10, ModItems.DA_FENG_SHOU_BOWL.get(), 1, 8, 30));     // 大丰收：10 绿宝石 1 碗
    }
}
