#Requires -Version 7
<#
    大盆（7 道大盆菜）方块模型生成器 —— **像素圆**版本。

    做法：
      1. 先按直径算出 mc 风格的"像素圆"（格子圆心落在圆内即算），每层盆壁就是这个像素圆的**一圈**；
         一层层往上叠、半径逐层变大，得到像素阶梯状的盆壁（不再用旋转面片，精度和 mc 一致）。
      2. 盆身贴图按国民脸盆画：奶白盆身 + 盆口红边 + 红花绿叶（横向平铺，绕盆一周重复若干次）。
      3. 盆里做出"真的肉、菜、贴饼"：一层汤面 + 肉块 / 菜块 / 豆腐块 + 贴着盆壁的贴饼子。

    用法：
      pwsh -File tools\generate_basin_models.ps1                 # 生成 7 道菜 × 5 个份数
      pwsh -File tools\generate_basin_models.ps1 -Only da_feng_shou
      pwsh -File tools\generate_basin_models.ps1 -RefreshBasinTextures   # 顺手重画盆体占位贴图
#>
param(
    [string]$Root = 'H:\IdeaProjects\dongbei_delight1.21.1',
    [string]$Only = '',
    [switch]$SkipTextures,
    [switch]$RefreshBasinTextures
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing.Common

$assets = Join-Path $Root 'src\main\resources\assets\dongbei_delight'
$modelsDir = Join-Path $assets 'models\block'
$blockTexDir = Join-Path $assets 'textures\block'
$itemTexDir = Join-Path $assets 'textures\item'

$dishes = @(
    @{ id = 'da_feng_shou';           item = 'da_feng_shou';           salt = 11 }
    @{ id = 'di_guo_ji';              item = 'di_guo_ji';              salt = 23 }
    @{ id = 'di_guo_pai_gu';          item = 'di_guo_pai_gu';          salt = 37 }
    @{ id = 'sha_zhu_cai';            item = 'sha_zhu_cai';            salt = 53 }
    @{ id = 'zhu_rou_dun_fen_tiao';   item = 'zhu_rou_dun_fen_tiao';   salt = 71 }
    @{ id = 'suan_cai_dun_gu_tou';    item = 'suan_cai_dun_gu_tou';    salt = 89 }
    @{ id = 'xiao_ji_dun_mo_gu';      item = 'xiao_ji_dun_mo_gu';      salt = 97 }
    @{ id = 'suan_cai_hai_xian_guo';  item = 'suan_cai_hai_xian_guo';  salt = 101 }
)

# ---------- 盆的尺寸（单位：模型像素，1 = 1/16 格；圆心在格子角 (8,8)） ----------
$CX = 8.0
$CZ = 8.0
# 去漏斗版（2026-09-24 用户要求）：去掉原来最下面两层半径很小的盆壁
# （4.6 / 5.6 那两圈就是看着像漏斗的部分），剩下的整体下移 2 像素，
# 盆底 / 盆脚按新盆壁内径补宽，免得盆底外圈露出一圈缝。
$Y_FOOT = 0        # 盆脚（1 像素高）
$Y_FLOOR = 1       # 盆底（1 像素厚）
$Y_RIM = 5         # 盆沿所在层（原 7，下移 2）
$Y_TOP = 6         # 盆口顶面高度（原 8，下移 2）
$R_FOOT = 5.4
$R_FLOOR = 5.4
$WALL_LAYER = @{ 2 = 6.35; 3 = 6.9; 4 = 7.2 }
$R_RIM = 7.2
# 盆壁 / 盆沿只做 **1 像素** 厚：2 像素时盆口那一圈看着又厚又笨（用户原话：像马桶）。
# 1 像素的时候外侧还是花纹、内侧还是素白（贴图按面朝向分，和厚度无关），
# 但盆口只剩薄薄一圈红边，才像搪瓷脸盆。
$WALL_THICK = 1
$RIM_THICK = 1

# 菜的液面高度（格子 y）：4 份最满，1 份最浅（留 1 像素盆沿边，好看清盆身花纹）
$FILL_Y = @{ 4 = 3; 3 = 2; 2 = 1; 1 = 0 }   # 整体下移 2 像素

#  每一层"盆内净空"的半径：盆壁那一圈的内边界。食材只能摆在这个半径以内，
#  否则会嵌进盆壁里（把盆壁的贴图戳出个洞）。
$INNER = @{}
foreach ($k in $WALL_LAYER.Keys) { $INNER[[int]$k] = ([double]$WALL_LAYER[$k]) - $WALL_THICK }
$INNER[$Y_RIM] = $R_RIM - $RIM_THICK
$INNER[$Y_FOOT] = $R_FLOOR - $WALL_THICK      # 盆脚那一层（水位最低时食材落在这里）
$INNER[$Y_FLOOR] = $R_FLOOR - $WALL_THICK
# 盆口那一层往上：食材可以堆出盆沿（大盆菜本来就是堆得冒尖的），宽度限制在盆口内径以内
$INNER[$Y_TOP] = $R_RIM - $RIM_THICK

function Test-CellInside([int]$y, [int]$cellX, [int]$cellZ) {
    if (-not $INNER.ContainsKey($y)) { return $false }
    $dx = $cellX + 0.5 - $CX
    $dz = $cellZ + 0.5 - $CZ
    return ([Math]::Sqrt($dx * $dx + $dz * $dz) -le ([double]$INNER[$y] - 0.12))
}

# ---------- 像素圆 ----------
# 返回每行 z 上的格子跨度： j -> @(iLo, iHi)，格子中心落在半径 r 的圆内即算
function Get-DiscSpans([double]$r) {
    $spans = @{}
    for ($j = 0; $j -lt 16; $j++) {
        $dz = $j + 0.5 - $CZ
        $t = $r * $r - $dz * $dz
        if ($t -lt 0) { continue }
        $dxMax = [Math]::Sqrt($t)
        $iLo = [int][Math]::Ceiling($CX - $dxMax - 0.5)
        $iHi = [int][Math]::Floor($CX + $dxMax - 0.5)
        if ($iHi -lt $iLo) { continue }
        $spans[$j] = @($iLo, $iHi)
    }
    return $spans
}

#  把格子塞进体素表（key = "x,y,z"）
$script:VOX = @{}
function Set-Vox([int]$x, [int]$y, [int]$z, [string]$mat) {
    $script:VOX["$x,$y,$z"] = $mat
}
function Add-Disc([int]$y, [double]$r, [string]$mat) {
    foreach ($kv in (Get-DiscSpans $r).GetEnumerator()) {
        $j = [int]$kv.Key
        for ($i = $kv.Value[0]; $i -le $kv.Value[1]; $i++) { Set-Vox $i $y $j $mat }
    }
}
function Add-Ring([int]$y, [double]$r, [int]$thick, [string]$mat) {
    # 按"到圆心的距离"取环：半径落在 [r-厚, r] 之间的格子都算。
    # 不能写成"每行取最外侧两格" —— 那样在前/后（圆很扁的那几行）会把中间格子漏掉，
    # 盆口就会在正前、正后各缺一小块（用户看到的"盆口没封闭"）。
    $inner = $r - $thick
    foreach ($kv in (Get-DiscSpans $r).GetEnumerator()) {
        $j = [int]$kv.Key
        for ($i = $kv.Value[0]; $i -le $kv.Value[1]; $i++) {
            $dx = $i + 0.5 - $CX
            $dz = $j + 0.5 - $CZ
            if ([Math]::Sqrt($dx * $dx + $dz * $dz) -ge $inner) { Set-Vox $i $y $j $mat }
        }
    }
    # 像素圆的斜角上，相邻两格只有对角相接，斜着看会漏缝 —— 把斜角补上（补内侧那一格，不动外轮廓）
    $cells = @{}
    foreach ($k in $script:VOX.Keys) {
        $p = $k -split ','
        if ([int]$p[1] -eq $y) { $cells["$($p[0]),$($p[2])"] = $true }
    }
    $add = @()
    foreach ($key in $cells.Keys) {
        $p = $key -split ','
        $x = [int]$p[0]; $z = [int]$p[1]
        foreach ($d in @(@(1, 1), @(1, -1), @(-1, 1), @(-1, -1))) {
            $nx = $x + $d[0]; $nz = $z + $d[1]
            if (-not $cells.ContainsKey("$nx,$nz")) { continue }
            if ($cells.ContainsKey("$nx,$z") -or $cells.ContainsKey("$x,$nz")) { continue }
            # 两个候选格里选离圆心更近的（= 补在内侧，外轮廓不变）
            $candA = ($nx + 0.5 - $CX) * ($nx + 0.5 - $CX) + ($z + 0.5 - $CZ) * ($z + 0.5 - $CZ)
            $candB = ($x + 0.5 - $CX) * ($x + 0.5 - $CX) + ($nz + 0.5 - $CZ) * ($nz + 0.5 - $CZ)
            if ($candA -le $candB) { $add += @($nx, $z) } else { $add += @($x, $nz) }
        }
    }
    for ($i = 0; $i -lt $add.Count; $i += 2) { Set-Vox $add[$i] $y $add[$i + 1] $mat }
}

function Get-FaceMask([int]$x, [int]$y, [int]$z) {
    $m = @()
    if (-not $script:VOX.ContainsKey("$x,$($y + 1),$z")) { $m += 'up' }
    if (-not $script:VOX.ContainsKey("$x,$($y - 1),$z")) { $m += 'down' }
    if (-not $script:VOX.ContainsKey("$($x + 1),$y,$z")) { $m += 'east' }
    if (-not $script:VOX.ContainsKey("$($x - 1),$y,$z")) { $m += 'west' }
    if (-not $script:VOX.ContainsKey("$x,$y,$($z + 1)")) { $m += 'south' }
    if (-not $script:VOX.ContainsKey("$x,$y,$($z - 1)")) { $m += 'north' }
    return ($m -join ',')
}

#  面用哪张贴图：盆体外侧 = 花纹盆身，内侧 = 素白，盆沿顶面 = 红边，盆底 = 底面
function Get-FaceTexture([string]$mat, [string]$dir, [int]$x, [int]$y, [int]$z) {
    switch ($mat) {
        'basin' {
            switch ($dir) {
                # 盆沿顶面 = 贴图最上面那行红边；台阶踏面取"同一格在盆身上对应的那一个像素"，
                # 这样台阶不会在花纹中间插一条白线（整行压到 1 像素上会变成花条纹，别用）
                'up'   {
                    if ($y -ge $Y_RIM) { return @('#basin', @(0, 0, 16, 1)) }
                    # 只有外侧那一圈的踏面才跟着花纹走，内侧那圈用素白
                    $dx = $x + 0.5 - $CX; $dz = $z + 0.5 - $CZ
                    if ([Math]::Abs($dx) -ge [Math]::Abs($dz)) { $nx = if ($dx -ge 0) { 1 } else { -1 }; $nz = 0 }
                    else { $nx = 0; $nz = if ($dz -ge 0) { 1 } else { -1 } }
                    if (-not $script:VOX.ContainsKey("$($x + $nx),$y,$($z + $nz)")) {
                        $row = [Math]::Max(0, [Math]::Min(15, 16 - $y - 1))
                        return @('#basin', @($x, $row, ($x + 1), ($row + 1)))
                    }
                    return @('#basin_inner', $null)
                }
                'down' { return @('#basin_bottom', $null) }
                default {
                    $nx = if ($dir -eq 'east') { 1.0 } elseif ($dir -eq 'west') { -1.0 } else { 0.0 }
                    $nz = if ($dir -eq 'south') { 1.0 } elseif ($dir -eq 'north') { -1.0 } else { 0.0 }
                    $dot = ($x + 0.5 - $CX) * $nx + ($z + 0.5 - $CZ) * $nz
                    if ($dot -gt 0) { return @('#basin', $null) } else { return @('#basin_inner', $null) }
                }
            }
        }
        'floor' {
            if ($dir -eq 'down') { return @('#basin_bottom', $null) }
            return @('#basin_inner', $null)
        }
        default { return @("#$mat", $null) }
    }
}

function ConvertTo-Elements {
    $columns = @{}
    foreach ($k in $script:VOX.Keys) {
        $p = $k -split ','
        $colKey = "$($p[0]),$($p[2])"
        if (-not $columns.ContainsKey($colKey)) { $columns[$colKey] = New-Object System.Collections.Generic.List[int] }
        $columns[$colKey].Add([int]$p[1])
    }
    $els = New-Object System.Collections.Generic.List[object]
    foreach ($colKey in $columns.Keys) {
        $p = $colKey -split ','
        $x = [int]$p[0]; $z = [int]$p[1]
        $ys = $columns[$colKey] | Sort-Object
        $runStart = $ys[0]; $runEnd = $ys[0]
        $runMask = Get-FaceMask $x $ys[0] $z
        $runMat = $script:VOX["$x,$($ys[0]),$z"]
        $flush = {
            if ($runMask.Length -eq 0) { return }
            $faces = [ordered]@{}
            foreach ($dir in $runMask.Split(',')) {
                $ft = Get-FaceTexture $runMat $dir $x $runStart $z
                $faceObj = [ordered]@{ texture = $ft[0] }
                if ($ft[1]) { $faceObj.uv = $ft[1] }
                $faces[$dir] = $faceObj
            }
            $els.Add([ordered]@{
                from  = @($x, $runStart, $z)
                to    = @(($x + 1), ($runEnd + 1), ($z + 1))
                faces = $faces
            })
        }
        for ($i = 1; $i -lt $ys.Count; $i++) {
            $y = $ys[$i]
            $mask = Get-FaceMask $x $y $z
            $mat = $script:VOX["$x,$y,$z"]
            if ($y -eq ($runEnd + 1) -and $mask -eq $runMask -and $mat -eq $runMat) {
                $runEnd = $y
            } else {
                & $flush
                $runStart = $y; $runEnd = $y; $runMask = $mask; $runMat = $mat
            }
        }
        & $flush
    }
    return $els
}

# ---------- 贴图 ----------
function New-Image16 {
    $bmp = New-Object System.Drawing.Bitmap (16, 16)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    for ($y = 0; $y -lt 16; $y++) {
        for ($x = 0; $x -lt 16; $x++) { $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, 0, 0, 0)) }
    }
    $g.Dispose()
    return $bmp
}

