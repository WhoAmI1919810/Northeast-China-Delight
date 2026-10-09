#Requires -Version 7
<#
    给成熟阶段的黄瓜植株贴图补上"黄瓜果实"。

    为什么要单独做这一步：其余作物成熟时都有自己颜色的果实能认（茄子是紫的、辣椒是红的、
    玉米是黄的、大白菜是一个菜球），而黄瓜原来的成熟贴图只有绿叶 —— 瓜本身的绿色和叶子太像，
    所以成熟后反而看不出是什么（用户反馈"区分度太低"）。

    做法：直接在**现有贴图**上画两三个挂着的黄瓜（亮黄绿瓜身 + 深色描边 + 高光条 + 瓜刺点），
    不重画叶子，保留原来的美术风格。

    用法： pwsh -File tools\draw_cucumber_fruit.ps1
#>
param(
    [string]$Root = ''
)

$ErrorActionPreference = 'Stop'
if (-not $Root) { $Root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path }
Add-Type -AssemblyName System.Drawing.Common

$texDir = Join-Path $Root 'src\main\resources\assets\northeast_china_delight\textures\block'

function Col([string]$hex) { [System.Drawing.ColorTranslator]::FromHtml($hex) }

# 两套配色：
#   嫩瓜（stage6）= 亮黄绿，一眼看出是"刚结的小瓜"；
#   熟瓜（stage7）= 深绿（真实黄瓜的颜色）。熟瓜的绿比叶子还深，光靠"绿"是不够的，
#   所以给它配了**更亮的高光条**和**接近黑的描边**：远看是"深绿瓜身 + 一道亮边"，
#   反而比浅绿更跳。
$YOUNG = @{
    outline = Col '#24430E'; hilite = Col '#DCEBA0'; body = Col '#A8CC5A'
    shade = Col '#7CA83C'; bump = Col '#8FBB48'; stem = Col '#6A8F35'
}
$RIPE = @{
    # 试过更深的 #3F7A1E，成片种在田里就像"黑条 + 白线"，太突兀；
    # 现在这档是"比叶子稍深、但同一个色系"，描边也回到深绿而不是近黑。
    outline = Col '#24430E'; hilite = Col '#C8E088'; body = Col '#5E9B32'
    shade = Col '#3E7420'; bump = Col '#6FA83C'; stem = Col '#4E7A28'
}
$PETAL   = Col '#EFE18C'   # 花
$PETAL2  = Col '#D9C05E'

function Set-Px([System.Drawing.Bitmap]$bmp, [int]$x, [int]$y, $color) {
    if ($x -lt 0 -or $x -gt 15 -or $y -lt 0 -or $y -gt 15) { return }
    $bmp.SetPixel($x, $y, $color)
}

