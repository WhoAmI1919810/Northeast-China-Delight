#Requires -Version 7
<#
    把「大脸盆物品贴图」和「大盆方块模型的渲染图」放在一起比：

      面板 1：物品贴图（放大）
      面板 2：模型渲染（从物品贴图大致相同的视角：yaw 45 / pitch 25）
      面板 3：把两者按**宽度对齐**叠起来，红线圈出模型的外轮廓

    宽度对齐后高度差一眼就能看出来 —— 高度比物品贴图高，就是"太深了/像桶"。

    用法： pwsh -File tools\compare_basin.ps1 [-Model <json>] [-Icon <png>] [-Out <png>] [-Yaw 45] [-Pitch 25]
#>
param(
    [string]$Model = '',
    [string]$Icon = '',
    [string]$Out = '',
    [double]$Yaw = 45,
    [double]$Pitch = 25,
    [int]$Panel = 360
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing.Common

$tools = Split-Path -Parent $MyInvocation.MyCommand.Path
$root = Split-Path -Parent $tools
if (-not $Model) { $Model = Join-Path $root 'src\main\resources\assets\northeast_china_delight\models\block\da_feng_shou_pot_servings0.json' }
if (-not $Icon) { $Icon = Join-Path $root 'src\main\resources\assets\northeast_china_delight\textures\item\large_basin.png' }
if (-not $Out) { $Out = Join-Path $root '.tmp\basin_vs_icon.png' }
$tmpShot = Join-Path $root '.tmp\cmp_basin_render.png'

#  —— 1. 渲染模型（大画布，之后按轮廓裁剪） ——
& pwsh -NoProfile -File (Join-Path $tools 'render_model.ps1') -Model $Model -Out $tmpShot `
    -Views "$Yaw/$Pitch" -Tile 900 -Scale 46 | Out-Null

function Get-MaskInfo([System.Drawing.Bitmap]$bmp, [scriptblock]$isFg) {
    $minX = $bmp.Width; $minY = $bmp.Height; $maxX = -1; $maxY = -1
    $mask = New-Object 'bool[,]' $bmp.Width, $bmp.Height
    for ($y = 0; $y -lt $bmp.Height; $y++) {
        for ($x = 0; $x -lt $bmp.Width; $x++) {
            $p = $bmp.GetPixel($x, $y)
            if (& $isFg $p) {
                $mask[$x, $y] = $true
                if ($x -lt $minX) { $minX = $x }; if ($x -gt $maxX) { $maxX = $x }
                if ($y -lt $minY) { $minY = $y }; if ($y -gt $maxY) { $maxY = $y }
            }
        }
    }
    return @{ mask = $mask; x = $minX; y = $minY; w = ($maxX - $minX + 1); h = ($maxY - $minY + 1) }
}

$bmpIcon = [System.Drawing.Bitmap]::FromFile((Resolve-Path $Icon).Path)
$bmpRendFull = [System.Drawing.Bitmap]::FromFile((Resolve-Path $tmpShot).Path)
# 渲染图上面有一条标题栏（26 px），裁掉，只留方块那一格
$bmpRend = New-Object System.Drawing.Bitmap 900, 900
$gcut = [System.Drawing.Graphics]::FromImage($bmpRend)
$gcut.DrawImage($bmpRendFull, (New-Object System.Drawing.Rectangle 0, 0, 900, 900),
    (New-Object System.Drawing.Rectangle 0, 26, 900, 900), [System.Drawing.GraphicsUnit]::Pixel)
$gcut.Dispose()
$bmpRendFull.Dispose()

$infoIcon = Get-MaskInfo $bmpIcon { param($p) $p.A -gt 8 }
# 渲染图的背景是浅灰棋盘格 (150,160,172)/(164,174,186)，其余算模型
$infoRend = Get-MaskInfo $bmpRend {
    param($p)
    -not (($p.R -eq 150 -and $p.G -eq 160 -and $p.B -eq 172) -or ($p.R -eq 164 -and $p.G -eq 174 -and $p.B -eq 186))
}
Write-Host ("物品贴图里的盆：{0}x{1}" -f $infoIcon.w, $infoIcon.h) -ForegroundColor DarkGray
Write-Host ("模型渲染的盆：  {0}x{1}（高宽比 {2:N2} vs 贴图 {3:N2}）" -f `
        $infoRend.w, $infoRend.h, ($infoRend.h / $infoRend.w), ($infoIcon.h / $infoIcon.w)) -ForegroundColor DarkGray

