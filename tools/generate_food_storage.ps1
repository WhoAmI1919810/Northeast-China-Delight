#Requires -Version 7
<#
    生成「方块」—— 箱装（蔬菜/腌菜 9 个压 1 箱）与袋装（谷物/山珍 9 个装 1 袋）。

    素材来源：临时素材\食物方块.zip（先解压成 <SourceRoot>\方块\箱装\*.png 与 方块\袋装\*.png）
    产出：
      · 贴图   assets\dongbei_delight\textures\block\<id>_side.png / <id>_top.png 等
      · 方块状态 / 方块模型 / 物品模型 / 掉落表 / 配方（9→1 与 1→9）

    代码侧的注册（ModBlocks.CRATES / SACKS、ModItems 的方块物品与第三个创造模式物品栏）
    与语言文件是手写的，不在这里生成。

    用法：
      pwsh -File tools\generate_food_storage.ps1
      pwsh -File tools\generate_food_storage.ps1 -SourceRoot E:\codex\dd_tmp\foodblocks
#>
param(
    [string]$SourceRoot = 'E:\codex\dd_tmp\foodblocks'
)

$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$assets = Join-Path $projectRoot 'src\main\resources\assets\dongbei_delight'
$data = Join-Path $projectRoot 'src\main\resources\data\dongbei_delight'
$texDir = Join-Path $assets 'textures\block'
$blockStateDir = Join-Path $assets 'blockstates'
$blockModelDir = Join-Path $assets 'models\block'
$itemModelDir = Join-Path $assets 'models\item'
$lootDir = Join-Path $data 'loot_table\blocks'
$recipeDir = Join-Path $data 'recipe'

foreach ($dir in @($texDir, $blockStateDir, $blockModelDir, $itemModelDir, $lootDir, $recipeDir)) {
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
}

# 箱装：id / 中文名（对应 zip 里的贴图名）/ 装的是什么物品
$crates = @(
    @{ id = 'napa_cabbage_crate';         cn = '大白菜';   item = 'napa_cabbage' },
    @{ id = 'cucumber_crate';             cn = '黄瓜';     item = 'cucumber' },
    @{ id = 'green_radish_crate';         cn = '青萝卜';   item = 'green_radish' },
    @{ id = 'green_onion_crate';          cn = '大葱';     item = 'green_onion' },
    @{ id = 'eggplant_crate';             cn = '茄子';     item = 'eggplant' },
    @{ id = 'green_pepper_crate';         cn = '青椒';     item = 'green_pepper' },
    @{ id = 'red_chili_crate';            cn = '辣椒';     item = 'red_chili' },
    @{ id = 'green_beans_crate';          cn = '豆角';     item = 'green_beans' },
    @{ id = 'corn_crate';                 cn = '玉米';     item = 'corn' },
    @{ id = 'sweet_potato_crate';         cn = '地瓜';     item = 'sweet_potato' },
    @{ id = 'sour_cabbage_crate';         cn = '酸菜';     item = 'sour_cabbage' },
    @{ id = 'spicy_cabbage_crate';        cn = '辣白菜';   item = 'spicy_cabbage' },
    @{ id = 'pickled_cucumber_crate';     cn = '酸黄瓜';   item = 'pickled_cucumber' },
    @{ id = 'pickled_carrot_crate';       cn = '腌胡萝卜'; item = 'pickled_carrot' },
    @{ id = 'pickled_green_radish_crate'; cn = '腌青萝卜'; item = 'pickled_green_radish' }
)

