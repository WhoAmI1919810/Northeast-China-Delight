#Requires -Version 7
<#
    把 VatItemShapes.java 里定义的"大缸食材小模型"离线渲染出来看效果。

    做法：从 Java 源码里把每个 return List.of(box(...)) 段抠出来 → 生成临时方块模型
    （每个盒子按 16 倍缩放到 0~16 单位）→ 用 render_model.ps1 渲染 → 拼成一张对比图。
    注意：颜色倍数（tint）在预览里不生效（预览只用物品贴图原色）。

    用法： pwsh -File tools\preview_vat_shapes.ps1 [-Out vat_shapes.png]
#>
param(
    [string]$Out = '',
    [int]$Tile = 190,
    [double]$Scale = 9
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing.Common

$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
if (-not $Out) { $Out = Join-Path $root '.tmp\vat_shapes_preview.png' }
$javaFile = Join-Path $root 'src\main\java\com\gunmu\northeast_china_delight\client\VatItemShapes.java'
$assets = Join-Path $root 'src\main\resources\assets\northeast_china_delight'
$renderer = Join-Path $root 'tools\render_model.ps1'
# 临时文件放工程内的 .tmp\（不入库）
$tmpDir = Join-Path $root '.tmp\vat_shapes'
if (Test-Path $tmpDir) { Remove-Item $tmpDir -Recurse -Force }
New-Item -ItemType Directory -Force -Path $tmpDir | Out-Null

$src = Get-Content -Raw $javaFile

# 形状名字（按源码里出现的顺序，和注释对应）
$names = @(
    '白菜/酸菜/辣白菜', '黄瓜/胡萝卜/萝卜（含腌的）', '猪肉/咸腊肉', '生鱼/咸鱼',
    '大虾', '玉米粒/荞麦/黄豆/酸玉米粒', '小麦', '豆芽', '面包', '酱块/酱渣',
    '盐', '辣椒酱/鱼露/虾酱', '水面团', '其它(默认)'
)

# 按源码里的 // ===== 分段（每个分段就是一种形状）
$marker = '// ====='
$idx0 = $src.IndexOf('static List<Box> of(')
$body0 = $src.Substring($idx0)
$parts = @()
$lines = $body0 -split "`r?`n"
$current = $null
foreach ($line in $lines) {
    if ($line.Contains($marker)) {
        if ($current) { $parts += $current }
        $current = @{ title = $line.Trim(); text = ''; }
    } elseif ($current) {
        $current.text += $line + "`n"
    }
}
if ($current) { $parts += $current }
Write-Host ("找到 {0} 种形状" -f $parts.Count) -ForegroundColor Cyan

$made = @()
$idx = 0
foreach ($part in $parts) {
    $body = $part.text
    $boxes = [regex]::Matches($body, 'box\(([^)]*)\)')
    if ($boxes.Count -eq 0) { continue }

    # 代表贴图：这一段里第一个 ModItems.X 或 Items.X
    $pre = $body
    $tex = $null
    $mm = [regex]::Match($pre, 'ModItems\.([A-Z0-9_]+)')
    if ($mm.Success) {
        $tex = 'northeast_china_delight:item/' + $mm.Groups[1].Value.ToLower()
    } else {
        $mm = [regex]::Match($pre, 'Items\.([A-Z0-9_]+)')
        if ($mm.Success) { $tex = 'minecraft:item/' + $mm.Groups[1].Value.ToLower() }
    }
    if (-not $tex) { $tex = 'minecraft:item/stone' }

    $label = ($part.title -replace '// =====', '').Trim()
    if (-not $label) { $label = if ($idx -lt $names.Count) { $names[$idx] } else { "形状$idx" } }
    $elements = @()
    foreach ($b in $boxes) {
        $args = $b.Groups[1].Value -split ',' | ForEach-Object { $_.Trim() }
        $vals = @()
        for ($i = 0; $i -lt 6; $i++) {
            $v = $args[$i] -replace 'F$', ''
            $vals += [double]$v * 16.0
        }
        $faces = [ordered]@{}
        foreach ($f in @('up', 'down', 'north', 'south', 'west', 'east')) {
            # 不给 uv：让渲染器按盒子坐标取物品图标里对应的那一小块（和游戏里一致）
            $faces[$f] = [ordered]@{ texture = $tex }
        }
        $elements += [ordered]@{ from = @($vals[0], $vals[1], $vals[2]); to = @($vals[3], $vals[4], $vals[5]); faces = $faces }
    }
    $model = [ordered]@{
        parent      = 'block/block'
        render_type = 'minecraft:cutout'
        textures    = [ordered]@{ particle = $tex }
        elements    = $elements
    }
    $jsonPath = Join-Path $tmpDir ("shape{0}.json" -f $idx)
    ($model | ConvertTo-Json -Depth 10 -Compress) | Set-Content -LiteralPath $jsonPath -Encoding UTF8
    $png = Join-Path $tmpDir ("shape{0}.png" -f $idx)
    & pwsh -NoProfile -File $renderer -Model $jsonPath -Out $png -Views '35/25' -Tile $Tile -Scale $Scale | Out-Null
    $made += @{ png = $png; label = $label; tex = $tex }
    $idx++
}

# —— 拼图 ——
$cols = 5
$rows = [int][Math]::Ceiling($made.Count / $cols)
$cellW = $Tile
$cellH = $Tile + 34
$bmp = New-Object System.Drawing.Bitmap ($cellW * $cols), ($cellH * $rows)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.Clear([System.Drawing.Color]::FromArgb(255, 26, 28, 32))
$font = New-Object System.Drawing.Font 'Microsoft YaHei', 10
$brush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 226, 230, 236))
for ($i = 0; $i -lt $made.Count; $i++) {
    $col = $i % $cols; $row = [int]($i / $cols)
    $x = $col * $cellW; $y = $row * $cellH
    $img = [System.Drawing.Bitmap]::FromFile($made[$i].png)
    $g.DrawImage($img, $x, ($y + 26), $Tile, $Tile)
    $img.Dispose()
    $g.DrawString($made[$i].label, $font, $brush, ($x + 6), ($y + 6))
}
$g.Dispose()
$bmp.Save($Out, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Host "已生成 $($made.Count) 种食材形状 -> $Out" -ForegroundColor Green
