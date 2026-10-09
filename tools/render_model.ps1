#Requires -Version 7
<#
    离线渲染方块模型：把 models/block/*.json 用自写的软件光栅化器画成 PNG。
    用途：在启动游戏之前反复核对建模（比如大盆）到底像不像，省掉一轮轮进游戏的等待。

    与游戏内一致性：
      * 元素坐标 0..16（1 单位 = 1 像素），Y 轴向上
      * rotation 单轴 x/y/z，角度 0/±22.5/±45（原版 BlockElement 的限制）
      * rescale 默认 false；为 true 时按原版 FaceBakery 的 RESCALE 公式缩放
      * 面明暗按“旋转后顶点算出的实际朝向”决定（等价于 FaceBakery.calculateFacing）：
        up 1.0 / down 0.5 / north,south 0.8 / west,east 0.6
      * cutout：alpha < 8 的像素直接丢弃（透明处保留后面的东西）
      * UV 按 BlockElementFace 的朝向映射，纹理 rotation 支持 0/90/180/270

    用法：
      pwsh -File tools\render_model.ps1 -Model <json> -Out <png> [-Views "45/22,135/22,0/10,0/85"]
          [-Scale 20] [-Tile 340] [-PanX 0] [-PanY 0]
#>
param(
    [Parameter(Mandatory = $true)][string]$Model,
    [Parameter(Mandatory = $true)][string]$Out,
    [string]$Views = '45/22,135/22,0/10,0/85',
    [double]$Scale = 20,
    [int]$Tile = 340,
    [double]$PanX = 0,
    [double]$PanY = 0,
    [string]$AssetsRoot = '',
    [string]$VanillaJar = '',
    [string]$Label = ''
)

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
if (-not $AssetsRoot) { $AssetsRoot = Join-Path $repoRoot 'src\main\resources\assets' }
if (-not $VanillaJar) {
    # 原版客户端 jar：优先取 GRADLE_USER_HOME，缺省用 ~/.gradle（Gradle 默认位置），可用 -VanillaJar 覆盖
    $gradleHome = if ($env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME } else { Join-Path $env:USERPROFILE '.gradle' }
    $VanillaJar = Join-Path $gradleHome 'caches\neoformruntime\artifacts\minecraft_1.21.1_client.jar'
}

if ($env:PV_STRICT) { $ErrorActionPreference = 'Stop' }

Add-Type -AssemblyName System.Drawing.Common
Add-Type -AssemblyName System.IO.Compression

$csharp = @'
using System;

public class PvFace {
    public float[] Px = new float[4];
    public float[] Py = new float[4];
    public float[] Pz = new float[4];
    public float[] U = new float[4];   // 归一化纹理坐标 0..1
    public float[] V = new float[4];
    public int Tex = -1;               // 贴图索引，-1 = 洋红占位
}

public class PvRenderer {
    // 注意：GDI+ 的 Format24bppRgb 在内存里是 B,G,R 顺序
    public static void FillBg(byte[] rgb, int tile, int stride, int br, int bg, int bb) {
        for (int y = 0; y < tile; y++) {
            int ck = (y / 8) % 2;
            for (int x = 0; x < tile; x++) {
                int c = ((x / 8) % 2 == ck) ? 0 : 14;
                int o = y * stride + x * 3;
                rgb[o] = (byte)Math.Min(255, bb + c);
                rgb[o + 1] = (byte)Math.Min(255, bg + c);
                rgb[o + 2] = (byte)Math.Min(255, br + c);
            }
        }
    }

    static float ShadeOf(double nx, double ny, double nz) {
        double ax = Math.Abs(nx), ay = Math.Abs(ny), az = Math.Abs(nz);
        if (ay >= ax && ay >= az) return ny >= 0 ? 1.00f : 0.50f;
        if (az >= ax) return 0.80f;
        return 0.60f;
    }

    public static void Render(PvFace[] faces, byte[][] texData, int[] texW, int[] texH,
                              byte[] rgb, int tile, int stride,
                              double yawDeg, double pitchDeg, double scale, double panX, double panY) {
        double a = yawDeg * Math.PI / 180.0, b = pitchDeg * Math.PI / 180.0;
        double[] zc = { Math.Sin(a) * Math.Cos(b), Math.Sin(b), Math.Cos(a) * Math.Cos(b) };
        double[] yc = { -Math.Sin(a) * Math.Sin(b), Math.Cos(b), -Math.Cos(a) * Math.Sin(b) };
        double[] xc = { Math.Cos(a), 0, -Math.Sin(a) };

        int n = tile * tile;
        float[] zb = new float[n];
        for (int i = 0; i < n; i++) zb[i] = -1e9f;

        double cx = tile / 2.0 + panX, cy = tile / 2.0 + panY;
        var sx = new double[4]; var sy = new double[4]; var sz = new double[4];
        int[] tris = { 0, 1, 2, 0, 2, 3 };

        foreach (var f in faces) {
            for (int i = 0; i < 4; i++) {
                double px = f.Px[i] - 8.0, py = f.Py[i] - 8.0, pz = f.Pz[i] - 8.0;
                double dx = px * xc[0] + py * xc[1] + pz * xc[2];
                double dy = px * yc[0] + py * yc[1] + pz * yc[2];
                double dz = px * zc[0] + py * zc[1] + pz * zc[2];
                sx[i] = cx + dx * scale;
                sy[i] = cy - dy * scale;
                sz[i] = dz * 1000.0;
            }
            // 与 FaceBakery.calculateFacing 相同：n = (P2 - P1) x (P0 - P1)
            double ax = f.Px[0] - f.Px[1], ay = f.Py[0] - f.Py[1], az = f.Pz[0] - f.Pz[1];
            double bx = f.Px[2] - f.Px[1], by = f.Py[2] - f.Py[1], bz = f.Pz[2] - f.Pz[1];
            double nx = by * az - bz * ay, ny = bz * ax - bx * az, nz = bx * ay - by * ax;
            // 背面剔除（cutout/solid 都是 CULL）
            if (nx * zc[0] + ny * zc[1] + nz * zc[2] <= 0) continue;
            float shade = ShadeOf(nx, ny, nz);

            byte[] tex = (f.Tex >= 0) ? texData[f.Tex] : null;
            int tw = (f.Tex >= 0) ? texW[f.Tex] : 1;
            int th = (f.Tex >= 0) ? texH[f.Tex] : 1;

            for (int t = 0; t < 6; t += 3) {
                int i0 = tris[t], i1 = tris[t + 1], i2 = tris[t + 2];
                double x0 = sx[i0], y0 = sy[i0], x1 = sx[i1], y1 = sy[i1], x2 = sx[i2], y2 = sy[i2];
                double area = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0);
                if (Math.Abs(area) < 1e-9) continue;
                int minX = (int)Math.Floor(Math.Min(x0, Math.Min(x1, x2)));
                int maxX = (int)Math.Ceiling(Math.Max(x0, Math.Max(x1, x2)));
                int minY = (int)Math.Floor(Math.Min(y0, Math.Min(y1, y2)));
                int maxY = (int)Math.Ceiling(Math.Max(y0, Math.Max(y1, y2)));
                if (minX < 0) minX = 0; if (minY < 0) minY = 0;
                if (maxX > tile - 1) maxX = tile - 1;
                if (maxY > tile - 1) maxY = tile - 1;
                for (int py = minY; py <= maxY; py++) {
                    for (int px = minX; px <= maxX; px++) {
                        double pxc = px + 0.5, pyc = py + 0.5;
                        double w0 = ((x1 - x0) * (pyc - y0) - (pxc - x0) * (y1 - y0)) / area;
                        double w1 = ((pxc - x0) * (y2 - y0) - (x2 - x0) * (pyc - y0)) / area;
                        double w2 = 1.0 - w0 - w1;
                        if (w0 < 0 || w1 < 0 || w2 < 0) continue;
                        double depth = w2 * sz[i0] + w1 * sz[i1] + w0 * sz[i2];
                        int idx = py * tile + px;
                        if (depth <= zb[idx]) continue;
                        double uu = w2 * f.U[i0] + w1 * f.U[i1] + w0 * f.U[i2];
                        double vv = w2 * f.V[i0] + w1 * f.V[i1] + w0 * f.V[i2];
                        int r, g, bl, al;
                        if (tex != null) {
                            // 贴图边缘按最近像素夹取（和原版图集一致，不做平铺）
                            int tx = (int)Math.Floor(uu * tw); if (tx < 0) tx = 0; if (tx > tw - 1) tx = tw - 1;
                            int ty = (int)Math.Floor(vv * th); if (ty < 0) ty = 0; if (ty > th - 1) ty = th - 1;
                            int ti = (ty * tw + tx) * 4;
                            r = tex[ti]; g = tex[ti + 1]; bl = tex[ti + 2]; al = tex[ti + 3];
                        } else {
                            r = 255; g = 0; bl = 255; al = 255;
                        }
                        if (al < 8) continue;
                        zb[idx] = (float)depth;
                        int o = py * stride + px * 3;
                        rgb[o] = (byte)Math.Min(255.0, bl * shade);
                        rgb[o + 1] = (byte)Math.Min(255.0, g * shade);
                        rgb[o + 2] = (byte)Math.Min(255.0, r * shade);
                    }
                }
            }
        }
    }
}
'@

Add-Type -TypeDefinition $csharp

$mdl = Get-Content -Raw $Model | ConvertFrom-Json
$texRefs = @{}
if ($mdl.textures) { foreach ($p in $mdl.textures.PSObject.Properties) { $texRefs[$p.Name] = $p.Value } }

$jar = $null
if (Test-Path $VanillaJar) { $jar = [System.IO.Compression.ZipFile]::OpenRead($VanillaJar) }

function Resolve-TexturePath([string]$ref) {
    if (-not $ref) { return $null }
    $id = $ref
    if ($ref.StartsWith('#')) { $id = $texRefs[$ref.Substring(1)] }
    if (-not $id) { return $null }
    if ($id -notmatch ':') { $id = "minecraft:$id" }
    $parts = $id -split ':', 2
    $ns = $parts[0]; $path = $parts[1]
    if ($ns -eq 'minecraft') { return "jar:assets/minecraft/textures/$path.png" }
    $local = Join-Path $AssetsRoot (Join-Path $ns (Join-Path 'textures' ($path + '.png')))
    return $local
}

$texIndex = @{}
$texData = New-Object System.Collections.Generic.List[byte[]]
$texW = New-Object System.Collections.Generic.List[int]
$texH = New-Object System.Collections.Generic.List[int]

function Get-TextureIndex([string]$resolved) {
    if (-not $resolved) { return -1 }
    if ($texIndex.ContainsKey($resolved)) { return $texIndex[$resolved] }
    $bmp = $null
    if ($resolved.StartsWith('jar:')) {
        $entry = $jar.GetEntry($resolved.Substring(4))
        if ($entry) {
            $ms = New-Object System.IO.MemoryStream
            $s = $entry.Open(); $s.CopyTo($ms); $s.Close()
            $ms.Position = 0
            $bmp = New-Object System.Drawing.Bitmap $ms
        }
    }
    elseif (Test-Path $resolved) { $bmp = New-Object System.Drawing.Bitmap $resolved }
    if (-not $bmp) { return -1 }
    $w = $bmp.Width; $h = $bmp.Height
    $buf = New-Object byte[] ($w * $h * 4)
    $rect = New-Object System.Drawing.Rectangle 0, 0, $w, $h
    $data = $bmp.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::ReadOnly, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $tmp = New-Object byte[] ($data.Stride * $h)
    [System.Runtime.InteropServices.Marshal]::Copy($data.Scan0, $tmp, 0, $tmp.Length)
    $bmp.UnlockBits($data)
    for ($y = 0; $y -lt $h; $y++) {
        for ($x = 0; $x -lt $w; $x++) {
            $s = $y * $data.Stride + $x * 4
            $d = ($y * $w + $x) * 4
            $buf[$d + 0] = $tmp[$s + 2]
            $buf[$d + 1] = $tmp[$s + 1]
            $buf[$d + 2] = $tmp[$s + 0]
            $buf[$d + 3] = $tmp[$s + 3]
        }
    }
    $bmp.Dispose()
    $texData.Add($buf); $texW.Add($w); $texH.Add($h)
    $i = $texData.Count - 1
    $texIndex[$resolved] = $i
    return $i
}

#  以下顶点顺序完全照抄原版 net.minecraft.client.renderer.FaceInfo
function Get-FaceCorners([string]$d, [double]$x0, [double]$y0, [double]$z0, [double]$x1, [double]$y1, [double]$z1) {
    switch ($d) {
        'down'  { return @(@($x0, $y0, $z1), @($x0, $y0, $z0), @($x1, $y0, $z0), @($x1, $y0, $z1)) }
        'up'    { return @(@($x0, $y1, $z0), @($x0, $y1, $z1), @($x1, $y1, $z1), @($x1, $y1, $z0)) }
        'north' { return @(@($x1, $y1, $z0), @($x1, $y0, $z0), @($x0, $y0, $z0), @($x0, $y1, $z0)) }
        'south' { return @(@($x0, $y1, $z1), @($x0, $y0, $z1), @($x1, $y0, $z1), @($x1, $y1, $z1)) }
        'west'  { return @(@($x0, $y1, $z0), @($x0, $y0, $z0), @($x0, $y0, $z1), @($x0, $y1, $z1)) }
        'east'  { return @(@($x1, $y1, $z1), @($x1, $y0, $z1), @($x1, $y0, $z0), @($x1, $y1, $z0)) }
    }
    return $null
}

#  与 BlockElement.uvsByFace 一致（贴图缺省 uv 时使用）
function Get-DefaultUv([string]$d, [double]$x0, [double]$y0, [double]$z0, [double]$x1, [double]$y1, [double]$z1) {
    # 注意：PowerShell 会把 @($a, 16 - $b, ...) 这种写法解析坏，必须先算好再拼数组
    $U1 = 0.0; $V1 = 0.0; $U2 = 0.0; $V2 = 0.0
    switch ($d) {
        'down'  { $U1 = $x0; $V1 = 16.0 - $z1; $U2 = $x1; $V2 = 16.0 - $z0 }
        'up'    { $U1 = $x0; $V1 = $z0; $U2 = $x1; $V2 = $z1 }
        'north' { $U1 = 16.0 - $x1; $V1 = 16.0 - $y1; $U2 = 16.0 - $x0; $V2 = 16.0 - $y0 }
        'south' { $U1 = $x0; $V1 = 16.0 - $y1; $U2 = $x1; $V2 = 16.0 - $y0 }
        'west'  { $U1 = $z0; $V1 = 16.0 - $y1; $U2 = $z1; $V2 = 16.0 - $y0 }
        'east'  { $U1 = 16.0 - $z1; $V1 = 16.0 - $y1; $U2 = 16.0 - $z0; $V2 = 16.0 - $y0 }
        default { $U1 = 0.0; $V1 = 0.0; $U2 = 16.0; $V2 = 16.0 }
    }
    return @($U1, $V1, $U2, $V2)
}

#  与 BlockFaceUV.getU/getV 一致：shifted = (i + rotation/90) % 4
function Get-UvFraction([int]$i, [int]$rotDeg) {
    $shifted = ($i + [int]($rotDeg / 90)) % 4
    $uf = if ($shifted -eq 0 -or $shifted -eq 1) { 0.0 } else { 1.0 }
    $vf = if ($shifted -eq 0 -or $shifted -eq 3) { 0.0 } else { 1.0 }
    return @($uf, $vf)
}

function Rotate-Point([double]$x, [double]$y, [double]$z, $rot) {
    if (-not $rot) { return @($x, $y, $z) }
    $ox = [double]$rot.origin[0]; $oy = [double]$rot.origin[1]; $oz = [double]$rot.origin[2]
    $ang = [double]$rot.angle * [Math]::PI / 180.0
    $c = [Math]::Cos($ang); $s = [Math]::Sin($ang)
    $rx = $x - $ox; $ry = $y - $oy; $rz = $z - $oz
    $sx = 1.0; $sy = 1.0; $sz = 1.0
    switch ($rot.axis) {
        'y' { $nx = $rx * $c + $rz * $s; $ny = $ry; $nz = -$rx * $s + $rz * $c }
        'x' { $nx = $rx; $ny = $ry * $c - $rz * $s; $nz = $ry * $s + $rz * $c }
        'z' { $nx = $rx * $c - $ry * $s; $ny = $rx * $s + $ry * $c; $nz = $rz }
        default { $nx = $rx; $ny = $ry; $nz = $rz }
    }
    if ($rot.rescale -eq $true) {
        $k = if ([Math]::Abs([double]$rot.angle) -eq 22.5) { 1.0 / [Math]::Cos([Math]::PI / 8) - 1.0 } else { 1.0 / [Math]::Cos([Math]::PI / 4) - 1.0 }
        switch ($rot.axis) {
            'y' { $sx = 1.0 + $k; $sz = 1.0 + $k }
            'x' { $sy = 1.0 + $k; $sz = 1.0 + $k }
            'z' { $sx = 1.0 + $k; $sy = 1.0 + $k }
        }
        $nx = $nx * $sx; $ny = $ny * $sy; $nz = $nz * $sz
    }
    return @(($nx + $ox), ($ny + $oy), ($nz + $oz))
}

$faces = New-Object System.Collections.Generic.List[PvFace]
$skipNoTex = 0
foreach ($el in $mdl.elements) {
    $x0 = [double]$el.from[0]; $y0 = [double]$el.from[1]; $z0 = [double]$el.from[2]
    $x1 = [double]$el.to[0];   $y1 = [double]$el.to[1];   $z1 = [double]$el.to[2]
    $rot = $el.rotation
    foreach ($d in 'up', 'down', 'north', 'south', 'west', 'east') {
        $faceDef = $el.faces.$d
        if (-not $faceDef -or -not $faceDef.texture) { continue }
        if ((@($el.from).Count -ne 3) -or (@($el.to).Count -ne 3)) {
            Write-Warning ("元素 from/to 不是 3 个数：from={0} to={1}" -f ($el.from -join ','), ($el.to -join ','))
            continue
        }
        $corners = Get-FaceCorners $d $x0 $y0 $z0 $x1 $y1 $z1
        if (-not $corners) { continue }
        $uv = $faceDef.uv
        if ($uv) { $u1 = [double]$uv[0]; $v1 = [double]$uv[1]; $u2 = [double]$uv[2]; $v2 = [double]$uv[3] }
        else {
            $duv = Get-DefaultUv $d $x0 $y0 $z0 $x1 $y1 $z1
            $u1 = $duv[0]; $v1 = $duv[1]; $u2 = $duv[2]; $v2 = $duv[3]
        }
        $rotDeg = 0
        if ($faceDef.rotation) { $rotDeg = [int]$faceDef.rotation }
        $f = New-Object PvFace
        $f.Tex = Get-TextureIndex (Resolve-TexturePath $faceDef.texture)
        if ($f.Tex -lt 0) { $skipNoTex++ }
        for ($i = 0; $i -lt 4; $i++) {
            $c = $corners[$i]
            $p = Rotate-Point $c[0] $c[1] $c[2] $rot
            $f.Px[$i] = $p[0]; $f.Py[$i] = $p[1]; $f.Pz[$i] = $p[2]
            $frac = Get-UvFraction $i $rotDeg
            $f.U[$i] = ($u1 + $frac[0] * ($u2 - $u1)) / 16.0
            $f.V[$i] = ($v1 + $frac[1] * ($v2 - $v1)) / 16.0
        }
        $faces.Add($f)
    }
}

$viewList = @($Views -split ',' | ForEach-Object { $_.Trim() })
$stride = [int]([Math]::Ceiling($Tile * 3 / 4.0) * 4)
$buf = New-Object byte[] ($stride * $Tile)
$faceArr = $faces.ToArray()
$texArr = $texData.ToArray()
$twArr = $texW.ToArray()
$thArr = $texH.ToArray()

$bmpOut = New-Object System.Drawing.Bitmap ($Tile * $viewList.Count), ($Tile + 26)
$g = [System.Drawing.Graphics]::FromImage($bmpOut)
$g.Clear([System.Drawing.Color]::FromArgb(255, 24, 26, 30))
$g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAlias
$font = New-Object System.Drawing.Font 'Segoe UI', 10
$brushText = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 220, 224, 230))

$idx = 0
foreach ($v in $viewList) {
    $sp = $v -split '/'
    $yaw = [double]$sp[0]; $pitch = [double]$sp[1]
    [PvRenderer]::FillBg($buf, $Tile, $stride, 150, 160, 172)
    [PvRenderer]::Render($faceArr, $texArr, $twArr, $thArr, $buf, $Tile, $stride, $yaw, $pitch, $Scale, $PanX, $PanY)
    $tileBmp = New-Object System.Drawing.Bitmap ($Tile, $Tile, [System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
    $rect = New-Object System.Drawing.Rectangle 0, 0, $Tile, $Tile
    $data = $tileBmp.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::WriteOnly, [System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
    [System.Runtime.InteropServices.Marshal]::Copy($buf, 0, $data.Scan0, $buf.Length)
    $tileBmp.UnlockBits($data)
    $g.DrawImage($tileBmp, ($idx * $Tile), 26)
    $g.DrawString(("yaw {0} / pitch {1}" -f $yaw, $pitch), $font, $brushText, ($idx * $Tile + 8), 5)
    $tileBmp.Dispose()
    $idx++
}
if ($Label) { $g.DrawString($Label, $font, $brushText, 8, ($Tile + 30)) }
$g.Dispose()
$dir = Split-Path -Parent $Out
if ($dir -and -not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
$bmpOut.Save($Out, [System.Drawing.Imaging.ImageFormat]::Png)
$bmpOut.Dispose()
if ($jar) { $jar.Dispose() }
Write-Host ("已渲染 {0} 个面（{1} 张贴图，{2} 个面找不到贴图）-> {3}" -f $faces.Count, $texData.Count, $skipNoTex, $Out) -ForegroundColor Green
