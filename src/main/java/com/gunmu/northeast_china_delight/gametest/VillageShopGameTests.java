package com.gunmu.northeast_china_delight.gametest;

import com.gunmu.chinese_regional_culinary_ingredients.worldgen.UniqueShopPoolElement;
import com.gunmu.chinese_regional_culinary_ingredients.trade.CrciVillagerProfessions;
import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraftforge.fml.ModList;
//? if <1.20.2 {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} else {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

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

    @GameTest(template = "empty")
    public static void createShopStructuresOverrideBasePack(GameTestHelper helper) {
        if (!ModList.get().isLoaded("create")) {
            helper.succeed();
            return;
        }
        for (String biome : BIOMES) {
            ResourceLocation id = id("chinese_regional_culinary_ingredients",
                    "village/side_dish_shop_" + biome);
            StructureTemplate template = helper.getLevel().getStructureManager().get(id).orElse(null);
            helper.assertTrue(template != null, "缺少副食店结构 " + id);
            CompoundTag saved = template.save(new CompoundTag());
            ListTag palette = saved.getList("palette", Tag.TAG_COMPOUND);
            boolean hasCreateBlock = false;
            for (int index = 0; index < palette.size(); index++) {
                if (palette.getCompound(index).getString("Name").startsWith("create:")) {
                    hasCreateBlock = true;
                    break;
                }
            }
            helper.assertTrue(hasCreateBlock, "Create 版副食店资源包未覆盖结构: " + id);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void sideDishJobSiteIsAcquirableWithRegionalProfile(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().registryOrThrow(Registries.POINT_OF_INTEREST_TYPE);
        TagKey<PoiType> tag = TagKey.create(Registries.POINT_OF_INTEREST_TYPE,
                id("minecraft", "acquirable_job_site"));
        boolean acquirable = registry.getTag(tag)
                .map(values -> values.stream().anyMatch(holder ->
                        holder.value() == CrciVillagerProfessions.SIDE_DISH_POI.get()))
                .orElse(false);
        helper.assertTrue(acquirable, "地区档案启用副食店时，副食商工作站必须可被村民认领");
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
        /*return ResourceLocation.fromNamespaceAndPath(namespace, path);
        *///?} else {
        return new ResourceLocation(namespace, path);
        //?}
    }
}
