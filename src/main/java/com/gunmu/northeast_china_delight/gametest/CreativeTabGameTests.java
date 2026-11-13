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
    public static void regionalModUsesOneTabAndKeepsSeaWaterBucketHidden(GameTestHelper helper) {
        List<Item> creative = ModItems.CREATIVE_TAB_ITEMS.stream().map(holder -> (Item) holder.get()).toList();
        Set<Item> unique = new HashSet<>(creative);

        helper.assertTrue(unique.size() == creative.size(), "东北乐事创造栏不得出现重复物品");
        helper.assertTrue(creative.contains(ModItems.NEW_STYLE_GUO_BAO_ROU.get()), "菜肴必须并入主创造栏");
        helper.assertTrue(creative.contains(ModItems.PAPER_CUT.get()), "方块和建材必须并入主创造栏");
        helper.assertTrue(creative.contains(ModItems.MUSIC_DISC_NORTHEAST.get()), "唱片必须并入主创造栏");
        helper.assertFalse(creative.contains(ModItems.SEA_WATER_BUCKET.get()), "海水桶必须保持隐藏");
        helper.assertTrue(ModItems.HIDDEN_CREATIVE_ITEMS.contains(ModItems.SEA_WATER_BUCKET),
                "海水桶必须登记在隐藏集合中");
        helper.succeed();
    }
}
