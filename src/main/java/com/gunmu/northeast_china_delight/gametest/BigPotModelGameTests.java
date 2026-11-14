package com.gunmu.northeast_china_delight.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.util.DdIds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.neoforged.fml.ModList;
//? if <1.20.2 {
/*import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** 防止拆分时再次出现“配方在、模型还在旧命名空间”的静默退化。 */
@GameTestHolder(NortheastChinaDelight.MODID)
@PrefixGameTestTemplate(false)
public final class BigPotModelGameTests {

    private BigPotModelGameTests() {
    }

    @GameTest(template = "empty")
    public static void everyBigPotRecipeHasDishModelInItemNamespace(GameTestHelper helper) {
        var resourceManager = helper.getLevel().getServer().getResourceManager();
        int recipes = 0;
        for (String directory : List.of("recipe/big_pot_cooking", "recipes/big_pot_cooking")) {
            for (Map.Entry<ResourceLocation, Resource> entry : resourceManager.listResources(directory,
                    path -> path.getPath().endsWith(".json")).entrySet()) {
                JsonObject root = read(helper, entry.getKey(), entry.getValue());
                if (root == null) {
                    return;
                }
                JsonObject config = root.getAsJsonObject("config");
                if (config == null || config.has("disabled") && config.get("disabled").getAsBoolean()) {
                    continue;
                }
                JsonObject result = config.getAsJsonObject("result");
                String itemId = result.get("item").getAsString();
                ResourceLocation item = DdIds.parse(itemId);
                String expected = "assets/" + item.getNamespace()
                        + "/models/block/big_pot/dish/" + item.getPath() + ".json";
                if (!BuiltInRegistries.ITEM.containsKey(item) || !hasDishModel(item)) {
                    helper.fail("大锅配方成品没有同命名空间的锅里模型：" + entry.getKey()
                            + " -> " + itemId + "，期望 " + expected);
                    return;
                }
                recipes++;
            }
        }
        if (recipes == 0) {
            helper.fail("没有找到任何大锅烹饪配方，模型一致性检查失效");
            return;
        }

        ResourceLocation mysteryStew = DdIds.of("chinese_regional_culinary_ingredients", "mystery_stew");
        if (!BuiltInRegistries.ITEM.containsKey(mysteryStew) || !hasDishModel(mysteryStew)) {
            helper.fail("盆装谜之炖菜已迁入 CRCI，但锅里模型没有跟着迁移：" + mysteryStew);
            return;
        }
        helper.succeed();
    }

    private static boolean hasDishModel(ResourceLocation item) {
        var modFileInfo = ModList.get().getModFileById(item.getNamespace());
        if (modFileInfo == null) {
            return false;
        }
        Path model = modFileInfo.getFile().findResource("assets", item.getNamespace(),
                "models", "block", "big_pot", "dish", item.getPath() + ".json");
        return model != null && Files.isRegularFile(model);
    }

    private static JsonObject read(GameTestHelper helper, ResourceLocation id, Resource resource) {
        try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException | RuntimeException exception) {
            helper.fail("读取大锅配方失败：" + id + "：" + exception.getMessage());
            return null;
        }
    }
}
