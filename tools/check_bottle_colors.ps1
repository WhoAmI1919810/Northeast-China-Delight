<#
    调料液体色号 × 原版药水色号 比对工具

    做两件事：
      1. 从 NeoForge 的 Minecraft 源码 jar 里解析出原版所有药水的色号；
      2. 和 BottleColors.java 里的调料色号逐个算 CIEDE2000 色差，挑出「撞色」的。

    用法（在项目根目录）：
        pwsh -NoProfile -File tools\check_bottle_colors.ps1
        pwsh -NoProfile -File tools\check_bottle_colors.ps1 -Threshold 12

    判定标准：与原版任意药水的 ΔE00 小于阈值（默认 10）就算撞色，需要改。
    改完 BottleColors.java 记得同步 textures/block 下同名的流体贴图。
#>
param(
    [double]$Threshold = 10.0,
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem

$bottleSrcPath = Join-Path $ProjectRoot 'src\main\java\com\gunmu\northeast_china_delight\item\BottleColors.java'
if (-not (Test-Path -LiteralPath $bottleSrcPath)) { throw "找不到 $bottleSrcPath" }

$jarPath = Get-ChildItem -Path (Join-Path $ProjectRoot 'build\moddev\artifacts') -Filter 'neoforge-*-sources.jar' -ErrorAction SilentlyContinue |
    Sort-Object Name -Descending | Select-Object -First 1 -ExpandProperty FullName
if (-not $jarPath) { throw "找不到 Minecraft 源码 jar，请先跑一次 gradlew build" }

# ---------- 从源码 jar 里读原版数据 ----------
$zip = [System.IO.Compression.ZipFile]::OpenRead($jarPath)
function Get-EntryText($z, $name) {
    $e = $z.Entries | Where-Object { $_.FullName -eq $name }
    if (-not $e) { throw "jar 中找不到 $name" }
    $sr = New-Object System.IO.StreamReader($e.Open())
    $t = $sr.ReadToEnd()
    $sr.Close()
    return $t
}
$mobEffectsSrc = Get-EntryText $zip 'net/minecraft/world/effect/MobEffects.java'
$potionsSrc    = Get-EntryText $zip 'net/minecraft/world/item/alchemy/Potions.java'
$zip.Dispose()

# ---------- 色彩工具 ----------
function SrgbToLinear($c) {
    $v = $c / 255.0
    if ($v -le 0.04045) { return $v / 12.92 }
    return [Math]::Pow(($v + 0.055) / 1.055, 2.4)
}
function LabF($t) {
    if ($t -gt 0.008856) { return [Math]::Pow($t, 1.0 / 3.0) }
    return (841.0 / 108.0) * $t + (4.0 / 29.0)
}
function RgbToLab($rgb) {
    $r = SrgbToLinear (($rgb -shr 16) -band 0xFF)
    $g = SrgbToLinear (($rgb -shr 8) -band 0xFF)
    $b = SrgbToLinear ($rgb -band 0xFF)
    $X = ($r * 0.4124564 + $g * 0.3575761 + $b * 0.1804375) / 0.95047
    $Y = ($r * 0.2126729 + $g * 0.7151522 + $b * 0.0721750)
    $Z = ($r * 0.0193339 + $g * 0.1191920 + $b * 0.9503041) / 1.08883
    $fx = LabF $X; $fy = LabF $Y; $fz = LabF $Z
    return [pscustomobject]@{ L = 116.0 * $fy - 16.0; A = 500.0 * ($fx - $fy); B = 200.0 * ($fy - $fz) }
}
function DegRad($d) { return $d * [Math]::PI / 180.0 }
function Get-DeltaE00($lab1, $lab2) {
    $L1 = $lab1.L; $a1 = $lab1.A; $b1 = $lab1.B
    $L2 = $lab2.L; $a2 = $lab2.A; $b2 = $lab2.B
    $C1 = [Math]::Sqrt($a1 * $a1 + $b1 * $b1)
    $C2 = [Math]::Sqrt($a2 * $a2 + $b2 * $b2)
    $Cbar = ($C1 + $C2) / 2.0
    $p25 = [Math]::Pow(25.0, 7.0)
    $Cbar7 = [Math]::Pow($Cbar, 7.0)
    $G = 0.5 * (1.0 - [Math]::Sqrt($Cbar7 / ($Cbar7 + $p25)))
    $a1p = (1.0 + $G) * $a1
    $a2p = (1.0 + $G) * $a2
    $C1p = [Math]::Sqrt($a1p * $a1p + $b1 * $b1)
    $C2p = [Math]::Sqrt($a2p * $a2p + $b2 * $b2)
    $h1p = 0.0
    if (-not (($b1 -eq 0) -and ($a1p -eq 0))) {
        $h1p = [Math]::Atan2($b1, $a1p) * 180.0 / [Math]::PI
        if ($h1p -lt 0) { $h1p += 360.0 }
    }
    $h2p = 0.0
    if (-not (($b2 -eq 0) -and ($a2p -eq 0))) {
        $h2p = [Math]::Atan2($b2, $a2p) * 180.0 / [Math]::PI
        if ($h2p -lt 0) { $h2p += 360.0 }
    }
    $dLp = $L2 - $L1
    $dCp = $C2p - $C1p
    $dhp = 0.0
    if (($C1p * $C2p) -ne 0) {
        $dhp = $h2p - $h1p
        if ($dhp -gt 180.0) { $dhp -= 360.0 }
        elseif ($dhp -lt -180.0) { $dhp += 360.0 }
    }
    $dHp = 2.0 * [Math]::Sqrt($C1p * $C2p) * [Math]::Sin((DegRad $dhp) / 2.0)
    $Lbarp = ($L1 + $L2) / 2.0
    $Cbarp = ($C1p + $C2p) / 2.0
    $hbarp = 0.0
    if (($C1p * $C2p) -eq 0) { $hbarp = $h1p + $h2p }
    elseif ([Math]::Abs($h1p - $h2p) -le 180.0) { $hbarp = ($h1p + $h2p) / 2.0 }
    elseif (($h1p + $h2p) -lt 360.0) { $hbarp = ($h1p + $h2p + 360.0) / 2.0 }
    else { $hbarp = ($h1p + $h2p - 360.0) / 2.0 }
    $T = 1.0 `
        - 0.17 * [Math]::Cos((DegRad ($hbarp - 30.0))) `
        + 0.24 * [Math]::Cos((DegRad (2.0 * $hbarp))) `
        + 0.32 * [Math]::Cos((DegRad (3.0 * $hbarp + 6.0))) `
        - 0.20 * [Math]::Cos((DegRad (4.0 * $hbarp - 63.0)))
    $dTheta = 30.0 * [Math]::Exp(-[Math]::Pow(($hbarp - 275.0) / 25.0, 2.0))
    $Cbarp7 = [Math]::Pow($Cbarp, 7.0)
    $Rc = 2.0 * [Math]::Sqrt($Cbarp7 / ($Cbarp7 + $p25))
    $Sl = 1.0 + 0.015 * [Math]::Pow($Lbarp - 50.0, 2.0) / [Math]::Sqrt(20.0 + [Math]::Pow($Lbarp - 50.0, 2.0))
    $Sc = 1.0 + 0.045 * $Cbarp
    $Sh = 1.0 + 0.015 * $Cbarp * $T
    $RT = -[Math]::Sin((DegRad (2.0 * $dTheta))) * $Rc
    $t1 = $dLp / $Sl
    $t2 = $dCp / $Sc
    $t3 = $dHp / $Sh
    return [Math]::Sqrt($t1 * $t1 + $t2 * $t2 + $t3 * $t3 + $RT * $t2 * $t3)
}
function Get-Redmean($a, $b) {
    $r1 = ($a -shr 16) -band 0xFF; $g1 = ($a -shr 8) -band 0xFF; $b1 = $a -band 0xFF
    $r2 = ($b -shr 16) -band 0xFF; $g2 = ($b -shr 8) -band 0xFF; $b2 = $b -band 0xFF
    $rm = ($r1 + $r2) / 2.0
    $dr = $r1 - $r2; $dg = $g1 - $g2; $db = $b1 - $b2
    return [Math]::Sqrt((2 + $rm / 256.0) * $dr * $dr + 4.0 * $dg * $dg + (2 + (255 - $rm) / 256.0) * $db * $db)
}

# ---------- 状态效果色号 ----------
$effectColor = @{}
$rxEffect = [regex]'([A-Za-z_]+)\s*=\s*register\(\s*"[a-z_]+"\s*,\s*new\s+\w+\s*\(\s*MobEffectCategory\.[A-Z_]+\s*,\s*(\d+)'
foreach ($m in $rxEffect.Matches($mobEffectsSrc)) {
    $effectColor[$m.Groups[1].Value] = [int]$m.Groups[2].Value
}

# ---------- 药水定义 → 色号 ----------
# 算法同 PotionContents.getColorOptional()：按效果等级（amplifier+1）加权平均效果颜色；
# 没有效果的药水（water / mundane / thick / awkward）用原版回退色 BASE_POTION_COLOR。
$potionList = New-Object System.Collections.ArrayList
$chunks = [regex]::Split($potionsSrc, 'public static final Holder<Potion> ')
for ($i = 1; $i -lt $chunks.Count; $i++) {
    $c = $chunks[$i]
    $mn = [regex]::Match($c, 'register\(\s*"([a-z_]+)"')
    if (-not $mn.Success) { continue }
    $regName = $mn.Groups[1].Value
    $r = 0; $g = 0; $b = 0; $w = 0
    foreach ($e in [regex]::Matches($c, 'MobEffects\.([A-Z_]+)\s*,\s*(\d+)\s*(?:,\s*(\d+))?')) {
        $efName = $e.Groups[1].Value
        if (-not $effectColor.ContainsKey($efName)) { throw "未知状态效果：$efName（药水 $regName）" }
        $col = $effectColor[$efName]
        $amp = 0
        if ($e.Groups[3].Success) { $amp = [int]$e.Groups[3].Value }
        $n = $amp + 1
        $r += $n * (($col -shr 16) -band 0xFF)
        $g += $n * (($col -shr 8) -band 0xFF)
        $b += $n * ($col -band 0xFF)
        $w += $n
    }
    if ($w -gt 0) {
        $rr = [int][Math]::Floor($r / $w)
        $gg = [int][Math]::Floor($g / $w)
        $bb = [int][Math]::Floor($b / $w)
        $rgb = (($rr -shl 16) -bor ($gg -shl 8) -bor $bb)
    } else {
        $rgb = 0x385DC6
    }
    [void]$potionList.Add([pscustomobject]@{ Name = $regName; Rgb = $rgb; Hex = ('#{0:X6}' -f $rgb) })
}

# ---------- 调料色号 ----------
$seasoning = New-Object System.Collections.ArrayList
foreach ($line in (Get-Content -LiteralPath $bottleSrcPath)) {
    $m = [regex]::Match($line, 'put\(ModItems\.(\w+),\s*0x([0-9A-Fa-f]{6})\)')
    if (-not $m.Success) { continue }
    $label = $m.Groups[1].Value
    $cm = [regex]::Match($line, '//\s*(\S+?)[:：]')
    if ($cm.Success) { $label = $cm.Groups[1].Value }
    $rgb = [Convert]::ToInt32($m.Groups[2].Value, 16)
    [void]$seasoning.Add([pscustomobject]@{ Id = $m.Groups[1].Value; Label = $label; Rgb = $rgb; Hex = ('#{0:X6}' -f $rgb) })
}

# ---------- 输出 ----------
Write-Output ''
Write-Output ("原版药水色号（{0} 种药水，{1} 种颜色）" -f $potionList.Count, ($potionList | Group-Object Hex).Count)
Write-Output '--------------------------------------------------'
foreach ($u in ($potionList | Group-Object Hex | Sort-Object Name)) {
    Write-Output ('  {0}   {1}' -f $u.Name, (($u.Group | ForEach-Object { $_.Name }) -join ', '))
}
Write-Output ''

Write-Output ("调料液体色号（判定阈值 ΔE00 < {0}）" -f $Threshold)
Write-Output '--------------------------------------------------'
$flagged = New-Object System.Collections.ArrayList
foreach ($s in $seasoning) {
    $lab1 = RgbToLab $s.Rgb
    $scored = New-Object System.Collections.ArrayList
    foreach ($p in $potionList) {
        [void]$scored.Add([pscustomobject]@{ Potion = $p.Name; Hex = $p.Hex; DE = (Get-DeltaE00 $lab1 (RgbToLab $p.Rgb)); RM = (Get-Redmean $s.Rgb $p.Rgb) })
    }
    $top = $scored | Sort-Object DE | Select-Object -First 3
    $mark = 'OK    '
    if ($top[0].DE -lt $Threshold) { $mark = '撞色 !' }
    Write-Output ('  {0} {1,-8} {2}   最近: {3}' -f $mark, $s.Label, $s.Hex, (($top | ForEach-Object { '{0} {1} ΔE00={2:N1}' -f $_.Potion, $_.Hex, $_.DE }) -join ' | '))
    foreach ($t in $top) {
        if ($t.DE -lt $Threshold) {
            [void]$flagged.Add([pscustomobject]@{ Seasoning = $s.Label; Hex = $s.Hex; Potion = $t.Potion; PH = $t.Hex; DE = [Math]::Round($t.DE, 2); RM = [Math]::Round($t.RM, 1) })
        }
    }
}
Write-Output ''

Write-Output '需要处理的撞色项'
Write-Output '--------------------------------------------------'
if ($flagged.Count -eq 0) {
    Write-Output '  无'
} else {
    foreach ($f in ($flagged | Sort-Object DE)) {
        Write-Output ('  {0,-8} {1}  ≈  {2,-18} {3}   ΔE00={4}  redmean={5}' -f $f.Seasoning, $f.Hex, $f.Potion, $f.PH, $f.DE, $f.RM)
    }
}
Write-Output ''

Write-Output '调料之间的近似情况（ΔE00 < 10 基本看不出差别）'
Write-Output '--------------------------------------------------'
$any = $false
for ($i = 0; $i -lt $seasoning.Count; $i++) {
    for ($j = $i + 1; $j -lt $seasoning.Count; $j++) {
        $de = Get-DeltaE00 (RgbToLab $seasoning[$i].Rgb) (RgbToLab $seasoning[$j].Rgb)
        if ($de -lt 10.0) {
            $any = $true
            Write-Output ('  {0} {1}  ≈  {2} {3}   ΔE00={4:N1}' -f $seasoning[$i].Label, $seasoning[$i].Hex, $seasoning[$j].Label, $seasoning[$j].Hex, $de)
        }
    }
}
if (-not $any) { Write-Output '  无' }
Write-Output ''
