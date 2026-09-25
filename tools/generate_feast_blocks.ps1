#Requires -Version 7
<#
    ⚠️ 旧版：这个脚本生成的是「把物品图标贴在盒子顶上」的占位大盆（<id>_pot_full / <id>_pot_leftovers）。
    七道大盆菜现在用的是新版生成器：tools\generate_basin_models.ps1（像素圆盆壁 + 手工摆的食材堆头
    + 专用 pot_* 小贴图 × 5 档份数）。**不要再跑这个脚本** —— 它会把 <id>_pot_servingsN.json
    和 blockstate 覆盖回旧模型。留在这里只作备查。

    给 7 道「大盆菜」生成方块资源：模型、方块状态、掉落表。

    做法照农夫乐事的 FeastBlock（牧羊人派那套）：
      两个方块状态属性 —— facing（朝向）+ servings（剩几份 0~4），没有方块实体。
      模型 = 一个「菜」的矮盒子 + 一个「大脸盆」的薄板：
        <id>_pot_full      有菜（servings 1~4 共用）
        <id>_pot_leftovers 空盆（servings 0）
      贴图先借用菜品的物品贴图（顶视图）+ 大脸盆物品贴图，属于占位；
      正式美术要出的是：菜顶面 / 菜侧面 / 盆面 / 盆底 四张 16×16 方块贴图。

    用法： pwsh -File tools\generate_feast_blocks.ps1
#>
param(
    [string]$AssetsRoot = (Join-Path $PSScriptRoot '..\src\main\resources\assets\dongbei_delight'),
    [string]$DataRoot = (Join-Path $PSScriptRoot '..\src\main\resources\data\dongbei_delight')
)

$blockStateDir = Join-Path $AssetsRoot 'blockstates'
$blockModelDir = Join-Path $AssetsRoot 'models\block'
$itemModelDir = Join-Path $AssetsRoot 'models\item'
$lootDir = Join-Path $DataRoot 'loot_table\blocks'
foreach ($dir in @($blockStateDir, $blockModelDir, $itemModelDir, $lootDir)) {
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
}

