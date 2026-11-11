# 拆分支与 CRCI 迁移样板进度（历史，2026-11-12 更新）

> 当前有效进度以 `docs/拆分与CRCI迁移进度.md` 为准。本文保留样板阶段的决策和踩坑记录；除大豆样板外的全量通用内容迁移已经完成。

## 最终状态

- CRCI：`main`，版本 `0.1.0`，1.20.1 / 1.21.1 双版本可构建。
- 中国东北乐事拆分版：`split-1.21.1` / `split-1.20.1`，版本 `4.0.2.001`，两个分支均可构建。
- 东北乐事已把 CRCI 声明为必需前置，通用食材、种子、作物、箱袋、加工原料、调料、调味流体、通用器具和机器不再在地区模组内重复注册。
- 旧东北乐事 `main` / `1.20.1` 仍是冻结线，只接受 bug 修复。

## 仓库与分支

- 东北乐事原仓库：`https://github.com/WhoAmI1919810/Northeast-China-Delight.git`
  - `split-1.21.1`：从 `main` 的 `1f7695dd` 创建。
  - `split-1.20.1`：从 `1.20.1` 的 `97694578` 创建。
  - 旧 `main` 与旧 `1.20.1` 保持原样，只作为冻结线修 bug。
- CRCI：`https://github.com/WhoAmI1919810/Chinese-Culinary-Ingredients.git`
  - 本地目录：`H:\IdeaProjects\chinese_regional_culinary_ingredients`。
  - mod id：`chinese_regional_culinary_ingredients`；版本：`0.1.0`。
  - Stonecutter 节点：`1.20.1`、`1.21.1`。

## 大豆样板解决了什么

样板只迁移大豆，用来验证跨模组注册、资源归属和双版本构建；全量迁移后来沿用同一套做法。

- 大豆物品、8 阶段植株、方块状态、模型、贴图、语言、掉落表、`c:crops/soybean` 与 `c:seeds/soybean` 标签迁入 CRCI。
- 东北乐事拆分分支改为按注册名取 CRCI 的 `soybean` 和 `soybean_crop`，不再自己注册或打包这两样资源。
- 东北乐事创造栏移除大豆，图标改为大白菜；堆肥作物列表改为引用 CRCI 的堆肥物品列表。
- `soybean_sack`、豆芽配方、大缸显示模型、村民交易等仍留在东北乐事，只把引用改成 CRCI 大豆。
- 三份村庄农田处理器中的大豆规则改为引用 CRCI 大豆物品和植株。
- 箱子战利品配置暂时把 CRCI 命名空间视同东北乐事命名空间；后续应由统一内容启用配置接管。
- 两个拆分分支的模组元数据都增加对 CRCI 的必需依赖。
- CRCI 补入 MIT `LICENSE`。

## 样板阶段的构建结论

- `--offline` 与 `E:\codex\gradle-user-home` 的构建约定继续有效。
- 1.20.1 的 `legacy-data-port.gradle` 与配置缓存不兼容，构建必须加 `--no-configuration-cache`。
- 1.20.1 构建的 `legacy-data-port.gradle` 会在展开 `neoforge:compound` 时剔除 `minecraft:mace`，避免 1.20.1 JEI 把带重锤的舂捣配方判为无效；1.21.1 仍保留重锤。
- 样板阶段记录的旧构建体积和 SHA-256 已被全量迁移产物取代，不再引用；最新产物见当前进度文档。

## 全量迁移后仍待实现的行为层

- `data/<地区>/crci/profile.json` 的地区档案、`enabled_content` 校验和并集。
- CRCI 的 `regional_farm_crop` 处理器与气候加权池；当前农田仍是地区规则，只换了通用内容引用。
- 农民、流浪商人、副食商、屠夫、渔夫的统一交易池和运行时可用性守卫。
- 副食店结构与副食商的 CRCI 化；无任何地区乐事安装时不生成副食店。
- 创造栏、JEI、配方书、战利品对“未启用内容”的完整过滤和客户端同步。
- 各地区乐事对 CRCI 共通内容的启用接口，以及本地区专属交易的注入接口。
- 未来地区乐事接入 CRCI 的端到端测试和旧存档兼容验证。

## 约定

- README 以用户本地版本为准，不自动改、不自动提交。
- 拆分分支和 CRCI 构建统一加 `--offline`，使用 `E:\codex\gradle-user-home`，不往 C 盘写构建缓存。
- 共享代码改动要同时落到 `split-1.21.1` 和 `split-1.20.1`。
- 拆分版使用 `4.0.2.001` 这样的递增版本号，不再使用 `S-` 前缀。