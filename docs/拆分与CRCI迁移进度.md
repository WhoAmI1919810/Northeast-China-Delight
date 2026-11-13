# 拆分与 CRCI 迁移进度（2026-11-12）

## 结论

拆分主体与行为层迁移已完成，CRCI 与中国东北乐事两个拆分分支均可独立构建，双版本全量 GameTest 通过。完整对照见 `docs/旧版功能对照与扩展接口审计.md`。当前交付状态为：

- CRCI：`main`，版本 `0.1.0`
- 中国东北乐事 1.21.1：`split-1.21.1`，版本 `4.0.2.001`
- 中国东北乐事 1.20.1：`split-1.20.1`，版本 `4.0.2.001`
- 旧东北乐事 `main` / `1.20.1` 继续冻结，只做 bug 修复

本轮不再把通用食材、器具或机器留在东北乐事中重复注册；东北乐事通过必需前置 `chinese_regional_culinary_ingredients` 使用这些内容。地区菜肴、盆装菜、东北结构、进度、唱片和地区玩法仍归东北乐事。

## 仓库与分支

- CRCI：`https://github.com/WhoAmI1919810/Chinese-Culinary-Ingredients.git`，分支 `main`
- 新东北乐事：`https://github.com/WhoAmI1919810/Northeast-China-Delight.git`
  - 1.21.1：`split-1.21.1`
  - 1.20.1：`split-1.20.1`
- 旧东北乐事只保持维护，不参与本轮拆分提交

## 已完成

### CRCI 通用层

- 通用食材、种子、作物、箱袋、加工原料、调料和通用调味流体迁入 CRCI。
- 通用器具与机器迁入 CRCI，包括大缸、大铁锅、烤架、木臼、木槌、大脸盆、水盆、脸盆架、铜碗等。
- 通用 blockstate、模型、贴图、语言、标签、战利品表、配方和创造栏条目已落到 CRCI 命名空间。
- 脸盆架配方已迁入 CRCI，东北乐事不再保留重复配方。
- 1.20.1 的必要能力注册已核对，`BigPotBlockEntity`、`VatBlockEntity` 的 Forge 能力没有因迁移丢失。

### 新东北乐事

- 删除与 CRCI 重复的通用注册、资源、配方和道具定义。
- 菜肴、大缸、烤架、JEI、Jade、进度、结构战利品、农田处理和村民交易中的通用内容引用改为 `chinese_regional_culinary_ingredients:*`。
- 新东北乐事声明 CRCI 为必需前置，并在 `libs/` 引用对应版本构建产物。
- 修复拆分后资源归属：
  - 25 个地区菜肴、烤串和干豆腐模型改回东北乐事命名空间；
  - 8 个盆装菜空盆模型中的 `basin_top`、`basin_bottom` 改回 CRCI；
  - 干豆腐模型改用实际存在的 `mung_bean_sheet_*` 贴图；
  - 删除无效的 `items/vat.json`、`items/unfired_vat_blank.json`；
  - 9 个进度父节点恢复为东北乐事自己的进度树，避免引用不存在的 CRCI 进度。

### 双版本同步

- 1.21.1 与 1.20.1 的资源修复已同步。
- 两版本都完成 `build`，并生成 `4.0.2.001` 测试包。
- 1.20.1 的 `recipes`、`loot_tables`、`advancements` 目录由构建期资源转换生成，已按转换后的路径复核。

## 验证结果

### 构建产物

- `chinese_regional_culinary_ingredients-0.1.0+1.21.1.jar`，1,604 个条目
- `chinese_regional_culinary_ingredients-0.1.0+1.20.1.jar`，1,610 个条目
- `northeast_china_delight-4.0.2.001+1.21.1.jar`，2,957 个条目
- `northeast_china_delight-4.0.2.001+1.20.1.jar`，2,932 个条目

### 跨 jar 审计

- 两个东北乐事 jar 中，CRCI 类、CRCI `assets`、CRCI `data` 夹带数均为 0。
- 两个东北乐事 jar 与 CRCI 的同名相对路径只剩目录项和 `lang` 文件；语言文件分开维护是预期行为。
- 对东北乐事引用 CRCI 的 120 个物品/方块引用、1 个标签引用、1 个配方 ID 引用全部存在。
- 92 个跨 jar 模型/贴图引用全部存在。
- 30 个 1.21.1 进度、29 个 1.20.1 进度的父节点全部可解析。
- 2026-11-12 当时三仓工作区已清理并推送；2026-11-13 的收尾改动已随本轮提交入库，见下方。

