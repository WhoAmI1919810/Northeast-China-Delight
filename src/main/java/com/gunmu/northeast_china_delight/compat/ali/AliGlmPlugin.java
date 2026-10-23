package com.gunmu.northeast_china_delight.compat.ali;

import com.gunmu.northeast_china_delight.NortheastChinaConfig;
import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.item.ModItems;
import com.gunmu.northeast_china_delight.loot.ButcherLootModifier;
import com.gunmu.northeast_china_delight.loot.ChestLootConfigModifier;
import com.gunmu.northeast_china_delight.loot.FishermanLootModifier;
import com.gunmu.northeast_china_delight.loot.NortheastFishingLootModifier;
import com.gunmu.northeast_china_delight.loot.PigOffalLootModifier;
import com.gunmu.northeast_china_delight.loot.StructureSeedsLootModifier;
import com.gunmu.northeast_china_delight.loot.VillageSeedsLootModifier;
import com.gunmu.northeast_china_delight.util.DdIds;
import com.gunmu.northeast_china_delight.villager.SideDishMerchantTrades;
import com.yanny.aci.api.NumberExpr;
import com.yanny.ali.api.AliEntrypoint;
import com.yanny.ali.api.IOperation;
import com.yanny.ali.api.IServerUtils;
import com.yanny.ali.api.IServerRegistry;
import com.yanny.ali.api.TradeLevelInfo;
import com.yanny.ali.plugin.glm.GlobalLootModifierUtils;
import com.yanny.ali.plugin.glm.IGlobalLootModifierPlugin;
import com.yanny.ali.plugin.glm.IPageLootModifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * 高级战利品显示（Advanced Loot Info / ALI）兼容插件。
 *
 * <p>本模组的掉落有两半：数据包里的战利品表（作物、板条箱、小院箱子）ALI 本来就会读，
 * 而猪副产物、钓鱼、村庄/结构箱子里的种子这些写在 Java 里的全局掉落修改器，ALI 只能看到
 * 「类型 + 条件」、看不到物品，于是显示不出东西。这个类把「我们到底会掉什么」交给 ALI：
 * 按 ALI 的第三方扩展点（{@code @AliEntrypoint} + {@link IGlobalLootModifierPlugin}）注册每个
 * GLM 的页面。</p>
 *
 * <p><b>纯粹新增，不参与原有掉落逻辑</b>：只读地拿掉落表（反射，见 {@link AliGlmReflection}）和
 * 概率常量（见 {@link AliGlmData}），不调用任何 {@code doApply}，也不改动 GLM 类。
 * 本模组没装 ALI 时这个类不会被加载（ALI 靠注解扫描发现插件），所以 ALI 只是编译期依赖。</p>
 */
@AliEntrypoint
public final class AliGlmPlugin implements IGlobalLootModifierPlugin {
    @Override
    public String getModId() {
        return NortheastChinaDelight.MODID;
    }

    @Override
    public void registerGlobalLootModifier(IGlobalLootModifierPlugin.IRegistry registry) {
        registry.registerGlobalLootModifier(PigOffalLootModifier.class, AliGlmPlugin::pigOffal);
        registry.registerGlobalLootModifier(NortheastFishingLootModifier.class, AliGlmPlugin::fishing);
        registry.registerGlobalLootModifier(ButcherLootModifier.class, AliGlmPlugin::butcher);
        registry.registerGlobalLootModifier(FishermanLootModifier.class, AliGlmPlugin::fisherman);
        registry.registerGlobalLootModifier(StructureSeedsLootModifier.class, AliGlmPlugin::structureSeeds);
        registry.registerGlobalLootModifier(VillageSeedsLootModifier.class, AliGlmPlugin::villageSeeds);
        registry.registerGlobalLootModifier(ChestLootConfigModifier.class, AliGlmPlugin::chestLootConfig);
    }

