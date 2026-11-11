package com.gunmu.northeast_china_delight.gametest;

import com.gunmu.chinese_regional_culinary_ingredients.worldgen.UniqueShopPoolElement;
import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
//? if <1.20.2 {
/*import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder(NortheastChinaDelight.MODID)
@PrefixGameTestTemplate(false)
public final class VillageShopGameTests {
    private static final String[] BIOMES = {"plains", "desert", "savanna", "snowy", "taiga"};

    private VillageShopGameTests() {
    }

    @GameTest(template = "empty")
    public static void villageShopPoolsUseTheUniqueElement(GameTestHelper helper) {
        var pools = helper.getLevel().registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
        for (String biome : BIOMES) {
            assertUniqueShop(helper, pools, "village/" + biome + "/houses");
            assertUniqueShop(helper, pools, "village/" + biome + "/zombie/houses");
        }
        helper.succeed();
    }

    private static void assertUniqueShop(GameTestHelper helper,
                                         net.minecraft.core.Registry<StructureTemplatePool> pools,
                                         String path) {
        ResourceLocation id = id("minecraft", path);
        StructureTemplatePool pool = pools.get(id);
        helper.assertTrue(pool != null, "缺少村庄模板池 " + id);
        boolean found = pool.getShuffledTemplates(RandomSource.create(0)).stream()
                .anyMatch(element -> element instanceof UniqueShopPoolElement);
        helper.assertTrue(found, "模板池未加载 CRCI 唯一副食店元素: " + id);
    }

    private static ResourceLocation id(String namespace, String path) {
        //? if >=1.20.5 {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
        //?} else {
        /*return new ResourceLocation(namespace, path);
        *///?}
    }
}
