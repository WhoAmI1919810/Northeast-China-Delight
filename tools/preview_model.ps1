#Requires -Version 7
<#
    离线预览方块模型：把 models/block/*.json 用等距投影画成 PNG，方便在动手进游戏之前先看效果。

    只支持本项目用到的东西：elements = 轴对齐盒子（可选绕 Y 轴旋转）、六面 faces 里的 texture 键。
    贴图不真采样，按贴图名给一个近似颜色（basin / basin_inner / food_*）。

    用法： pwsh -File tools\preview_model.ps1 -Model <模型json路径> -Out <png路径>
#>
param(
    [Parameter(Mandatory = $true)][string]$Model,
    [Parameter(Mandatory = $true)][string]$Out,
    [int]$Scale = 26,
    [double]$YawDeg = 35,
    [double]$PitchDeg = 28
)

Add-Type -AssemblyName System.Drawing

$json = Get-Content -Raw $Model | ConvertFrom-Json
$colors = @{
    '#basin'       = '#7A5A3E'
    '#basin_inner' = '#A9855F'
    '#food_top'    = '#C98A3A'
    '#food_side'   = '#8A5A20'
}

$yaw = $YawDeg * [Math]::PI / 180.0
$pitch = $PitchDeg * [Math]::PI / 180.0
$cy = [Math]::Cos($yaw); $sy = [Math]::Sin($yaw)
$cp = [Math]::Cos($pitch); $sp = [Math]::Sin($pitch)

function Project($x, $y, $z, $scale) {
    $x1 = $x * $cy - $z * $sy
    $z1 = $x * $sy + $z * $cy
    $sx = $x1 * $scale
    $sy2 = ($z1 * $sp - $y * $cp) * $scale
    $depth = $z1 * $cp + $y * $sp
    return @($sx, $sy2, $depth)
}

$faces = @()
foreach ($el in $json.elements) {
    $fx0 = [double]$el.from[0]; $fy0 = [double]$el.from[1]; $fz0 = [double]$el.from[2]
    $fx1 = [double]$el.to[0];   $fy1 = [double]$el.to[1];   $fz1 = [double]$el.to[2]
    $rot = $null
    if ($el.rotation -and $el.rotation.axis -eq 'y') {
        $rot = @{
            ox = [double]$el.rotation.origin[0]
            oy = [double]$el.rotation.origin[1]
            oz = [double]$el.rotation.origin[2]
            a  = [double]$el.rotation.angle * [Math]::PI / 180.0
        }
    }
    $corners = @{}
    foreach ($ix in 0, 1) {
        foreach ($iy in 0, 1) {
            foreach ($iz in 0, 1) {
                $x = if ($ix -eq 0) { $fx0 } else { $fx1 }
                $y = if ($iy -eq 0) { $fy0 } else { $fy1 }
                $z = if ($iz -eq 0) { $fz0 } else { $fz1 }
                if ($rot) {
                    $dx = $x - $rot.ox; $dz = $z - $rot.oz
                    $x = $rot.ox + $dx * [Math]::Cos($rot.a) - $dz * [Math]::Sin($rot.a)
                    $z = $rot.oz + $dx * [Math]::Sin($rot.a) + $dz * [Math]::Cos($rot.a)
                }
                $corners["$ix$iy$iz"] = @($x, $y, $z)
            }
        }
    }
    $defs = @{
        up    = @('000', '001', '101', '100')
        down  = @('010', '110', '111', '011')
        north = @('000', '010', '011', '001')
        south = @('100', '110', '111', '101')
        west  = @('000', '100', '101', '001')
        east  = @('010', '110', '111', '011')
    }
    $shade = @{ up = 1.0; down = 0.55; north = 0.8; south = 0.8; west = 0.7; east = 0.7 }
    foreach ($name in $defs.Keys) {
        $tex = $el.faces.$name.texture
        if (-not $tex) { continue }
        $pts = @(); $dsum = 0.0
        foreach ($key in $defs[$name]) {
            $c = $corners[$key]
            $p = Project $c[0] $c[1] $c[2] $Scale
            $pts += @($p[0], $p[1])
            $dsum += $p[2]
        }
        $faces += @{ pts = $pts; depth = $dsum / 4.0; color = $colors[$tex]; shade = $shade[$name] }
    }
}

$size = 640
$bmp = New-Object System.Drawing.Bitmap $size, $size
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.Clear([System.Drawing.Color]::FromArgb(255, 200, 214, 226))
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias

foreach ($face in ($faces | Sort-Object { $_.depth })) {
    $c = [System.Drawing.ColorTranslator]::FromHtml($face.color)
    $r = [Math]::Min(255, [int]($c.R * $face.shade))
    $gg = [Math]::Min(255, [int]($c.G * $face.shade))
    $b = [Math]::Min(255, [int]($c.B * $face.shade))
    $brush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, $r, $gg, $b))
    $pen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(60, 0, 0, 0)), 1
    $p = $face.pts
    $points = @()
    for ($i = 0; $i -lt 4; $i++) {
        $points += New-Object System.Drawing.PointF (($size / 2.0 + $p[$i * 2]), ($size / 2.0 + $p[$i * 2 + 1]))
    }
    $g.FillPolygon($brush, $points)
    $g.DrawPolygon($pen, $points)
    $brush.Dispose(); $pen.Dispose()
}
$g.Dispose()
$bmp.Save($Out, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Host "已渲染 $Model -> $Out" -ForegroundColor Green
