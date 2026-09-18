#Requires -Version 7
<#
    为 8 种作物生成：方块状态、模型、占位贴图（4 个生长阶段）、种子贴图与模型，以及掉落表。

    贴图是开发期占位用的，正式美术直接覆盖
    assets/dongbei_delight/textures/block/<作物>_crop_stage<N>.png 即可。
    掉落规则（产量、抢夺加成）也由本脚本生成，调整产量时改下面的 PLACEHOLDER 段。

    用法： pwsh -File tools/generate_crop_files.ps1
#>
param(
    [string]$AssetsRoot = (Join-Path $PSScriptRoot '..\src\main\resources\assets\dongbei_delight'),
    [string]$DataRoot = (Join-Path $PSScriptRoot '..\src\main\resources\data\dongbei_delight')
)

Add-Type -AssemblyName System.Drawing

$blockStateDir = Join-Path $AssetsRoot 'blockstates'
$blockModelDir = Join-Path $AssetsRoot 'models\block'
$blockTextureDir = Join-Path $AssetsRoot 'textures\block'
$itemModelDir = Join-Path $AssetsRoot 'models\item'
$itemTextureDir = Join-Path $AssetsRoot 'textures\item'
$lootDir = Join-Path $DataRoot 'loot_table\blocks'

foreach ($dir in @($blockStateDir, $blockModelDir, $blockTextureDir, $itemModelDir, $itemTextureDir, $lootDir)) {
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
}

function C([string]$hex) { [System.Drawing.ColorTranslator]::FromHtml($hex) }
function Brush([string]$hex) { New-Object System.Drawing.SolidBrush (C $hex) }
function Pen([string]$hex) { New-Object System.Drawing.Pen (C $hex), 1 }

function Oval($g, $fill, $edge, $x, $y, $w, $h) {
    $b = Brush $fill; $p = Pen $edge
    $g.FillEllipse($b, $x, $y, $w, $h)
    $g.DrawEllipse($p, $x, $y, $w, $h)
    $b.Dispose(); $p.Dispose()
}

function Bar($g, $color, $x1, $y1, $x2, $y2, $width) {
    $p = New-Object System.Drawing.Pen (C $color), $width
    $p.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $p.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
    $g.DrawLine($p, $x1, $y1, $x2, $y2)
    $p.Dispose()
}

function New-Canvas {
    $bitmap = New-Object System.Drawing.Bitmap 16, 16
    $g = [System.Drawing.Graphics]::FromImage($bitmap)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
    $g.Clear([System.Drawing.Color]::Transparent)
    return @{ Bitmap = $bitmap; Graphics = $g }
}

function Draw-CropStage($g, [int]$stage, [string]$leaf, [string]$fruit) {
    switch ($stage) {
        0 {   # 嫩芽
            Bar $g $leaf 8 15 8 12 1
            Bar $g $leaf 8 13 6 12 1
            Bar $g $leaf 8 13 10 12 1
        }
        1 {   # 小苗
            Bar $g $leaf 8 15 8 9 1
            Bar $g $leaf 8 12 5 10 1
            Bar $g $leaf 8 11 11 9 1
        }
        2 {   # 长大，开始结果
            Bar $g $leaf 8 15 8 5 1
            Bar $g $leaf 8 12 4 9 1
            Bar $g $leaf 8 10 12 7 1
            Oval $g $fruit $leaf 7 5 3 4
        }
        3 {   # 成熟
            Bar $g $leaf 8 15 8 3 1
            Bar $g $leaf 8 12 3 8 1
            Bar $g $leaf 8 9 13 5 1
            Oval $g $fruit $leaf 6 3 5 6
            Oval $g $fruit $leaf 4 8 4 5
            Oval $g $fruit $leaf 9 8 4 5
        }
    }
}

function Draw-Seed($g, [string]$seedColor) {
    Oval $g $seedColor '#8A7440' 3 4 5 6
    Oval $g $seedColor '#8A7440' 8 6 5 6
    Oval $g $seedColor '#8A7440' 5 10 5 5
}

