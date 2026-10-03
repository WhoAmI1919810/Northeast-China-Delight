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

/**
 * 屠夫的交易表。
 *
 * <p>需求给了 7 档，原版只有 5 级，所以：
 * 1/2/3 级各占原版 1/2/3 级，需求 4、5 级并进原版 4 级，
 * 需求 6、7 级并进原版 5 级（大师）。</p>
 */
public final class ModButcherTrades
{
    private ModButcherTrades()
    {
    }

    public static void addTrades(VillagerTradesEvent event)
    {
        if (event.getType() != VillagerProfession.BUTCHER)
        {
            return;
        }
        Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();

        // ===== 原版 1 级：收购下水 =====
        trades.get(1).add(buy(ModItems.PIG_LIVER.get(), 6, 16, 2));        // 猪肝 6 换 1
        trades.get(1).add(buy(ModItems.PORK_HOCK.get(), 4, 16, 2));        // 猪肘 4 换 1

        // ===== 原版 2 级：出售鲜肉 =====
        // 排骨/油边是正经猪肉部位，比生猪排贵
        trades.get(2).add(sell(6, ModItems.PORK_RIBS.get(), 1, 16, 5));    // 排骨：6 绿宝石 1 份
        trades.get(2).add(sell(5, ModItems.OIL_EDGE.get(), 1, 16, 5));     // 油边：5 绿宝石 1 份

        // ===== 原版 3 级：收购灌肠原料 =====
        trades.get(3).add(buy(ModItems.PORK_INTESTINE.get(), 10, 16, 10)); // 收猪肠 10 换 1
        trades.get(3).add(buy(ModItems.PIG_BLOOD.get(), 12, 16, 10));      // 收猪血 12 换 1

        // ===== 原版 4 级：烤货和灌肠 =====
        // 烤鸡架=鸡架+炭火，烤油边=油边+炭火+调料，灌肠=肠衣+肉/血/米
        trades.get(4).add(sell(8, ModItems.GRILLED_CHICKEN_FRAME.get(), 1, 12, 15));  // 烤鸡架：8 绿宝石 1 份
        trades.get(4).add(sell(9, ModItems.GRILLED_OIL_EDGE.get(), 1, 12, 15));       // 烤油边：9 绿宝石 1 份
        trades.get(4).add(sell(7, ModItems.RED_SAUSAGE.get(), 1, 12, 15));            // 红肠：7 绿宝石 1 根
        trades.get(4).add(sell(6, ModItems.BLOOD_SAUSAGE.get(), 1, 12, 15));          // 血肠：6 绿宝石 1 根
        trades.get(4).add(sell(6, ModItems.RICE_SAUSAGE.get(), 1, 12, 15));           // 米肠：6 绿宝石 1 根

        // ===== 原版 5 级（大师）：收油块、卖硬菜 =====
        trades.get(5).add(buyBlock(ModBlocks.ANIMAL_OIL_BLOCK.get(), 1, 12, 30));     // 收动物油块 1 换 1
        // 硬菜 = 主料 + 配菜 + 调料 + 工时，按食材总价略低定价
        trades.get(5).add(sell(14, ModItems.HONG_SHAO_PAI_GU.get(), 1, 12, 30));         // 红烧排骨：14 绿宝石（排骨6+糖3+酱油5）
        trades.get(5).add(sell(16, ModItems.XIAO_JI_DUN_MO_GU_BOWL.get(), 1, 8, 30));    // 小鸡炖蘑菇：16 绿宝石（鸡腿+榛蘑+粉条+酱油）
        trades.get(5).add(sell(12, ModItems.ZHU_ROU_DUN_FEN_TIAO_BOWL.get(), 1, 8, 30)); // 猪肉炖粉条：12 绿宝石（猪肉+粉条+酸菜）
        trades.get(5).add(sell(13, ModItems.XUN_JIANG_PIN_PAN.get(), 1, 8, 30));         // 熏酱拼盘：13 绿宝石（熏鸡+酱肉+红肠）
        trades.get(5).add(sell(15, ModItems.OLD_STYLE_GUO_BAO_ROU.get(), 1, 8, 30));     // 老派锅包肉：15 绿宝石（里脊+淀粉+糖+醋）
        trades.get(5).add(sell(16, ModItems.NEW_STYLE_GUO_BAO_ROU.get(), 1, 8, 30));     // 新派锅包肉：16 绿宝石（同上+番茄酱）
        trades.get(5).add(sell(17, ModItems.ORANGE_GUO_BAO_ROU.get(), 1, 8, 30));        // 橙汁锅包肉：17 绿宝石（同上+橙汁）
        trades.get(5).add(sell(13, ModItems.LIU_ROU_DUAN.get(), 1, 8, 30));              // 溜肉段：13 绿宝石（里脊+青椒+淀粉）
    }
}