function Set-Row([System.Drawing.Bitmap]$bmp, [int]$y, [string]$cols) {
    $map = @{
        'R' = [System.Drawing.Color]::FromArgb(255, 196, 58, 48)    # 正红
        'D' = [System.Drawing.Color]::FromArgb(255, 150, 34, 30)    # 暗红
        'W' = [System.Drawing.Color]::FromArgb(255, 243, 238, 228)  # 奶白
        'E' = [System.Drawing.Color]::FromArgb(255, 224, 216, 202)  # 灰白
        'Y' = [System.Drawing.Color]::FromArgb(255, 240, 206, 96)   # 花心黄
        'G' = [System.Drawing.Color]::FromArgb(255, 96, 146, 66)    # 叶绿
        'K' = [System.Drawing.Color]::FromArgb(255, 62, 104, 48)    # 深绿
        'B' = [System.Drawing.Color]::FromArgb(255, 66, 96, 150)    # 蓝（国民盆的蓝边）
    }
    for ($x = 0; $x -lt 16 -and $x -lt $cols.Length; $x++) {
        $bmp.SetPixel($x, $y, $map["$($cols[$x])"])
    }
}

function Set-Pixel([System.Drawing.Bitmap]$bmp, [int]$x, [int]$y, [string]$key) {
    if ($x -lt 0 -or $x -gt 15 -or $y -lt 0 -or $y -gt 15) { return }
    $map = @{
        'R' = [System.Drawing.Color]::FromArgb(255, 214, 92, 46)   # 橙红花瓣
        'D' = [System.Drawing.Color]::FromArgb(255, 168, 56, 34)   # 深红花瓣 / 盆口
        'E' = [System.Drawing.Color]::FromArgb(255, 243, 238, 228)
        'Y' = [System.Drawing.Color]::FromArgb(255, 240, 206, 96)
        'G' = [System.Drawing.Color]::FromArgb(255, 96, 146, 66)
        'K' = [System.Drawing.Color]::FromArgb(255, 62, 104, 48)
    }
    $bmp.SetPixel($x, $y, $map[$key])
}

