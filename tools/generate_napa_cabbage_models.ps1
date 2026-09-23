#Requires -Version 7
<#
    生成大白菜 8 个生长阶段的方块模型。

    **严格照农夫乐事的卷心菜**：
      - 模型 = 本模组的 template_crop_cross（两片 45° 交叉的薄片、shade:false、从 y=-1 到 y=15），
        和农夫乐事的 template_crop_cross 结构一致；
      - 方块 = DdCabbageCropBlock（每龄形状高度 2/3/5/7/8/9/9/10 像素，抄自 CabbageBlock）。
    每个生长阶段一份模型，只有贴图不同。

    用法： pwsh -File tools\generate_napa_cabbage_models.ps1
#>
param(
    [string]$AssetsRoot = (Join-Path $PSScriptRoot '..\src\main\resources\assets\dongbei_delight')
)

$modelDir = Join-Path $AssetsRoot 'models\block'
New-Item -ItemType Directory -Force -Path $modelDir | Out-Null

for ($stage = 0; $stage -le 7; $stage++) {
    $model = [ordered]@{
        parent      = 'dongbei_delight:block/template_crop_cross'
        render_type = 'minecraft:cutout'
        textures    = [ordered]@{
            cross = "dongbei_delight:block/napa_cabbage_crop_stage$stage"
        }
    }
    $model | ConvertTo-Json -Depth 8 | Set-Content -Path (Join-Path $modelDir "napa_cabbage_crop_stage$stage.json") -Encoding utf8
}

Write-Host '已生成大白菜 8 个阶段的模型（农夫乐事那套 45 度交叉薄片）' -ForegroundColor Green