#  画一根朝下挂着的黄瓜：细长（长:宽 ≈ 3:1）、两端收尖、整圈描边、略微歪一点
function Draw-Cucumber([System.Drawing.Bitmap]$bmp, [int]$x, [int]$y, [int]$len, [int]$width = 3, [double]$slope = 0.5, [hashtable]$pal = $null) {
    if (-not $pal) { $pal = $YOUNG }
    $rnd = New-Object System.Random ($x * 31 + $y * 7 + $len)
    # 每一行算一次中心：斜着挂（$slope = 每往下一格往右偏多少像素），
    # 直上直下的绿条在 16×16 里看着就是块门板 —— 物品图标里的黄瓜也是斜的。
    $spans = @()
    for ($i = 0; $i -lt $len; $i++) {
        $t = $i / [double]([Math]::Max(1, $len - 1))
        $w = if ($t -lt 0.12 -or $t -gt 0.88) { [Math]::Max(2, $width - 1) } else { $width }
        $cx = $x + $i * $slope
        $x0 = [int][Math]::Round($cx - ($w - 1) / 2.0)
        $spans += , @($x0, $w)
    }
    # 描一整圈深色轮廓（左右两列 + 上下两行），瓜才能从叶子里"跳"出来
    for ($i = 0; $i -lt $len; $i++) {
        Set-Px $bmp ($spans[$i][0] - 1) ($y + $i) $pal.outline
        Set-Px $bmp ($spans[$i][0] + $spans[$i][1]) ($y + $i) $pal.outline
    }
    for ($k = -1; $k -le $spans[0][1]; $k++) { Set-Px $bmp ($spans[0][0] + $k) ($y - 1) $pal.outline }
    for ($k = -1; $k -le $spans[$len - 1][1]; $k++) { Set-Px $bmp ($spans[$len - 1][0] + $k) ($y + $len) $pal.outline }
    # 再填瓜身：左高光、右暗面、中间瓜身 + 零星瓜刺
    for ($i = 0; $i -lt $len; $i++) {
        $x0 = $spans[$i][0]; $w = $spans[$i][1]
        for ($k = 0; $k -lt $w; $k++) {
            $c = $pal.body
            if ($k -eq 0) { $c = $pal.hilite }
            elseif ($k -eq ($w - 1)) { $c = $pal.shade }
            elseif ($rnd.Next(0, 100) -lt 18) { $c = $pal.bump }
            Set-Px $bmp ($x0 + $k) ($y + $i) $c
        }
    }
    # 瓜把（跟着顶端那一格）+ 瓜屁股的小尖
    Set-Px $bmp ($spans[0][0] + 1) ($y - 2) $pal.stem
    Set-Px $bmp ($spans[0][0] + 1) ($y - 3) $pal.stem
    $last = $spans[$len - 1][0] + [int]($spans[$len - 1][1] / 2)
    Set-Px $bmp $last ($y + $len + 1) $pal.shade
}

function Draw-Flower([System.Drawing.Bitmap]$bmp, [int]$x, [int]$y) {
    Set-Px $bmp $x $y $PETAL
    Set-Px $bmp ($x + 1) $y $PETAL
    Set-Px $bmp $x ($y + 1) $PETAL2
    Set-Px $bmp ($x + 1) ($y + 1) $PETAL
}

#  读进来再复制一份：直接从文件 new 出来的 Bitmap 会一直占着文件句柄，盖回原路径会报 GDI+ 错
function Open-Copy([string]$path) {
    $src = New-Object System.Drawing.Bitmap $path
    $copy = New-Object System.Drawing.Bitmap $src
    $src.Dispose()
    return $copy
}

# ---- stage7：两根成熟的大黄瓜（深绿熟瓜配色，比叶子更深、靠亮高光+近黑描边跳出来） ----
$p7 = Join-Path $texDir 'cucumber_crop_stage7.png'
$b7 = Open-Copy $p7
Draw-Cucumber $b7 2 5 9 3 0.55 $RIPE   # 左边一根，往右下斜着挂
Draw-Cucumber $b7 12 4 8 3 -0.6 $RIPE  # 右边一根，往左下斜着挂
$b7.Save($p7, [System.Drawing.Imaging.ImageFormat]::Png)
$b7.Dispose()

# ---- stage6：嫩瓜小一点（亮黄绿），屁股上还顶着花 ----
$p6 = Join-Path $texDir 'cucumber_crop_stage6.png'
$b6 = Open-Copy $p6
Draw-Cucumber $b6 3 7 6 3 0.5 $YOUNG   # 左小瓜
Draw-Cucumber $b6 11 7 5 3 -0.5 $YOUNG # 右小瓜
Draw-Flower $b6 5 14            # 嫩瓜屁股上的花
$b6.Save($p6, [System.Drawing.Imaging.ImageFormat]::Png)
$b6.Dispose()

Write-Host '已给黄瓜 stage6 / stage7 补上果实（嫩瓜亮黄绿、熟瓜深绿）' -ForegroundColor Green
