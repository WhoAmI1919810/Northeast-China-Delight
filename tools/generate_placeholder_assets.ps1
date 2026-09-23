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

function Shape-Mushroom($g, $main, $dark, $accent) {
    Oval $g $main $dark 2 2 12 7
    Rect $g $accent $dark 6 8 4 6
}

function Shape-Basin($g, $main, $dark, $accent) {
    # 大脸盆：宽口的搪瓷盆 —— 先画盆身，再叠一圈亮色盆口
    Oval $g $main $dark 1 6 14 8
    Oval $g $accent $dark 2 3 12 5
    Bar $g $dark 3 8 12 8 1
}

function Shape-Pancake($g, $main, $dark, $accent) {
    # 辣白菜饼：扁圆的金黄饼 + 上面几块红色辣白菜碎
    Oval $g $main $dark 1 4 14 8
    Oval $g $accent $dark 4 6 3 3
    Oval $g $accent $dark 9 7 3 3
}

function Shape-Grate($g, $main, $dark, $accent) {
    # 烧烤架（俯视）：一圈外框 + 中间几根横竖条
    Rect $g $main $dark 1 3 14 11
    Bar $g $dark 4 4 4 12 1
    Bar $g $dark 7 4 7 12 1
    Bar $g $dark 10 4 10 12 1
    Bar $g $dark 2 7 13 7 1
    Bar $g $accent 2 4 13 4 1
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
    'sweet_potato'            = @('long',      '#B5623C', '#6B3220', '#D98A5A')
    'green_onion'             = @('leafy',     '#7FB25A', '#3E6B24', '#F2F6E4')
    # 山珍与海味
    'hazelnut'                = @('cluster',   '#A9743A', '#6B4420', '#D9A868')
    'hazel_mushroom'          = @('mushroom',  '#C08A4A', '#6B4420', '#E8D9B0')
    'wood_ear'                = @('cluster',   '#4E3A2E', '#241A12', '#6B5240')
    'hairtail'                = @('long',      '#C9D2D8', '#7A848C', '#F2F5F8')
    'oyster'                  = @('sheet',     '#B8B09A', '#6B6452', '#E8E4D4')
    'sea_cucumber'            = @('pod',       '#4A3A2E', '#241A12', '#6B5240')
    'ginseng'                 = @('long',      '#E0CE9A', '#8A7440', '#4E9A3D')
    'chicken_frame'           = @('bone',      '#E8D9A8', '#9A8850', '#C4645C')
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
    'animal_oil'              = @('bottle',    '#F2E7C4', '#B0A480', '#FFFDF0')
    'kvass'                   = @('bottle',    '#A05A1E', '#6B3A10', '#C9843C')
    'bibimbap'                = @('bowl',      '#E8DCC0', '#8A7440', '#C43A2A')
    'kimchi_pancake'          = @('pancake',   '#D9A04A', '#8A5A18', '#C43A2A')
    'pork_ribs'               = @('bone',      '#C4645C', '#7A332E', '#F0EDE4')
    'pork_intestine'          = @('pod',       '#D98A9A', '#8A4E58', '#F0B0BC')
    'pork_hock'               = @('bone',      '#C4645C', '#7A332E', '#E8C8A0')
    'pig_blood'               = @('cluster',   '#8A1A1A', '#4E0D0D', '#C43A3A')
    'pork_fat'                = @('cluster',   '#F0DCC8', '#B08A6A', '#FFFFFF')
    'oil_edge'                = @('long',      '#E8B0A0', '#8A4E48', '#FFF6F0')
    'cracklings'              = @('cluster',   '#C98A3A', '#7A4A12', '#E8B85A')
    'green_radish'            = @('long',      '#A8C46A', '#4E7A24', '#EAF2D8')
    'cold_noodle_sheet'       = @('sheet',     '#EFE3C0', '#B0A060', '#FFF6DC')
    'snowy_bean_paste'        = @('bowl',      '#F2EDE0', '#B0A583', '#8A4A2A')
    'candied_peanuts'         = @('bowl',      '#D9A04A', '#8A5A18', '#F2E0B0')
    'tiger_salad'             = @('bowl',      '#7FA85A', '#3E6B24', '#C43A2A')
    'grilled_corn'            = @('long',      '#F0C93F', '#A88A1E', '#8A5A18')
    'grilled_oil_edge'        = @('long',      '#B5534A', '#6B2A24', '#F0D8C0')
    'grilled_cold_noodles'    = @('sheet',     '#E8C98A', '#A88A1E', '#C43A2A')
    'raw_sausage'             = @('pod',       '#C4645C', '#7A332E', '#E8A88A')
    'blood_sausage'           = @('pod',       '#5E1414', '#2E0808', '#8A2A2A')
    'rice_sausage'            = @('pod',       '#E8DCC0', '#9A8A60', '#C4645C')
    'soy_sauce'               = @('bottle',    '#3E2410', '#1A0E06', '#5E3A1A')
    'vinegar'                 = @('bottle',    '#6B3A16', '#3A1E0A', '#8A5A28')
    'sour_water'              = @('bottle',    '#C9C09A', '#8A8058', '#E8E2C4')
    'white_vinegar'           = @('bottle',    '#E4DCC2', '#A89E7E', '#F5F0DE')
    'braised_pork_hock'       = @('bowl',      '#8A4A2A', '#4E2410', '#C4845A')
    'braised_pork_strips'     = @('bowl',      '#9A5A3A', '#4E2A18', '#D8A08A')
    'salt'                    = @('grain',     '#F2F2F2', '#A8A8A8', '#DCDCDC')
    'salted_pork'             = @('sheet',     '#B5534A', '#6B2A24', '#D98A7A')
    'salted_fish'             = @('long',      '#9AA8B4', '#5E6870', '#E0D8C8')
    'soy_paste_chunk'         = @('sheet',     '#7A4A1E', '#3E2410', '#9A6B2A')
    'soy_residue'             = @('cluster',   '#8A6B3A', '#4E3A18', '#A88A54')
    # 料理
    # 豆浆是用碗盛的（喝完返还空碗），占位图也用碗
    'soy_milk'                = @('bowl',      '#F2F2E8', '#9A9A8A', '#D8D8C8')
    'old_style_guo_bao_rou'   = @('bowl',      '#C87A3C', '#7A4218', '#E8C8A0')
    'new_style_guo_bao_rou'   = @('bowl',      '#C4502C', '#7A2A14', '#E8A860')
    'sweet_potato_porridge'   = @('bowl',      '#E8C46A', '#A8842A', '#F5E4B0')
    'zhu_rou_dun_fen_tiao'    = @('bowl',      '#B58A6A', '#5E3A24', '#E8DCC0')
    'suan_cai_chao_fen_tiao'  = @('bowl',      '#D9C08A', '#8A7440', '#E8DCC0')
    'sweet_potato_starch'     = @('grain',     '#F2EDDC', '#B0A583', '#FFFFFF')
    'vermicelli'              = @('noodles',   '#E8DCC0', '#9A8A60', '#F5EEDC')
    'baked_sweet_potato'      = @('long',      '#8A4A26', '#4E2410', '#C97A3C')
    'red_chili'               = @('long',      '#C42A1E', '#6B1410', '#E85A3A')
    'red_chili_seeds'         = @('grain',     '#E8DCAA', '#A89058', '#F5EED0')
    'shrimp'                  = @('pod',       '#E8826A', '#9A4636', '#F2B0A0')
    'spicy_cabbage'           = @('leafy',     '#C4442A', '#7A2416', '#E8A08A')
    'fish_sauce'              = @('bottle',    '#6B4416', '#3A2408', '#8A5A24')
    'shrimp_paste'            = @('bottle',    '#8A4A34', '#4E2418', '#A8624A')
    'buckwheat_cold_noodles'  = @('bowl',      '#D8CCB0', '#8A7A58', '#C4442A')
    'chili_oil'               = @('bottle',    '#C4442A', '#6B1410', '#E8825A')
    'chili_sauce'             = @('bowl',      '#B5301A', '#6B1410', '#E05A3A')
    'liang_ban_xian_cai'      = @('bowl',      '#D9A03A', '#8A5A18', '#7A9A3A')
    'ming_tai_yu_si'          = @('bowl',      '#E8DCC0', '#A89058', '#C4442A')
    'zhan_jiang_cai'          = @('bowl',      '#7FA05A', '#3E5A24', '#8A5A2B')
    'da_fan_bao'              = @('leafy',     '#DCE8C0', '#7A8C55', '#F2F6E4')
    'jia_xian_huang_gua_pao_cai' = @('speckled', '#7A9A3A', '#44561C', '#C4442A')
    'liang_ban_hua_cai'       = @('bowl',      '#EDE3C8', '#9A8A60', '#C4442A')
    'da_feng_shou'            = @('bowl',      '#B58A5A', '#5E3A1E', '#F0C93F')
    'bai_cai_dou_fu_dun_fen_tiao' = @('bowl',  '#EFE3C8', '#9A8A60', '#E8DCC0')
    'la_niu_rou_tang_fan'     = @('bowl',      '#C4502C', '#6B2414', '#E8DCC0')
    'la_bai_cai_chao_fan'     = @('bowl',      '#D8503A', '#7A2416', '#F2E8C0')
    'tu_dou_bing'             = @('bowl',      '#E8C46A', '#A8842A', '#C4442A')
    'xia_ren_zhu_rou_xian_shui_jiao' = @('dumpling', '#EDE3C8', '#9A8A60', '#E8826A')
    'xia_jiang_chao_ji_dan'   = @('bowl',      '#E8B93C', '#96700F', '#E8826A')
    'xia_jiang_dun_dou_fu'    = @('bowl',      '#E8D9A8', '#9A8850', '#8A4A34')
    'hai_xian_dou_fu_tang'    = @('bowl',      '#E8DCC0', '#8A7A58', '#E8826A')
    'san_xian_xian_shui_jiao' = @('dumpling',  '#EDE3C8', '#9A8A60', '#7FA05A')
    'bean_sprouts'            = @('grain',     '#E8F0C0', '#8AA05A', '#FFFFFF')
    'sour_corn_kernels'       = @('grain',     '#F0D45C', '#A88A1E', '#C9B06A')
    'peanut'                  = @('cluster',   '#C9A05A', '#8A6B3A', '#E8D0A0')
    'red_bean'                = @('grain',     '#B5301A', '#6B1410', '#D8503A')
    'pig_liver'               = @('sheet',     '#8A3A3A', '#4E1A1A', '#B5534A')
    'red_sausage'             = @('pod',       '#B5301A', '#6B1410', '#D8503A')
    'smoked_pork_hock'        = @('bone',      '#8A5A3A', '#4E2A14', '#F0EDE4')
    'smoked_pig_liver'        = @('sheet',     '#6B3A2A', '#3A1E14', '#9A5A3A')
    'yu_mi_lao'               = @('bowl',      '#F0C93F', '#A88A1E', '#E8B93C')
    'cha_zi_zhou'             = @('bowl',      '#EFD98A', '#8A7440', '#F2E8C0')
    'roasted_peanuts'         = @('cluster',   '#C97A3C', '#8A4E20', '#E8B06A')
    'nian_dou_bao'            = @('dumpling',  '#EDE3C8', '#9A8A60', '#B5301A')
    'xun_jiang_pin_pan'       = @('bowl',      '#8A5A3A', '#4E2A14', '#B5301A')
    'peanut_butter'           = @('bottle',    '#C9A05A', '#8A6B3A', '#E8D0A0')
    'orange_guo_bao_rou'      = @('bowl',      '#E08A2E', '#8A4E12', '#F0C93F')
    'la_pi'                   = @('sheet',     '#E8DCC0', '#A89058', '#F5EEDC')
    'jiang_ban_la_pi'         = @('bowl',      '#E8DCC0', '#A89058', '#C4442A')
    'la_bai_cai_tang_fan'     = @('bowl',      '#D8503A', '#7A2416', '#EFE3C8')
    'cong_shao_hai_shen'      = @('bowl',      '#5E3A2E', '#2E1A12', '#7FB25A')
    'hai_shen_dou_fu_tang'    = @('bowl',      '#E8DCC0', '#8A7A58', '#4A3A2E')
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
    'xian_yu_bing_zi'         = @('bowl',      '#D9B45C', '#8A6B1E', '#9AA8B4')
    'da_jiang_tang'           = @('bowl',      '#B08048', '#5E3F1E', '#E8DCC0')
    'de_mo_li_dun_yu'         = @('bowl',      '#B58A5A', '#5E3A1E', '#9AA8B4')
    'su_bo_tang'              = @('bowl',      '#C4502C', '#7A2A14', '#E8DCC0')
    'suan_cai_hai_xian_guo'   = @('bowl',      '#D9C08A', '#8A7440', '#E8826A')
    'shen_ji_tang'            = @('bowl',      '#E8E0C0', '#9A8A60', '#E0CE9A')
    'xiao_ji_dun_mo_gu'       = @('bowl',      '#B58A5A', '#5E3A1E', '#C08A4A')
    # 容器
    'large_basin'             = @('basin',     '#C9CFD4', '#6B7378', '#EDF2F5')
    'grilled_chicken_frame'   = @('bone',      '#C97A3C', '#7A4218', '#E8B06A')
}

