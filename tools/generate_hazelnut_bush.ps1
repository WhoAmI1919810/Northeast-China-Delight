#Requires -Version 7
<#
    生成榛子丛的：方块状态、4 个生长阶段的模型与占位贴图、掉落表。

    榛子丛照搬原版甜浆果丛：4 个生长年龄（0~3），贴图是十字交叉的 cross 模型。
    这里的贴图只是开发期占位，正式美术直接覆盖
    assets/dongbei_delight/textures/block/hazelnut_bush_stage<N>.png 即可。

    用法： pwsh -File tools/generate_hazelnut_bush.ps1
    （只动 hazelnut_bush 自己的文件，不会碰其它作物素材）
#>
param(
    [string]$AssetsRoot = (Join-Path $PSScriptRoot '..\src\main\resources\assets\dongbei_delight'),
    [string]$DataRoot = (Join-Path $PSScriptRoot '..\src\main\resources\data\dongbei_delight')
)

Add-Type -AssemblyName System.Drawing

$blockStateDir = Join-Path $AssetsRoot 'blockstates'
$blockModelDir = Join-Path $AssetsRoot 'models\block'
$blockTextureDir = Join-Path $AssetsRoot 'textures\block'
$lootDir = Join-Path $DataRoot 'loot_table\blocks'

foreach ($dir in @($blockStateDir, $blockModelDir, $blockTextureDir, $lootDir)) {
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
}

function C([string]$hex) { [System.Drawing.ColorTranslator]::FromHtml($hex) }
function Brush([string]$hex) { New-Object System.Drawing.SolidBrush (C $hex) }
function Pen([string]$hex) { New-Object System.Drawing.Pen (C $hex), 1 }

function Oval($g, $fill, $edge, $x, $y, $w, $h) {
    $b = Brush $fill; $p = Pen $edge
    $g.FillEllipse($b, $x, $y, $w, $h)
    $g.DrawEllipse($p, $x, $y, $w, $h)
    $b.Dispose(); $p.Dispose()
}

function Bar($g, $color, $x1, $y1, $x2, $y2, $width) {
    $p = New-Object System.Drawing.Pen (C $color), $width
    $p.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $p.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
    $g.DrawLine($p, $x1, $y1, $x2, $y2)
    $p.Dispose()
}

function New-Canvas {
    $bitmap = New-Object System.Drawing.Bitmap 16, 16
    $g = [System.Drawing.Graphics]::FromImage($bitmap)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
    $g.Clear([System.Drawing.Color]::Transparent)
    return @{ Bitmap = $bitmap; Graphics = $g }
}

$leaf = '#4E8A32'
$leafDark = '#356B22'
$leafLight = '#69A63F'
$unripe = '#A8C46A'
$ripe = '#9A5F2E'

function Draw-Bush($g, [int]$stage) {
    switch ($stage) {
        0 {   # 幼苗：一小丛嫩叶
            Bar $g $leafDark 8 15 8 11 1
            Oval $g $leaf $leafDark 5 10 6 5
            Oval $g $leafLight $leafDark 8 11 5 4
        }
        1 {   # 长成一丛
            Bar $g $leafDark 8 15 8 10 1
            Oval $g $leaf $leafDark 3 8 9 7
            Oval $g $leafLight $leafDark 8 9 7 6
            Bar $g $leafLight 5 12 9 10 1
        }
        2 {   # 开始结青果
            Bar $g $leafDark 8 15 8 9 1
            Oval $g $leaf $leafDark 2 6 11 9
            Oval $g $leafLight $leafDark 8 7 8 8
            Oval $g $leaf $leafDark 5 3 8 6
            Bar $g $leafLight 4 11 11 8 1
            Oval $g $unripe '#5E7A2E' 4 8 3 3
            Oval $g $unripe '#5E7A2E' 10 6 3 3
            Oval $g $unripe '#5E7A2E' 8 11 3 3
        }
        3 {   # 完熟：满丛叶子 + 三颗榛子
            Bar $g $leafDark 8 15 8 8 1
            Oval $g $leaf $leafDark 1 5 12 10
            Oval $g $leaf $leafDark 7 3 9 9
            Oval $g $leafLight $leafDark 2 3 8 7
            Oval $g $leafLight $leafDark 8 9 7 6
            Bar $g $leafLight 3 12 12 9 1
            Oval $g $ripe '#5E3A18' 3 6 4 4
            Oval $g $ripe '#5E3A18' 9 4 4 4
            Oval $g $ripe '#5E3A18' 7 11 4 4
        }
    }
}

