# 高级战利品显示（Advanced Loot Info）兼容评估

核查日期：2026-10-16。对象：ALI（Modrinth `advanced-loot-info`，CurseForge 1205426）+ 前置 ACI（`advanced-core-info`，CurseForge 1661923）。

## 1. ALI 能看到的三种来源

| 来源 | 存在形式 | ALI 的处理 |
| --- | --- | --- |
| 方块 / 结构箱子战利品表 | 数据包 `data/<ns>/loot_table/**`（本模组 57 份：`blocks/*_crop.json`、`blocks/*_crate.json`、`pots/*`、`chests/courtyard_*`） | 直接解析成页面，无需任何适配 |
| 全局掉落修改器（GLM） | 数据包 `data/<ns>/loot_modifiers/*.json` + Java 里的 `LootModifier` 子类 | 只有「类型被 ALI（或上游 ALICompat）认识」时才能画出物品；否则退化为自动检测节点 |
| 村民交易 | Java `VillagerTrades.ItemListing` | 逐类注册 `IItemListing` 才能显示 |

## 2. 本模组实测现状（1.21.1 / NeoForge，ALI 2.4.0 + ACI 1.4.0）

`run/1.21.1/logs/latest.log` 内的 ALI 自述：

- `[ali] Building 2289 pages`、`[ali] Skipped 1227 block loot tables dropping only the block itself` —— 数据包表全部被解析，作物（`corn_crop` 等）的掉落本来就能看到。
- `[ali] Registered 49 loot modifiers` / `Evaluating 49 global loot modifiers against 2289 pages` —— 我们的 7 个 GLM 已被收进评估流程。
- `[ali] Missing GLM for com.gunmu.northeast_china_delight.loot.PigOffalLootModifier`（另有 `NortheastFishingLootModifier`、`StructureSeedsLootModifier`、`ButcherLootModifier`、`VillageSeedsLootModifier`、`ChestLootConfigModifier`）—— 类型未被识别，走 `GlobalLootModifierUtils.getMissingGlobalLootModifier` 的自动检测兜底：只画「原始 JSON + Java 字段」的文本节点，不画猪肘 / 猪血这类真实物品条目。
- `[ali] 2 of 49 loot modifiers ended up on no page` —— 有 GLM 找不到归属页面（钓鱼那条条件只声明了 `this` 为 `fishing_bobber`，页面侧判不出实体类型）。
- `[aci] Missing trade item listings for com.gunmu.northeast_china_delight.villager.YardMapForEmeralds`（另有 `ModWandererTrades$$Lambda`、`SideDishMerchantTrades$$Lambda`）—— 村民交易同理。

## 3. 为什么农夫乐事的 `ham` 能显示

FD 的猪肉掉落是**它自己也被适配过**的数据包 GLM：`data/farmersdelight/loot_modifiers/scavenging_ham_from_pig.json`，`"type": "farmersdelight:add_item"`，掉落物写在 JSON 的 `item` 字段里，条件（手持 `#farmersdelight:tools/knives`、`this` 为 `minecraft:pig`、是否着火）也全在 JSON 里。ALI 侧由上游 ALICompat 的 `alicompat/*/src/compat/farmersdelight/FarmersDelightCompat` 注册 `AddItemModifier` / `FDAddTableLootModifier` / `ReplaceItemModifier` / `PastrySlicingModifier` 的 accessor，于是能画成「实体掉落物」下的一条。

本模组的区别不是「没进数据包」，而是数据包里只声明了 `type` 与 `conditions`，物品清单、数量、概率全在 Java（`PigOffalLootModifier` 的 `Entry[] table` + 洗牌 + 每级抢夺 +5% + 最多 2 种 + 着火换熏制）。ALI 无法静态推导，只能兜底。

## 4. 适配办法（官方扩展点，不需要等上游）

ALI 2.4.x 对外提供插件 API，且 **1.20.1（Forge）与 1.21.1（NeoForge）是同一套**（两分支都存在 `ali/common/.../api/AliEntrypoint.java` 与 `plugin/glm/IGlobalLootModifierPlugin.java`）：

```java
@AliEntrypoint
public final class AliCompatPlugin implements IGlobalLootModifierPlugin {
    @Override public String getModId() { return "northeast_china_delight"; }

    @Override public void registerGlobalLootModifier(IRegistry registry) {
        registry.registerGlobalLootModifier(PigOffalLootModifier.class, AliCompatPlugin::pigOffal);
        registry.registerGlobalLootModifier(NortheastFishingLootModifier.class, AliCompatPlugin::fishing);
        // …每个 GLM 一个 getter
    }

    // 用 ALI 自己的机制把「条件 + 产出操作」拼成页面
    private static Optional<IPageLootModifier> pigOffal(IServerUtils utils, PigOffalLootModifier m) {
        return Optional.of(GlobalLootModifierUtils.getLootModifier(utils, m, m.getConditions(),
                (page, unexplained) -> List.of(new IOperation.AddOperation(i -> true, /* 物品节点 */)));
    }
}
```