# 袋装：id / 中文名（对应 zip 里的贴图名）/ 装的是什么物品
$sacks = @(
    @{ id = 'corn_seeds_sack';     cn = '玉米粒'; item = 'corn_seeds' },
    @{ id = 'buckwheat_sack';      cn = '荞麦';   item = 'buckwheat' },
    @{ id = 'soybean_sack';        cn = '大豆';   item = 'soybean' },
    @{ id = 'red_bean_sack';       cn = '红豆';   item = 'red_bean' },
    @{ id = 'peanut_sack';         cn = '花生';   item = 'peanut' },
    @{ id = 'hazelnut_sack';       cn = '榛子';   item = 'hazelnut' },
    @{ id = 'hazel_mushroom_sack'; cn = '榛蘑';   item = 'hazel_mushroom' },
    @{ id = 'wood_ear_sack';       cn = '木耳';   item = 'wood_ear' },
    @{ id = 'ginseng_sack';        cn = '人参';   item = 'ginseng' }
)

function Write-Json([string]$path, [string]$json) {
    [System.IO.File]::WriteAllText($path, $json.Trim() + "`n", (New-Object System.Text.UTF8Encoding($false)))
}

function Copy-Texture([string]$from, [string]$toName) {
    if (-not (Test-Path -LiteralPath $from)) { throw "找不到素材：$from" }
    Copy-Item -LiteralPath $from -Destination (Join-Path $texDir $toName) -Force
}

# 共用的底面 / 侧面
Copy-Texture (Join-Path $SourceRoot '方块\箱装\箱子底面.png') 'crate_bottom.png'
Copy-Texture (Join-Path $SourceRoot '方块\袋装\麻袋侧面.png') 'sack_side.png'
Copy-Texture (Join-Path $SourceRoot '方块\袋装\麻袋侧面（带绳子）.png') 'sack_side_rope.png'
Copy-Texture (Join-Path $SourceRoot '方块\袋装\麻袋底面.png') 'sack_bottom.png'

$written = 0

# ===== 箱装 =====
foreach ($c in $crates) {
    $id = $c.id
    $cn = $c.cn
    Copy-Texture (Join-Path $SourceRoot "方块\箱装\${cn}侧面.png") "${id}_side.png"
    Copy-Texture (Join-Path $SourceRoot "方块\箱装\${cn}顶面.png") "${id}_top.png"

    Write-Json (Join-Path $blockStateDir "$id.json") @"
{
  "variants": {
    "": { "model": "dongbei_delight:block/$id" }
  }
}
"@

    Write-Json (Join-Path $blockModelDir "$id.json") @"
{
  "parent": "minecraft:block/cube_bottom_top",
  "textures": {
    "top": "dongbei_delight:block/${id}_top",
    "bottom": "dongbei_delight:block/crate_bottom",
    "side": "dongbei_delight:block/${id}_side"
  }
}
"@

    Write-Json (Join-Path $itemModelDir "$id.json") @"
{
  "parent": "dongbei_delight:block/$id"
}
"@

    Write-Json (Join-Path $lootDir "$id.json") @"
{
  "type": "minecraft:block",
  "pools": [
    {
      "rolls": 1,
      "bonus_rolls": 0,
      "entries": [ { "type": "minecraft:item", "name": "dongbei_delight:$id" } ],
      "conditions": [ { "condition": "minecraft:survives_explosion" } ]
    }
  ]
}
"@

    Write-Json (Join-Path $recipeDir "$id.json") @"
{
  "type": "minecraft:crafting_shaped",
  "category": "building",
  "pattern": [ "XXX", "XXX", "XXX" ],
  "key": { "X": { "item": "dongbei_delight:$($c.item)" } },
  "result": { "count": 1, "id": "dongbei_delight:$id" }
}
"@

    Write-Json (Join-Path $recipeDir "${id}_unpack.json") @"
{
  "type": "minecraft:crafting_shapeless",
  "category": "misc",
  "ingredients": [ { "item": "dongbei_delight:$id" } ],
  "result": { "count": 9, "id": "dongbei_delight:$($c.item)" }
}
"@
    $written += 6
}

