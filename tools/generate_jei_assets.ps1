#Requires -Version 7
<#
    生成 JEI「大缸」页面里用的大缸图标：textures/gui/jei_vat.png

    这张图不是手画的，而是把游戏里的贴图做一次等距投影：
      缸口（顶面）用 block/vat_top，缸身两面用 block/vat_side（右面压暗 15%）
      缸口中间那片透明区原样保留 —— JEI 里先在下面画液面，再盖上这张图，
      液体就会从缸口里透出来。

    几何约定（改这里的话，VatRecipeCategory 里的常量要一起改）：
      输出 32×32；顶面中心 (16, 9)、半宽 14、半高 7；缸身高 12
      左面 L(2,9) B(16,16) B'(16,28) L'(2,21)
      右面 B(16,16) R(30,9) R'(30,21) B'(16,28)

    正式美术替换时，只要保持「缸口中心 (16,9)、半宽 9、半高 4 的菱形是透明的」即可。

    用法： pwsh -File tools/generate_jei_assets.ps1
#>
param(
    [string]$AssetsRoot = (Join-Path $PSScriptRoot '..\src\main\resources\assets\northeast_china_delight')
)

Add-Type -AssemblyName System.Drawing

$blockDir = Join-Path $AssetsRoot 'textures\block'
$guiDir = Join-Path $AssetsRoot 'textures\gui'
New-Item -ItemType Directory -Force -Path $guiDir | Out-Null

$top = [System.Drawing.Bitmap]::FromFile((Join-Path $blockDir 'vat_top.png'))
$side = [System.Drawing.Bitmap]::FromFile((Join-Path $blockDir 'vat_side.png'))

$size = 32
$cx = 16.0
$cy = 9.0
$halfW = 14.0
$halfH = 7.0
$bodyH = 12.0
$leftX = 2.0
$rightX = 30.0
$topY = 9.0

$out = New-Object System.Drawing.Bitmap $size, $size
$holeMinX = 99; $holeMaxX = -1; $holeMinY = 99; $holeMaxY = -1

for ($py = 0; $py -lt $size; $py++) {
    for ($px = 0; $px -lt $size; $px++) {
        $x = $px + 0.5
        $y = $py + 0.5
        $color = $null

        # 左面：逆映射到贴图坐标
        $sxL = ($x - $leftX) * 16.0 / ($cx - $leftX)
        if ($sxL -ge 0 -and $sxL -lt 16) {
            $syL = ($y - $topY - $sxL * $halfH / 16.0) * 16.0 / $bodyH
            if ($syL -ge 0 -and $syL -lt 16) {
                # 注意用 Floor：PowerShell 的 [int] 是四舍五入，15.6 会变成 16 越界
                $color = $side.GetPixel([Math]::Min(15, [int][Math]::Floor($sxL)), [Math]::Min(15, [int][Math]::Floor($syL)))
            }
        }
        # 右面：镜像，并压暗一点做出立体感
        if ($null -eq $color) {
            $sxR = ($rightX - $x) * 16.0 / ($rightX - $cx)
            if ($sxR -ge 0 -and $sxR -lt 16) {
                $syR = ($y - $topY - $sxR * $halfH / 16.0) * 16.0 / $bodyH
                if ($syR -ge 0 -and $syR -lt 16) {
                    $c = $side.GetPixel([Math]::Min(15, [int][Math]::Floor($sxR)), [Math]::Min(15, [int][Math]::Floor($syR)))
                    $color = [System.Drawing.Color]::FromArgb($c.A, [int]($c.R * 0.85), [int]($c.G * 0.85), [int]($c.B * 0.85))
                }
            }
        }
        # 顶面：菱形区域（|dx|/半宽 + |dy|/半高 <= 1），最后画，盖住棱边
        # 注意别写成椭圆方程，否则缸口会变成椭圆形
        $dx = $x - $cx
        $dy = $y - $cy
        if (([Math]::Abs($dx) / $halfW + [Math]::Abs($dy) / $halfH) -le 1.0) {
            $sxT = 8.0 + ($dx * 16.0 / $halfW + $dy * 16.0 / $halfH) / 2.0
            $syT = 8.0 + ($dy * 16.0 / $halfH - $dx * 16.0 / $halfW) / 2.0
            $ix = [Math]::Min(15, [Math]::Max(0, [int][Math]::Floor($sxT)))
            $iy = [Math]::Min(15, [Math]::Max(0, [int][Math]::Floor($syT)))
            if ($ix -ge 0 -and $ix -lt 16 -and $iy -ge 0 -and $iy -lt 16) {
                $c = $top.GetPixel($ix, $iy)
                if ($c.A -gt 0) {
                    $color = $c
                } else {
                    # 缸口：保持透明，JEI 里由液面透出来
                    $color = $null
                    if ($px -lt $holeMinX) { $holeMinX = $px }
                    if ($px -gt $holeMaxX) { $holeMaxX = $px }
                    if ($py -lt $holeMinY) { $holeMinY = $py }
                    if ($py -gt $holeMaxY) { $holeMaxY = $py }
                }
            }
        }

        if ($null -ne $color) {
            $out.SetPixel($px, $py, [System.Drawing.Color]::FromArgb($color.A, $color.R, $color.G, $color.B))
        }
    }
}

$target = Join-Path $guiDir 'jei_vat.png'
$out.Save($target, [System.Drawing.Imaging.ImageFormat]::Png)
$out.Dispose()
$top.Dispose()
$side.Dispose()

Write-Host "已生成 JEI 大缸图标：$target" -ForegroundColor Green
Write-Host "缸口透明区：x $holeMinX~$holeMaxX，y $holeMinY~$holeMaxY（液面画在这块里）"