#  —— 2. 把两边都按宽度缩放到 Panel 宽 ——
function New-Scaled([System.Drawing.Bitmap]$bmp, $info, [int]$targetW, [int]$targetH) {
    $dst = New-Object System.Drawing.Bitmap $targetW, $targetH
    $g = [System.Drawing.Graphics]::FromImage($dst)
    $g.Clear([System.Drawing.Color]::FromArgb(0, 0, 0, 0))
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
    $src = New-Object System.Drawing.Rectangle $info.x, $info.y, $info.w, $info.h
    $dstRect = New-Object System.Drawing.Rectangle 0, 0, $targetW, $targetH
    $g.DrawImage($bmp, $dstRect, $src, [System.Drawing.GraphicsUnit]::Pixel)
    $g.Dispose()
    return $dst
}

$hIcon = [int][Math]::Round($Panel * $infoIcon.h / $infoIcon.w)
$hRend = [int][Math]::Round($Panel * $infoRend.h / $infoRend.w)

$imgIcon = New-Scaled $bmpIcon $infoIcon $Panel $hIcon
$imgRend = New-Scaled $bmpRend $infoRend $Panel $hRend

$canvasW = $Panel * 3 + 40
$canvasH = [Math]::Max([Math]::Max($hIcon, $hRend), $Panel) + 40
$canvas = New-Object System.Drawing.Bitmap $canvasW, $canvasH
$g = [System.Drawing.Graphics]::FromImage($canvas)
$g.Clear([System.Drawing.Color]::FromArgb(255, 30, 32, 38))
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAlias
$font = New-Object System.Drawing.Font 'Segoe UI', 11
$brush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 225, 228, 234))
$labelY = 6
$imgY = 30

$g.DrawString('大脸盆物品贴图', $font, $brush, 10, $labelY)
$g.DrawImage($imgIcon, 0, $imgY)
$g.DrawString(('模型 yaw {0} / pitch {1}' -f $Yaw, $Pitch), $font, $brush, ($Panel + 20), $labelY)
$g.DrawImage($imgRend, ($Panel + 20), $imgY)
$g.DrawString(('叠加（红线=模型轮廓）'), $font, $brush, ($Panel * 2 + 30), $labelY)
$g.DrawImage($imgIcon, ($Panel * 2 + 30), $imgY)

#  —— 3. 叠加上模型轮廓（红线） ——
$pen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(255, 220, 40, 40)), 2
$inner = New-Object System.Drawing.Bitmap $Panel, $hRend
$gi = [System.Drawing.Graphics]::FromImage($inner)
$gi.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$gi.DrawImage($imgRend, 0, 0)
$gi.Dispose()
$bg = [System.Drawing.Color]::FromArgb(0, 0, 0, 0)
for ($y = 0; $y -lt $hRend; $y++) {
    for ($x = 0; $x -lt $Panel; $x++) {
        $p = $inner.GetPixel($x, $y)
        if ($p.A -le 8) { continue }
        $edge = $false
        foreach ($d in @(@(1, 0), @(-1, 0), @(0, 1), @(0, -1))) {
            $nx = $x + $d[0]; $ny = $y + $d[1]
            if ($nx -lt 0 -or $ny -lt 0 -or $nx -ge $Panel -or $ny -ge $hRend) { $edge = $true; continue }
            if ($inner.GetPixel($nx, $ny).A -le 8) { $edge = $true }
        }
        if ($edge) { $g.DrawRectangle($pen, ($Panel * 2 + 30 + $x), ($imgY + $y), 1, 1) }
    }
}
$inner.Dispose()
$pen.Dispose(); $font.Dispose(); $brush.Dispose()
$g.Dispose()
$canvas.Save($Out, [System.Drawing.Imaging.ImageFormat]::Png)
$canvas.Dispose(); $imgIcon.Dispose(); $imgRend.Dispose(); $bmpIcon.Dispose(); $bmpRend.Dispose()
Write-Host "对比图已生成 -> $Out" -ForegroundColor Green