## 内容治理与兼容（已完成）

- 地区档案：`data/<地区>/crci/profile.json`、`enabled_content` 校验和跨地区并集。
- CRCI 农田处理器与气候加权作物池；地区档案只启用本地区允许进入农田的作物。
- 农民、流浪商人、副食商、屠夫、渔夫的统一交易池与运行时启用守卫。
- CRCI 副食店结构和副食商的完整迁移；无任何地区乐事安装时不加载副食店数据包。
- 未启用内容在创造栏、JEI、配方书、箱子战利品和客户端同步中的过滤。
- 1.21.1 与 1.20.1 均已接入 JEI、配方书、箱子战利品、野生生成和 ALI 可用性过滤。
- 1.21.1 与 1.20.1 均已修复迁移后的 20 个结构 NBT 引用；结构中的方块与战利品表全部指向 CRCI。
- 东北乐事已删除 10 份房屋池覆盖，CRCI 的 10 个普通/僵尸村庄模板池统一指向 `chinese_regional_culinary_ingredients:unique_side_dish_shop`。
- `UniqueShopPoolElement` 的唯一性判断已改为按首个副食店构件的放置坐标判断，避免同一模板池复用元素实例时失效。
- 地区乐事启用 CRCI 共通内容的接口，以及注入本地区专属交易的接口已落地。

## 行为层进展（2026-11-12）

### 已完成

- 新增 `data/<地区>/crci/profile.json` 解析器，校验 `schema`、`owner`、模组加载状态、内容归属和农田方块状态。
- 新增 `CrciAvailability`、`CrciUseSite` 和不可变档案快照；所有地区档案原子替换，并合并 `enabled_content` 并集。
- 新增 CRCI 通用配置：`villager_farm_crops_enabled`、`chest_loot_enabled`、`trades_enabled`，只控制获取途径。
- 新增服务端到客户端可用物品同步：数据包重载和玩家进入服务器时发送当前启用集合。
- 创造栏已改为使用完整的 `INGREDIENT_TAB_ITEMS`，并按 `CrciAvailability` 过滤；未启用食材仍保留注册，可用 `/give` 获取。
- 未启用且非基础内容的 CRCI 食材会显示“没有模组用到这个物品”提示。
- 新东北乐事已提供首份地区档案，启用当前使用的 CRCI 食材，并迁入 16 种作物的六类气候池与权重。
- 地区农田处理器已迁入 CRCI：新增 `RegionalFarmCropProcessor`、`CrciFarmRegistry`，替换小麦时只从当前档案中“已启用且属于该气候池”的作物里加权抽取；池为空时保留原版小麦。
- 原版五类村庄农田处理器列表与 `farm_taiga_small` 已由 CRCI 提供，迁移后六类气候的总替换概率保持原值（沙漠 0.2256、平原 0.3352、热带草原 0.2262、雪原 0.4596、针叶林 0.367、小型针叶林 0.4686）。
- 东北乐事删除旧的 6 份农田处理器列表、`VillageFarmCropRuleTest` 和旧规则注册，`village/taiga/houses.json` 改指 `chinese_regional_culinary_ingredients:farm_taiga_small`。
- 谜之炖菜的 `kinds/*`、`rarity/*` 标签与 `c:foods/soy_milk`、`c:milk/soy_milk`、`c:soy_milks`、`c:foods/kvass` 已迁入 CRCI，修复拆分后谜之炖菜效果为空、干豆腐风干配方匹配不到的问题。
- 修复野生榛子丛、野生木耳的 configured/placed feature 与 biome modifier 错误引用 CRCI 命名空间导致的注册表加载失败。
- 修复 CRCI 命名空间菜肴（豆浆）无法潜行摆放的问题；摆菜事件现在接受东北与 CRCI 两个命名空间中真实存在的菜肴方块。
- CRCI 配置分节调整为 `[worldgen] village_farm_crops_enabled`、`[chest_loot] enabled`、`[trades] enabled`。

### 最终验证