# 这些「调料瓶」照原版药水的方式画：模型只有两层（原版的液面 + 玻璃瓶），颜色由代码染，
# 所以它们不需要单独的物品贴图，模型也不要用 layer0 = 自己的贴图那种写法。
$potionBottles = @(
    'soy_sauce', 'vinegar', 'sour_water', 'white_vinegar', 'fish_sauce',
    'shrimp_paste', 'chili_oil', 'peanut_butter', 'cooking_oil', 'animal_oil'
    , 'kvass'
)

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
    'mushroom' = ${function:Shape-Mushroom}
    'basin'    = ${function:Shape-Basin}
    'grate'    = ${function:Shape-Grate}
    'pancake'  = ${function:Shape-Pancake}
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
    if ($potionBottles -contains $id) {
        $model = [ordered]@{
            parent   = 'minecraft:item/generated'
            textures = [ordered]@{
                layer0 = 'minecraft:item/potion_overlay'
                layer1 = 'minecraft:item/potion'
            }
        }
    } else {
        $model = [ordered]@{
            parent   = 'item/generated'
            textures = [ordered]@{ layer0 = "dongbei_delight:item/$id" }
        }
    }
    $model | ConvertTo-Json -Depth 5 | Set-Content -Path (Join-Path $modelDir "$id.json") -Encoding utf8
}

Write-Host "已生成 $($ids.Count) 个物品的占位贴图与模型：" -ForegroundColor Green
Write-Host "  贴图: $textureDir"
Write-Host "  模型: $modelDir"
