#Requires -Version 7
<#
    生成大盆菜里各种食材的专用小贴图（16×16，纯像素风）。

    这些贴图是给"盆里的食材模型"用的：每种食材一张，颜色照实物、
    图案只用最简单的几种（点/格/条/上下分色），保证贴近 mc 的像素画风，
    又不会碎成一堆看不出是什么的色块。

    用法： pwsh -File tools\generate_pot_food_textures.ps1
#>
param(
    [string]$Root = 'H:\IdeaProjects\dongbei_delight1.21.1'
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing.Common
$outDir = Join-Path $Root 'src\main\resources\assets\dongbei_delight\textures\block'

function C([string]$hex, [int]$a = 255) {
    $c = [System.Drawing.ColorTranslator]::FromHtml($hex)
    return [System.Drawing.Color]::FromArgb($a, $c.R, $c.G, $c.B)
}

# name = 贴图名；base = 主色；alt = 点缀色；pattern = 图案
$defs = @(
    @{ name = 'pot_broth';        base = '#4A3222'; alt = '#331F12'; pattern = 'spots'; density = 7 }
    @{ name = 'pot_meat';         base = '#6E3B22'; alt = '#8A4A2A'; pattern = 'spots'; density = 5 }
    @{ name = 'pot_chicken';      base = '#9A6B3E'; alt = '#B98550'; pattern = 'spots'; density = 5 }
    # 排骨：肉色为主，骨头纹放在贴图第 5~7 行。
    # 注意 MC 的规则：面没写 uv 时，uv 是按元素坐标从贴图里取的 —— 我们盆里的食材
    # 每个都是 1 格大小，所以**侧面只会采到第 8、9 行**（俯视面采的是 (x,z) 那一点）。
    # 骨头纹要是画在第 8、9 行上，整块排骨就会全白（看着像豆腐）。放在 5~7 行，
    # 肉块顶面才会出现一道骨头纹，侧面保持酱色。
    @{ name = 'pot_rib';          base = '#7A3F24'; alt = '#F0EAE0'; pattern = 'bone';  density = 0 }
    # 骨头：带一点暖黄（象牙白），比盆内壁的冷灰白更有"骨头"味，也不至于和盆壁糊在一起
    # 注意 density 不能高：每个食材方块只采到贴图上的一个像素，斑点太密骨头就会变成"花斑"的
    @{ name = 'pot_bone';         base = '#E7D9BE'; alt = '#8A7458'; pattern = 'spots'; density = 4 }
    @{ name = 'pot_corn';         base = '#E8C048'; alt = '#C09A28'; pattern = 'grid';  density = 0 }
    # 土豆：偏黄一点，别白得像豆腐
    @{ name = 'pot_potato';       base = '#D6BC6C'; alt = '#B89A48'; pattern = 'spots'; density = 4 }
    @{ name = 'pot_eggplant';     base = '#6A4A78'; alt = '#8A6A98'; pattern = 'spots'; density = 4 }
    @{ name = 'pot_pepper';       base = '#4E8C3A'; alt = '#6AA84E'; pattern = 'spots'; density = 4 }
    @{ name = 'pot_bean';         base = '#5E8C46'; alt = '#3F6A30'; pattern = 'stripe'; density = 0 }
    @{ name = 'pot_sourcabbage';  base = '#C8D890'; alt = '#A8BC70'; pattern = 'stripe'; density = 0 }
    @{ name = 'pot_noodle';       base = '#E8E0C8'; alt = '#C8BCA0'; pattern = 'stripe'; density = 0 }
    @{ name = 'pot_mushroom';     base = '#6A5038'; alt = '#C8B490'; pattern = 'cap';   density = 0 }
    @{ name = 'pot_bing';         base = '#E0C078'; alt = '#B8863C'; pattern = 'spots'; density = 6 }
    @{ name = 'pot_sausage';      base = '#4A2A30'; alt = '#E8D8C8'; pattern = 'spots'; density = 8 }
    @{ name = 'pot_fat';          base = '#F0E8DC'; alt = '#E0C8B8'; pattern = 'spots'; density = 3 }
    # 酸菜海鲜锅用的两样：虾（粉橙）和豆腐（奶白）
    @{ name = 'pot_shrimp';       base = '#E88A5A'; alt = '#F4B088'; pattern = 'spots'; density = 5 }
    @{ name = 'pot_tofu';         base = '#F2EBD8'; alt = '#DCCFB4'; pattern = 'spots'; density = 3 }
    # 生蚝：灰白壳 + 深一点的壳缘
    @{ name = 'pot_oyster';       base = '#B5AFA2'; alt = '#6B655A'; pattern = 'spots'; density = 7 }
)

foreach ($d in $defs) {
    $bmp = New-Object System.Drawing.Bitmap (16, 16)
    $rnd = New-Object System.Random ($d.name.GetHashCode() -band 0x7fffffff)
    for ($y = 0; $y -lt 16; $y++) {
        for ($x = 0; $x -lt 16; $x++) {
            $col = C $d.base
            switch ($d.pattern) {
                'spots'  { if ($rnd.Next(0, 100) -lt ($d.density * 4)) { $col = C $d.alt } }
                'grid'   { if (($x % 3) -eq 0 -or ($y % 3) -eq 0) { $col = C $d.alt } }
                'stripe' { if (($x % 4) -eq 0) { $col = C $d.alt } }
                'bone'   { if ($y -ge 5 -and $y -le 7) { $col = C $d.alt } }
                'cap'    { if ($y -lt 10) { $col = C $d.base } else { $col = C $d.alt } }
            }
            $bmp.SetPixel($x, $y, $col)
        }
    }
    $path = Join-Path $outDir ($d.name + '.png')
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Host ("已生成 {0}.png（{1}）" -f $d.name, $d.pattern) -ForegroundColor DarkGray
}
Write-Host ("共 {0} 张食材贴图 -> {1}" -f $defs.Count, $outDir) -ForegroundColor Green