- CRCI `:1.21.1:build`、`:1.20.1:build` 通过。
- CRCI 双版本 GameTest：各 7/7 通过。
- 东北乐事 1.21.1 `:1.21.1:build` 通过，GameTest 123/123 通过。
- 东北乐事 1.20.1 `:1.20.1:build` 通过，GameTest 123/123 通过。
- 四个最终 jar 的依赖元数据已直接检查：CRCI 双版本必需农夫乐事；东北乐事双版本必需农夫乐事和 CRCI；Create、Jade、JEI、Display Delight 均为可选。
- 服务端日志确认地区档案加载：`CRCI loaded 1 regional profile(s), 123 enabled content id(s), revision 1`。
- 旧版 1.21.1 `assets` 3,192 个，新版 Northeast + CRCI 并集 3,170 个；缺少的 22 个均已核对为未注册旧流体资源或已删除旧物品定义。
- 旧版 120 项 GameTest 的行为均有新版等价或更强覆盖；新版共 123 项。

### 本轮新增验收覆盖

- `CrciAvailabilityGameTests`：只启用大豆时，大豆在所有 `CrciUseSite` 可用，大白菜不可用，基础器具与原版物品不受影响；农田、箱子、交易三项获取开关互不影响；平原农田池只保留大豆条目。
- `CrciTradeGameTests`：验证旧职业别名、任意完整职业 ID 和重载后的报价同步。
- `ShopPoolElementGameTests`：无地区档案时 10 个普通/僵尸村庄房屋模板池均不出现副食店；同一唯一元素实例在不同坐标下只接受第一座副食店。
- 东北乐事 `VillageShopGameTests`：装有机械动力时，五个最终副食店 NBT 的 palette 必须实际包含 `create:*` 方块。
- 东北乐事 `GenerationConfigGameTests`：新增 `naturalDropsRespectEnabledIngredients`，确认钓鱼与猪副产物不会绕过 CRCI 启用集合。

### 1.20.1 GameTest 环境说明

- 1.20.1 开发运行时已改为使用开发映射的 CRCI 运行时依赖，Create 和农夫乐事的 Mixin refmap 不再阻断 GameTest。
- `-PgameTestWithoutCreate=true` 保留为无 Create 的兼容回归模式；鼓风机专属用例会在该模式下跳过，不再计为失败。
- 1.20.1 的正式构建、资源转换、打包和全量 GameTest 均已通过。

### 待人工确认（非阻塞）

- 在真实村庄生成中抽样确认每村至多一座副食店。
- 只安装 CRCI 时确认世界中不出现副食店或副食商；服务端模板池、唯一性和交易守卫已有 GameTest 覆盖。
- 只启用大豆时确认 JEI、创造栏等客户端界面的最终视觉效果；服务端可用性与同步已有 GameTest 覆盖。
- `/reload` 后确认 JEI 是否立即刷新；创造栏、配方书和村民报价已有刷新逻辑。

### 已补齐的扩展缺口

- `CrciProfessions` 支持五类默认职业别名和任意完整职业 ID，未带命名空间的未来职业键落到档案 owner 命名空间。
- 地区档案动态遍历 `trades` 中的所有职业键，不再硬编码职业白名单。
- `CrciTradeRegistry.syncVillagerOffers()` 在数据包重载后清理失效报价并补入当前交易。
- `CrciAvailability.isContentEnabled(Item)` 提供不依赖农田、箱子和交易开关的内容启用查询，已用于钓鱼与猪副产物守卫。
- 东北乐事双分支删除失效的旧农田配置键；农田启用与允许作物现由 CRCI 配置和地区档案管理。

## 副食店与副食商迁移（2026-11-12）

- CRCI 已注册 `side_dish_merchant` 职业和同名 POI，职业站点使用 CRCI 大缸；东北乐事不再注册副食商职业。
- CRCI 新增两个内置服务端数据包：
  - `resourcepacks/side_dish_shops`：五个原版气候的副食店结构和 10 份普通/僵尸村庄房屋池覆盖；
  - `resourcepacks/side_dish_shops_create`：装有机械动力时覆盖为带机械动力方块的版本。
