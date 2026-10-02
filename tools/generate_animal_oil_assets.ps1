#Requires -Version 7
<#
    生成动物油这一套占位素材：
      textures/block/animal_oil_still.png、animal_oil_flow.png   流体贴图（管道 / 储罐 / 倒出来）
      textures/block/animal_oil_block_top/side/bottom.png        动物油块的三个面（比蜂蜜块更白）
      blockstates/animal_oil_block.json + models/block/animal_oil_block.json

    动物油块的结构照抄原版蜂蜜块（外圈 16³ + 内圈 14³ 两层壳），
    只是换成自己的贴图，并显式写 render_type = translucent（透明渲染类型必须写在模型里，
    否则透明像素会被画成黑色）。

    正式美术做好后直接用同名文件覆盖即可，不需要改代码。

    用法： pwsh -File tools/generate_animal_oil_assets.ps1
#>
param(
    [string]$AssetsRoot = (Join-Path $PSScriptRoot '..\src\main\resources\assets\northeast_china_delight')
)

Add-Type -AssemblyName System.Drawing

$textureDir = Join-Path $AssetsRoot 'textures\block'
$blockStateDir = Join-Path $AssetsRoot 'blockstates'
$blockModelDir = Join-Path $AssetsRoot 'models\block'
foreach ($dir in @($textureDir, $blockStateDir, $blockModelDir)) {
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
}