function Draw-Trellis($g) {
    # 黄瓜用的木架：两根立柱 + 两道横梁
    Bar $g '#8A6B3A' 3 2 3 15 1
    Bar $g '#8A6B3A' 12 2 12 15 1
    Bar $g '#A8854A' 3 5 12 5 1
    Bar $g '#A8854A' 3 11 12 11 1
}

function Draw-CornStalk($g, [string]$leaf, [string]$fruit) {
    # 玉米上方那一节：主茎贯穿 + 叶片 + 玉米穗
    Bar $g $leaf 8 15 8 0 1
    Bar $g $leaf 8 10 3 6 1
    Bar $g $leaf 8 7 13 3 1
    Oval $g $fruit $leaf 9 3 4 6
    Oval $g $fruit $leaf 2 9 3 5
}

function Draw-CornStalkItem($g) {
    # 物品图标：一节茎 + 两片叶子
    Bar $g '#B7A24A' 8 15 8 1 2
    Bar $g '#6BA83A' 8 11 4 8 1
    Bar $g '#6BA83A' 8 7 12 4 1
}

function Draw-WholeCabbage($g, [int]$stage, [string]$leaf, [string]$fruit) {
    # 大白菜：整棵长在地上，越成熟叶球越大
    switch ($stage) {
        0 {
            Oval $g $leaf '#3E5A24' 6 11 5 5
        }
        1 {
            Oval $g $leaf '#3E5A24' 5 9 7 7
        }
        2 {
            Oval $g $fruit '#4E6B24' 4 6 9 10
            Bar $g '#4E6B24' 8 6 8 15 1
        }
        3 {
            Oval $g $fruit '#4E6B24' 2 3 12 13
            Bar $g '#4E6B24' 8 3 8 15 1
            Bar $g '#4E6B24' 5 4 5 13 1
            Bar $g '#4E6B24' 11 4 11 13 1
        }
    }
}

# 作物定义
#   produce    收获产物
#   seed       种子 id；填 $null 表示「自身即种子」（大豆、荞麦）
#   leaf/fruit/seedColor  配色
#   trellis    是否带木架（黄瓜）
#   wholePlant 是否整棵长在地上（大白菜）
$crops = [ordered]@{
    'soybean' = @{
        produce = 'dongbei_delight:soybean'; seed = $null
        leaf = '#4E8A32'; fruit = '#E4D5A0'; seedColor = '#D8C88A'
        trellis = $false; wholePlant = $false
    }
    'eggplant' = @{
        produce = 'dongbei_delight:eggplant'; seed = 'eggplant_seeds'
        leaf = '#4E8A32'; fruit = '#6A3E9E'; seedColor = '#C9B06A'
        trellis = $false; wholePlant = $false
    }
    'green_pepper' = @{
        produce = 'dongbei_delight:green_pepper'; seed = 'green_pepper_seeds'
        leaf = '#3E7A2A'; fruit = '#4E9A3D'; seedColor = '#D8C98A'
        trellis = $false; wholePlant = $false
    }
    'corn' = @{
        produce = 'dongbei_delight:corn'; seed = 'corn_seeds'
        leaf = '#6BA83A'; fruit = '#F0C93F'; seedColor = '#E8C860'
        trellis = $false; wholePlant = $false
    }
    'green_beans' = @{
        produce = 'dongbei_delight:green_beans'; seed = 'green_beans_seeds'
        leaf = '#4E8A32'; fruit = '#5F9E3A'; seedColor = '#C9B06A'
        trellis = $false; wholePlant = $false
    }
    'buckwheat' = @{
        produce = 'dongbei_delight:buckwheat'; seed = $null
        leaf = '#5F9E3A'; fruit = '#9A6B3A'; seedColor = '#8A6B3A'
        trellis = $false; wholePlant = $false
    }
    'napa_cabbage' = @{
        produce = 'dongbei_delight:napa_cabbage'; seed = 'napa_cabbage_seeds'
        leaf = '#7A8C55'; fruit = '#DCE8C0'; seedColor = '#6A5A3A'
        trellis = $false; wholePlant = $true
    }
    'cucumber' = @{
        produce = 'dongbei_delight:cucumber'; seed = 'cucumber_seeds'
        leaf = '#3E7A2A'; fruit = '#4F9B3F'; seedColor = '#E8DCAA'
        trellis = $true; wholePlant = $false
    }
}

