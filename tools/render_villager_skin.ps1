#Requires -Version 7
<#
    把一个 64×64 的村民职业皮肤渲染到村民模型上，输出预览图（不用开游戏）。

    · 模型几何取自原版 net.minecraft.client.model.VillagerModel（各部位的 texOffs / 尺寸 / 姿态，
      包括 0.51 格的头套外层、0.5 格的外套外层、-90° 转平的帽檐、向前倾 -0.75 弧度的手臂）；
    · 先画基础皮肤（type：plains / taiga / snow …），再把职业皮肤叠上去 —— 和游戏里的绘制顺序一致，
      所以没画的部分会露出基础皮肤的脸；
    · 三个视角（正面、左前 3/4、右后 3/4）并排，各面按法线做了明暗。

    用法：
      # 渲染自己的皮肤（叠在 plains 基础皮肤上）
      pwsh -File tools\render_villager_skin.ps1 -Skin src\main\resources\assets\dongbei_delight\textures\entity\villager\profession\fushi_merchant.png

      # 想先看看脚本效果：直接渲染原版职业皮肤
      pwsh -File tools\render_villager_skin.ps1 -Profession farmer
#>
param(
    [string]$Skin = '',
    [string]$Profession = '',
    [string]$Type = 'plains',
    [int]$Scale = 5,
    [string]$Out = '',
    [string]$GameVersion = '1.21.1'
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing.Common
Add-Type -AssemblyName System.IO.Compression.FileSystem

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$assetsJar = Get-ChildItem (Join-Path $projectRoot "versions\$GameVersion\build\moddev\artifacts") `
        -Filter '*client-extra*' -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $assetsJar) { throw "找不到原版资源 jar" }

function Load-FromJar([string]$entry) {
    $zip = [System.IO.Compression.ZipFile]::OpenRead($assetsJar.FullName)
    try {
        $e = $zip.Entries | Where-Object { $_.FullName -eq $entry } | Select-Object -First 1
        if (-not $e) { throw "jar 里没有 $entry" }
        $ms = New-Object System.IO.MemoryStream
        $s = $e.Open(); $s.CopyTo($ms); $s.Dispose(); $ms.Position = 0
        $bmp = [System.Drawing.Bitmap]::new([System.Drawing.Image]::FromStream($ms))
        $ms.Dispose()
        return $bmp
    } finally { $zip.Dispose() }
}

# ===== 载入贴图 =====
$typeTex = Load-FromJar "assets/minecraft/textures/entity/villager/type/$Type.png"
if ($Skin) {
    if (-not (Test-Path -LiteralPath $Skin)) { throw "找不到皮肤文件：$Skin" }
    $skinTex = [System.Drawing.Bitmap]::new((Resolve-Path -LiteralPath $Skin).Path)
    $skinName = Split-Path -Leaf $Skin
} elseif ($Profession) {
    $skinTex = Load-FromJar "assets/minecraft/textures/entity/villager/profession/$Profession.png"
    $skinName = $Profession
} else {
    throw "要指定 -Skin <png> 或 -Profession <原版职业名>"
}
if ($skinTex.Width -ne 64 -or $skinTex.Height -ne 64) { throw "皮肤必须是 64×64（现在是 $($skinTex.Width)x$($skinTex.Height)）" }

if (-not $Out) {
    $base = [System.IO.Path]::GetFileNameWithoutExtension($skinName)
    $Out = Join-Path $projectRoot "临时素材\村民皮肤预览-$base.png"
}

# ===== 模型：部位 → 方块清单 =====
# 每个方块：texOffs(u,v)、原始尺寸(w,h,d)、在部位局部坐标里的角点(x,y,z)、可选 grow（外层膨胀）
# 部位：pivot（旋转中心）+ 绕 X 轴的旋转（弧度）；模型坐标里 +y 向下
$parts = @(
    @{ pivot = @(0, 0, 0); rx = 0; boxes = @(
        @{ u = 0;  v = 0; w = 8; h = 10; d = 8; x = -4; y = -10; z = -4; grow = 0 }     # 头
        @{ u = 32; v = 0; w = 8; h = 10; d = 8; x = -4; y = -10; z = -4; grow = 0.51 }  # 头套外层
    )},
    @{ pivot = @(0, -2, 0); rx = 0; boxes = @(
        @{ u = 24; v = 0; w = 2; h = 4; d = 2; x = -1; y = -1; z = -6; grow = 0 }         # 鼻子（挂在自己的 pivot 上）
    )},
    @{ pivot = @(0, 0, 0); rx = -[Math]::PI / 2; boxes = @(
        @{ u = 30; v = 47; w = 16; h = 16; d = 1; x = -8; y = -8; z = -6; grow = 0 }      # 帽檐（转平）
    )},
    @{ pivot = @(0, 0, 0); rx = 0; boxes = @(
        @{ u = 16; v = 20; w = 8; h = 12; d = 6; x = -4; y = 0; z = -3; grow = 0 }        # 身体
        @{ u = 0;  v = 38; w = 8; h = 20; d = 6; x = -4; y = 0; z = -3; grow = 0.5 }      # 外套外层
    )},
    @{ pivot = @(0, 3, -1); rx = -0.75; boxes = @(
        @{ u = 44; v = 22; w = 4; h = 8; d = 4; x = -8; y = -2; z = -2; grow = 0 }        # 左臂
        @{ u = 44; v = 22; w = 4; h = 8; d = 4; x = 4;  y = -2; z = -2; grow = 0; mirror = $true }  # 右臂（镜像）
        @{ u = 40; v = 38; w = 8; h = 4; d = 4; x = -4; y = 2;  z = -2; grow = 0 }        # 手 / 袖口
    )},
    @{ pivot = @(-2, 12, 0); rx = 0; boxes = @(
        @{ u = 0; v = 22; w = 4; h = 12; d = 4; x = -2; y = 0; z = -2; grow = 0 }         # 右腿
    )},
    @{ pivot = @(2, 12, 0); rx = 0; boxes = @(
        @{ u = 0; v = 22; w = 4; h = 12; d = 4; x = -2; y = 0; z = -2; grow = 0; mirror = $true }  # 左腿（镜像）
    )}
)

# 每个面：纹理矩形相对 texOffs 的偏移与宽高，以及"矩形 U 轴 / V 轴"对应的模型轴向和正负
$faceDefs = @(
    @{ name = 'top';    ox = { param($b) $b.d };             oy = { param($b) 0 };       fw = { param($b) $b.w }; fh = { param($b) $b.d };
       uAxis = @(1, 0, 0); uFlip = $false; vAxis = @(0, 0, 1); vFlip = $false; normal = @(0, -1, 0) },
    @{ name = 'bottom'; ox = { param($b) $b.d + $b.w };      oy = { param($b) 0 };       fw = { param($b) $b.w }; fh = { param($b) $b.d };
       uAxis = @(1, 0, 0); uFlip = $false; vAxis = @(0, 0, 1); vFlip = $true;  normal = @(0, 1, 0) },
    @{ name = 'west';   ox = { param($b) 0 };                oy = { param($b) $b.d };     fw = { param($b) $b.d }; fh = { param($b) $b.h };
       uAxis = @(0, 0, 1); uFlip = $false; vAxis = @(0, 1, 0); vFlip = $false; normal = @(-1, 0, 0) },
    @{ name = 'east';   ox = { param($b) $b.d + $b.w };      oy = { param($b) $b.d };     fw = { param($b) $b.d }; fh = { param($b) $b.h };
       uAxis = @(0, 0, 1); uFlip = $true;  vAxis = @(0, 1, 0); vFlip = $false; normal = @(1, 0, 0) },
    @{ name = 'north';  ox = { param($b) $b.d };             oy = { param($b) $b.d };     fw = { param($b) $b.w }; fh = { param($b) $b.h };
       uAxis = @(1, 0, 0); uFlip = $false; vAxis = @(0, 1, 0); vFlip = $false; normal = @(0, 0, -1) },
    @{ name = 'south';  ox = { param($b) $b.d + $b.w + $b.d }; oy = { param($b) $b.d };    fw = { param($b) $b.w }; fh = { param($b) $b.h };
       uAxis = @(1, 0, 0); uFlip = $true;  vAxis = @(0, 1, 0); vFlip = $false; normal = @(0, 0, 1) }
)

# ===== 生成所有面（已应用部位姿态）=====
$faces = @()
foreach ($part in $parts) {
    foreach ($b in $part.boxes) {
        $grow = if ($b.grow) { $b.grow } else { 0 }
        $x0 = $b.x - $grow; $y0 = $b.y - $grow; $z0 = $b.z - $grow
        $x1 = $b.x + $b.w + $grow; $y1 = $b.y + $b.h + $grow; $z1 = $b.z + $b.d + $grow
        foreach ($f in $faceDefs) {
            $fw = & $f.fw $b; $fh = & $f.fh $b
            # 面的四个角：用 (u,v) 参数在面内走 (0,0) (1,0) (1,1) (0,1)
            # 面的原点 = 方块角点按轴向选：这里直接用"该面固定的轴 + 另两个轴的 0/1"
            # 具体：top/bottom 固定 y，west/east 固定 x，north/south 固定 z
            $corners = @()
            foreach ($uv in @(@(0, 0), @(1, 0), @(1, 1), @(0, 1))) {
                $a = $uv[0]; $c = $uv[1]
                $px = 0.0; $py = 0.0; $pz = 0.0
                switch ($f.name) {
                    'top'    { $px = $x0 + $a * ($x1 - $x0); $py = $y0; $pz = $z0 + $c * ($z1 - $z0) }
                    'bottom' { $px = $x0 + $a * ($x1 - $x0); $py = $y1; $pz = $z0 + $c * ($z1 - $z0) }
                    'west'   { $px = $x0; $py = $y0 + $c * ($y1 - $y0); $pz = $z0 + $a * ($z1 - $z0) }
                    'east'   { $px = $x1; $py = $y0 + $c * ($y1 - $y0); $pz = $z0 + $a * ($z1 - $z0) }
                    'north'  { $px = $x0 + $a * ($x1 - $x0); $py = $y0 + $c * ($y1 - $y0); $pz = $z0 }
                    'south'  { $px = $x0 + $a * ($x1 - $x0); $py = $y0 + $c * ($y1 - $y0); $pz = $z1 }
                }
                if ($part.rx -ne 0) {
                    # 绕部位 pivot 的 X 轴旋转（直接算，不走函数，省得参数展开出问题）
                    $pvx = [double]$part.pivot[0]; $pvy = [double]$part.pivot[1]; $pvz = [double]$part.pivot[2]
                    $lx = $px - $pvx; $ly = $py - $pvy; $lz = $pz - $pvz
                    $ca = [Math]::Cos([double]$part.rx); $sa = [Math]::Sin([double]$part.rx)
                    $px = $pvx + $lx
                    $py = $pvy + ($ly * $ca - $lz * $sa)
                    $pz = $pvz + ($ly * $sa + $lz * $ca)
                }
                $pt = @($px, $py, $pz)
                $corners += , $pt
            }
            $faces += [pscustomobject]@{
                corners = $corners
                u = $b.u + (& $f.ox $b)
                v = $b.v + (& $f.oy $b)
                fw = $fw
                fh = $fh
                normal = $f.normal
                name = $f.name
            }
        }
    }
}

# ===== 渲染 =====
$cos30 = [Math]::Cos([Math]::PI / 6)
$sin30 = 0.5
$views = @(0.0, -40.0, 145.0)     # 正面 / 左前 3/4 / 右后 3/4
$viewW = 220 * $Scale / 5
$viewH = 300 * $Scale / 5
$header = 34
$canvasW = [int]($viewW * $views.Count + 20)
$canvasH = [int]($viewH + $header + 20)
$bmp = [System.Drawing.Bitmap]::new($canvasW, $canvasH)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.Clear([System.Drawing.Color]::FromArgb(235, 235, 240))
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias

# 光源方向（模型坐标里 +y 向下，所以 y 取负 = 从上方照）
$lightX = 0.35; $lightY = -1.0; $lightZ = 0.55
$lightLen = [Math]::Sqrt($lightX * $lightX + $lightY * $lightY + $lightZ * $lightZ)
$lightX /= $lightLen; $lightY /= $lightLen; $lightZ /= $lightLen

$titleFont = [System.Drawing.Font]::new('Microsoft YaHei', 11, [System.Drawing.FontStyle]::Bold)
$titleBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(40, 40, 50))

for ($vi = 0; $vi -lt $views.Count; $vi++) {
    $yaw = $views[$vi] * [Math]::PI / 180
    $cy = [Math]::Cos($yaw); $sy = [Math]::Sin($yaw)

    $project = {
        param($p)
        # 先绕竖直轴（模型 y）转
        $x = $p[0] * $cy - $p[2] * $sy
        $z = $p[0] * $sy + $p[2] * $cy
        $y = $p[1]
        # 等距投影：模型坐标 +y 向下，屏幕 y 也向下 →
        #   屏幕 x = (x - z) * cos30
        #   屏幕 y = (x + z) * sin30 + y_model
        #   深度   = x + z - y_model（越大越远）
        return @((($x - $z) * $cos30), ((($x + $z) * $sin30) + $y), (($x + $z) - $y))
    }

    # 先算一遍所有面，剔除背面，按深度排序
    $drawList = @()
    foreach ($f in $faces) {
        $proj = @()
        foreach ($c in $f.corners) { $proj += , (& $project $c) }
        # 法线也转一下，用来判断朝向与明暗
        $n = $f.normal
        $nx = $n[0] * $cy - $n[2] * $sy
        $nz = $n[0] * $sy + $n[2] * $cy
        $ny = $n[1]
        $depth = ($proj[0][2] + $proj[2][2]) / 2
        $bright = 0.62 + 0.38 * [Math]::Max(0.0, ($nx * $lightX + $ny * $lightY + $nz * $lightZ))
        $drawList += [pscustomobject]@{ proj = $proj; depth = $depth; bright = $bright; f = $f }
    }
    # 全部按深度从远到近画（外层膨胀量很小，画家算法足够）
    $ordered = $drawList | Sort-Object depth -Descending

    $ox = 10 + $vi * $viewW + $viewW / 2
    $oy = $header + 120 * $Scale / 5

    foreach ($d in $ordered) {
        # 中心缩放到画布
        $pts = @()
        foreach ($p in $d.proj) {
            $pts += , ([System.Drawing.PointF]::new([single]($ox + $p[0] * $Scale), [single]($oy + $p[1] * $Scale)))
        }
        $src = [System.Drawing.RectangleF]::new($d.f.u, $d.f.v, $d.f.fw, $d.f.fh)
        $dest3 = [System.Drawing.PointF[]]@($pts[0], $pts[1], $pts[3])
        try {
            $g.DrawImage($typeTex, $dest3, $src, [System.Drawing.GraphicsUnit]::Pixel)
            $g.DrawImage($skinTex, $dest3, $src, [System.Drawing.GraphicsUnit]::Pixel)
        } catch { }
        # 明暗
        $alpha = [int]([Math]::Round(255 * (1.0 - $d.bright) * 0.85))
        if ($alpha -gt 0) {
            $shade = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb($alpha, 0, 0, 0))
            $g.FillPolygon($shade, [System.Drawing.PointF[]]@($pts[0], $pts[1], $pts[2], $pts[3]))
            $shade.Dispose()
        }
        # 描边，免得相邻面糊在一起
        $pen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(60, 0, 0, 0), 1)
        $g.DrawPolygon($pen, [System.Drawing.PointF[]]@($pts[0], $pts[1], $pts[2], $pts[3]))
        $pen.Dispose()
    }

    $label = switch ($vi) { 0 { '正面' } 1 { '左前 3/4' } default { '右后 3/4' } }
    $g.DrawString($label, $titleFont, $titleBrush, (10 + $vi * $viewW + $viewW / 2 - 30), 8)
}

$g.DrawString("皮肤：$skinName    基础皮肤：$Type", $titleFont, $titleBrush, 10, ($canvasH - 24))

$dir = Split-Path -Parent $Out
if ($dir -and -not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
$bmp.Save($Out, [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose(); $bmp.Dispose(); $typeTex.Dispose(); $skinTex.Dispose()
Write-Host "已生成预览：$Out" -ForegroundColor Green
