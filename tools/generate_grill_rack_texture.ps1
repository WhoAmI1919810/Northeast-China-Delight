#Requires -Version 7
<#
    生成烤架铁条自己的贴图：textures/block/grill_rack_bars.png（16×16）

    为什么不能直接借原版的 iron_bars 贴图：
      原版 iron_bars.png 是给「铁栏杆方块模型」用的 —— 整张图里只有
      x = 2~3 / 7~8 / 12~13 这三列是真正的铁条（不透明、亮灰），
      其余列都是 alpha≈32 的深色填充像素（做栏杆的暗部用的）。
      我们的烤架是用代码拼出来的一根根 1px 铁条，会在 16 像素里采到**任意一列**，
      采到那几列暗像素时，铁条就变成一条半透明的黑条（被营火一照还发褐，看着像木条）。

      所以这里自己出一张「整张都是金属」的贴图：整体浅灰、带一点点深浅噪点和竖向高光，
      每一列都像铁条，怎么采都不会出现黑条。正式美术可以直接覆盖同名文件
      （建议保持整体均匀、别放透明像素）。

    用法： pwsh -File tools\generate_grill_rack_texture.ps1
#>
param(
    [string]$AssetsRoot = (Join-Path $PSScriptRoot '..\src\main\resources\assets\dongbei_delight')
)

Add-Type -AssemblyName System.Drawing

$textureDir = Join-Path $AssetsRoot 'textures\block'
New-Item -ItemType Directory -Force -Path $textureDir | Out-Null

# 基色 + 两档明暗：整体是浅灰的金属，深浅只做极轻微的噪点，保证「采哪一列都是铁」
$base  = [System.Drawing.ColorTranslator]::FromHtml('#B7BBC0')
$light = [System.Drawing.ColorTranslator]::FromHtml('#C9CDD2')
$dark  = [System.Drawing.ColorTranslator]::FromHtml('#A2A7AD')
$edge  = [System.Drawing.ColorTranslator]::FromHtml('#8E939A')

$bmp = New-Object System.Drawing.Bitmap 16, 16
for ($y = 0; $y -lt 16; $y++) {
    for ($x = 0; $x -lt 16; $x++) {
        # 固定图案（不用随机数，重跑结果一致）：斜向的浅色高光 + 少量暗点
        $color = $base
        if ((($x + $y) % 8) -eq 0 -or (($x * 3 + $y) % 13) -eq 0) { $color = $light }
        elseif ((($x + $y * 2) % 11) -eq 0) { $color = $dark }
        # 贴图最外一圈略暗一点，铁条堆在一起时能看出边界
        if ($x -eq 0 -or $x -eq 15 -or $y -eq 0 -or $y -eq 15) { $color = $edge }
        $bmp.SetPixel($x, $y, $color)
    }
}

$bmp.Save((Join-Path $textureDir 'grill_rack_bars.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()

Write-Host '已生成烤架铁条贴图：textures/block/grill_rack_bars.png' -ForegroundColor Green