# 4 个视觉阶段对应的生长年龄
$stageByAge = @(0, 0, 1, 1, 2, 2, 3, 3)
$stageCount = 4

foreach ($cropId in $crops.Keys) {
    $crop = $crops[$cropId]
    $produce = $crop.produce
    $selfSeeding = $null -eq $crop.seed
    # 自身即种子的作物，掉落表里的「种子」就是产物本身
    $seedDrop = if ($selfSeeding) { $produce } else { "dongbei_delight:$($crop.seed)" }
    $blockId = "${cropId}_crop"

    # --- 作物贴图与模型 ---
    for ($stage = 0; $stage -lt $stageCount; $stage++) {
        $canvas = New-Canvas
        if ($crop.wholePlant) {
            Draw-WholeCabbage $canvas.Graphics $stage $crop.leaf $crop.fruit
        } elseif ($crop.trellis) {
            Draw-Trellis $canvas.Graphics
            Draw-CropStage $canvas.Graphics $stage $crop.leaf $crop.fruit
        } else {
            Draw-CropStage $canvas.Graphics $stage $crop.leaf $crop.fruit
        }
        $canvas.Graphics.Dispose()
        $canvas.Bitmap.Save((Join-Path $blockTextureDir "${blockId}_stage$stage.png"), [System.Drawing.Imaging.ImageFormat]::Png)
        $canvas.Bitmap.Dispose()

        $model = [ordered]@{
            parent      = 'minecraft:block/crop'
            render_type = 'minecraft:cutout'
            textures    = [ordered]@{ crop = "dongbei_delight:block/${blockId}_stage$stage" }
        }
        $model | ConvertTo-Json -Depth 5 | Set-Content -Path (Join-Path $blockModelDir "${blockId}_stage$stage.json") -Encoding utf8
    }

    # --- 方块状态 ---
    $variants = [ordered]@{}
    for ($age = 0; $age -lt 8; $age++) {
        $variants["age=$age"] = [ordered]@{ model = "dongbei_delight:block/${blockId}_stage$($stageByAge[$age])" }
    }
    ([ordered]@{ variants = $variants }) | ConvertTo-Json -Depth 8 | Set-Content -Path (Join-Path $blockStateDir "$blockId.json") -Encoding utf8

    # --- 种子贴图与模型（自身即种子的作物不需要）---
    if (-not $selfSeeding) {
        $seedId = $crop.seed
        $canvas = New-Canvas
        Draw-Seed $canvas.Graphics $crop.seedColor
        $canvas.Graphics.Dispose()
        $canvas.Bitmap.Save((Join-Path $itemTextureDir "$seedId.png"), [System.Drawing.Imaging.ImageFormat]::Png)
        $canvas.Bitmap.Dispose()

        $seedModel = [ordered]@{
            parent   = 'item/generated'
            textures = [ordered]@{ layer0 = "dongbei_delight:item/$seedId" }
        }
        $seedModel | ConvertTo-Json -Depth 5 | Set-Content -Path (Join-Path $itemModelDir "$seedId.json") -Encoding utf8
    }

    # --- 掉落表 ---
    # 成熟（age=7）掉产物 + 种子（受抢夺影响），未成熟只掉 1 个种子，与原版小麦一致
    $mature = [ordered]@{
        condition  = 'minecraft:block_state_property'
        block      = "dongbei_delight:$blockId"
        properties = [ordered]@{ age = '7' }
    }
    $loot = [ordered]@{
        type      = 'minecraft:block'
        functions = @([ordered]@{ function = 'minecraft:explosion_decay' })
        pools     = @(
            [ordered]@{
                bonus_rolls = 0.0
                rolls       = 1.0
                entries     = @(
                    [ordered]@{
                        type     = 'minecraft:alternatives'
                        children = @(
                            [ordered]@{ type = 'minecraft:item'; name = $produce; conditions = @($mature) },
                            [ordered]@{ type = 'minecraft:item'; name = $seedDrop }
                        )
                    }
                )
            },
            [ordered]@{
                bonus_rolls = 0.0
                rolls       = 1.0
                conditions  = @($mature)
                entries     = @(
                    [ordered]@{
                        type      = 'minecraft:item'
                        name      = $seedDrop
                        functions = @(
                            [ordered]@{
                                function    = 'minecraft:apply_bonus'
                                enchantment = 'minecraft:fortune'
                                formula     = 'minecraft:binomial_with_bonus_count'
                                parameters  = [ordered]@{ extra = 3; probability = 0.5714286 }
                            }
                        )
                    }
                )
            }
        )
        random_sequence = "dongbei_delight:blocks/$blockId"
    }
    $loot | ConvertTo-Json -Depth 12 | Set-Content -Path (Join-Path $lootDir "$blockId.json") -Encoding utf8

    # 玉米：成熟时额外掉落一节茎秆（可当燃料）
    if ($cropId -eq 'corn') {
        $loot.pools += [ordered]@{
            bonus_rolls = 0.0
            rolls       = 1.0
            conditions  = @($mature)
            entries     = @(
                [ordered]@{ type = 'minecraft:item'; name = 'dongbei_delight:corn_stalk' }
            )
        }
        $loot | ConvertTo-Json -Depth 12 | Set-Content -Path (Join-Path $lootDir "$blockId.json") -Encoding utf8
    }
}