- 数据包仅在至少一个已加载模组的 `data/<modid>/crci/profile.json` 写入 `side_dish_shop: true` 时挂载。只安装 CRCI 时不会加载结构或覆盖原版模板池，也不会生成副食商。
- 五个群系的副食店 NBT 已迁入 CRCI。旧 `northeast_china_delight` 方块的引用已全部清除：大缸、箱装和袋装作物改用 CRCI 同名方块，窗、桌布、水泵、凳子、门帘、屋顶和暖气片改用原版通用方块；`farmersdelight:tomato_crate` 改为 CRCI 蔬菜箱。
- 店内装饰陶罐改为带战利品表的木桶，使用 `chinese_regional_culinary_ingredients:chests/side_dish_shop`。
- 东北乐事已删除 10 份房屋池覆盖、15 份旧副食店 NBT、内置机械动力副食店数据包和 `CreateVillageShops` 注册代码。
- CRCI 双版本构建通过；1.20.1 转换处理 10 个内置资源包结构 NBT，东北乐事 1.21.1 构建通过。

## 2026-11-13 收尾同步

- 将旧模组 `three_effects` 的组合判据同步到两个拆分分支：`greasy + warmth + nourishing` 或
  `refreshing + warmth + nourishing` 同时存在才解锁；效果 ID 使用 CRCI 命名空间。
- `进度设计.md` 与 `回归测试表.md` 补充组合判据说明和 ADV-08。
- 修复 CRCI 1.20.1 `LegacyNortheastAliases` 的流体类型取值：Forge 侧改为
  `ForgeRegistries.FLUID_TYPES.get().getValue(...)`。
- 创造栏统一：CRCI 移除 `equipment` 页；东北乐事只保留一个「东北乐事」创造页，海水桶、创造大缸和灵魂酸菜保持隐藏；
  新增 `CreativeTabGameTests` / `CrciCreativeTabGameTests` 校验单栏、无重复和隐藏项。
- 拆分版 `VillageShopGameTests` 改走 `OptionalMods.create()`，移除对版本专属 `ModList` 的直接引用，
  修复 Stonecutter 1.20.1 转换后的编译失败。
- 同步旧模组 `25925d69` / `0561dedd` 的谜之炖菜挑战判定修复：盛出时在 `give()` 前固定菜品判定，
  避免物品入包后菜栈被消费导致 `mystery_stew_feast` 漏发；拆分版新增对应 GameTest。
- 1.20.1 三张窗花贴图同步为最新版。
- 构建验证：CRCI 1.21.1 / 1.20.1、东北乐事 split 1.21.1 / 1.20.1 均 `BUILD SUCCESSFUL`；
  CRCI 新 jar 已同步到两个拆分仓库的 `libs/`。
- 2026-11-13 收尾改动与旧大锅迁移修复已提交；后续改动仍按仓库分别确认。

## 大锅旧存档迁移（2026-11-13）

- 旧东北乐事最早版大锅的 `Water` 字段现已兼容，不再因只读 `LiquidMb` 而归零。
- Forge 1.20.1 为物品、方块、方块实体、流体和流体类型补登记名字别名；旧 NBT 中的 `northeast_china_delight:big_pot` 与旧酱油流体不会再被当成未知 ID。
- `BigPotMigrationGameTests` 覆盖四种历史布局，并验证旧方块实体 ID 能经 `BlockEntity.loadStatic` 恢复。
- CRCI 双版本 GameTest 13/13 通过；拆分版双版本构建通过，新 CRCI jar 已同步到两个 `libs/`。

## 旧模组最新同步（2026-11-13）

- 同步旧东北乐事 `2fc38816`（1.20.1 对应 `87a4883f`）：更新瓦房院与长房院结构 NBT。
- 同步旧东北乐事 `278cf8cf`（1.20.1 对应 `7c29493c`）：盆装谜之炖菜移出创造栏，并登记为隐藏项。
- 拆分版在东北乐事主创造栏与 CRCI 共享创造栏同时隐藏 `mystery_stew`，两边 GameTest 均增加隐藏断言。
- 新结构中的共通内容 ID 已改写回 CRCI；东北专属菜肴、建材、结构与小院战利品表保持东北命名空间。

## 约定

- 不向 C 盘写入任何内容。
- 构建统一使用 `E:\codex\gradle-user-home`，1.20.1 构建加 `--no-configuration-cache`。
- 构建和客户端运行加 `--offline`。
- 共享改动同时落到 `split-1.21.1` 与 `split-1.20.1`。
- README 以用户本地版本为准，不自动扩写更新记录。
- 技术进度以本文档为准，提交前确认三仓无未预期文件。
