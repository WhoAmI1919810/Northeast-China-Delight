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

/**
 * 副食商的交易表：调料单卖、食材买卖、成品加价卖。
 *
 * <p>低等级（1~3 级）的瓶装调料只装能用 1~2 次的那点量（拿在手里就是掉过耐久的瓶子），
 * 高等级（4~5 级）才卖整瓶 250 mB。所有「收」的价格都盯住比同物品「卖」价低，
 * 免得玩家来回倒腾刷绿宝石。</p>
 */
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

        // ===== 1 级：新手小货郎 =====
        // 基础食材按「n 绿宝石 → 1 个」定价，比你自己种地便宜一点
        trades.get(1).add(sell(2, ModItems.SALT.get(), 1, 16, 2));                  // 盐：2 绿宝石 1 个
        trades.get(1).add(sell(3, Items.SUGAR, 1, 16, 2));                          // 糖：3 绿宝石 1 个
        trades.get(1).add(sell(4, ModItems.TOFU.get(), 1, 16, 2));                  // 豆腐：4 绿宝石 1 块（=2 大豆+水）
        trades.get(1).add(sellWithUses(ModItems.SOY_SAUCE.get(), 1, 3, 16, 2));     // 酱油（剩 1 次）：3 绿宝石
        trades.get(1).add(buy(ModItems.SOYBEAN.get(), 24, 16, 2));                  // 收大豆 24 换 1

        // ===== 2 级：学徒 =====
        trades.get(2).add(sell(6, ModItems.DRIED_TOFU.get(), 1, 16, 5));            // 干豆腐：6 绿宝石 1 个（≈豆浆+风干）
        trades.get(2).add(sell(8, ModItems.VERMICELLI.get(), 1, 16, 5));            // 粉条：8 绿宝石 1 个（淀粉+水）
        trades.get(2).add(sell(5, ModItems.SOY_PASTE_CHUNK.get(), 1, 16, 5));       // 大酱块：5 绿宝石 1 个（黄豆+盐+时间）
        trades.get(2).add(coldOnly(sellWithUses(ModItems.SOY_PASTE.get(), 2, 4, 12, 5))); // 大酱（剩 2 次）：4 绿宝石
        trades.get(2).add(buy(ModItems.NAPA_CABBAGE.get(), 18, 16, 5));             // 收大白菜 18 换 1

        // ===== 3 级：行家（酱料区） =====
        trades.get(3).add(sellWithUses(ModItems.VINEGAR.get(), 2, 4, 12, 10));      // 醋（剩 2 次）：4 绿宝石
        trades.get(3).add(sellWithUses(ModItems.WHITE_VINEGAR.get(), 2, 4, 12, 10));// 白醋（剩 2 次）：4 绿宝石
        trades.get(3).add(sellWithUses(ModItems.CHILI_SAUCE.get(), 2, 5, 12, 10));  // 辣椒酱（剩 2 次）：5 绿宝石
        // 酸菜 = 大白菜 + 盐 + 时间 → 食材总价 1+2+人工 ≈ 6~7，卖 5
        trades.get(3).add(coldOnly(sell(5, ModItems.PICKLED_GREEN_RADISH.get(), 1, 12, 10))); // 腌青萝卜：5 绿宝石
        trades.get(3).add(coldOnly(sell(6, ModItems.SOUR_CABBAGE.get(), 1, 12, 10)));         // 酸菜：6 绿宝石
        // 辣白菜 = 大白菜 + 盐 + 辣椒酱 + 鱼露 → 食材总价 1+2+5+3 ≈ 11，卖 9
        trades.get(3).add(coldOnly(sell(9, ModItems.SPICY_CABBAGE.get(), 1, 12, 10)));        // 辣白菜：9 绿宝石
        trades.get(3).add(buy(ModItems.RED_CHILI.get(), 18, 16, 10));               // 收红辣椒 18 换 1

        // ===== 4 级：老掌柜（整瓶调料 + 加工品） =====
        trades.get(4).add(sellFull(ModItems.SOY_SAUCE.get(), 8, 12, 15));           // 整瓶酱油 250mB：8 绿宝石
        trades.get(4).add(sellFull(ModItems.CHILI_OIL.get(), 10, 12, 15));          // 整瓶辣椒油 250mB：10 绿宝石
        trades.get(4).add(sellFull(ModItems.FISH_SAUCE.get(), 9, 12, 15));          // 整瓶鱼露 250mB：9 绿宝石
        // 拉皮 = 淀粉 + 水 + 时间，冷面片 = 面粉 + 鸡蛋 → 食材总价高，卖 12/10
        trades.get(4).add(sell(12, ModItems.LA_PI.get(), 1, 12, 15));               // 拉皮：12 绿宝石 1 个
        trades.get(4).add(sell(10, ModItems.COLD_NOODLE_SHEET.get(), 1, 12, 15));   // 冷面片：10 绿宝石 1 张
        trades.get(4).add(buy(ModItems.CORN.get(), 20, 16, 15));                    // 收玉米 20 换 1

        // ===== 5 级：大掌柜 =====
        trades.get(5).add(sellFull(ModItems.SHRIMP_PASTE.get(), 12, 12, 30));       // 整瓶虾酱 250mB：12 绿宝石
        trades.get(5).add(sellFull(ModItems.ANIMAL_OIL.get(), 10, 12, 30));         // 整瓶动物油 250mB：10 绿宝石
        trades.get(5).add(sell(6, ModItems.SWEET_POTATO_STARCH.get(), 1, 12, 30));  // 淀粉：6 绿宝石 1 个（地瓜磨的）
        trades.get(5).add(sell(4, ModItems.ROASTED_PEANUTS.get(), 1, 12, 30));      // 熟花生米：4 绿宝石 1 个
        trades.get(5).add(sell(5, ModItems.BEAN_SPROUTS.get(), 1, 12, 30));         // 豆芽：5 绿宝石 1 个（豆子泡的）
        // 荞麦冷面 = 荞麦 + 水 + 鸡蛋，粘豆包 = 玉米面 + 红豆 + 糖 → 食材总价高
        trades.get(5).add(sell(14, ModItems.BUCKWHEAT_COLD_NOODLES.get(), 1, 8, 30));// 荞麦冷面：14 绿宝石 1 碗
        trades.get(5).add(sell(7, ModItems.CANDIED_PEANUTS.get(), 1, 12, 30));      // 糖花生：7 绿宝石 1 个（花生+糖）
        trades.get(5).add(sell(9, ModItems.NIAN_DOU_BAO.get(), 1, 12, 30));         // 粘豆包：9 绿宝石 1 个（玉米面+红豆+糖）
        trades.get(5).add(sell(6, ModItems.SOY_MILK.get(), 1, 12, 30));             // 豆浆：6 绿宝石 1 碗（8 黄豆）
        trades.get(5).add(sell(8, ModItems.KVASS.get(), 1, 12, 30));                // 格瓦斯：8 绿宝石 1 瓶（面包+水+时间）
        trades.get(5).add(buy(ModItems.PEANUT.get(), 22, 16, 30));                  // 收花生 22 换 1
    }

    /**
     * 低等级调料：卖出去的是「已经用掉一部分」的瓶子，只剩 1~2 次用量。
     * 耐久条 = 已经用掉的 mB，所以把剩余量倒着算回去就行。
     */
    private static VillagerTrades.ItemListing sellWithUses(Item item, int uses, int emeralds, int maxUses, int xp)
    {
        ItemStack stack = new ItemStack(item);
        stack.setDamageValue(SeasoningBottleItem.CAPACITY_MB - uses * SeasoningBottleItem.DOSE_MB);
        return new BasicItemListing(emeralds, stack, maxUses, xp);
    }

    /** 高等级调料：整瓶 250 mB。 */
    private static VillagerTrades.ItemListing sellFull(Item item, int price, int maxUses, int xp)
    {
        return new BasicItemListing(price, new ItemStack(item), maxUses, xp);
    }

    /**
     * 只在寒冷群系村庄出现的交易。
     *
     * <p>村民的群系类型决定供不供货：雪原（{@link VillagerType#SNOW}）和针叶林
     * （{@link VillagerType#TAIGA}）才摆这几样，别处的副食商返回空报价，游戏会跳过这条。</p>
     */
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
