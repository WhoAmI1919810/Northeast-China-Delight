#Requires -Version 7
<#
    盆体"漏光"检测：往盆里塞一个贴着缺失贴图的盒子（渲染成洋红），
    再从低于盆口的角度绕一圈拍图 —— 只要图里出现洋红，就说明那个方向盆壁漏缝了。

    用法： pwsh -File tools\check_bowl_leak.ps1 -Model <模型json> [-Scale 12]
#>
param(
    [Parameter(Mandatory = $true)][string]$Model,
    [int]$Scale = 12,
    [string]$OutDir = 'E:\codex\dd_tmp\bowl_leak'
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing.Common

$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
if (Test-Path $OutDir) { Remove-Item $OutDir -Recurse -Force }
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

$mdl = Get-Content -Raw $Model | ConvertFrom-Json
# 见证盒：放在盆内，故意给一个不存在的贴图 → 渲染器会画成洋红
$mdl.textures | Add-Member -NotePropertyName witness -NotePropertyValue 'northeast_china_delight:block/__missing_witness__' -Force
$faces = [ordered]@{}
foreach ($f in @('up', 'down', 'north', 'south', 'west', 'east')) {
    $faces[$f] = [ordered]@{ texture = '#witness' }
}
$witness = [ordered]@{ from = @(4.5, 2.5, 4.5); to = @(11.5, 7.5, 11.5); faces = $faces }
$mdl.elements += $witness
$patched = Join-Path $OutDir 'with_witness.json'
($mdl | ConvertTo-Json -Depth 12 -Compress) | Set-Content -LiteralPath $patched -Encoding UTF8

$tile = 260
$views = @()
foreach ($az in 0, 45, 90, 135, 180, 225, 270, 315) {
    foreach ($pitch in -12, -4, 4) { $views += ("{0}/{1}" -f $az, $pitch) }
}
$joined = $views -join ','
$shot = Join-Path $OutDir 'leak.png'
& pwsh -NoProfile -File (Join-Path $root 'tools\render_model.ps1') -Model $patched -Out $shot `
    -Views $joined -Tile $tile -Scale $Scale | Out-Null

# 逐格扫描洋红像素（渲染器用 255,0,255 表示贴图缺失）
$bmp = [System.Drawing.Bitmap]::FromFile($shot)
$cols = $views.Count
$stride = $tile
$bad = @()
for ($v = 0; $v -lt $cols; $v++) {
    $ox = $v * $stride
    $count = 0
    for ($y = 26; $y -lt (26 + $tile); $y++) {
        for ($x = $ox; $x -lt ($ox + $tile); $x++) {
            $p = $bmp.GetPixel($x, $y)
            if ($p.R -gt 240 -and $p.G -lt 40 -and $p.B -gt 240) { $count++ }
        }
    }
    if ($count -gt 0) { $bad += ("{0} 洋红 {1} 像素" -f $views[$v], $count) }
}
$bmp.Dispose()

if ($bad.Count -eq 0) {
    Write-Host '所有低角度视角都没漏光：盆壁是封闭的 ✅' -ForegroundColor Green
} else {
    Write-Host ("有 {0} 个视角漏光：" -f $bad.Count) -ForegroundColor Yellow
    $bad | ForEach-Object { Write-Host "  $_" }
}
Write-Host "（对照图：$shot）" -ForegroundColor DarkGray