function New-BasinSideTexture([string]$path) {
    $bmp = New-Image16
    # 底色：奶白
    for ($y = 0; $y -lt 16; $y++) { Set-Row $bmp $y 'EEEEEEEEEEEEEEEE' }
    # v = 16 - y：row9 是盆沿外缘那一圈细红边，row0 给盆沿顶面用
    Set-Row $bmp 0 'RRRRRRRRRRRRRRRR'
    Set-Row $bmp 9 'RRRRRRRRRRRRRRRR'
    # 国民脸盆的花：两朵橙红花 + 黄心 + 绿叶（照着搪瓷盆照片的配色）
    foreach ($c in @(3, 11)) {
        Set-Pixel $bmp ($c - 1) 11 'D'
        Set-Pixel $bmp $c 11 'D'
        Set-Pixel $bmp ($c + 1) 11 'D'
        Set-Pixel $bmp ($c - 2) 12 'R'
        Set-Pixel $bmp ($c - 1) 12 'R'
        Set-Pixel $bmp ($c + 1) 12 'R'
        Set-Pixel $bmp ($c + 2) 12 'R'
        Set-Pixel $bmp $c 12 'Y'
        Set-Pixel $bmp $c 13 'D'
        Set-Pixel $bmp ($c - 1) 13 'R'
        Set-Pixel $bmp ($c + 1) 13 'R'
        Set-Pixel $bmp ($c - 3) 14 'G'
        Set-Pixel $bmp ($c + 3) 14 'K'
    }
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

function New-PlainTexture([string]$path, [System.Drawing.Color]$c, [int]$noise = 0) {
    $bmp = New-Image16
    $rnd = New-Object System.Random 12345
    for ($y = 0; $y -lt 16; $y++) {
        for ($x = 0; $x -lt 16; $x++) {
            $d = if ($noise -gt 0) { $rnd.Next(-$noise, $noise + 1) } else { 0 }
            $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255,
                [Math]::Max(0, [Math]::Min(255, $c.R + $d)),
                [Math]::Max(0, [Math]::Min(255, $c.G + $d)),
                [Math]::Max(0, [Math]::Min(255, $c.B + $d))))
        }
    }
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

