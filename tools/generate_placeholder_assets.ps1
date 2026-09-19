#Requires -Version 7
<#
    生成开发期占位用的物品贴图（16x16 像素风）与对应的物品模型 JSON。

    这些贴图只是为了「能在游戏里看到东西」，正式美术资源做好后
    直接用同名文件覆盖 textures/item/*.png 即可，不需要改动代码。

    用法： pwsh -File tools/generate_placeholder_assets.ps1
          pwsh -File tools/generate_placeholder_assets.ps1 -Only tofu,cooking_oil

    注意：不带 -Only 会重画全部物品；已经交付正式素材的物品请用 -Only 点名生成，
    否则会把正式贴图覆盖回占位图。
#>
param(
    [string]$AssetsRoot = (Join-Path $PSScriptRoot '..\src\main\resources\assets\dongbei_delight'),
    [string[]]$Only = @()
)

Add-Type -AssemblyName System.Drawing

$textureDir = Join-Path $AssetsRoot 'textures\item'
$modelDir = Join-Path $AssetsRoot 'models\item'
New-Item -ItemType Directory -Force -Path $textureDir, $modelDir | Out-Null

function C([string]$hex) { [System.Drawing.ColorTranslator]::FromHtml($hex) }

function Brush([string]$hex) { New-Object System.Drawing.SolidBrush (C $hex) }

function Pen([string]$hex) { New-Object System.Drawing.Pen (C $hex), 1 }

function Oval($g, $fill, $edge, $x, $y, $w, $h) {
    $b = Brush $fill; $p = Pen $edge
    $g.FillEllipse($b, $x, $y, $w, $h)
    $g.DrawEllipse($p, $x, $y, $w, $h)
    $b.Dispose(); $p.Dispose()
}

function Rect($g, $fill, $edge, $x, $y, $w, $h) {
    $b = Brush $fill; $p = Pen $edge
    $g.FillRectangle($b, $x, $y, $w, $h)
    $g.DrawRectangle($p, $x, $y, $w, $h)
    $b.Dispose(); $p.Dispose()
}

function Bar($g, $color, $x1, $y1, $x2, $y2, $width) {
    $p = New-Object System.Drawing.Pen (C $color), $width
    $p.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $p.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
    $g.DrawLine($p, $x1, $y1, $x2, $y2)
    $p.Dispose()
}

# ---- 各类形状 ----

function Shape-Cluster($g, $main, $dark, $accent) {
    Oval $g $main $dark 2 4 7 7
    Oval $g $main $dark 7 6 7 7
    Oval $g $accent $dark 5 10 7 5
}

function Shape-Long($g, $main, $dark, $accent) {
    Oval $g $main $dark 4 1 8 14
    Rect $g $accent $dark 5 1 6 3
}

function Shape-Speckled($g, $main, $dark, $accent) {
    Oval $g $main $dark 4 1 8 14
    Rect $g $accent $dark 6 4 1 1
    Rect $g $accent $dark 9 8 1 1
    Rect $g $accent $dark 6 11 1 1
}

function Shape-Pod($g, $main, $dark, $accent) {
    Bar $g $main 3 12 12 4 3
    Bar $g $main 5 14 14 6 3
    Bar $g $accent 4 8 10 3 2
}

function Shape-Grain($g, $main, $dark, $accent) {
    Oval $g $main $dark 3 3 5 6
    Oval $g $main $dark 8 6 5 6
    Oval $g $accent $dark 5 9 5 6
}

function Shape-Leafy($g, $main, $dark, $accent) {
    Oval $g $main $dark 3 1 10 14
    Bar $g $accent 6 3 6 13 1
    Bar $g $accent 9 4 9 12 1
}

function Shape-Bowl($g, $main, $dark, $accent) {
    Oval $g $main $dark 2 6 12 8
    Oval $g $accent $dark 4 4 8 6
    Bar $g $dark 2 11 13 11 1
}

function Shape-Bottle($g, $main, $dark, $accent) {
    Rect $g $main $dark 5 5 6 10
    Rect $g $accent $dark 6 1 4 4
    Bar $g $dark 5 9 10 9 1
}

function Shape-Noodles($g, $main, $dark, $accent) {
    Bar $g $main 3 4 12 4 2
    Bar $g $main 3 8 12 8 2
    Bar $g $main 3 12 12 12 2
    Bar $g $accent 3 4 3 12 2
}

function Shape-Sheet($g, $main, $dark, $accent) {
    Rect $g $main $dark 2 4 12 8
    Bar $g $accent 2 8 13 8 1
}

function Shape-Bone($g, $main, $dark, $accent) {
    Oval $g $main $dark 3 5 10 8
    Bar $g $accent 2 3 5 6 3
    Bar $g $accent 11 11 13 14 3
}

function Shape-Dumpling($g, $main, $dark, $accent) {
    Oval $g $main $dark 2 6 6 6
    Oval $g $main $dark 8 6 6 6
    Oval $g $accent $dark 5 10 6 5
}

# ---- 物品清单：id = 形状, 主色, 描边, 点缀色 ----

$items = [ordered]@{
    # 农作物
    'soybean'                 = @('cluster',   '#E4D5A0', '#8A7440', '#F2E8C0')
    'eggplant'                = @('long',      '#6A3E9E', '#3A2158', '#4E9A3D')
    'green_pepper'            = @('long',      '#4E9A3D', '#2C5A22', '#7BC25F')
    'corn'                    = @('long',      '#F0C93F', '#A88A1E', '#B7D06A')
    'green_beans'             = @('pod',       '#5F9E3A', '#31601F', '#8CC46A')
    'buckwheat'               = @('grain',     '#9A6B3A', '#5E3F1E', '#C79A63')
    'napa_cabbage'            = @('leafy',     '#DCE8C0', '#7A8C55', '#F2F6E4')
    'cucumber'                = @('speckled',  '#4F9B3F', '#2A5A21', '#2F6B26')
    # 食材与加工品
    'soy_paste'               = @('bowl',      '#8A5A2B', '#4A2E12', '#6B4420')
    'pickled_cucumber'        = @('speckled',  '#7A9A3A', '#44561C', '#4E6B22')
    'pickled_carrot'          = @('long',      '#E08A2E', '#8A4E12', '#7A9A3A')
    'crushed_corn'            = @('grain',     '#F2D45C', '#A88A1E', '#E8B93C')
    'water_dough'             = @('dumpling',  '#EFE3B8', '#A89460', '#D8C78A')
    'buckwheat_noodles'       = @('noodles',   '#A8895C', '#5E4A2A', '#C9AC7E')
    'dried_tofu'              = @('sheet',     '#E8D9A8', '#9A8850', '#C9B77E')
    'tofu'                    = @('sheet',     '#F2EDDC', '#B0A583', '#FFFFFF')
    'cooking_oil'             = @('bottle',    '#E8C33C', '#96760F', '#FFF0A0')
    'pork_ribs'               = @('bone',      '#C4645C', '#7A332E', '#F0EDE4')
    'pork_intestine'          = @('pod',       '#D98A9A', '#8A4E58', '#F0B0BC')
    'pork_hock'               = @('bone',      '#C4645C', '#7A332E', '#E8C8A0')
    'pig_blood'               = @('cluster',   '#8A1A1A', '#4E0D0D', '#C43A3A')
    'raw_sausage'             = @('pod',       '#C4645C', '#7A332E', '#E8A88A')
    'blood_sausage'           = @('pod',       '#5E1414', '#2E0808', '#8A2A2A')
    'rice_sausage'            = @('pod',       '#E8DCC0', '#9A8A60', '#C4645C')
    'soy_sauce'               = @('bottle',    '#3E2410', '#1A0E06', '#5E3A1A')
    'braised_pork_hock'       = @('bowl',      '#8A4A2A', '#4E2410', '#C4845A')
    'braised_pork_strips'     = @('bowl',      '#9A5A3A', '#4E2A18', '#D8A08A')
    'salt'                    = @('grain',     '#F2F2F2', '#A8A8A8', '#DCDCDC')
    'salted_pork'             = @('sheet',     '#B5534A', '#6B2A24', '#D98A7A')
    'soy_paste_chunk'         = @('sheet',     '#7A4A1E', '#3E2410', '#9A6B2A')
    # 料理
    'soy_milk'                = @('bottle',    '#F2F2E8', '#9A9A8A', '#D8D8C8')
    'guo_bao_rou'             = @('bowl',      '#C87A3C', '#7A4218', '#E8A860')
    'di_san_xian'             = @('bowl',      '#8A7A3C', '#4E4418', '#B7A45E')
    'jian_jiao_gan_dou_fu'    = @('bowl',      '#7FA05A', '#3E5A24', '#D8C79A')
    'suan_cai_dun_gu_tou'     = @('bowl',      '#D9C08A', '#8A7440', '#F0EDE4')
    'suan_cai_jiao_zi'        = @('dumpling',  '#EDE3C8', '#9A8A60', '#C9BE9A')
    'di_guo_ji'               = @('bowl',      '#9A6B3F', '#4E3116', '#C79A63')
    'di_guo_pai_gu'           = @('bowl',      '#9A6B3F', '#4E3116', '#F0EDE4')
    'jiang_da_gu'             = @('bowl',      '#7A451E', '#3A1E0C', '#F0EDE4')
    'sha_zhu_cai'             = @('bowl',      '#B58A6A', '#5E3A24', '#E8B0A0')
    'la_rou_dun_dou_jiao'     = @('bowl',      '#8A5A3A', '#4A2C14', '#7FA05A')
    'jiang_niu_rou'           = @('bowl',      '#5E3A1E', '#2E1A0A', '#B5534A')
    'liu_rou_duan'            = @('bowl',      '#C88A3C', '#7A4E18', '#7FA05A')
    'hong_shao_pai_gu'        = @('bowl',      '#9A4A2A', '#4E2010', '#F0EDE4')
    'ba_si_tu_dou'            = @('bowl',      '#E8B93C', '#96700F', '#F2E8C0')
    'suan_huang_gua_chao_rou_si' = @('bowl',   '#D98A9A', '#8A4E58', '#7A9A3A')
    'sour_tangzi'             = @('bowl',      '#EFD98A', '#8A7440', '#F2E8C0')
    'egg_soy_paste'           = @('bowl',      '#D9B441', '#8A6B1E', '#F2D45C')
}

$shapeMap = @{
    'cluster'  = ${function:Shape-Cluster}
    'long'     = ${function:Shape-Long}
    'speckled' = ${function:Shape-Speckled}
    'pod'      = ${function:Shape-Pod}
    'grain'    = ${function:Shape-Grain}
    'leafy'    = ${function:Shape-Leafy}
    'bowl'     = ${function:Shape-Bowl}
    'bottle'   = ${function:Shape-Bottle}
    'noodles'  = ${function:Shape-Noodles}
    'sheet'    = ${function:Shape-Sheet}
    'bone'     = ${function:Shape-Bone}
    'dumpling' = ${function:Shape-Dumpling}
}

# 用 -File 调用时，PowerShell 会把 -Only a,b,c 当成一个逗号字符串，这里统一拆开
$onlyIds = @($Only | ForEach-Object { $_ -split ',' } | ForEach-Object { $_.Trim() } | Where-Object { $_ })
$ids = if ($onlyIds.Count -gt 0) { $onlyIds } else { @($items.Keys) }

foreach ($id in $ids) {
    if (-not $items.Contains($id)) {
        Write-Warning "跳过未知物品 id：$id"
        continue
    }
    $spec = $items[$id]
    $kind, $main, $dark, $accent = $spec

    $bitmap = New-Object System.Drawing.Bitmap 16, 16
    $g = [System.Drawing.Graphics]::FromImage($bitmap)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
    $g.Clear([System.Drawing.Color]::Transparent)

    & $shapeMap[$kind] $g $main $dark $accent

    $g.Dispose()
    $bitmap.Save((Join-Path $textureDir "$id.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $bitmap.Dispose()

    # 物品模型
    $model = [ordered]@{
        parent   = 'item/generated'
        textures = [ordered]@{ layer0 = "dongbei_delight:item/$id" }
    }
    $model | ConvertTo-Json -Depth 5 | Set-Content -Path (Join-Path $modelDir "$id.json") -Encoding utf8
}

Write-Host "已生成 $($ids.Count) 个物品的占位贴图与模型：" -ForegroundColor Green
Write-Host "  贴图: $textureDir"
Write-Host "  模型: $modelDir"
