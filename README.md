# 东北乐事 (Northeast China Delight)

main 分支为 1.21.1（NeoForge）开发主线；1.20.1（Forge）版本线见 `1.20.1` 分支。

## 当前最新版本

**3.1.0**

- Minecraft 1.21.1：NeoForge 21.1
- Minecraft 1.20.1：Forge 47.1

需要[农夫乐事](https://github.com/vectorwing/FarmersDelight)与[机械动力](https://github.com/Creators-of-Create/Create)作为前置。

## 更新记录

**3.1.1**
- 修复部分大缸配方错误的bug，更改辣白菜的大缸配方
- 优化部分菜肴的贴图和模型
- 新增豆腐的厨锅配方
- 新增食材：冻豆腐，调整酸菜海鲜锅、杀猪菜的配方
- 提高大豆、花生的掉落数量
- 新增果园乐事联动食品：黄桃罐头，可用水蜜桃和糖制作，也可以从副食商处购买

**3.1.0**

- 优化大缸配方中对液体的处理
- 优化大缸液面的渲染，将使用相应流体的材质
- 修复分液器不能正确处理用过的瓶装调料的问题
- 新增食物：红豆松饼、花生松饼、打糕
- 新增方块：木臼，以及相应的打糕配方，木臼会生成在东北小院里
- 新增物品：木槌，用于在木臼里舂捣打糕
- 新增关于木臼和木槌的进度
- 红石比较器可以将大缸的制作进度输出为信号
- 新增唱片：大东北是我的家乡，并实现歌词的显示（仅1.21.1），该唱片会生成在东北小院里
- 初步实现对 ALI 等辅助模组呈现信息的适配
- JEI 中新增冻梨的信息页

**3.0.0**

- 优化部分物品与状态效果的贴图
- 调料瓶的提示信息中只显示余量
- 修复部分大缸配方的问题，统一部分配方的原料与产物的液体量
- 优化配方的 JEI 显示，合并重复的原料选项
- 为盆装菜统一更换新的大盆外观，并优化其视觉效果
- 破坏非完整状态的盆装菜，不再直接掉落完整的盆装菜掉落物
- 盆装菜方块被装完后不立刻掉落，而会变为大脸盆
- 村庄的农田中，有20%以上比例的作物，会按气候生成为本模组新作物
- 大缸、烤架、批量风干配方改为带稳定 ID 的数据包配方，支持配方重载、覆盖或禁用单条配方
- 新增关于海洋取水的配置，可在获取海水桶、带海水标记的普通水桶、普通取水三种模式中切换
- 新增关于箱子战利品表的配置，用于规定是否允许将本模组物品添加到战利品表中，以及具体的物品种类
- 新增关于村庄农田作物的配置，用于规定是否允许村庄农田生成本模组作物，以及具体的作物种类

**2.9.3**

- 现在大脸盆拥有模型，可以被放置为方块形式，可以用作头盔，提供1点护甲值
- 手持和丢出盆装菜时，显示方块模型
- 修复瓶装调料的用量问题，烹饪开始前会检查其余量，不够则不开始烹饪
- 调料瓶现在不可以被附魔
- 优化部分配方所用食材及状态效果时长
- 修复烧烤架配方的调料使用问题

**2.9.2**

- 修复“批量风干”导致其他鼓风机配方不可用的问题
- 优化战利品与副食商交易，使所有食材的获取不均依赖于机械动力模组

**2.9.1**

- 修复部分大缸配方的冲突问题
- 优化部分厨锅配方
- 优化音效和贴图

**2.9.0**

- 增加了1种东北小院的结构
- 优化了与其他乐事类模组的适配
- 调整了食用部分食物所得状态效果

## 大缸配方数据包

大缸配方 ID 取资源路径，例如 `northeast_china_delight:vat/bean_sprouts`。把文件放在数据包内同一路径即可覆盖：1.21.1 为 `data/northeast_china_delight/recipe/vat/bean_sprouts.json`，1.20.1 为 `data/northeast_china_delight/recipes/vat/bean_sprouts.json`。若要禁用，只需覆盖为：

```json
{"type":"northeast_china_delight:vat","config":{"disabled":true}}
```

修改现有配方时复制模组自带的 JSON 再编辑 `config`；本模组默认有 14 条大缸配方。1.20.1 数据包中的通用标签使用 `forge:`，1.21.1 使用 `c:`。

下面以豆芽为例展示完整格式（工序动词直接写文字，其他规则与默认配方一致）：

```json
{
  "type": "northeast_china_delight:vat",
  "config": {
    "kind": "BEAN_SPROUTS",
    "verb": "发芽",
    "priority": 20,
    "seal": "CLOTH",
    "seconds": 30,
    "liquid": {
      "water_min": 1,
      "water_max": 1,
      "water_after": "CLEAR"
    },
    "slots": [
      {
        "ingredient": {"matcher": "soybean"},
        "fate": "CONVERT",
        "bounds": {"type": "between", "min": 1, "max": 2},
        "result": "northeast_china_delight:bean_sprouts"
      }
    ],
    "biome_seconds": {"COLD": 1.5, "HOT": 0.75}
  }
}
```

`config` 中的主要字段：

| 字段 | 含义 |
| --- | --- |
| `kind` | 历史遗留的枚举，只能填已有的名字（`BEAN_SPROUTS`、`PICKLE`、`KVASS` 等），填别的会加载失败。它现在只是个标记，不决定产物；新配方借用任意一个即可。副作用是旧存档里只记了 kind 的缸，会被同 kind 里优先级最高的配方认领 |
| `verb` | 可选工序动词，用于右键进度、Jade 和 JEI；未填写时按 `kind` 使用默认动词。只是一个词，真正显示的整句由下面的句式字段决定 |
| `priority` | 可选匹配优先级，默认 `0`，越大越优先 |
| `seal` | 封口要求：`NONE`、`PRESS`、`CLOTH` 或 `CARPET`；示例的 `CLOTH` 使用 `farmersdelight:canvas_rug` |
| `seconds` | 基础加工时长，单位为秒 |
| `liquid` | 清水要求、产物液体与装瓶。`water_min`、`water_max` 是 `0–3` 层水位，每层 `1000 mB`；`water_after=CLEAR` 表示完成后清掉原来的清水。`product` 写流体 ID（如 `create:tea`），`product_mb` 是总量，上限 `3000 mB`；`product_per_layer=true` 时按水位层数倍增。旧数据包里不带冒号的枚举名（`PASTE`、`SOY_SAUCE` 等）仍可解析 |
| `slots` | 1–12 个投料条目；`ingredient` 支持 `item`、`tag`、内置 `matcher` 或任选数组；示例的 `bounds` 允许 1–2 份大豆，`CONVERT` 将每份原料转为 `result` 指定的豆芽 |
| `biome_seconds` | 可选群系时长倍率；示例在冷群系乘 `1.5`，热群系乘 `0.75`，未指定的气候类型乘 `1` |

`verb` 还支持 `{"text":"催芽"}` 或 `{"translate":"example.process.sprouting"}`；翻译键需在客户端语言资源中定义，直接写文字则不需要额外资源。默认的显示句式是中文语序（「腌制中 50%」），别的语言会拼不对，所以每处显示都可以换整句的翻译键，不写就用默认的：

| 字段 | 显示位置 | 占位符 |
| --- | --- | --- |
| `progress` | Jade 进度条，进行中 | 动词、百分比 |
| `done` | Jade 进度条，完成 | 动词 |
| `time` | JEI 的时长 | 动词、时长 |
| `hint.long` / `hint.short` | 游戏内动作条，剩余超过 / 不到一分钟 | 动词、百分比、分、秒 / 动词、百分比、秒 |

装瓶写在 `liquid.serving_container` 和 `liquid.serving_item`，两个必须成对出现：拿着前者右键，每份扣 `250 mB`，得到后者。例如 `"serving_container": "minecraft:bucket"` 配 `"serving_item": "minecraft:lava_bucket"`。都不写时按流体给默认装法：大酱用碗，酱油、醋、白醋、鱼露、虾酱、格瓦斯、酸引水用玻璃瓶，其余流体没有默认容器，只能用泵抽走。

固体处理：`KEEP` 和 `CONVERT` 的成品完成后留在缸里，空手逐份取出；`ABSORB` 和 `CONSUME` 在发酵完成时就消耗掉，`ABSORB` 可以写 `refund` 返还一个容器（辣椒酱还一个碗），没有 `refund` 的直接消失。复杂数量规则请以对应的内置配方为基础修改，不要仅修改 `kind` 来决定产物。

完成后先移除封口物。固液混合配方可分别取货，取走固体不会清掉剩余液体。JEI 会显示取货方式及对应容器，容器不算加工原料。

新增配方使用不同文件名或命名空间即可获得新 ID，例如 `data/example/recipe/vat/bean_sprouts.json`（1.20.1 使用 `recipes/`）。配方应作为世界数据包加载，而不是客户端资源包；执行 `/reload` 后重新加载规则并刷新 JEI 页面。

## 烤架配方数据包

烤架使用 `northeast_china_delight:grill` 配方类型，默认四条配方 ID 为 `northeast_china_delight:grill/grilled_chicken_frame`、`grill/grilled_corn`、`grill/grilled_oil_edge`、`grill/grilled_cold_noodles`（后三条同样使用本模组命名空间）。覆盖烤冷面时，1.21.1 文件路径为 `data/northeast_china_delight/recipe/grill/grilled_cold_noodles.json`，1.20.1 为 `data/northeast_china_delight/recipes/grill/grilled_cold_noodles.json`。禁用示例：

```json
{"type":"northeast_china_delight:grill","config":{"disabled":true}}
```

新增配方使用新的文件名或命名空间，例如 1.21.1 的 `data/example/recipe/grill/sweet_carrot.json`：

```json
{
  "type": "northeast_china_delight:grill",
  "config": {
    "priority": 0,
    "input": {"item": "minecraft:carrot"},
    "seasonings": [{"item": "minecraft:sugar"}],
    "result": {"item": "minecraft:golden_carrot", "count": 1},
    "seconds": 20
  }
}
```

`input` 和每个调料条目均支持物品、标签及任选数组；`seasonings` 可省略，最多四份，重复条目代表需要多份。瓶装调料仍需用刷子，每份消耗 10 mB。相同食材按 `priority` 从高到低、再按配方 ID 排序。配方属于服务端数据，应作为世界数据包加载，而不是客户端资源包；执行 `/reload` 后实际烤制和 JEI 都使用更新后的配方，标签候选仍在同一格滚动展示。

每份架上食材都会保存所绑定的配方 ID；规则重载后重新计时并保留仍适用的调料。已绑定配方被禁用、删除或改成不匹配的食材时停止加工，不会自动切换到另一条配方，原料仍可空手取回。无 ID 的旧烤架存档首次读取时按食材匹配并绑定。

## 批量风干配方数据包

批量风干使用 `northeast_china_delight:drying` 类型，干豆腐配方 ID 为 `northeast_china_delight:drying/dried_tofu`。1.21.1 的路径是 `data/northeast_china_delight/recipe/drying/dried_tofu.json`，1.20.1 使用 `recipes/drying/`。默认规则如下，1.20.1 将 `c:` 标签改为 `forge:`：

```json
{
  "type": "northeast_china_delight:drying",
  "config": {
    "priority": 0,
    "input": {"tag": "c:foods/soy_milk"},
    "results": [
      {"item": "northeast_china_delight:dried_tofu", "count": 1},
      {"item": "minecraft:bowl", "count": 1}
    ]
  }
}
```

支持新 ID 新增、同 ID 覆盖及 `{"type":"northeast_china_delight:drying","config":{"disabled":true}}` 禁用。原料支持物品、标签和任选数组；产物为 1–2 项，按每份原料扩展数量并自动分堆。加工时长仍由机械动力的鼓风机配置决定，不增加独立的配方时长。执行 `/reload` 后新规则用于实际风干，并刷新 1.21.1 的 JEI 页面；1.20.1 保持原有风干分类不注册的状态。

## 海洋取水配置

通用配置文件为 `config/northeast_china_delight-common.toml`，修改后重启游戏或服务端：

```toml
[sea_water]
bucket_mode = 1
```

- `1`（默认）：保留原行为，在干净的海洋水柱中取水得到海水桶。
- `2`：在相同环境取水得到带海水标记的原版水桶，可正常倒水，也能用于厨锅及烟熏炉制盐。
- `3`：关闭本模组的特殊取水，不拦截原版或其他模组的取水流程；普通水源给出未标记的水桶。

未标记的普通水桶不能制盐。标记只附在物品堆叠上，不把全部水桶加入海水标签，不改变水流方块；模式 2 的水倒到陆地后再舀起，不会自动保留海水标记。切换模式不会删除已有海水桶或标记水桶。

## 自然生成配置

在 `config/northeast_china_delight-common.toml` 中，农田与箱子分别有一个开关和一个允许列表：

| 配置项 | 默认值 / 含义 |
| --- | --- |
| `worldgen.village_farm_crops_enabled` | `true`，允许本模组作物在村庄农田生成 |
| `worldgen.village_farm_allowed_crops` | 全部 14 种农田作物的播种物品 ID |
| `chest_loot.enabled` | `true`，允许本模组物品在箱子战利品生成 |
| `chest_loot.allowed_items` | 全部 63 种现有箱子候选的物品 ID |

列表由游戏自动写入完整默认值，可直接删除不希望生成的条目；填写完整注册 ID，例如 `northeast_china_delight:soybean`、`northeast_china_delight:corn_seeds`，不要填写中文名。设为 `[]` 表示不允许生成任何本模组候选，开关设为 `false` 则直接关闭对应生成。

两项配置彼此独立；默认保持原有气候分配及概率。农田只影响新生成的村庄，不影响玩家种植和野外植物；箱子包含村庄、其他结构及东北小院，只限制本模组物品，不影响原版/其他模组物品、钓鱼和实体/方块掉落。修改后重启游戏或服务端；已有作物、已经生成的战利品和玩家库存不会被删除。

允许列表只筛选已有候选，不会自动增加新的作物分配或战利品条目；需要新增候选时仍应编辑数据包。多人游戏以服务端配置为准。

## 许可证

[MIT](LICENSE)
