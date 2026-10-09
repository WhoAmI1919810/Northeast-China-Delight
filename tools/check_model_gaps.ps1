#Requires -Version 7
<#
    检查方块模型"从正上方看有没有透光"（有没有漏缝）。

    做法：用 render_model.ps1 从正上方（pitch 90）离屏渲染，再按模型格子采样，
    凡是采到背景色的格子就是"从这里能看穿"的位置，会打印成字符画（# = 有东西，. = 漏缝）。

    用法： pwsh -File tools\check_model_gaps.ps1 -Model <模型json路径> [-Scale 24]
#>
param(
    [Parameter(Mandatory = $true)][string]$Model,
    [int]$Scale = 24,
    [string]$Out = ''
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing.Common

$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
if (-not $Out) { $Out = Join-Path $root '.tmp\gap_check.png' }

& pwsh -NoProfile -File (Join-Path $root 'tools\render_model.ps1') -Model $Model -Out $Out `
    -Views '0/89.5' -Tile (16 * $Scale + 2 * $Scale) -Scale $Scale | Out-Null

$full = [System.Drawing.Bitmap]::FromFile($Out)
# 渲染图上面有 26 像素的标题条，裁掉，只留方块那一格
$tile = 16 * $Scale + 2 * $Scale
$bmp = New-Object System.Drawing.Bitmap $tile, $tile
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.DrawImage($full, (New-Object System.Drawing.Rectangle 0, 0, $tile, $tile),
    (New-Object System.Drawing.Rectangle 0, 26, $tile, $tile), [System.Drawing.GraphicsUnit]::Pixel)
$g.Dispose()
$full.Dispose()
$bg1 = @(150, 160, 172)
$bg2 = @(164, 174, 186)
function Test-Bg($p) {
    return (($p.R -eq $bg1[0] -and $p.G -eq $bg1[1] -and $p.B -eq $bg1[2]) -or
            ($p.R -eq $bg2[0] -and $p.G -eq $bg2[1] -and $p.B -eq $bg2[2]))
}

# 找模型在画面里的范围
$minX = $bmp.Width; $maxX = -1; $minY = $bmp.Height; $maxY = -1
for ($y = 0; $y -lt $bmp.Height; $y++) {
    for ($x = 0; $x -lt $bmp.Width; $x++) {
        if (-not (Test-Bg $bmp.GetPixel($x, $y))) {
            if ($x -lt $minX) { $minX = $x }; if ($x -gt $maxX) { $maxX = $x }
            if ($y -lt $minY) { $minY = $y }; if ($y -gt $maxY) { $maxY = $y }
        }
    }
}
if ($maxX -lt 0) { Write-Warning '整个画面都是背景，模型没渲染出来'; $bmp.Dispose(); return }

$w = $maxX - $minX + 1
$h = $maxY - $minY + 1
Write-Host ("模型占 {0}x{1} 像素（1 模型像素 ≈ {2} 像素）" -f $w, $h, ($w / 16.0)) -ForegroundColor DarkGray

$step = $w / 16.0
$holes = 0
for ($gz = 0; $gz -lt 16; $gz++) {
    $line = ''
    for ($gx = 0; $gx -lt 16; $gx++) {
        $px = $minX + [int](($gx + 0.5) * $step)
        $py = $minY + [int](($gz + 0.5) * ($h / 16.0))
        if ($px -gt $maxX) { $px = $maxX }
        if ($py -gt $maxY) { $py = $maxY }
        if (Test-Bg $bmp.GetPixel($px, $py)) { $line += '.'; $holes++ } else { $line += '#' }
    }
    Write-Host "  $line"
}
$bmp.Dispose()
if ($holes -eq 0) {
    Write-Host '俯视没有透光：模型是封闭的 ✅' -ForegroundColor Green
} else {
    Write-Host ("俯视有 {0} 格能看穿（'.'）" -f $holes) -ForegroundColor Yellow
}