function New-FeastBlockAssets {
    param([string]$Id, [double]$FoodHeight)

    $blockId = "${Id}_pot"

    function New-FoodBox {
        param([double]$Inset, [double]$Top)
        $faces = [ordered]@{
            up    = [ordered]@{ uv = @(0, 0, 16, 16); texture = '#food_top' }
            north = [ordered]@{ uv = @(0, 0, 16, 16); texture = '#food_side' }
            south = [ordered]@{ uv = @(0, 0, 16, 16); texture = '#food_side' }
            west  = [ordered]@{ uv = @(0, 0, 16, 16); texture = '#food_side' }
            east  = [ordered]@{ uv = @(0, 0, 16, 16); texture = '#food_side' }
        }
        return [ordered]@{ from = @($Inset, 2, $Inset); to = @(16 - $Inset, $Top, 16 - $Inset); faces = $faces }
    }
    $basinFaces = [ordered]@{
        up    = [ordered]@{ uv = @(0, 0, 16, 16); texture = '#basin' }
        down  = [ordered]@{ uv = @(0, 0, 16, 16); texture = '#basin'; cullface = 'down' }
        north = [ordered]@{ uv = @(0, 12, 16, 16); texture = '#basin' }
        south = [ordered]@{ uv = @(0, 12, 16, 16); texture = '#basin' }
        west  = [ordered]@{ uv = @(0, 12, 16, 16); texture = '#basin' }
        east  = [ordered]@{ uv = @(0, 12, 16, 16); texture = '#basin' }
    }
    $basin = [ordered]@{ from = @(1, 0, 1); to = @(15, 2, 15); faces = $basinFaces }
    $basinInner = [ordered]@{
        from  = @(2, 1, 2)
        to    = @(14, 3, 14)
        faces = [ordered]@{
            up   = [ordered]@{ uv = @(2, 2, 14, 14); texture = '#basin_inner' }
            down = [ordered]@{ uv = @(2, 2, 14, 14); texture = '#basin_inner' }
            north = [ordered]@{ uv = @(2, 2, 14, 6); texture = '#basin_inner' }
            south = [ordered]@{ uv = @(2, 2, 14, 6); texture = '#basin_inner' }
            west  = [ordered]@{ uv = @(2, 2, 14, 6); texture = '#basin_inner' }
            east  = [ordered]@{ uv = @(2, 2, 14, 6); texture = '#basin_inner' }
        }
    }

    $textures = [ordered]@{
        particle    = "dongbei_delight:item/$Id"
        food_top    = "dongbei_delight:item/$Id"
        food_side   = "dongbei_delight:item/$Id"
        basin       = 'dongbei_delight:item/large_basin'
        basin_inner = 'dongbei_delight:item/large_basin'
    }

    # 5 档份数：4 = 堆得冒尖，越取越少，0 = 只剩空盆
    # heap：冒尖那一小块（只在最满的时候有）
    $stages = @(
        @{ servings = 4; inset = 2.0; top = $FoodHeight;        heap = 2.0 },
        @{ servings = 3; inset = 3.0; top = $FoodHeight - 2.0;  heap = 0.0 },
        @{ servings = 2; inset = 4.0; top = $FoodHeight - 4.0;  heap = 0.0 },
        @{ servings = 1; inset = 5.0; top = $FoodHeight - 6.0;  heap = 0.0 },
        @{ servings = 0; inset = 0.0; top = 0.0;                heap = 0.0 }
    )
    foreach ($stage in $stages) {
        $elements = @()
        if ($stage.inset -gt 0) {
            $elements += New-FoodBox -Inset $stage.inset -Top $stage.top
            if ($stage.heap -gt 0) {
                $elements += New-FoodBox -Inset ($stage.inset + 2) -Top ($stage.top + $stage.heap)
            }
            $elements += $basinInner
        }
        $elements += $basin
        $model = [ordered]@{
            parent      = 'block/block'
            render_type = 'minecraft:cutout'
            textures    = $textures
            elements    = $elements
        }
        $model | ConvertTo-Json -Depth 12 | Set-Content -Path (Join-Path $blockModelDir "${blockId}_servings$($stage.servings).json") -Encoding utf8
    }

    $leftovers = [ordered]@{
        parent      = 'block/block'
        render_type = 'minecraft:cutout'
        textures    = [ordered]@{
            particle = 'dongbei_delight:item/large_basin'
            basin    = 'dongbei_delight:item/large_basin'
        }
        elements    = @($basin)
    }
    $leftovers | ConvertTo-Json -Depth 12 | Set-Content -Path (Join-Path $blockModelDir "${blockId}_leftovers.json") -Encoding utf8

    $variants = [ordered]@{}
    foreach ($facing in @(@{ name = 'north'; y = 0 }, @{ name = 'east'; y = 90 }, @{ name = 'south'; y = 180 }, @{ name = 'west'; y = 270 })) {
        for ($servings = 0; $servings -le 4; $servings++) {
            $model = "dongbei_delight:block/${blockId}_servings$servings"
            $variant = [ordered]@{ model = $model }
            if ($facing.y -ne 0) { $variant['y'] = $facing.y }
            $variants["facing=$($facing.name),servings=$servings"] = $variant
        }
    }
    ([ordered]@{ variants = $variants }) | ConvertTo-Json -Depth 8 | Set-Content -Path (Join-Path $blockStateDir "$blockId.json") -Encoding utf8

    $itemModel = [ordered]@{
        parent   = 'item/generated'
        textures = [ordered]@{ layer0 = "dongbei_delight:item/$Id" }
    }
    $itemModel | ConvertTo-Json -Depth 5 | Set-Content -Path (Join-Path $itemModelDir "${blockId}.json") -Encoding utf8

    $pools = @()
    foreach ($servings in 1..4) {
        $pools += [ordered]@{
            rolls      = 1.0
            conditions = @([ordered]@{
                condition  = 'minecraft:block_state_property'
                block      = "dongbei_delight:$blockId"
                properties = [ordered]@{ servings = "$servings" }
            })
            entries    = @([ordered]@{ type = 'minecraft:item'; name = "dongbei_delight:$Id" })
        }
    }
    $pools += [ordered]@{
        rolls      = 1.0
        conditions = @([ordered]@{
            condition  = 'minecraft:block_state_property'
            block      = "dongbei_delight:$blockId"
            properties = [ordered]@{ servings = '0' }
        })
        entries    = @([ordered]@{ type = 'minecraft:item'; name = 'dongbei_delight:large_basin' })
    }
    $loot = [ordered]@{
        type            = 'minecraft:block'
        pools           = $pools
        random_sequence = "dongbei_delight:blocks/$blockId"
    }
    $loot | ConvertTo-Json -Depth 12 | Set-Content -Path (Join-Path $lootDir "$blockId.json") -Encoding utf8
}

New-FeastBlockAssets -Id 'da_feng_shou'         -FoodHeight 9
New-FeastBlockAssets -Id 'di_guo_ji'            -FoodHeight 8
New-FeastBlockAssets -Id 'di_guo_pai_gu'        -FoodHeight 8
New-FeastBlockAssets -Id 'sha_zhu_cai'          -FoodHeight 9
New-FeastBlockAssets -Id 'zhu_rou_dun_fen_tiao' -FoodHeight 8
New-FeastBlockAssets -Id 'suan_cai_dun_gu_tou'  -FoodHeight 9
New-FeastBlockAssets -Id 'xiao_ji_dun_mo_gu'    -FoodHeight 8

Write-Host '已生成 7 道大盆菜的方块模型 / 方块状态 / 掉落表' -ForegroundColor Green
