package com.gunmu.dongbei_delight.villager;

import com.gunmu.dongbei_delight.item.ModItems;
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

/**
 * 副食商的交易表（兜底版本，数值随时可以改）。
 *
 * 分工照二阶段计划走：
 *  - 卖调料：盐、大酱、酱油、醋、白醋、辣椒油、鱼露、虾酱、动物油
 *  - 卖食材：豆腐、干豆腐、粉条、拉皮、冷面片、大酱块
 *  - 收作物：大豆、大白菜、红辣椒、玉米、花生（换绿宝石）
 *
 * 每一级都混着「他卖给你」和「你卖给他」，这样村民升级路线不会只有一头。
 * 改数值就在这张表里改，不用碰职业注册。
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

        // ===== 1 级：新手货郎 =====
        trades.get(1).add(sell(1, ModItems.SALT.get(), 4, 16, 2));
        trades.get(1).add(sell(1, ModItems.TOFU.get(), 3, 12, 2));
        trades.get(1).add(buy(ModItems.SOYBEAN.get(), 20, 16, 2));

        // ===== 2 级：学徒 =====
        trades.get(2).add(sell(1, ModItems.DRIED_TOFU.get(), 3, 12, 5));
        trades.get(2).add(sell(1, ModItems.VERMICELLI.get(), 2, 12, 5));
        trades.get(2).add(sell(1, ModItems.SOY_PASTE_CHUNK.get(), 2, 12, 5));
        trades.get(2).add(buy(ModItems.NAPA_CABBAGE.get(), 16, 16, 5));

        // ===== 3 级：行家 =====
        trades.get(3).add(sell(2, ModItems.WHITE_VINEGAR.get(), 1, 12, 10));
        trades.get(3).add(sell(2, ModItems.VINEGAR.get(), 1, 12, 10));
        trades.get(3).add(buy(ModItems.RED_CHILI.get(), 16, 16, 10));

        // ===== 寒冷群系（雪原 / 针叶林）村庄的专属货 =====
        // 辣白菜、酸菜、腌青萝卜、大酱：只有寒冷群系的副食商才卖，别处的村民不进货。
        trades.get(2).add(coldOnly(sell(2, ModItems.PICKLED_GREEN_RADISH.get(), 2, 12, 5)));
        trades.get(3).add(coldOnly(sell(2, ModItems.SOUR_CABBAGE.get(), 2, 12, 10)));
        trades.get(3).add(coldOnly(sell(2, ModItems.SPICY_CABBAGE.get(), 2, 12, 10)));
        trades.get(3).add(coldOnly(sell(2, ModItems.SOY_PASTE.get(), 1, 12, 10)));

        // ===== 4 级：老掌柜 =====
        trades.get(4).add(sell(2, ModItems.SOY_SAUCE.get(), 1, 12, 15));
        trades.get(4).add(sell(3, ModItems.CHILI_OIL.get(), 1, 12, 15));
        trades.get(4).add(sell(3, ModItems.FISH_SAUCE.get(), 1, 12, 15));
        trades.get(4).add(buy(ModItems.CORN.get(), 16, 16, 15));

        // ===== 5 级：大掌柜 =====
        trades.get(5).add(sell(3, ModItems.SHRIMP_PASTE.get(), 1, 12, 20));
        trades.get(5).add(sell(4, ModItems.ANIMAL_OIL.get(), 1, 12, 20));
        trades.get(5).add(sell(2, ModItems.LA_PI.get(), 2, 12, 20));
        trades.get(5).add(sell(2, ModItems.COLD_NOODLE_SHEET.get(), 2, 12, 20));
        trades.get(5).add(buy(ModItems.PEANUT.get(), 20, 16, 20));
    }

    /** 卖：给村民绿宝石，他把东西给你 */
    private static VillagerTrades.ItemListing sell(int emeralds, Item item, int count, int maxUses, int xp)
    {
        return new BasicItemListing(emeralds, new ItemStack(item, count), maxUses, xp);
    }

    /**
     * 只在寒冷群系村庄出现的交易。
     *
     * <p>村民的「群系类型」决定他卖不卖这几样：{@link VillagerType#SNOW}（雪原）和
     * {@link VillagerType#TAIGA}（针叶林）才供货，其他群系的副食商返回空报价，
     * 游戏会把这个交易条目跳过（原版 {@code EmeraldsForVillagerTypeItem} 也是这么区分群系的）。
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

    /** 收：他把你的东西收走，给你绿宝石 */
    private static VillagerTrades.ItemListing buy(Item item, int count, int maxUses, int xp)
    {
        return new BasicItemListing(new ItemStack(item, count), new ItemStack(Items.EMERALD), maxUses, xp, 0.05F);
    }
}
