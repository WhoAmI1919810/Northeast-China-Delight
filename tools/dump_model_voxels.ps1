#Requires -Version 7
<#
    把方块模型按"体素"打成字符画，用来核对像素圆对不对。
    每个高度层打一张俯视图：'#' = 有元素，'.' = 空。
    用法： pwsh -File tools\dump_model_voxels.ps1 -Model <json> [-MaxLayers 8]
#>
param(
    [Parameter(Mandatory = $true)][string]$Model,
    [int]$MaxLayers = 8
)

$mdl = Get-Content -Raw $Model | ConvertFrom-Json
$layers = @{}
foreach ($el in $mdl.elements) {
    $x0 = [int][Math]::Floor($el.from[0]); $x1 = [int][Math]::Ceiling($el.to[0])
    $y0 = [int][Math]::Floor($el.from[1]); $y1 = [int][Math]::Ceiling($el.to[1])
    $z0 = [int][Math]::Floor($el.from[2]); $z1 = [int][Math]::Ceiling($el.to[2])
    for ($y = $y0; $y -lt $y1; $y++) {
        if (-not $layers.ContainsKey($y)) { $layers[$y] = @{} }
        for ($x = $x0; $x -lt $x1; $x++) {
            for ($z = $z0; $z -lt $z1; $z++) { $layers[$y]["$x,$z"] = $true }
        }
    }
}
foreach ($y in ($layers.Keys | Sort-Object)) {
    if ($y -ge $MaxLayers) { continue }
    Write-Host ("y = {0}" -f $y) -ForegroundColor Cyan
    for ($z = 0; $z -lt 16; $z++) {
        $line = ''
        for ($x = 0; $x -lt 16; $x++) {
            $line += if ($layers[$y].ContainsKey("$x,$z")) { '#' } else { '.' }
        }
        Write-Host "  $line"
    }
}
