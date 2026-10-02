#Requires -Version 7
<#
    生成「黄铜碗」的占位贴图与物品模型（16×16）。

    只是开发期占位：正式美术做好后直接用同名文件覆盖
    assets/northeast_china_delight/textures/item/brass_bowl.png 即可，不用改代码。

    用法： pwsh -File tools\generate_brass_bowl.ps1
#>
param(
    [string]$AssetsRoot = (Join-Path $PSScriptRoot '..\src\main\resources\assets\northeast_china_delight')
)

Add-Type -AssemblyName System.Drawing

$textureDir = Join-Path $AssetsRoot 'textures\item'
$modelDir = Join-Path $AssetsRoot 'models\item'
New-Item -ItemType Directory -Force -Path $textureDir, $modelDir | Out-Null

function C([string]$hex) { [System.Drawing.ColorTranslator]::FromHtml($hex) }
function Brush([string]$hex) { New-Object System.Drawing.SolidBrush (C $hex) }
function Pen([string]$hex) { New-Object System.Drawing.Pen (C $hex), 1 }

$bitmap = New-Object System.Drawing.Bitmap 16, 16
$g = [System.Drawing.Graphics]::FromImage($bitmap)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
$g.Clear([System.Drawing.Color]::Transparent)

$brass = '#C9A23F'
$brassLight = '#E7CE84'
$brassDark = '#8A6516'
$shadow = '#5E4408'

# 碗身：从碗口（y≈7）收到碗底（y≈12）的梯形
$p1 = New-Object System.Drawing.Point 2, 7
$p2 = New-Object System.Drawing.Point 14, 7
$p3 = New-Object System.Drawing.Point 11, 12
$p4 = New-Object System.Drawing.Point 5, 12
$body = [System.Drawing.Point[]]@($p1, $p2, $p3, $p4)
$g.FillPolygon((Brush $brass), $body)
$g.DrawPolygon((Pen $brassDark), $body)

# 碗底圈足
$g.FillEllipse((Brush $brassDark), 4, 11, 8, 3)
$g.DrawEllipse((Pen $shadow), 4, 11, 8, 3)

# 碗口：椭圆 + 内侧阴影（内壁比外壁暗）
$g.FillEllipse((Brush $brassLight), 1, 4, 14, 6)
$g.FillEllipse((Brush $brassDark), 3, 6, 10, 3)
$g.FillEllipse((Brush $shadow), 4, 7, 8, 2)
$g.DrawEllipse((Pen $brassDark), 1, 4, 14, 6)

# 左上角高光
$g.DrawLine((Pen $brassLight), 3, 6, 6, 5)
$g.DrawLine((Pen $brassLight), 4, 9, 4, 11)

$g.Dispose()
$bitmap.Save((Join-Path $textureDir 'brass_bowl.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$bitmap.Dispose()

$model = [ordered]@{
    parent   = 'item/generated'
    textures = [ordered]@{ layer0 = 'northeast_china_delight:item/brass_bowl' }
}
$model | ConvertTo-Json -Depth 5 | Set-Content -Path (Join-Path $modelDir 'brass_bowl.json') -Encoding utf8

Write-Host '已生成黄铜碗的占位贴图与物品模型' -ForegroundColor Green
