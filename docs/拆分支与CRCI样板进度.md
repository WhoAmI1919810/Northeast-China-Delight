# 拆分支与 CRCI 样板进度（2026-11-11）

## 仓库与分支

- 东北乐事原仓库：`https://github.com/WhoAmI1919810/Northeast-China-Delight.git`
  - `split-1.21.1`：从 `main` 的 `1f7695dd` 创建。
  - `split-1.20.1`：从 `1.20.1` 的 `97694578` 创建。
  - 旧 `main` 与旧 `1.20.1` 保持原样，只作为冻结线修 bug。
- CRCI：`https://github.com/WhoAmI1919810/Chinese-Culinary-Ingredients.git`
  - 本地目录：`H:\IdeaProjects\chinese_regional_culinary_ingredients`。
  - mod id：`chinese_regional_culinary_ingredients`；版本：`0.1.0`。
  - Stonecutter 节点：`1.20.1`、`1.21.1`。

## 本轮样板范围

只迁移大豆，用来验证跨模组注册、资源归属和双版本构建，不迁移其他食材，也不实现完整档案系统。

- 大豆物品、8 阶段植株、方块状态、模型、贴图、语言、掉落表、`c:crops/soybean` 与 `c:seeds/soybean` 标签迁入 CRCI。
- 东北乐事拆分分支改为按注册名取 CRCI 的 `soybean` 和 `soybean_crop`，不再自己注册或打包这两样资源。
- 东北乐事创造栏移除大豆，图标改为大白菜；堆肥作物列表移除大豆。
- `soybean_sack`、豆芽配方、大缸显示模型、村民交易等仍留在东北乐事，只把引用改成 CRCI 大豆。
- 三份村庄农田处理器中的大豆规则改为引用 CRCI 大豆物品和植株。
- 箱子战利品配置暂时把 CRCI 命名空间视同东北乐事命名空间，保证拆分后大豆仍受原配置名单控制；这是样板兼容，不是最终方案。
- 两个拆分分支的模组元数据都增加对 CRCI 的必需依赖。
- CRCI 补入 MIT `LICENSE`。

## 构建结果

已通过：

- CRCI `:1.21.1:build --offline`
- CRCI `:1.20.1:build --offline`
- 东北乐事 `split-1.21.1` 的 `:1.21.1:build --offline`
- 东北乐事 `split-1.20.1` 的 `:1.20.1:build --offline --no-configuration-cache`

1.20.1 的 `legacy-data-port.gradle` 与配置缓存不兼容，拆分树构建要加 `--no-configuration-cache`；这不是大豆迁移引入的错误。

## 尚未实现

- `data/<地区>/crci/profile.json` 的地区档案、`enabled_content` 校验和并集。
- CRCI 的 `regional_farm_crop` 处理器与气候加权池；当前农田仍是东北乐事旧规则，只换了引用。
- 农民、流浪商人、副食商、屠夫、渔夫的统一交易池和运行时可用性守卫。
- 副食店结构与副食商的 CRCI 化。
- 创造栏、JEI、配方书、战利品对“未启用内容”的完整过滤和客户端同步。
- 除大豆外的食材、作物、调料、机器和工具的迁移。

因此本轮是“资源搬迁样板”，不是完整的 CRCI 端到端行为验证。下一步按 `docs/区域作物启用与售卖实施评估.md` 的阶段 A、B 做档案、可用性快照和农田处理器，再决定是否迁移全部东北作物。

## 约定

- README 以用户本地版本为准，不自动改、不自动提交。
- 拆分分支和 CRCI 构建统一加 `--offline`，使用 `E:\codex\gradle-user-home`，不往 C 盘写构建缓存。
- 共享代码改动要同时落到 `split-1.21.1` 和 `split-1.20.1`。

## 版本命名与最新构建

- 拆分版从 `S-4.0.2-test001` 起改用正式测试版本号格式：`4.0.2.001`，后续递增最后一段，不再使用 `S-` 前缀。
- 2026-11-11 已按 `4.0.2.001` 构建：`split-1.21.1` 产物 `libs/northeast_china_delight-4.0.2.001+1.21.1.jar`（6,273,712 B，SHA-256 `4218810270194A8C19AF171A5C3FA8D33469FCFDE0557B3229AF21CB48F3BA55`）；`split-1.20.1` 产物 `libs/northeast_china_delight-4.0.2.001+1.20.1.jar`（6,527,903 B，SHA-256 `A83B75C990E22702989D9226D3D89FD6B5036DB9E0C0B2847199204C82B2A67A`）。
- 1.20.1 构建的 `legacy-data-port.gradle` 现会在展开 `neoforge:compound` 时剔除 `minecraft:mace`，避免 1.20.1 JEI 把带重锤的舂捣配方判为无效；1.21.1 仍保留重锤。
