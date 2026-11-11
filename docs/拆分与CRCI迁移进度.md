# 拆分与 CRCI 迁移进度（2026-11-12）

## 结论

拆分主体已完成，CRCI 与中国东北乐事两个拆分分支均可独立构建。当前交付状态为：

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
- 三个仓库工作区均已清理并推送。

## 尚未完成的功能

以下属于拆分之后的新阶段，不影响当前拆分包构建，但还不能宣称 CRCI 的地区内容治理全部完成：

- 地区档案：`data/<地区>/crci/profile.json`、`enabled_content` 校验和跨地区并集。
- CRCI 农田处理器与气候加权作物池；当前农田生成仍由地区模组决定。
- 农民、流浪商人、副食商、屠夫、渔夫的统一交易池与运行时启用守卫。
- CRCI 副食店结构和副食商的完整迁移；无任何地区乐事安装时不得生成副食店。
- 未启用内容在创造栏、JEI、配方书、战利品表和客户端同步中的过滤。
- 地区乐事启用 CRCI 共通内容的接口，以及注入本地区专属交易的接口。
- 未来地区乐事接入 CRCI 的端到端测试和旧存档兼容验证。

## 约定

- 不向 C 盘写入任何内容。
- 构建统一使用 `E:\codex\gradle-user-home`，1.20.1 构建加 `--no-configuration-cache`。
- 构建和客户端运行加 `--offline`。
- 共享改动同时落到 `split-1.21.1` 与 `split-1.20.1`。
- README 以用户本地版本为准，不自动扩写更新记录。
- 技术进度以本文档为准，提交前确认三仓无未预期文件。