# --- 玉米上方的茎秆 ---
$cornLeaf = $crops['corn'].leaf
$cornFruit = $crops['corn'].fruit

$canvas = New-Canvas
Draw-CornStalk $canvas.Graphics $cornLeaf $cornFruit
$canvas.Graphics.Dispose()
$canvas.Bitmap.Save((Join-Path $blockTextureDir 'corn_stalk.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$canvas.Bitmap.Dispose()

$stalkModel = [ordered]@{
    parent      = 'minecraft:block/crop'
    render_type = 'minecraft:cutout'
    textures    = [ordered]@{ crop = 'dongbei_delight:block/corn_stalk' }
}
$stalkModel | ConvertTo-Json -Depth 5 | Set-Content -Path (Join-Path $blockModelDir 'corn_stalk.json') -Encoding utf8

([ordered]@{ variants = [ordered]@{ '' = [ordered]@{ model = 'dongbei_delight:block/corn_stalk' } } }) |
    ConvertTo-Json -Depth 8 | Set-Content -Path (Join-Path $blockStateDir 'corn_stalk.json') -Encoding utf8

# 茎秆方块本身不掉落任何东西：它只是玉米植株的上半截，
# 玉米茎秆这个物品统一从「破坏成熟的玉米植株」获得。
$stalkLoot = [ordered]@{
    type            = 'minecraft:block'
    pools           = @()
    random_sequence = 'dongbei_delight:blocks/corn_stalk'
}
$stalkLoot | ConvertTo-Json -Depth 5 | Set-Content -Path (Join-Path $lootDir 'corn_stalk.json') -Encoding utf8

# --- 玉米茎秆的物品图标 ---
$canvas = New-Canvas
Draw-CornStalkItem $canvas.Graphics
$canvas.Graphics.Dispose()
$canvas.Bitmap.Save((Join-Path $itemTextureDir 'corn_stalk.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$canvas.Bitmap.Dispose()

$stalkItemModel = [ordered]@{
    parent   = 'item/generated'
    textures = [ordered]@{ layer0 = 'dongbei_delight:item/corn_stalk' }
}
$stalkItemModel | ConvertTo-Json -Depth 5 | Set-Content -Path (Join-Path $itemModelDir 'corn_stalk.json') -Encoding utf8

Write-Host "已为 $($crops.Count) 种作物生成方块状态 / 模型 / 贴图 / 掉落表" -ForegroundColor Green
Write-Host "  含玉米茎秆 corn_stalk" -ForegroundColor Green
Write-Host "  方块状态: $blockStateDir"
Write-Host "  方块模型: $blockModelDir"
Write-Host "  方块贴图: $blockTextureDir"
Write-Host "  物品贴图: $itemTextureDir"
Write-Host "  掉落表  : $lootDir"
