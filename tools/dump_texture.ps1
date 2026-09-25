#Requires -Version 7
<#
    把一张 PNG 按像素打成字符画，方便在终端里核对贴图形状/透明度。
    用法： pwsh -File tools\dump_texture.ps1 -Path <png> [-CellsPerPixel 1]
#>
param(
    [Parameter(Mandatory = $true)][string]$Path,
    [int]$CellsPerPixel = 1
)

Add-Type -AssemblyName System.Drawing
$bmp = [System.Drawing.Bitmap]::FromFile((Resolve-Path $Path))
Write-Host ("{0}  {1}x{2}" -f (Split-Path $Path -Leaf), $bmp.Width, $bmp.Height) -ForegroundColor Cyan
for ($y = 0; $y -lt $bmp.Height; $y += $CellsPerPixel) {
    $line = ''
    for ($x = 0; $x -lt $bmp.Width; $x += $CellsPerPixel) {
        $p = $bmp.GetPixel($x, $y)
        if ($p.A -lt 16) { $line += '.'; continue }
        $r = $p.R; $g = $p.G; $b = $p.B
        $max = [Math]::Max($r, [Math]::Max($g, $b))
        $min = [Math]::Min($r, [Math]::Min($g, $b))
        if ($max - $min -lt 24) {
            if ($max -gt 220) { $line += 'W' }
            elseif ($max -gt 160) { $line += 'w' }
            elseif ($max -gt 90) { $line += 'g' }
            else { $line += 'k' }
        }
        elseif ($r -ge $g -and $r -ge $b) {
            if ($g -gt $b * 1.4) { $line += 'y' }   # 黄/橙
            elseif ($b -gt $g * 1.2) { $line += 'm' } # 品红
            else { $line += 'r' }
        }
        elseif ($g -ge $r -and $g -ge $b) { $line += 'G' }   # 绿
        else { $line += 'B' }                                # 蓝
    }
    Write-Host $line
}
$bmp.Dispose()