    /**
     * 副食商是 Java 里动态注册的职业，ALI 无法从原版交易表自动发现；这里把同一份候选表交给 ALI。
     * 1–4 级随机展示 2 条，5 级还会由 Mixin 保底补入黄桃罐头，所以展示 3 条。
     */
    @Override
    public void registerServer(IServerRegistry registry) {
        registry.registerTrades(
                DdIds.of(NortheastChinaDelight.MODID, "side_dish_merchant"),
                EntityType.VILLAGER,
                SideDishMerchantTrades::tradeListingArrays,
                level -> new TradeLevelInfo(NumberExpr.constant(level >= 5 ? 3.0 : 2.0)));
    }

    /** 宰猪：每种副产物基础 10%（Java 里再按抢夺等级 +5%/级、一次最多 2 种），着火时换成熏制版 */
    private static Optional<IPageLootModifier> pigOffal(IServerUtils utils, PigOffalLootModifier modifier) {
        return Optional.of(page(utils, modifier, conditions -> {
            List<IOperation> operations = new ArrayList<>();
            NumberExpr one = AliGlmNodes.count(1, 1);
            float chance = PigOffalLootModifier.BASE_CHANCE;

            for (Item item : AliGlmData.pigOffalItems()) {
                operations.add(AliGlmNodes.add(utils, conditions, item, chance, one));
            }

            for (Item[] pair : AliGlmData.pigOffalSmoked()) {
                operations.add(AliGlmNodes.replace(utils, conditions, pair[0], pair[1], chance, one));
            }

            return operations;
        }));
    }

    /** 钓鱼：三张表（带鱼/生蚝/通用）按水域任选其一，表内权重就是实际占比 */
    private static Optional<IPageLootModifier> fishing(IServerUtils utils, NortheastFishingLootModifier modifier) {
        return Optional.of(page(utils, modifier, conditions ->
                AliGlmNodes.union(utils, conditions, AliGlmReflection.tablesOf(NortheastFishingLootModifier.class),
                        AliGlmNodes.count(1, 1))));
    }

    /** 屠夫箱子：生料 18%、肉菜 10% */
    private static Optional<IPageLootModifier> butcher(IServerUtils utils, ButcherLootModifier modifier) {
        return Optional.of(chestPage(utils, modifier, conditions -> twoTier(utils, conditions, ButcherLootModifier.class,
                AliGlmData.BUTCHER_RAW, 1, 2, AliGlmData.BUTCHER_DISH, 1, 1)));
    }

    /** 渔夫箱子：生鲜 18%、海味菜 10% */
    private static Optional<IPageLootModifier> fisherman(IServerUtils utils, FishermanLootModifier modifier) {
        return Optional.of(chestPage(utils, modifier, conditions -> twoTier(utils, conditions, FishermanLootModifier.class,
                AliGlmData.FISHERMAN_RAW, 1, 2, AliGlmData.FISHERMAN_DISH, 1, 1)));
    }

    /** 结构箱子：15% 出一份种子 */
    private static Optional<IPageLootModifier> structureSeeds(IServerUtils utils, StructureSeedsLootModifier modifier) {
        return Optional.of(chestPage(utils, modifier, conditions -> {
            AliGlmReflection.Table table = tableAt(AliGlmReflection.tablesOf(StructureSeedsLootModifier.class), "SEEDS", 0);

            return table == null ? List.of()
                    : AliGlmNodes.table(utils, conditions, table, AliGlmData.STRUCTURE_SEEDS, AliGlmNodes.count(1, 2));
        }));
    }

