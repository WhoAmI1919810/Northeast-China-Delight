# 东北乐事 (Northeast China Delight)

一个 NeoForge 模组，添加东北风味的内容，是 [农夫乐事 (Farmer's Delight)](https://github.com/vectorwing/FarmersDelight) 的附属模组。

## 支持的版本

| Minecraft | NeoForge | 农夫乐事 | 说明 |
| --- | --- | --- | --- |
| 1.21.1 | 21.1.250 | 1.3.4 | 仓库主版本（VCS version），提交前源码保持此状态 |

目标版本必须跟随农夫乐事：它目前只发布到 1.21.1。待其发布新版本线后，在 `settings.gradle` 中追加对应节点即可。
（`versions/1.21.4/` 里还留着早期试探多版本时生成的构建产物，没有注册到 Stonecutter，可以随时删除。）

## 环境要求

- JDK 21（`JAVA_HOME` 必须指向 JDK 21，Stonecutter 与 Gradle 9 都要求 JVM 21）
- 首次构建需要联网下载 Gradle、NeoForge 与 Minecraft 反编译产物

## 依赖：农夫乐事

农夫乐事没有发布到公共 Maven，工程通过 [Modrinth Maven](https://api.modrinth.com/maven) 获取，坐标 `maven.modrinth:farmers-delight:<版本>`，版本号在 `versions/1.21.1/gradle.properties` 的 `farmersdelight_version` 中配置。

- `compileOnly`：编译期可引用其 API，但不会打进本模组的 jar
- `localRuntime`：开发运行时自动加载，便于本地测试

模组元数据中已声明 `farmersdelight` 为 `required` 依赖，玩家必须同时安装农夫乐事。

## 常用命令

```bash
./gradlew build                      # 构建所有版本（不指定项目路径时 Gradle 会对每个版本节点各执行一次）
./gradlew :1.21.1:build              # 只构建 1.21.1
./gradlew :1.21.4:build              # 只构建 1.21.4
./gradlew :1.21.1:runClient          # 启动 1.21.1 客户端
./gradlew :1.21.1:runServer          # 启动 1.21.1 服务端（首次需在 run/1.21.1/eula.txt 同意 EULA）
./gradlew "Set active project to 1.21.4"   # 切换激活版本（会重写 src/ 中的条件注释）
./gradlew "Reset active project"     # 提交前恢复为 VCS 版本（1.21.1）
```

产物位于 `versions/<游戏版本>/build/libs/dongbei_delight-<模组版本>+<游戏版本>.jar`。
每个游戏版本使用独立的运行目录 `run/<游戏版本>/`，存档与配置互不干扰。

## 日志约定

启动 Minecraft 产生的日志统一放在工程根目录的 **`logs/`**（不要放到 Codex 的工作目录里）。
用下面的脚本启动客户端，它会自动建目录、记录控制台日志，并在退出后把游戏的 `latest.log` 归档一份：

```powershell
pwsh -File tools\start_client.ps1          # 默认启动 1.21.1
```

| 文件 | 内容 |
| --- | --- |
| `logs/client-<版本>-<时间戳>.log` | Gradle + 启动器 + 游戏的控制台完整输出 |
| `logs/minecraft-<版本>-<时间戳>.log` | 游戏自身 `latest.log` 的归档 |

游戏目录仍是 `run/<游戏版本>/`（存档、配置、以及游戏自己写的 `logs/latest.log`），同样在工程目录内。
`logs/` 里的日志文件不提交 git。

## 美术素材

全部贴图清单（含规格、命名、优先级，以及还没做但迟早要补的素材）见
[docs/美术素材清单.md](docs/美术素材清单.md)。当前贴图都是脚本生成的占位图，正式素材同名覆盖即可，无需改代码。

当前开发进度与后续更新计划（含山珍、海产、新食材、村民、模组适配等）见
[docs/更新计划.md](docs/更新计划.md)。

## 目录结构

```
src/main/java                     所有版本共享的源码（用条件注释区分版本）
src/main/resources                所有版本共享的资源
src/main/templates                模组元数据模板（构建时按版本填充）
versions/<游戏版本>/
    gradle.properties             该版本使用的 Minecraft / NeoForge / Parchment 版本
    src/main/resources            仅该版本使用的资源（会覆盖同名共享资源）
stonecutter.gradle                当前激活的版本
```

## 多版本开发规则

本工程使用 [Stonecutter](https://stonecutter.kikugie.dev/) 管理多版本，**根目录 `src/` 中是一份被条件注释切分的共享源码**。

### 1. Java 代码差异

用条件注释包裹，**当前激活版本的分支是未注释的，其它分支必须写在块注释里**：

```java
//? if <1.21.4 {
import net.minecraft.world.ItemInteractionResult;
//?}
```

```java
//? if >=1.21.4 {
/*@Override
protected @NotNull InteractionResult useItemOn(...) {
    ...
}*/
//?} else {
@Override
protected @NotNull ItemInteractionResult useItemOn(...) {
    ...
}
//?}
```

### 2. 资源文件差异

Stonecutter 只处理文本源码，**不会处理 JSON 等资源**，因此版本相关的资源要放到 `versions/<游戏版本>/src/main/resources/` 下，例如配方文件。

### 3. 已知的版本差异

| 内容 | 1.21.1 | 1.21.4 |
| --- | --- | --- |
| 方块注册 | `BLOCKS.register(...)`（无需 id） | 必须带 id，统一用 `BLOCKS.registerBlock(...)` 自动绑定 |
| 物品模型 | `assets/<ns>/models/item/*.json` | `assets/<ns>/items/*.json` 引用 `models/item/*.json`（两套并存即可兼容） |
| 配方 ingredient | `"ingredient": {"item": "..."}` | `"ingredient": "..."` |
| `useItemOn` 返回值 | `ItemInteractionResult`（`PASS_TO_DEFAULT_BLOCK_INTERACTION`） | `InteractionResult`（`TRY_WITH_EMPTY_HAND`） |

新增版本时：在 `settings.gradle` 的 `versions` 里登记、创建 `versions/<版本>/gradle.properties` 与资源目录，然后按上面的方式处理代码差异。

## 与农夫乐事的联动

数据驱动的配方直接写在 `src/main/resources/data/dongbei_delight/recipe/` 下即可。

**炖锅（Cooking Pot）**

```json
{
  "type": "farmersdelight:cooking",
  "experience": 1.0,
  "ingredients": [
    { "item": "minecraft:apple" },
    { "item": "minecraft:sugar" }
  ],
  "recipe_book_tab": "drinks",
  "result": { "count": 1, "id": "dongbei_delight:soul_cabbage" }
}
```

**砧板（Cutting Board）**

```json
{
  "type": "farmersdelight:cutting",
  "ingredients": [{ "item": "dongbei_delight:sour_cabbage" }],
  "result": [{ "item": { "count": 1, "id": "dongbei_delight:soul_cabbage" } }],
  "sound": { "sound_id": "minecraft:item.axe.strip" },
  "tool": [{ "tag": "farmersdelight:tools/knives" }]
}
```

常用的农夫乐事标签：`farmersdelight:tools/knives`（刀具）、`farmersdelight:meals`（正餐）、`farmersdelight:snacks`（零食）、`farmersdelight:drinks`（饮品）、`farmersdelight:flat_on_cutting_board`（可平放于砧板）。