for ($stage = 0; $stage -le 3; $stage++) {
    $canvas = New-Canvas
    Draw-Bush $canvas.Graphics $stage
    $canvas.Graphics.Dispose()
    $canvas.Bitmap.Save((Join-Path $blockTextureDir "hazelnut_bush_stage$stage.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $canvas.Bitmap.Dispose()

    # 和原版甜浆果丛一样，用十字交叉的 cross 模型。
    # render_type 必须写在模型里：原版是把「甜浆果丛」这一个方块硬编码成 cutout 的，
    # 榛子丛是另一个方块、不在那份名单里，不写就会退回 solid —— 透明像素会变成黑块。
    $model = [ordered]@{
        parent      = 'minecraft:block/cross'
        render_type = 'minecraft:cutout'
        textures    = [ordered]@{ cross = "dongbei_delight:block/hazelnut_bush_stage$stage" }
    }
    $model | ConvertTo-Json -Depth 5 | Set-Content -Path (Join-Path $blockModelDir "hazelnut_bush_stage$stage.json") -Encoding utf8
}

$variants = [ordered]@{}
for ($age = 0; $age -le 3; $age++) {
    $variants["age=$age"] = [ordered]@{ model = "dongbei_delight:block/hazelnut_bush_stage$age" }
}
([ordered]@{ variants = $variants }) | ConvertTo-Json -Depth 8 |
    Set-Content -Path (Join-Path $blockStateDir 'hazelnut_bush.json') -Encoding utf8

# --- 掉落表：与原版甜浆果丛一致 ---
#   age=3：2~3 个榛子（受抢夺影响）
#   age=2：1~2 个榛子（受抢夺影响）
#   age=0/1：什么都不掉
$loot = [ordered]@{
    type      = 'minecraft:block'
    functions = @([ordered]@{ function = 'minecraft:explosion_decay' })
    pools     = @()
    random_sequence = 'dongbei_delight:blocks/hazelnut_bush'
}

foreach ($entry in @(@{ age = '3'; min = 2.0; max = 3.0 }, @{ age = '2'; min = 1.0; max = 2.0 })) {
    $loot.pools += [ordered]@{
        bonus_rolls = 0.0
        conditions  = @(
            [ordered]@{
                block      = 'dongbei_delight:hazelnut_bush'
                condition  = 'minecraft:block_state_property'
                properties = [ordered]@{ age = $entry.age }
            }
        )
        entries     = @([ordered]@{ type = 'minecraft:item'; name = 'dongbei_delight:hazelnut' })
        functions   = @(
            [ordered]@{
                add      = $false
                count    = [ordered]@{ type = 'minecraft:uniform'; max = $entry.max; min = $entry.min }
                function = 'minecraft:set_count'
            },
            [ordered]@{
                enchantment = 'minecraft:fortune'
                formula     = 'minecraft:uniform_bonus_count'
                function    = 'minecraft:apply_bonus'
                parameters  = [ordered]@{ bonusMultiplier = 1 }
            }
        )
        rolls       = 1.0
    }
}

$loot | ConvertTo-Json -Depth 12 | Set-Content -Path (Join-Path $lootDir 'hazelnut_bush.json') -Encoding utf8

Write-Host '已生成榛子丛的方块状态 / 模型 / 占位贴图 / 掉落表' -ForegroundColor Green
Write-Host "  方块贴图: $blockTextureDir\hazelnut_bush_stage0..3.png"
Write-Host "  掉落表  : $lootDir\hazelnut_bush.json"
