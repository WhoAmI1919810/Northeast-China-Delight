#Requires -Version 7
<# 统计一张贴图里出现的颜色（按出现次数排序），用于核对贴图配色。 #>
param([Parameter(Mandatory = $true)][string[]]$Path, [int]$Top = 8)

Add-Type -AssemblyName System.Drawing
foreach ($p in $Path) {
    $file = Resolve-Path $p
    $bmp = [System.Drawing.Bitmap]::FromFile($file)
    $counts = @{}
    for ($y = 0; $y -lt $bmp.Height; $y++) {
        for ($x = 0; $x -lt $bmp.Width; $x++) {
            $px = $bmp.GetPixel($x, $y)
            $key = '{0},{1},{2},{3}' -f $px.R, $px.G, $px.B, $px.A
            if ($counts.ContainsKey($key)) { $counts[$key]++ } else { $counts[$key] = 1 }
        }
    }
    Write-Host ("== {0} ({1}x{2})" -f $bmp.Name, $bmp.Width, $bmp.Height) -ForegroundColor Cyan
    $counts.GetEnumerator() | Sort-Object -Property Value -Descending |
        Select-Object -First $Top | ForEach-Object { Write-Host ("   #{0}  x{1}" -f $_.Key, $_.Value) }
    $bmp.Dispose()
}
