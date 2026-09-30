# 东北乐事 (Northeast China Delight)

一个 NeoForge 模组，添加东北风味的内容，是 [农夫乐事 (Farmer's Delight)](https://github.com/vectorwing/FarmersDelight) 的附属模组。

## 版本号与更新记录

版本号是 `x.y.z` 三段：

- **x（大阶段）**：1 = 大缸与酿造、农作物、食材与调料、各种料理；2 = 村庄、结构与收纳。
- **y**：加入一批规模较大的新内容时 +1。
- **z**：修一次小问题 +1。

### 2.1.5

- 修好了**院墙根露砖、院子内外地面不齐**：清场件给院墙外那一圈垫土时少垫了「地面那一层」，
  墙外的地表比院子里低一格，墙脚那排砖就露在外面。现在墙外一圈垫到和院子地面齐平。
- 雪原里还会把院墙外的**薄雪片和积雪块**清掉：原版的高度图会把雪片也算成"一层方块"，
  按它对齐就会让院子比外面矮一格 —— 这正是之前"雪地里墙根漏砖"的原因。

### 2.1.4

- **晾衣架重做**：改用农夫乐事的**绳栅栏**当晾衣绳，两根木杆 3 格高、三道绳，就是那张图上的样子。
- **菜园不会结冰了**：浇水口改成**含水的木活板门**（一小块木板嵌在地里），冷地方冬天也不再冻成一排冰，
  照样能给耕地保湿；除了浇水的这一格，其余全是耕地和作物。
- **每个棚子、房子都补了灯笼**：牛棚、猪圈、鸡架、苞米楼子、粮囤、灶棚、旱厕、水井、门楼、
  正房和厢房里都挂上了灯，晚上院里不再一片漆黑。
- **四合院的厢房好进了**：门口补了一级石砖台阶，门也从"朝着院角"改成**朝院子开**（东厢房开西墙、西厢房开东墙）。
- **菜窖变成真的地窖**：以前那两个活板门只是画在地上的框——现在掀开盖子真能下去，
  窖里有一架梯子（爬得上来）、一个箱子、一筐地瓜、一个木桶和一盏灯。模板整体往下多垫了 4 层土，
  正好用来挖这间窖，院子地面本身还是和外面齐平。

### 2.1.3

- 小院现在会**躲开村庄**：附近（默认 64 格内）有村庄、或者按生成规则很可能会出现村庄的地方，小院就换地方长，
  不会再出现"院子的房子插进村里房子"。想让它躲得更远或更近，改 `dongbei_yard.json` 里的 `avoid.margin` 就行。
- **牛棚的地板修好了**：以前木板地板比院子地面高一格，水桶半个埋在木板里；现在地板就铺在地面那一层上，
  和外面的草地齐平，水槽、料槽、饲料桶都稳稳摆在地板上。
- **鸡架重做**：以前棚里塞满两格棕色羊毛，完全看不出是干什么用的；现在里面只有一格草窝，
  另外有饮水盆、门口歇脚横杆和三面矮栅栏，一眼就能看出是养鸡的地方。
- **正房的三间屋重新规划**：
  - 西屋 = **卧室**：南窗下一盘火炕，炕上有被褥和枕头，北墙摆柜子，墙上挂灯；
  - 中间 = **灶房**：砖砌灶台连着两口灶眼（明火 + 炖锅），旁边是水缸、案板、粮袋、米桶，屋子当中一张小桌；
  - 东屋 = **储物间**：箱子摞两层，成排的桶、菜筐、粮袋和腌菜大缸，角落里还有一个**菜窖口**。
- 以前屋里那两个**莫名其妙的活板门**处理掉了：小桌改用石板桌面，留下的那一处活板门是储物间的菜窖口，
  四周砌了石砖框，一看就知道是通往菜窖的盖子。
- 大户四合院的**鸡架**从东厢房里搬到了院子里（原来卡在厢房的墙和火炕上）。

### 2.1.2

- 修好了小院里的**"两个建筑叠在一起"**：以前有几种院子会把灶棚整个摆进正房的墙里，
  看起来就像两栋房子长到了一起；现在灶棚、水井、柴垛、粮囤、鸡窝、狗窝、菜窖这些
  都各自有地方，不再互相压着。
- 顺带修掉几处小毛病：两个鸡窝叠在一起、柴垛堆进猪圈里、粮囤贴着苞米楼子、
  晾晒场和菜窖压在石板路上、菜园被院墙和小路穿过。
- 院子中间的小路现在会绕开菜园和房子门口，不会再从菜地里穿过去。
- 这次只动"院子模板"的摆法，**已经生成过的院子不会变**，新开的世界或新生成的小院才是修好的样子。

### 2.1.1

- 小院**不再让地面比外面高一格**：之前是原版"地形适配"把小院周围的地面削低了一圈，看起来就成了高台，现在关掉它，
  院子里外的地面是平的。
- 小院**离水面至少 15 格**才会生成，不会再贴着河、湖、海边。
- 粮仓的**梯子**重做：最下面一格离开地面一格（不再插进土里），下面垫了立柱做支撑，
  爬上去正好从门口进仓。
- 生成时用的空气会**顶掉原本的树和地形**，被切断的树冠也会一起清干净，不再有悬空的树叶。
- 门楼不再往外多伸一格屋檐，**门前不会再横着一排草方块**。
- 小院周围**不会再出现空洞**：院子底下和门口正前方都填实（往下 32 格、往外 4 格），冰也一并换成泥土。
- 冰刺平原（`ice_spikes`）从生成群系里去掉，院子不会再落在冰面上。
- 小院里的路不再比地面高一格，改成贴地的**草径**；院墙、栅栏、门柱也一起沉到地面。
- 每间房子的门口补了**半砖台阶**，进出不用跳；门楼的门槛也从矮墙改成了半砖。
- **粮仓的梯子能爬上去了**（方向和缺口都修好）。
- 院子底下填得更深更宽，往下挖不容易突然掉进洞里。
- 小院不再生成在**河流和海洋**里。
- 结构放下去时会用空气把原本的树和地形顶平，不再出现"树穿过屋顶"。
- 副食店门口的台阶方向修正、加宽到和门一样宽；每个群系的副食店都换上了自己的颜色桌布
  （平原红、热带草原橙、沙漠黄、雪原浅蓝、针叶林绿）。

### 2.1.0

- 新增**东北小院**：7 种院子（菜园院、养殖院、苞米院、大户四合院、林区木刻楞、彩钢瓦房、老树院），
  会在寒冷群系里成片生成，一片最多 20 户，越往外越稀疏。
- 新增**副食店**：5 个群系的村庄里都会长出一间小店，店里有大缸、货架和当地特产；
  村里的村民会在店里当上**副食商**，卖调料和食材。
- 寒冷群系的副食商才会卖辣白菜、酸菜、腌青萝卜、大酱。

### 2.0.0

- 开始第二阶段：村庄、结构与收纳。

### 1.0.0

- 第一阶段完成：大缸与酿造（酱油、大酱、醋、白醋、鱼露、虾酱等）、14 种东北作物、
  各种食材与调料、几十道东北菜，以及厨锅、烧烤架、机械动力工作盆的联动。

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