function New-FluidTexture {
    param(
        [string]$Path,
        [string]$Base,
        [string]$Light,
        [string]$Dark,
        [int]$Seed,
        [switch]$Flow
    )

    $rand = [System.Random]::new($Seed)
    $baseColor = [System.Drawing.ColorTranslator]::FromHtml($Base)
    $lightColor = [System.Drawing.ColorTranslator]::FromHtml($Light)
    $darkColor = [System.Drawing.ColorTranslator]::FromHtml($Dark)

    $bmp = [System.Drawing.Bitmap]::new(16, 16)
    for ($y = 0; $y -lt 16; $y++) {
        for ($x = 0; $x -lt 16; $x++) {
            $color = $baseColor
            $roll = $rand.Next(100)
            if ($roll -lt 20) { $color = $lightColor }
            elseif ($roll -lt 44) { $color = $darkColor }
            if ($Flow) {
                # 流动贴图：加几条竖直的亮/暗纹
                if (($x + $y) % 7 -eq 0) { $color = $lightColor }
                elseif (($x * 2 + $y) % 11 -eq 0) { $color = $darkColor }
            }
            $bmp.SetPixel($x, $y, $color)
        }
    }
    $bmp.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

# 动物油：白色偏微黄（和 BottleColors 里的 #EBD98F 一致）
New-FluidTexture -Path (Join-Path $textureDir 'animal_oil_still.png') `
    -Base '#EBD98F' -Light '#F7EBC0' -Dark '#C9B266' -Seed 20261007
New-FluidTexture -Path (Join-Path $textureDir 'animal_oil_flow.png') `
    -Base '#E5D287' -Light '#F3E6B4' -Dark '#C0A85C' -Seed 20261008 -Flow

# 动物油块：比蜂蜜块更白一些的油膏质感（顶面亮、侧面有油痕、底面偏暗）
function New-OilBlockTexture {
    param([string]$Path, [string]$Base, [string]$Light, [string]$Dark, [int]$Seed, [switch]$Drips)

    $rand = [System.Random]::new($Seed)
    $baseColor = [System.Drawing.ColorTranslator]::FromHtml($Base)
    $lightColor = [System.Drawing.ColorTranslator]::FromHtml($Light)
    $darkColor = [System.Drawing.ColorTranslator]::FromHtml($Dark)

    $bmp = [System.Drawing.Bitmap]::new(16, 16)
    for ($y = 0; $y -lt 16; $y++) {
        for ($x = 0; $x -lt 16; $x++) {
            $color = $baseColor
            $roll = $rand.Next(100)
            if ($roll -lt 22) { $color = $lightColor }
            elseif ($roll -lt 40) { $color = $darkColor }
            if ($Drips) {
                # 侧面：隔几列画一条向下的油痕
                if ($x % 5 -eq 2 -and $y -gt 3) { $color = $lightColor }
            }
            $bmp.SetPixel($x, $y, $color)
        }
    }
    $bmp.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

New-OilBlockTexture -Path (Join-Path $textureDir 'animal_oil_block_top.png') `
    -Base '#F3E7B8' -Light '#FBF6DC' -Dark '#D6C078' -Seed 20261011
New-OilBlockTexture -Path (Join-Path $textureDir 'animal_oil_block_side.png') `
    -Base '#EFE0AA' -Light '#FAF2D2' -Dark '#CDB670' -Seed 20261012 -Drips
New-OilBlockTexture -Path (Join-Path $textureDir 'animal_oil_block_bottom.png') `
    -Base '#E4D294' -Light '#F2E7BC' -Dark '#BFA85E' -Seed 20261013

# 方块状态：单一方块，只有一种形态
([ordered]@{ variants = [ordered]@{ '' = [ordered]@{ model = 'northeast_china_delight:block/animal_oil_block' } } }) |
    ConvertTo-Json -Depth 6 | Set-Content -Path (Join-Path $blockStateDir 'animal_oil_block.json') -Encoding utf8

# 方块模型：外圈 16³ + 内圈 14³ 两层壳（照抄原版蜂蜜块的做法）
$model = [ordered]@{
    parent      = 'block/block'
    render_type = 'minecraft:translucent'
    textures    = [ordered]@{
        particle = 'northeast_china_delight:block/animal_oil_block_top'
        down     = 'northeast_china_delight:block/animal_oil_block_bottom'
        up       = 'northeast_china_delight:block/animal_oil_block_top'
        side     = 'northeast_china_delight:block/animal_oil_block_side'
    }
    elements    = @(
        [ordered]@{
            from  = @(0, 0, 0)
            to    = @(16, 16, 16)
            faces = [ordered]@{
                down  = [ordered]@{ texture = '#down'; cullface = 'down' }
                up    = [ordered]@{ texture = '#down'; cullface = 'up' }
                north = [ordered]@{ texture = '#down'; cullface = 'north' }
                south = [ordered]@{ texture = '#down'; cullface = 'south' }
                west  = [ordered]@{ texture = '#down'; cullface = 'west' }
                east  = [ordered]@{ texture = '#down'; cullface = 'east' }
            }
        },
        [ordered]@{
            from  = @(1, 1, 1)
            to    = @(15, 15, 15)
            faces = [ordered]@{
                down  = [ordered]@{ uv = @(1, 1, 15, 15); texture = '#down' }
                up    = [ordered]@{ uv = @(1, 1, 15, 15); texture = '#up' }
                north = [ordered]@{ uv = @(1, 1, 15, 15); texture = '#side' }
                south = [ordered]@{ uv = @(1, 1, 15, 15); texture = '#side' }
                west  = [ordered]@{ uv = @(1, 1, 15, 15); texture = '#side' }
                east  = [ordered]@{ uv = @(1, 1, 15, 15); texture = '#side' }
            }
        }
    )
}
$model | ConvertTo-Json -Depth 12 | Set-Content -Path (Join-Path $blockModelDir 'animal_oil_block.json') -Encoding utf8

# 方块物品：直接继承方块模型
([ordered]@{ parent = 'northeast_china_delight:block/animal_oil_block' }) |
    ConvertTo-Json -Depth 4 | Set-Content -Path (Join-Path $AssetsRoot 'models\item\animal_oil_block.json') -Encoding utf8

# 流体方块（管道里的动物油倒出来会变成这个方块）：状态 + 极简模型，和植物油那套一致
([ordered]@{ variants = [ordered]@{ '' = [ordered]@{ model = 'northeast_china_delight:block/animal_oil' } } }) |
    ConvertTo-Json -Depth 6 | Set-Content -Path (Join-Path $blockStateDir 'animal_oil.json') -Encoding utf8
([ordered]@{ textures = [ordered]@{ particle = 'northeast_china_delight:block/animal_oil_still' } }) |
    ConvertTo-Json -Depth 6 | Set-Content -Path (Join-Path $blockModelDir 'animal_oil.json') -Encoding utf8

Write-Host '已生成动物油的流体贴图、动物油块贴图与模型' -ForegroundColor Green
