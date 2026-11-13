package com.gunmu.northeast_china_delight.gametest;

import com.gunmu.northeast_china_delight.item.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
//? if <1.20.2 {
/*import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.gunmu.northeast_china_delight.NortheastChinaDelight.MODID;

@GameTestHolder(MODID)
@PrefixGameTestTemplate(false)
public final class CreativeTabGameTests {
    private CreativeTabGameTests() {
    }

    @GameTest(template = "empty")
    public static void oldStyleKeepsThreeTabsAndHiddenItemsStayOut(GameTestHelper helper) {
        List<Item> ingredients = ModItems.INGREDIENT_TAB_ITEMS.stream()
                .map(holder -> (Item) holder.get()).toList();
        List<Item> dishes = ModItems.DISH_TAB_ITEMS.stream()
                .map(holder -> (Item) holder.get()).toList();
        List<Item> blocks = ModItems.BLOCK_TAB_ITEMS.stream()
                .map(holder -> (Item) holder.get()).toList();
        List<Item> all = new ArrayList<>();
        all.addAll(ingredients);
        all.addAll(dishes);
        all.addAll(blocks);
        Set<Item> unique = new HashSet<>(all);

        helper.assertFalse(ingredients.isEmpty(), "食材与杂项物品栏不能为空");
        helper.assertFalse(dishes.isEmpty(), "菜肴物品栏不能为空");
        helper.assertFalse(blocks.isEmpty(), "方块与建材物品栏不能为空");
        helper.assertTrue(unique.size() == all.size(), "三个创造栏之间不得出现重复物品");
        helper.assertTrue(ingredients.contains(ModItems.SOYBEAN.get()), "大豆应在食材与杂项栏");
        helper.assertTrue(ingredients.contains(ModItems.MUSIC_DISC_NORTHEAST.get()), "唱片应在食材与杂项栏");
        helper.assertTrue(dishes.contains(ModItems.NEW_STYLE_GUO_BAO_ROU.get()), "锅包肉应在菜肴栏");
        helper.assertTrue(dishes.contains(ModItems.MYSTERY_STEW.get()), "盆装谜之炖菜应在菜肴栏");
        helper.assertTrue(blocks.contains(ModItems.PAPER_CUT.get()), "窗花应在方块与建材栏");
        helper.assertFalse(all.contains(ModItems.SEA_WATER_BUCKET.get()), "海水桶必须保持隐藏");
        helper.assertFalse(all.contains(ModItems.CREATIVE_VAT.get()), "创造大缸必须保持隐藏");
        helper.assertFalse(all.contains(ModItems.SOUL_CABBAGE.get()), "灵魂酸菜必须保持隐藏");
        helper.assertTrue(ModItems.HIDDEN_CREATIVE_ITEMS.contains(ModItems.SEA_WATER_BUCKET),
                "海水桶必须登记在隐藏集合中");
        helper.assertTrue(ModItems.HIDDEN_CREATIVE_ITEMS.contains(ModItems.CREATIVE_VAT),
                "创造大缸必须登记在隐藏集合中");
        helper.assertTrue(ModItems.HIDDEN_CREATIVE_ITEMS.contains(ModItems.SOUL_CABBAGE),
                "灵魂酸菜必须登记在隐藏集合中");
        helper.succeed();
    }
}
