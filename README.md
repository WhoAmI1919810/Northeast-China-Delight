# 东北乐事 (Dongbei Delight)

一个 NeoForge 模组，添加东北风味的内容。

## 支持的版本

| Minecraft | NeoForge | 说明 |
| --- | --- | --- |
| 1.21.1 | 21.1.250 | 仓库主版本（VCS version），提交前源码保持此状态 |
| 1.21.4 | 21.4.157 | 第二个目标版本 |

## 环境要求

- JDK 21（`JAVA_HOME` 必须指向 JDK 21，Stonecutter 与 Gradle 9 都要求 JVM 21）
- 首次构建需要联网下载 Gradle、NeoForge 与 Minecraft 反编译产物

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