要点：

- **发现机制**：ALI 的平台实现用 `ModFileScanData` 对 `@AliEntrypoint` 做 ASM 扫描（`ali/neoforge`、`ali/forge` 的 `getPlugins()`）；插件报告 `getModId()` 必须等于本模组 id，模组未加载时其插件会被跳过。
- **依赖是纯编译期**：ALI 未安装时 ALI 不会去加载这个类，因此 `compileOnly` 即可，无需 `implementation`。仓库 `build.gradle` 已配 `https://api.modrinth.com/maven`，两棵树 `build.gradle` 的对应节点依赖块已加上（落地情况见第 5 节）：
  `compileOnly "maven.modrinth:advanced-loot-info:1.21.1-2.4.0"`、`maven.modrinth:advanced-core-info:1.21.1-1.4.0`（1.20.1 树换成 `1.20.1-2.4.0` / `1.20.1-1.4.0`，均已发布）。
- **数量与概率**：ALI/ACI 有 `INumberProvider` / `NumberModifier` / `TooltipBuilder` 可以表达数量区间与概率；我们是 Java 洗牌表，需要逐个 GLM 手工翻译成物品节点。
- **跨版本**：compat 类只引用本模组自己的 GLM 类，不需要 stonecutter 分流；GLM 基类差异（`net.minecraftforge.common.loot.LootModifier` / `net.neoforged.neoforge.common.loot.LootModifier`）留在原有类里。
- **钓鱼的额外难点**：`NortheastFishingLootModifier` 按水域在三张表间挑一张并替换原版鱼，静态三值逻辑表达不了「按群系选表」，需要拆成候选分支，或声明为未绑定（受 ALI 配置 `showUnboundedGlobalLootModifiers` 影响）。
- **备选（零维护）**：把 compat 提给上游 ALICompat（`alicompat` 模块，其 survey 流程会自动扫描模组 jar 里的 GLM）。代价是发布节奏不由我们控制。

## 5. 落地实现（2026-10-16）

按上面的扩展点做了，两棵树各新增 4 个文件（`com.gunmu.northeast_china_delight.compat.ali`）：

| 文件 | 作用 |
| --- | --- |
| `AliGlmPlugin.java` | `@AliEntrypoint` 入口，`registerGlobalLootModifier` 里给 7 个 GLM 各挂一个 getter |
| `AliGlmReflection.java` | 反射按「形状」读 GLM 里 `private static final` 的产出表与 `conditions`，只读、无副作用 |
| `AliGlmData.java` | 反射读不到的手写补录（屠夫 / 渔夫的概率、结构种子 / 村庄种子的概率、猪副产物内联表） |
| `AliGlmNodes.java` | 拼页面的小工具：`add` / `table` / `union` / `replace` / `remove` / `node` |

- `chest_loot_config` 画成 `RemoveOperation`（它本来就是「按配置过滤本模组物品」），箱子注入器受 `NortheastChinaConfig.isChestLootEnabled()` 门控。
- 依赖按节点分开写：`versions/1.21.1/gradle.properties` 里 `ali_version=1.21.1-2.4.0`、`aci_version=1.21.1-1.4.0`；`versions/1.20.1/gradle.properties` 换成 `1.20.1-*`。
- 仓库解析：NeoForge 的 moddev 插件先注册 `maven.neoforged.net`，Gradle 按声明顺序查询，于是每个 `maven.modrinth` 依赖都会先打到那边（偶发超时直接让构建失败）。两棵树 `allprojects` 里加了 `repositories.configureEach { ... excludeGroup 'maven.modrinth' }`，把该组从 neoforged 仓库排除。
- 状态：两棵树 `build` 均通过（1.20.1 树 3.1.0 jar 已产出）。**待客户端实测** —— 装上 ALI 2.4.0 + ACI 1.4.0 后，日志里那 7 条 `Missing GLM for com.gunmu...` 应当消失，JEI 的猪「实体掉落物」应能看到猪肘 / 猪血，钓鱼页应能看到大虾 / 生蚝 / 带鱼 / 海参。

## 6. 遗留待决

- ~~`data/northeast_china_delight/loot_modifiers/village_seeds.json` 不在清单里、是不生效的死文件~~ —— 2026-10-16 已删除（两棵树）；真正生效的是 `village_seeds_plains/savanna/snowy/taiga/desert.json`。
- 2026-10-19：副食商交易已通过 `AliGlmPlugin#registerServer` + `IServerRegistry#registerTrades` 注册；`SideDishMerchantTrades.tradeListingArrays()` 与运行时共用同一份候选表，5 级展示 3 条（含黄桃罐头 Mixin 保底）。物品栏贴图同日换新。
- 流浪商与制图师的自定义交易 listing 仍待补齐；它们属于另外两个交易入口，不影响黄桃罐头在 ALI 中的展示。