    /** 村庄住宅箱子：种子抽两次（40% + 15%），再 15% 抽一份加工品 */
    private static Optional<IPageLootModifier> villageSeeds(IServerUtils utils, VillageSeedsLootModifier modifier) {
        return Optional.of(chestPage(utils, modifier, conditions -> {
            List<IOperation> operations = new ArrayList<>();
            AliGlmReflection.Table seeds = tableAt(AliGlmReflection.tablesOf(VillageSeedsLootModifier.class), "TEMPERATE", 0);

            if (seeds != null) {
                operations.addAll(AliGlmNodes.table(utils, conditions, seeds, AliGlmData.VILLAGE_SEEDS, AliGlmNodes.count(1, 2)));
            }

            float processed = AliGlmData.VILLAGE_SEEDS_PROCESSED / AliGlmData.VILLAGE_PROCESSED_TOTAL;

            operations.add(AliGlmNodes.add(utils, conditions, ModItems.CORN_FLOUR.get(),
                    processed * AliGlmData.VILLAGE_PROCESSED_CORN_FLOUR, AliGlmNodes.count(1, 3)));
            operations.add(AliGlmNodes.add(utils, conditions, ModItems.WATER_DOUGH.get(),
                    processed * AliGlmData.VILLAGE_PROCESSED_WATER_DOUGH, AliGlmNodes.count(1, 3)));
            operations.add(AliGlmNodes.add(utils, conditions, ModItems.PEANUT_BUTTER.get(),
                    processed * AliGlmData.VILLAGE_PROCESSED_PEANUT_BUTTER, AliGlmNodes.count(1, 1)));
            operations.add(AliGlmNodes.add(utils, conditions, ModItems.PICKLED_SNOW_MUSTARD.get(),
                    processed * AliGlmData.VILLAGE_PROCESSED_PICKLED_SNOW_MUSTARD, AliGlmNodes.count(1, 2)));

            return operations;
        }));
    }

    /**
     * 箱子战利品白名单：这是「过滤」而不是「产出」，所以画成移除——只在箱子表上生效，
     * 被配置拉黑的物品会被划掉。列表为空（默认全允许）时什么也不会移除。
     */
    private static Optional<IPageLootModifier> chestLootConfig(IServerUtils utils, ChestLootConfigModifier modifier) {
        return Optional.of(GlobalLootModifierUtils.getLootModifier(utils, modifier, List.of(), (page, unexplained) -> {
            if (page.paramSet() != LootContextParamSets.CHEST) {
                return List.of();
            }

            return List.of(AliGlmNodes.remove(stack -> !NortheastChinaConfig.isChestLootItemAllowed(
                    BuiltInRegistries.ITEM.getKey(stack.getItem()))));
        }));
    }

    /** 两档 roll 的箱子注入器（生料 / 成品），表按字段名取，名字变了就按声明顺序兜底 */
    private static List<IOperation> twoTier(IServerUtils utils, List<LootItemCondition> conditions, Class<?> owner,
                                           float rawChance, int rawMin, int rawMax,
                                           float dishChance, int dishMin, int dishMax) {
        List<IOperation> operations = new ArrayList<>();
        List<AliGlmReflection.Table> tables = AliGlmReflection.tablesOf(owner);
        AliGlmReflection.Table raw = tableAt(tables, "RAW", 0);
        AliGlmReflection.Table dish = tableAt(tables, "DISH", 1);

        if (raw != null) {
            operations.addAll(AliGlmNodes.table(utils, conditions, raw, rawChance, AliGlmNodes.count(rawMin, rawMax)));
        }

        if (dish != null) {
            operations.addAll(AliGlmNodes.table(utils, conditions, dish, dishChance, AliGlmNodes.count(dishMin, dishMax)));
        }

        return operations;
    }

    private static IPageLootModifier page(IServerUtils utils, Object modifier, Function<List<LootItemCondition>, List<IOperation>> builder) {
        List<LootItemCondition> conditions = AliGlmReflection.conditionsOf(modifier);

        return GlobalLootModifierUtils.getLootModifier(utils, modifier, conditions, (page, unexplained) -> builder.apply(unexplained));
    }

    /** 箱子注入器共用：配置里关掉箱子战利品时不显示 */
    private static IPageLootModifier chestPage(IServerUtils utils, Object modifier, Function<List<LootItemCondition>, List<IOperation>> builder) {
        return page(utils, modifier, conditions ->
                NortheastChinaConfig.isChestLootEnabled() ? builder.apply(conditions) : List.of());
    }

    private static AliGlmReflection.Table tableAt(List<AliGlmReflection.Table> tables, String name, int index) {
        for (AliGlmReflection.Table table : tables) {
            if (table.name().equals(name)) {
                return table;
            }
        }

        return index < tables.size() ? tables.get(index) : null;
    }

    public AliGlmPlugin() {}
}
