#Requires -Version 7
<#
    生成酱油 / 大酱的流体贴图（16x16 占位素材）：
      textures/block/soy_sauce_still.png、soy_sauce_flow.png
      textures/block/soy_paste_still.png、soy_paste_flow.png

    这些贴图只在流体储罐、管道和倒出到世界时显示。
    正式美术资源做好后，直接用同名文件覆盖即可，不需要改代码。

    用法： pwsh -File tools/generate_fluid_textures.ps1
#>
param(
    [string]$AssetsRoot = (Join-Path $PSScriptRoot '..\src\main\resources\assets\northeast_china_delight')
)

Add-Type -AssemblyName System.Drawing

$textureDir = Join-Path $AssetsRoot 'textures\block'
New-Item -ItemType Directory -Force -Path $textureDir | Out-Null

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
    # 注意：局部变量不能叫 $base / $light / $dark，
    # 否则会和 [string] 类型的参数 $Base / $Light / $Dark 视作同一个变量而被转回字符串。
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
                # 流动贴图：竖向条纹，方向感更明显
                if ((($x + [int]($y / 3)) % 5) -eq 0) { $color = $darkColor }
                elseif ((($x + 2 + [int]($y / 4)) % 7) -eq 0) { $color = $lightColor }
            }
            elseif ((($x * 3 + $y * 5) % 23) -eq 0) {
                # 静止贴图：零星高光，像液面反光
                $color = $lightColor
            }
            $bmp.SetPixel($x, $y, $color)
        }
    }

    $bmp.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Output "生成 $Path"
}

# 酱油：深褐色
New-FluidTexture -Path (Join-Path $textureDir 'soy_sauce_still.png') `
    -Base '#3A1E0E' -Light '#4E2C17' -Dark '#2A1408' -Seed 20260919
New-FluidTexture -Path (Join-Path $textureDir 'soy_sauce_flow.png') `
    -Base '#331A0C' -Light '#4A2915' -Dark '#241106' -Seed 20260920 -Flow

# 大酱：偏黄的酱褐色
New-FluidTexture -Path (Join-Path $textureDir 'soy_paste_still.png') `
    -Base '#7A4A1E' -Light '#94602C' -Dark '#5C3413' -Seed 20260921
New-FluidTexture -Path (Join-Path $textureDir 'soy_paste_flow.png') `
    -Base '#71441B' -Light '#8E5B29' -Dark '#552F10' -Seed 20260922 -Flow

# 豆浆：奶白偏米色
New-FluidTexture -Path (Join-Path $textureDir 'soy_milk_still.png') `
    -Base '#EFE6D2' -Light '#FBF6EA' -Dark '#D8CBB2' -Seed 20260923
New-FluidTexture -Path (Join-Path $textureDir 'soy_milk_flow.png') `
    -Base '#E9DFC9' -Light '#F7F1E3' -Dark '#D0C2A8' -Seed 20260924 -Flow

# 醋：深琥珀色
New-FluidTexture -Path (Join-Path $textureDir 'vinegar_still.png') `
    -Base '#6B3A16' -Light '#8A4E20' -Dark '#4A2610' -Seed 20260925
New-FluidTexture -Path (Join-Path $textureDir 'vinegar_flow.png') `
    -Base '#633414' -Light '#83481E' -Dark '#43220E' -Seed 20260926 -Flow

# 酸引水：发浑的淡黄色（泡菜卤水）
New-FluidTexture -Path (Join-Path $textureDir 'sour_water_still.png') `
    -Base '#D9D2AE' -Light '#EFEAD0' -Dark '#B8AE86' -Seed 20260927
New-FluidTexture -Path (Join-Path $textureDir 'sour_water_flow.png') `
    -Base '#D2CAA4' -Light '#E9E3C6' -Dark '#AFA57E' -Seed 20260928 -Flow

# 白醋：接近透明的浅色
New-FluidTexture -Path (Join-Path $textureDir 'white_vinegar_still.png') `
    -Base '#E8E0C8' -Light '#F7F3E6' -Dark '#CDC4A8' -Seed 20260929
New-FluidTexture -Path (Join-Path $textureDir 'white_vinegar_flow.png') `
    -Base '#E1D8BE' -Light '#F3EEDF' -Dark '#C6BC9E' -Seed 20260930 -Flow

# 鱼露：琥珀褐色
New-FluidTexture -Path (Join-Path $textureDir 'fish_sauce_still.png') `
    -Base '#6B4416' -Light '#8A5A24' -Dark '#4A2E0E' -Seed 20261001
New-FluidTexture -Path (Join-Path $textureDir 'fish_sauce_flow.png') `
    -Base '#634014' -Light '#825420' -Dark '#43290C' -Seed 20261002 -Flow

# 虾酱：偏红的酱褐色
New-FluidTexture -Path (Join-Path $textureDir 'shrimp_paste_still.png') `
    -Base '#8A4A34' -Light '#A8624A' -Dark '#663424' -Seed 20261003
New-FluidTexture -Path (Join-Path $textureDir 'shrimp_paste_flow.png') `
    -Base '#824432' -Light '#A05C46' -Dark '#5E3022' -Seed 20261004 -Flow

# 植物油：金黄色
New-FluidTexture -Path (Join-Path $textureDir 'vegetable_oil_still.png') `
    -Base '#E8C33C' -Light '#F5DC7A' -Dark '#B08A14' -Seed 20261005
New-FluidTexture -Path (Join-Path $textureDir 'vegetable_oil_flow.png') `
    -Base '#E0BA34' -Light '#F0D46E' -Dark '#A8820E' -Seed 20261006 -Flow
