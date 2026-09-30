#Requires -Version 7
<#
    生成「村民职业皮肤参考图」：把原版的职业皮肤放大、画上像素格与每个面的区域框、编号，
    方便照着画自己的职业皮肤。

    区域来自原版模型 net.minecraft.client.model.VillagerModel 的 texOffs / addBox，
    面与 UV 的换算按 Minecraft 方块模型的标准展开方式：

        设方块尺寸 (w, h, d)，texOffs 为 (u, v)，则六面分别是
          顶面  (u+d,     v)       w × d
          底面  (u+d+w,   v)       w × d
          右面  (u,       v+d)     d × h
          前面  (u+d,     v+d)     w × h
          左面  (u+d+w,   v+d)     d × h
          后面  (u+d+w+d, v+d)     w × h

    用法：
      pwsh -File tools\generate_villager_skin_reference.ps1
      pwsh -File tools\generate_villager_skin_reference.ps1 -Profession librarian -Scale 12
#>
param(
    [string]$Profession = 'farmer',
    # 想参考"基础皮肤"（plains/taiga/…）而不是职业皮肤时，加 -FromType
    [switch]$FromType,
    [int]$Scale = 10,
    [string]$Out = '',
    [string]$GameVersion = '1.21.1'
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing.Common

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
if (-not $Out) {
    $Out = Join-Path $projectRoot "临时素材\村民职业皮肤参考图.png"
}

# 原版素材在 dev 的 moddev 产物里（client-extra 那个 jar 就是解出来的原版资源）
$assetsJar = Get-ChildItem (Join-Path $projectRoot "versions\$GameVersion\build\moddev\artifacts") `
        -Filter '*client-extra*' -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $assetsJar) { throw "找不到原版资源 jar（versions\$GameVersion\build\moddev\artifacts）" }

Add-Type -AssemblyName System.IO.Compression.FileSystem
$folder = if ($FromType) { 'type' } else { 'profession' }
$entryName = "assets/minecraft/textures/entity/villager/$folder/$Profession.png"
$zip = [System.IO.Compression.ZipFile]::OpenRead($assetsJar.FullName)
try {
    $entry = $zip.Entries | Where-Object { $_.FullName -eq $entryName } | Select-Object -First 1
    if (-not $entry) { throw "jar 里没有 $entryName" }
    $ms = New-Object System.IO.MemoryStream
    $stream = $entry.Open()
    $stream.CopyTo($ms)
    $stream.Dispose()
    $ms.Position = 0
    $base = [System.Drawing.Image]::FromStream($ms)
} finally {
    $zip.Dispose()
}

# ===== 原版村民模型的各个方块（texOffs + 尺寸）=====
# 顺序 = 参考图上的编号顺序
$parts = @(
    @{ name = '头部';           u = 0;  v = 0;  w = 8;  h = 10; d = 8; color = '#E5484D'; note = '脸、后脑、两侧、头顶、下巴（材质里最大的那块）' },
    @{ name = '帽子（外层）';   u = 32; v = 0;  w = 8;  h = 10; d = 8; color = '#F76B15'; note = '罩在头上的一层：草帽、发带、头巾都画这里' },
    @{ name = '帽檐（平面）';   u = 30; v = 47; w = 16; h = 16; d = 1; color = '#FFB224'; note = '一块 16×16 的平板，模型里被转成水平，就是帽子的檐' },
    @{ name = '鼻子';           u = 24; v = 0;  w = 2;  h = 4;  d = 2; color = '#E93D82'; note = '村民标志性的大鼻子' },
    @{ name = '身体';           u = 16; v = 20; w = 8;  h = 12; d = 6; color = '#0090FF'; note = '躯干本体' },
    @{ name = '外套（外层）';   u = 0;  v = 38; w = 8;  h = 20; d = 6; color = '#00A2C7'; note = '罩在躯干外面的长袍 / 围裙，比身体高一截' },
    @{ name = '手臂';           u = 44; v = 22; w = 4;  h = 8;  d = 4; color = '#30A46C'; note = '两条手臂共用这一块 UV，右臂是镜像' },
    @{ name = '手（中间那块）'; u = 40; v = 38; w = 8;  h = 4;  d = 4; color = '#46A758'; note = '模型里夹在两条手臂之间的那一块（袖口 / 手中的东西）' },
    @{ name = '腿';             u = 0;  v = 22; w = 4;  h = 12; d = 4; color = '#8E4EC6'; note = '两条腿共用这一块 UV，左腿是镜像' }
)

$faces = @(
    @{ key = 'top';    label = '顶面'; dx = { param($p) $p.u + $p.d };          dy = { param($p) $p.v };      w = { param($p) $p.w }; h = { param($p) $p.d } },
    @{ key = 'bottom'; label = '底面'; dx = { param($p) $p.u + $p.d + $p.w };  dy = { param($p) $p.v };      w = { param($p) $p.w }; h = { param($p) $p.d } },
    @{ key = 'right';  label = '右面'; dx = { param($p) $p.u };                dy = { param($p) $p.v + $p.d }; w = { param($p) $p.d }; h = { param($p) $p.h } },
    @{ key = 'front';  label = '前面'; dx = { param($p) $p.u + $p.d };         dy = { param($p) $p.v + $p.d }; w = { param($p) $p.w }; h = { param($p) $p.h } },
    @{ key = 'left';   label = '左面'; dx = { param($p) $p.u + $p.d + $p.w };  dy = { param($p) $p.v + $p.d }; w = { param($p) $p.d }; h = { param($p) $p.h } },
    @{ key = 'back';   label = '后面'; dx = { param($p) $p.u + $p.d + $p.w + $p.d }; dy = { param($p) $p.v + $p.d }; w = { param($p) $p.w }; h = { param($p) $p.h } }
)

# 收集所有"面"区域并编号
$regions = @()
$n = 0
foreach ($p in $parts) {
    foreach ($f in $faces) {
        $n++
        $regions += [pscustomobject]@{
            index = $n
            part  = $p.name
            face  = $f.label
            x = & $f.dx $p
            y = & $f.dy $p
            w = & $f.w $p
            h = & $f.h $p
            color = $p.color
            note  = $p.note
        }
    }
}

# ===== 画图 =====
$texW = $base.Width
$texH = $base.Height
$margin = 46                      # 左侧 / 上方留给坐标尺
$pad = 16
$canvasW = $margin + $texW * $Scale + $pad
$canvasH = $margin + $texH * $Scale + $pad

$bmp = [System.Drawing.Bitmap]::new($canvasW, $canvasH)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.Clear([System.Drawing.Color]::FromArgb(28, 28, 32))
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
$g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::ClearTypeGridFit

# 先铺一层棋盘格：原版职业皮肤有大量透明像素，没棋盘格就看不出"哪些是空的"
$checkA = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 62, 62, 70))
$checkB = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 82, 82, 92))
for ($y = 0; $y -lt $texH; $y += 4) {
    for ($x = 0; $x -lt $texW; $x += 4) {
        $brush = if ((($x / 4) + ($y / 4)) % 2 -eq 0) { $checkA } else { $checkB }
        $g.FillRectangle($brush, ($margin + $x * $Scale), ($margin + $y * $Scale), (4 * $Scale), (4 * $Scale))
    }
}

$g.DrawImage($base, [System.Drawing.Rectangle]::new($margin, $margin, $texW * $Scale, $texH * $Scale))

$gridPen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(45, 255, 255, 255), 1)
$blockPen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(90, 255, 255, 255), 1)
for ($x = 0; $x -le $texW; $x++) {
    $pen = if ($x % 8 -eq 0) { $blockPen } else { $gridPen }
    $g.DrawLine($pen, $margin + $x * $Scale, $margin, $margin + $x * $Scale, $margin + $texH * $Scale)
}
for ($y = 0; $y -le $texH; $y++) {
    $pen = if ($y % 8 -eq 0) { $blockPen } else { $gridPen }
    $g.DrawLine($pen, $margin, $margin + $y * $Scale, $margin + $texW * $Scale, $margin + $y * $Scale)
}

# 坐标尺：每 8 像素标一次
$rulerFont = [System.Drawing.Font]::new('Consolas', 11, [System.Drawing.FontStyle]::Bold)
$rulerBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(200, 220, 220, 230))
for ($x = 0; $x -lt $texW; $x += 8) {
    $g.DrawString("$x", $rulerFont, $rulerBrush, ($margin + $x * $Scale + 2), ($margin - 20))
}
for ($y = 0; $y -lt $texH; $y += 8) {
    $g.DrawString("$y", $rulerFont, $rulerBrush, 4, ($margin + $y * $Scale + 2))
}

# 区域框 + 编号
$labelFont = [System.Drawing.Font]::new('Consolas', 10, [System.Drawing.FontStyle]::Bold)
foreach ($r in $regions) {
    $color = [System.Drawing.ColorTranslator]::FromHtml($r.color)
    $pen = [System.Drawing.Pen]::new($color, 2)
    $rect = [System.Drawing.Rectangle]::new(
        ($margin + $r.x * $Scale), ($margin + $r.y * $Scale), ($r.w * $Scale), ($r.h * $Scale))
    $g.DrawRectangle($pen, $rect)

    $text = "$($r.index)"
    $size = $g.MeasureString($text, $labelFont)
    $cx = $rect.X + ($rect.Width - $size.Width) / 2
    $cy = $rect.Y + ($rect.Height - $size.Height) / 2
    $bg = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(190, 0, 0, 0))
    $g.FillRectangle($bg, ($cx - 2), ($cy - 1), ($size.Width + 4), ($size.Height + 2))
    $g.DrawString($text, $labelFont, ([System.Drawing.SolidBrush]::new($color)), $cx, $cy)
    $bg.Dispose()
    $pen.Dispose()
}

$dir = Split-Path -Parent $Out
if ($dir -and -not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
$bmp.Save($Out, [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose(); $bmp.Dispose(); $base.Dispose()

Write-Host "已生成：$Out（$texW x $texH 放大 $Scale 倍）" -ForegroundColor Green
Write-Host ""
Write-Host "编号对照（坐标是原图 64x64 里的像素范围，左上角为 0,0）：" -ForegroundColor Cyan
foreach ($r in $regions) {
    "{0,3}  {1,-16} {2}  x {3,2}~{4,2}, y {5,2}~{6,2}" -f `
        $r.index, $r.part, $r.face, $r.x, ($r.x + $r.w - 1), $r.y, ($r.y + $r.h - 1)
}