#  菜品贴图 → 汤面色（取菜的部分的平均色）+ 汤面贴图
function New-StewTexture([string]$item, [string]$outPath) {
    $src = Join-Path $itemTexDir ($item + '.png')
    $bmp = New-Object System.Drawing.Bitmap $src
    $w = $bmp.Width; $h = $bmp.Height
    $sr = 0.0; $sg = 0.0; $sb = 0.0; $n = 0
    # 只取图标正中间那一块（菜本身），把自带的盆/碗、红边、描边全都排除掉，
    # 否则七道菜算出来的汤色会全都是同一种褐色。
    $x0 = [int]($w * 0.20); $x1 = [int]($w * 0.80) - 1
    $y0 = [int]($h * 0.14); $y1 = [int]($h * 0.58) - 1
    for ($y = $y0; $y -le $y1; $y++) {
        for ($x = $x0; $x -le $x1; $x++) {
            $p = $bmp.GetPixel($x, $y)
            if ($p.A -le 8) { continue }
            $sr += $p.R; $sg += $p.G; $sb += $p.B; $n++
        }
    }
    $bmp.Dispose()
    # 和暖褐色混一混，别让汤面颜色太跳；整体压暗一档，让浮在上面的食材能"跳"出来
    $avgR = if ($n -gt 0) { $sr / $n } else { 150 }
    $avgG = if ($n -gt 0) { $sg / $n } else { 105 }
    $avgB = if ($n -gt 0) { $sb / $n } else { 60 }
    $base = [System.Drawing.Color]::FromArgb(255,
        [int](($avgR * 0.62 + 118 * 0.38) * 0.88),
        [int](($avgG * 0.62 + 80 * 0.38) * 0.88),
        [int](($avgB * 0.62 + 44 * 0.38) * 0.88))
    $dark = [System.Drawing.Color]::FromArgb(255, [int]($base.R * 0.72), [int]($base.G * 0.72), [int]($base.B * 0.72))
    $light = [System.Drawing.Color]::FromArgb(255,
        [Math]::Min(255, [int]($base.R * 1.18 + 12)),
        [Math]::Min(255, [int]($base.G * 1.18 + 12)),
        [Math]::Min(255, [int]($base.B * 1.18 + 12)))
    $bmp2 = New-Image16
    $rnd = New-Object System.Random 777
    for ($y = 0; $y -lt 16; $y++) {
        for ($x = 0; $x -lt 16; $x++) {
            $v = $rnd.Next(0, 10)
            $c = if ($v -le 5) { $base } elseif ($v -le 8) { $dark } else { $light }
            $bmp2.SetPixel($x, $y, $c)
        }
    }
    $bmp2.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp2.Dispose()
}