# ===== 袋装 =====
foreach ($s in $sacks) {
    $id = $s.id
    $cn = $s.cn
    Copy-Texture (Join-Path $SourceRoot "方块\袋装\${cn}.png") "${id}_top.png"

    Write-Json (Join-Path $blockStateDir "$id.json") @"
{
  "variants": {
    "": { "model": "dongbei_delight:block/$id" }
  }
}
"@

    # 麻袋：底面 + 侧面，北面是带绳子的那一面，顶面放对应食材（袋口露出内容物）
    Write-Json (Join-Path $blockModelDir "$id.json") @"
{
  "parent": "minecraft:block/block",
  "textures": {
    "particle": "dongbei_delight:block/sack_side",
    "top": "dongbei_delight:block/${id}_top",
    "bottom": "dongbei_delight:block/sack_bottom",
    "side": "dongbei_delight:block/sack_side",
    "rope": "dongbei_delight:block/sack_side_rope"
  },
  "elements": [
    {
      "from": [0, 0, 0],
      "to": [16, 16, 16],
      "faces": {
        "up": { "texture": "#top" },
        "down": { "texture": "#bottom" },
        "north": { "texture": "#rope" },
        "south": { "texture": "#side" },
        "west": { "texture": "#side" },
        "east": { "texture": "#side" }
      }
    }
  ]
}
"@

    Write-Json (Join-Path $itemModelDir "$id.json") @"
{
  "parent": "dongbei_delight:block/$id"
}
"@

    Write-Json (Join-Path $lootDir "$id.json") @"
{
  "type": "minecraft:block",
  "pools": [
    {
      "rolls": 1,
      "bonus_rolls": 0,
      "entries": [ { "type": "minecraft:item", "name": "dongbei_delight:$id" } ],
      "conditions": [ { "condition": "minecraft:survives_explosion" } ]
    }
  ]
}
"@

    Write-Json (Join-Path $recipeDir "$id.json") @"
{
  "type": "minecraft:crafting_shaped",
  "category": "building",
  "pattern": [ "XXX", "XXX", "XXX" ],
  "key": { "X": { "item": "dongbei_delight:$($s.item)" } },
  "result": { "count": 1, "id": "dongbei_delight:$id" }
}
"@

    Write-Json (Join-Path $recipeDir "${id}_unpack.json") @"
{
  "type": "minecraft:crafting_shapeless",
  "category": "misc",
  "ingredients": [ { "item": "dongbei_delight:$id" } ],
  "result": { "count": 9, "id": "dongbei_delight:$($s.item)" }
}
"@
    $written += 6
}

Write-Host ("箱装 {0} 个、袋装 {1} 个；写出 JSON {2} 份，贴图已复制到 textures\block" -f `
    $crates.Count, $sacks.Count, $written) -ForegroundColor Green

# ===== 后来单独替换过的素材 =====
# 有些贴图是事后在 临时素材\ 根目录下单独给的（文件名和 zip 里的不一样）。
# 以后再有零散替换，往这张表里加一行「源文件名 -> 目标贴图名」就行；这里放在最后，
# 所以会覆盖前面从 zip 复制过来的那几张。
$replacements = @(
    @{ src = '青萝卜侧面.png';   to = 'green_radish_crate_side.png' },
    @{ src = '青萝卜顶面.png';   to = 'green_radish_crate_top.png' },
    @{ src = '腌青萝卜侧面.png'; to = 'pickled_green_radish_crate_side.png' },
    @{ src = '腌青萝卜顶部.png'; to = 'pickled_green_radish_crate_top.png' },
    @{ src = '大葱侧面.png';     to = 'green_onion_crate_side.png' },
    @{ src = '大葱顶部.png';     to = 'green_onion_crate_top.png' }
)
foreach ($r in $replacements) {
    $from = Join-Path $projectRoot "临时素材\$($r.src)"
    if (Test-Path -LiteralPath $from) {
        Copy-Item -LiteralPath $from -Destination (Join-Path $texDir $r.to) -Force
        Write-Host "  替换素材：$($r.src) -> $($r.to)" -ForegroundColor DarkGray
    }
}
