package com.gunmu.dongbei_delight.villager;

import com.gunmu.dongbei_delight.item.ModItems;
import com.gunmu.dongbei_delight.item.SeasoningBottleItem;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.BasicItemListing;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

import java.util.List;

import static com.gunmu.dongbei_delight.villager.TradeHelpers.buy;
import static com.gunmu.dongbei_delight.villager.TradeHelpers.sell;

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
        trades.get(1).add(sell(1, ModItems.SALT.get(), 4, 16, 2));                 // 盐 4 个换 1
        trades.get(1).add(sell(1, Items.SUGAR, 6, 16, 2));                          // 糖 6 个换 1
        trades.get(1).add(sell(1, ModItems.TOFU.get(), 3, 16, 2));                  // 豆腐 3 个换 1
        trades.get(1).add(sellWithUses(ModItems.SOY_SAUCE.get(), 1, 1, 16, 2));     // 酱油（只剩 1 次的量）
        trades.get(1).add(buy(ModItems.SOYBEAN.get(), 20, 16, 2));                  // 收大豆 20 换 1

        // ===== 2 级：学徒 =====
        trades.get(2).add(sell(1, ModItems.DRIED_TOFU.get(), 3, 16, 5));            // 干豆腐 3 个换 1
        trades.get(2).add(sell(1, ModItems.VERMICELLI.get(), 2, 16, 5));            // 粉条 2 个换 1
        trades.get(2).add(sell(1, ModItems.SOY_PASTE_CHUNK.get(), 2, 16, 5));       // 大酱块 2 个换 1
        trades.get(2).add(coldOnly(sellWithUses(ModItems.SOY_PASTE.get(), 2, 1, 12, 5))); // 大酱（寒冷群系，剩 2 次）
        trades.get(2).add(buy(ModItems.NAPA_CABBAGE.get(), 16, 16, 5));             // 收大白菜 16 换 1

        // ===== 3 级：行家（酱料区） =====
        trades.get(3).add(sellWithUses(ModItems.VINEGAR.get(), 2, 1, 12, 10));      // 醋（剩 2 次）
        trades.get(3).add(sellWithUses(ModItems.WHITE_VINEGAR.get(), 2, 1, 12, 10));// 白醋（剩 2 次）
        trades.get(3).add(sellWithUses(ModItems.CHILI_SAUCE.get(), 2, 1, 12, 10));  // 辣椒酱（剩 2 次）
        trades.get(3).add(coldOnly(sell(2, ModItems.PICKLED_GREEN_RADISH.get(), 2, 12, 10))); // 腌青萝卜
        trades.get(3).add(coldOnly(sell(2, ModItems.SOUR_CABBAGE.get(), 2, 12, 10)));         // 酸菜
        trades.get(3).add(coldOnly(sell(2, ModItems.SPICY_CABBAGE.get(), 2, 12, 10)));        // 辣白菜
        trades.get(3).add(buy(ModItems.RED_CHILI.get(), 16, 16, 10));               // 收红辣椒 16 换 1

        // ===== 4 级：老掌柜（整瓶调料 + 加工品） =====
        trades.get(4).add(sellFull(ModItems.SOY_SAUCE.get(), 2, 12, 15));           // 整瓶酱油 2 换 1
        trades.get(4).add(sellFull(ModItems.CHILI_OIL.get(), 2, 12, 15));           // 整瓶辣椒油 2 换 1
        trades.get(4).add(sellFull(ModItems.FISH_SAUCE.get(), 2, 12, 15));          // 整瓶鱼露 2 换 1
        trades.get(4).add(sell(1, ModItems.LA_PI.get(), 2, 12, 15));                // 拉皮 2 个换 1
        trades.get(4).add(sell(1, ModItems.COLD_NOODLE_SHEET.get(), 2, 12, 15));    // 冷面片 2 个换 1
        trades.get(4).add(buy(ModItems.CORN.get(), 16, 16, 15));                    // 收玉米 16 换 1

        // ===== 5 级：大掌柜 =====
        trades.get(5).add(sellFull(ModItems.SHRIMP_PASTE.get(), 2, 12, 30));        // 整瓶虾酱 2 换 1
        trades.get(5).add(sellFull(ModItems.ANIMAL_OIL.get(), 2, 12, 30));          // 整瓶动物油 2 换 1
        trades.get(5).add(sell(1, ModItems.SWEET_POTATO_STARCH.get(), 4, 12, 30));  // 淀粉 4 个换 1
        trades.get(5).add(sell(1, ModItems.ROASTED_PEANUTS.get(), 3, 12, 30));      // 熟花生米 3 个换 1
        trades.get(5).add(sell(1, ModItems.BEAN_SPROUTS.get(), 4, 12, 30));         // 豆芽 4 个换 1
        trades.get(5).add(sell(1, ModItems.BUCKWHEAT_COLD_NOODLES.get(), 1, 8, 30));// 荞麦冷面 1 碗换 1
        trades.get(5).add(sell(1, ModItems.CANDIED_PEANUTS.get(), 2, 12, 30));      // 糖花生
        trades.get(5).add(sell(1, ModItems.NIAN_DOU_BAO.get(), 2, 12, 30));         // 粘豆包
        trades.get(5).add(sell(1, ModItems.SOY_MILK.get(), 2, 12, 30));             // 豆浆
        trades.get(5).add(sell(1, ModItems.KVASS.get(), 2, 12, 30));                // 格瓦斯
        trades.get(5).add(buy(ModItems.PEANUT.get(), 20, 16, 30));                  // 收花生 20 换 1
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