function New-MeatTexture([string]$path) {
    $bmp = New-Image16
    for ($y = 0; $y -lt 16; $y++) {
        for ($x = 0; $x -lt 16; $x++) {
            $c = if ((($x + $y) % 5) -lt 2) { [System.Drawing.Color]::FromArgb(255, 150, 82, 48) } else { [System.Drawing.Color]::FromArgb(255, 118, 60, 34) }
            if ((($x * 3 + $y * 5) % 11) -eq 0) { $c = [System.Drawing.Color]::FromArgb(255, 178, 112, 68) }
            $bmp.SetPixel($x, $y, $c)
        }
    }
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

function New-VegTexture([string]$path) {
    $bmp = New-Image16
    for ($y = 0; $y -lt 16; $y++) {
        for ($x = 0; $x -lt 16; $x++) {
            $c = if ((($x + $y * 2) % 4) -lt 2) { [System.Drawing.Color]::FromArgb(255, 106, 158, 72) } else { [System.Drawing.Color]::FromArgb(255, 76, 122, 52) }
            $bmp.SetPixel($x, $y, $c)
        }
    }
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

function New-TofuTexture([string]$path) {
    $bmp = New-Image16
    for ($y = 0; $y -lt 16; $y++) {
        for ($x = 0; $x -lt 16; $x++) {
            $c = if ((($x + $y) % 6) -lt 4) { [System.Drawing.Color]::FromArgb(255, 246, 240, 224) } else { [System.Drawing.Color]::FromArgb(255, 226, 216, 194) }
            $bmp.SetPixel($x, $y, $c)
        }
    }
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

#  贴饼子：玉米面饼，边上带焦
function New-BingTexture([string]$path) {
    $bmp = New-Image16
    for ($y = 0; $y -lt 16; $y++) {
        for ($x = 0; $x -lt 16; $x++) {
            $edge = ($x -lt 2 -or $x -gt 13 -or $y -lt 2 -or $y -gt 13)
            $c = if ($edge) { [System.Drawing.Color]::FromArgb(255, 186, 132, 62) } else { [System.Drawing.Color]::FromArgb(255, 224, 182, 92) }
            if ((($x * 5 + $y * 3) % 13) -eq 0) { $c = [System.Drawing.Color]::FromArgb(255, 200, 150, 70) }
            $bmp.SetPixel($x, $y, $c)
        }
    }
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

if (-not $SkipTextures) {
    if ($RefreshBasinTextures) {
        New-BasinSideTexture (Join-Path $blockTexDir 'basin_side.png')
        New-PlainTexture (Join-Path $blockTexDir 'basin_top.png') ([System.Drawing.Color]::FromArgb(255, 231, 225, 213)) 3
        New-PlainTexture (Join-Path $blockTexDir 'basin_bottom.png') ([System.Drawing.Color]::FromArgb(255, 206, 200, 188)) 2
        Write-Host '已重画盆体贴图（国民脸盆花纹）' -ForegroundColor Yellow
    }
    New-MeatTexture (Join-Path $blockTexDir 'food_meat.png')
    New-VegTexture (Join-Path $blockTexDir 'food_veg.png')
    New-TofuTexture (Join-Path $blockTexDir 'food_tofu.png')
    New-BingTexture (Join-Path $blockTexDir 'food_bing.png')
    foreach ($d in $dishes) {
        if ($Only -and $Only -ne $d.id) { continue }
        New-StewTexture $d.item (Join-Path $blockTexDir "$($d.id)_pot_stew.png")
    }
}

# ---------- 七道菜的"料" ----------
# 每条 = @(dx, dz, 宽, 深, 高, 材质)，dx/dz 是相对盆心的格子偏移。
# 位置是手工排的（不是随机撒），保证一眼能看出是什么菜；贴图都是专用的 pot_* 小贴图。
# 每条 = @(dx, dz, 宽, 深, 离汤面几层, 几层高, 材质)
#   离汤面几层 = 1 → 直接坐在汤面上；2 → 压在第 1 层食材上面（堆头）
# 现实里的炖菜都是"底下垫一层、主料堆在中间、点缀撒在顶上"，
# 所以这里也是先铺一圈垫底的，再在中间码 2×2 的主料，最后在顶上点几块配菜 —— 不能摊成一盘马赛克。
$FOOD_LAYOUT = @{
    # 大丰收：排骨 + 玉米段 + 豆角 + 土豆 + 茄子 + 青椒
    da_feng_shou = @(
        @(-4, -1, 3, 1, 1, 1, 'pot_corn'),      # 左边一排玉米段
        @(2, -1, 3, 1, 1, 1, 'pot_corn'),       # 右边一排玉米段
        @(-1, -4, 1, 3, 1, 1, 'pot_bean'),      # 上边一绺豆角
        @(-1, 2, 1, 3, 1, 1, 'pot_bean'),       # 下边一绺豆角
        # 中间：四块料中间留出汤沟，堆头才有起有伏（不是一块大板子）
        @(-3, -3, 2, 2, 1, 2, 'pot_rib'),
        @(0, -3, 2, 2, 1, 1, 'pot_rib'),
        @(-3, 0, 2, 2, 1, 1, 'pot_potato'),
        @(0, 0, 2, 2, 1, 2, 'pot_potato'),
        @(0, -3, 1, 1, 2, 1, 'pot_eggplant'),   # 顶上：茄子
        @(1, -2, 1, 1, 2, 1, 'pot_eggplant'),
        @(-3, 0, 1, 1, 2, 1, 'pot_pepper'),     # 顶上：青椒
        @(-2, 1, 1, 1, 2, 1, 'pot_pepper')
    )
    # 地锅鸡：鸡块 + 青椒 + 锅边贴饼
    di_guo_ji = @(
        @(-3, -3, 2, 2, 1, 2, 'pot_chicken'),
        @(0, -3, 2, 2, 1, 1, 'pot_chicken'),
        @(-3, 0, 2, 2, 1, 1, 'pot_chicken'),
        @(0, 0, 2, 2, 1, 2, 'pot_chicken'),
        # 青椒撒在汤沟里
        @(-1, -2, 1, 1, 1, 1, 'pot_pepper'),
        @(0, -1, 1, 1, 1, 1, 'pot_pepper'),
        @(-1, 1, 1, 1, 1, 1, 'pot_pepper'),
        @(2, 1, 1, 1, 1, 1, 'pot_pepper'),
        @(2, -3, 1, 1, 1, 1, 'pot_pepper')
    )
    # 地锅排骨：排骨 + 土豆 + 青椒 + 锅边贴饼
    di_guo_pai_gu = @(
        @(-3, -3, 2, 2, 1, 2, 'pot_rib'),
        @(0, -3, 2, 2, 1, 1, 'pot_rib'),
        @(-3, 0, 2, 2, 1, 1, 'pot_rib'),
        @(0, 0, 2, 2, 1, 2, 'pot_potato'),
        @(0, -3, 1, 1, 2, 1, 'pot_potato'),     # 土豆块压在排骨上
        @(1, -2, 1, 1, 2, 1, 'pot_potato'),
        @(-1, -2, 1, 1, 1, 1, 'pot_pepper'),
        @(0, -1, 1, 1, 1, 1, 'pot_pepper'),
        @(-1, 1, 1, 1, 1, 1, 'pot_pepper'),
        @(2, 1, 1, 1, 1, 1, 'pot_pepper')
    )
    # 杀猪菜：酸菜铺满整盆，血肠 / 白肉片散在上面
    sha_zhu_cai = @(
        # 酸菜：铺满整个盆底（外圈也要有菜 —— 早先只在中间堆一小堆，外面一圈空荡荡的）
        @(-3, -4, 6, 1, 1, 1, 'pot_sourcabbage'),
        @(-5, -3, 10, 6, 1, 1, 'pot_sourcabbage'),
        @(-3, 3, 6, 1, 1, 1, 'pot_sourcabbage'),
        @(-4, 3, 8, 1, 1, 1, 'pot_sourcabbage'),
        # 上面这一层：先把十字缝补上酸菜，再压血肠 / 白肉
        @(-1, -3, 1, 2, 2, 1, 'pot_sourcabbage'),
        @(-1, 0, 1, 2, 2, 1, 'pot_sourcabbage'),
        @(-3, -1, 2, 1, 2, 1, 'pot_sourcabbage'),
        @(0, -1, 2, 1, 2, 1, 'pot_sourcabbage'),
        # 血肠：三大片 3×3 的肠片（早先是几个 2×2 的小块，"只是点了几个血色像素"看不出是血肠），
        # 每片上再点两颗白色肥肉丁 —— 血肠片的标志就是这个
        @(-5, -3, 3, 3, 2, 1, 'pot_sausage'),
        @(-4, -2, 1, 1, 2, 1, 'pot_fat'),
        @(-3, -1, 1, 1, 2, 1, 'pot_fat'),
        @(0, -1, 3, 3, 2, 1, 'pot_sausage'),
        @(1, 0, 1, 1, 2, 1, 'pot_fat'),
        @(2, -1, 1, 1, 2, 1, 'pot_fat'),
        @(-3, 1, 3, 3, 2, 1, 'pot_sausage'),
        @(-2, 2, 1, 1, 2, 1, 'pot_fat'),
        @(-3, 1, 1, 1, 2, 1, 'pot_fat'),
        # 白肉：两片
        @(1, -4, 2, 2, 2, 1, 'pot_fat'),
        @(1, 3, 2, 1, 2, 1, 'pot_fat')
    )
    # 猪肉炖粉条：几块方块肉（中间留汤）+ 粉条错开搭在肉上
    # 注意：粉条不能沿着汤沟摆成"正十字" —— 之前那样中间一道白十字，看着很怪。
    zhu_rou_dun_fen_tiao = @(
        # 底：四块方块肉（中间留出汤沟），两块高的两块矮的 —— 像一堆红烧肉
        @(-3, -3, 2, 2, 1, 2, 'pot_meat'),
        @(0, -3, 2, 2, 1, 1, 'pot_meat'),
        @(-3, 0, 2, 2, 1, 1, 'pot_meat'),
        @(0, 0, 2, 2, 1, 2, 'pot_meat'),
        # 外圈再补几块肉，别让盆边空着
        @(-1, -4, 2, 1, 1, 1, 'pot_meat'),
        @(-1, 1, 2, 1, 1, 1, 'pot_meat'),
        @(-4, -1, 1, 2, 1, 1, 'pot_meat'),
        @(1, -1, 1, 2, 1, 1, 'pot_meat'),
        # 粉条：搭在矮的两块肉上，再各搭一绺在高肉块的顶上（错开，不成十字）
        @(0, -3, 2, 1, 2, 1, 'pot_noodle'),
        @(-2, 0, 1, 2, 2, 1, 'pot_noodle'),
        @(-3, -3, 1, 2, 3, 1, 'pot_noodle'),
        @(1, 1, 1, 1, 3, 1, 'pot_noodle')
    )
    # 酸菜炖骨头：酸菜铺满整盆（有堆头），两根棒骨"骨筒 + 两头骨节"横躺在上面
    # 早先只是两根白条，看着太抽象；现在骨头是一段骨筒加两个鼓起的骨节（哑铃形），
    # 从上面看是"一头一尾两个关节 + 中间一段骨头"。
    # 酸菜炖大骨：酸菜打底 + 几块**带肉的脊骨**（肉块上露出一段白色骨断面）
    # 参考照片（猪脊骨炖酸菜）：一块块不规则的脊骨，肉多骨少、骨断面发白，
    # 所以每块 = 2×2 的肉 + 压在上面的一段骨头；不再是细长的"工"字形白条。
    # 酸菜炖大骨：酸菜打底 + 几块**带肉的脊骨**
    # 参考照片（猪脊骨炖酸菜）：脊骨是"肉多骨少"的块状，骨断面只有一小片发白，
    # 所以每块 = 同一层的 2×2 带骨肉（pot_rib / pot_meat）里嵌一格 pot_bone。
    # 注意骨头**不能**堆在肉块上面 —— 那样侧看像棉花糖，不像脊骨。
    # 酸菜炖大骨：酸菜打底 + 几块**带肉的脊骨**
    # 参考照片（猪脊骨炖酸菜）：脊骨是"肉多骨少"的块状，骨断面发白成条。
    # 每块 = 同一层的 2×2 带骨肉（pot_rib / pot_meat）里嵌一条 pot_bone 当骨断面；
    # 骨头**不能**堆在肉块上面，否则侧看像棉花糖。
    suan_cai_dun_gu_tou = @(
        @(-3, -4, 6, 1, 1, 1, 'pot_sourcabbage'),
        @(-5, -3, 10, 6, 1, 1, 'pot_sourcabbage'),
        @(-4, 3, 8, 1, 1, 1, 'pot_sourcabbage'),
        @(-3, 4, 6, 1, 1, 1, 'pot_sourcabbage'),
        @(-5, 1, 2, 2, 2, 1, 'pot_sourcabbage'),
        @(-2, -3, 2, 1, 2, 1, 'pot_sourcabbage'),
        @(-3, 3, 2, 1, 2, 1, 'pot_sourcabbage'),
        @(-4, -2, 2, 2, 2, 1, 'pot_rib'),
        @(-4, -2, 1, 2, 2, 1, 'pot_bone'),
        @(0, -3, 2, 2, 2, 1, 'pot_meat'),
        @(0, -3, 2, 1, 2, 1, 'pot_bone'),
        @(1, 0, 2, 2, 2, 1, 'pot_rib'),
        @(1, 0, 1, 2, 2, 1, 'pot_bone'),
        @(-3, 1, 2, 2, 2, 1, 'pot_meat'),
        @(-3, 1, 2, 1, 2, 1, 'pot_bone'),
        @(-1, -1, 1, 1, 2, 1, 'pot_meat'),
        @(-1, -1, 1, 1, 3, 1, 'pot_bone')
    )
    # 小鸡炖蘑菇：鸡块 + 榛蘑 + 粉条
    xiao_ji_dun_mo_gu = @(
        @(-4, -1, 3, 1, 1, 1, 'pot_noodle'),
        @(2, -1, 3, 1, 1, 1, 'pot_noodle'),
        @(-3, -3, 2, 2, 1, 2, 'pot_chicken'),
        @(0, -3, 2, 2, 1, 1, 'pot_chicken'),
        @(-3, 0, 2, 2, 1, 1, 'pot_chicken'),
        @(0, 0, 2, 2, 1, 2, 'pot_chicken'),
        # 榛蘑：深褐色的小方块，点在汤沟和鸡块上
        @(-1, -2, 1, 1, 1, 1, 'pot_mushroom'),
        @(2, -1, 1, 1, 1, 1, 'pot_mushroom'),
        @(-1, 1, 1, 1, 1, 1, 'pot_mushroom'),
        @(0, -3, 1, 1, 2, 1, 'pot_mushroom'),
        @(-3, 0, 1, 1, 2, 1, 'pot_mushroom'),
        @(1, -2, 1, 1, 2, 1, 'pot_mushroom')
    )
    # 酸菜海鲜锅：酸菜铺底 + 大虾 + 豆腐 + 生蚝 + 牛羊肉
    # 酸菜海鲜锅：酸菜打底 + 大虾（主角）+ 生蚝 + 豆腐 + 羊肉片 + 牛肉片
    # 参考实拍：红搪瓷锅、浅黄汤、淡白菜，虾最显眼 —— 所以虾排成一排摆在中间，
    # 外围特意留出汤面（不把整盆铺满），生蚝用专用壳色贴图 pot_oyster。
    suan_cai_hai_xian_guo = @(
        @(-3, -3, 6, 6, 1, 1, 'pot_sourcabbage'),
        @(-5, -1, 2, 2, 1, 1, 'pot_sourcabbage'),
        @(3, -1, 2, 2, 1, 1, 'pot_sourcabbage'),
        @(-1, -4, 2, 1, 1, 1, 'pot_sourcabbage'),
        @(-1, 3, 2, 1, 1, 1, 'pot_sourcabbage'),
        @(-3, -1, 2, 2, 2, 1, 'pot_sourcabbage'),
        @(1, 1, 2, 2, 2, 1, 'pot_sourcabbage'),
        @(-2, -1, 2, 1, 2, 1, 'pot_shrimp'),
        @(0, -3, 1, 2, 2, 1, 'pot_shrimp'),
        @(1, 0, 1, 2, 2, 1, 'pot_shrimp'),
        @(-3, 2, 2, 1, 2, 1, 'pot_shrimp'),
        @(-4, 0, 1, 1, 2, 1, 'pot_oyster'),
        @(2, -2, 1, 1, 2, 1, 'pot_oyster'),
        @(0, 2, 1, 1, 2, 1, 'pot_oyster'),
        @(-1, -2, 2, 1, 2, 1, 'pot_tofu'),
        @(2, 1, 1, 2, 2, 1, 'pot_tofu'),
        @(-2, 1, 1, 2, 2, 1, 'pot_fat'),
        @(1, -1, 2, 1, 2, 1, 'pot_meat')
    )
}


#  地锅类要在锅边贴一圈饼（照真实照片：浅黄玉米饼贴在锅内壁，立着，饼底贴着汤面）
$BING_DISHES = @('di_guo_ji', 'di_guo_pai_gu')

# ---------- 生成模型 ----------
foreach ($dish in $dishes) {
    if ($Only -and $Only -ne $dish.id) { continue }
    $id = $dish.id

    for ($servings = 0; $servings -le 4; $servings++) {
        $script:VOX = @{}
        Add-Ring $Y_FOOT $R_FOOT 1 'basin'
        Add-Disc $Y_FLOOR $R_FLOOR 'floor'
        foreach ($y in $WALL_LAYER.Keys) { Add-Ring ([int]$y) ([double]$WALL_LAYER[$y]) $WALL_THICK 'basin' }
        Add-Ring $Y_RIM $R_RIM $RIM_THICK 'basin'

        if ($servings -gt 0) {
            $fy = $FILL_Y[$servings]
            # fy 可能低于盆壁起始层（水位最低那一档落在盆底上），这时按盆底半径算
            $wallR = if ($WALL_LAYER.ContainsKey([int]$fy)) { [double]$WALL_LAYER[[int]$fy] } else { [double]$R_FLOOR }
            $rIn = $wallR - $WALL_THICK
            # 汤面不能顶到盆沿里去
            $stewR = [Math]::Min($rIn, ($R_RIM - $RIM_THICK)) - 0.30
            Add-Disc $fy $stewR 'stew'
            # 汤面就平平地铺在 fy 这一层 —— 食材从 fy+1 起摆，才能"浮在汤上"。
            # （以前在这里又加了一层堆起来的汤，结果食材和汤面一样高，看着像一张马赛克饼。）

            $rnd = New-Object System.Random ($dish.salt + $servings * 13)
            # 按每道菜手工排好的料摆上去（不是随机撒 —— 要让人一眼认得出是什么菜）
            $layout = $FOOD_LAYOUT[$id]
            if ($layout) {
                foreach ($p in $layout) {
                    $bx = [int][Math]::Round($CX - 0.5) + [int]$p[0]
                    $bz = [int][Math]::Round($CZ - 0.5) + [int]$p[1]
                    $w = [int]$p[2]; $d = [int]$p[3]
                    $y0 = [int]$p[4]; $h = [int]$p[5]; $mat = [string]$p[6]
                    for ($dx = 0; $dx -lt $w; $dx++) {
                        for ($dz = 0; $dz -lt $d; $dz++) {
                            $cx2 = $bx + $dx; $cz2 = $bz + $dz
                            # 底下必须有东西托着（汤面 / 盆壁 / 下面那层食材），不许悬空
                            if (-not $script:VOX.ContainsKey("$cx2,$($fy + $y0 - 1),$cz2")) { continue }
                            for ($dy = 0; $dy -lt $h; $dy++) {
                                $ly = $fy + $y0 + $dy
                                if (-not (Test-CellInside $ly $cx2 $cz2)) { continue }
                                Set-Vox $cx2 $ly $cz2 $mat
                            }
                        }
                    }
                }
            }
            # 贴饼子：只有地锅鸡 / 地锅排骨在锅内壁贴一圈 —— 立着的两块高，
            # 饼底坐在汤面那一层，像真实照片里贴在锅边的一圈玉米饼。
            # 注意：得把汤面最外圈那几个格子**全**铺上，才是一整圈饼；
            # 早先按 10 个角度取点，盆壁变薄、内径变大之后饼就散成一根根柱子了。
            $hasBing = $BING_DISHES -contains $id
            if ($hasBing) {
                $bingBand = 1.15      # 汤面最外一圈（约 1 像素宽）都贴饼
                $bingCells = @()
                foreach ($kv in (Get-DiscSpans $stewR).GetEnumerator()) {
                    $j = [int]$kv.Key
                    for ($i = $kv.Value[0]; $i -le $kv.Value[1]; $i++) {
                        $dx = $i + 0.5 - $CX
                        $dz = $j + 0.5 - $CZ
                        if ([Math]::Sqrt($dx * $dx + $dz * $dz) -lt ($stewR - $bingBand)) { continue }
                        $bingCells += , @($i, $j)
                    }
                }
                foreach ($c in $bingCells) {
                    $bx = $c[0]; $bz = $c[1]
                    if (-not $script:VOX.ContainsKey("$bx,$fy,$bz")) { continue }   # 底下要有汤面托着
                    for ($dy = 0; $dy -lt 2; $dy++) {
                        $ly = $fy + 1 + $dy
                        if (-not (Test-CellInside $ly $bx $bz)) { continue }
                        if ($script:VOX.ContainsKey("$bx,$ly,$bz")) { continue }
                        Set-Vox $bx $ly $bz 'pot_bing'
                    }
                }
            }
        }

        $els = ConvertTo-Elements
        # 每种食材自己的小贴图（pot_corn / pot_rib / ...）：脚本里材质名直接写贴图名，
        # Get-FaceTexture 的 default 分支会返回 '#'+材质名，所以这里必须逐个登记成模型材质键。
        $texTable = [ordered]@{
            particle     = "dongbei_delight:item/$($dish.item)"
            basin        = 'dongbei_delight:block/basin_side'
            basin_inner  = 'dongbei_delight:block/basin_top'
            basin_bottom = 'dongbei_delight:block/basin_bottom'
            stew         = "dongbei_delight:block/${id}_pot_stew"
        }
        foreach ($f in (Get-ChildItem -LiteralPath $blockTexDir -Filter 'pot_*.png')) {
            $n = [System.IO.Path]::GetFileNameWithoutExtension($f.Name)
            $texTable[$n] = "dongbei_delight:block/$n"
        }
        $model = [ordered]@{
            parent      = 'block/block'
            render_type = 'minecraft:cutout'
            textures    = $texTable
            elements    = $els
        }
        $json = $model | ConvertTo-Json -Depth 12 -Compress
        $outFile = Join-Path $modelsDir "${id}_pot_servings${servings}.json"
        Set-Content -LiteralPath $outFile -Value $json -Encoding UTF8
        Write-Host ("{0} 份数{1} 元素 {2}" -f $id, $servings, $els.Count) -ForegroundColor DarkGray
    }
    Write-Host ("已生成 {0} 的 5 个模型" -f $id) -ForegroundColor Green
}
