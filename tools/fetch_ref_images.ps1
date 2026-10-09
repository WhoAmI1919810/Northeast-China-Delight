#Requires -Version 7
<#
    抓「东北小院」参考图（做建筑还原度比对用）。

    用必应图片搜索拿缩略图原图链接，然后下到 -OutDir。
    下载失败、文件太小的直接跳过（不删已有文件）。

    用法：
        pwsh -File tools\fetch_ref_images.ps1
        pwsh -File tools\fetch_ref_images.ps1 -Want 30
#>
param(
    [string]$OutDir = '',
    [int]$Want = 30,
    [int]$MinBytes = 15000,
    [string[]]$Queries = @(
        '东北小院',
        '东北农村院子',
        '东北农家院 正房',
        '东北农村民居 红砖房 院子',
        '东北农村 苞米楼子 院子',
        '东北民居 院墙 大门 农村'
    )
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$ua = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36'

if (-not $OutDir) { $OutDir = Join-Path (Resolve-Path (Join-Path $PSScriptRoot '..')).Path '.tmp\refs' }
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

$urls = New-Object System.Collections.Generic.List[string]
foreach ($q in $Queries)
{
    $uri = 'https://www.bing.com/images/search?q=' + [uri]::EscapeDataString($q)
    try
    {
        $resp = Invoke-WebRequest -Uri $uri -UserAgent $ua -TimeoutSec 30
    }
    catch
    {
        Write-Host ("搜索失败：{0}" -f $q) -ForegroundColor DarkYellow
        continue
    }
    $matches = [regex]::Matches($resp.Content, 'murl&quot;:&quot;(.*?)&quot;')
    foreach ($m in $matches)
    {
        $u = $m.Groups[1].Value
        if ($u -match '\.(jpg|jpeg|png)(\?|$)')
        {
            $urls.Add($u)
        }
    }
    Write-Host ("{0} → 找到 {1} 条候选" -f $q, $matches.Count) -ForegroundColor DarkGray
}

$urls = $urls | Select-Object -Unique
Write-Host ("候选合计 {0} 张" -f $urls.Count) -ForegroundColor Cyan

$i = 0
$ok = 0
foreach ($u in $urls)
{
    if ($ok -ge $Want) { break }
    $i++
    $ext = if ($u -match '\.png') { 'png' } else { 'jpg' }
    $name = 'ref{0:d2}.{1}' -f $i, $ext
    $file = Join-Path $OutDir $name
    if (Test-Path $file) { continue }
    try
    {
        Invoke-WebRequest -Uri $u -UserAgent $ua -Headers @{ Referer = 'https://www.bing.com/' } -TimeoutSec 20 -OutFile $file
    }
    catch
    {
        continue
    }
    $len = (Get-Item -LiteralPath $file).Length
    if ($len -lt $MinBytes)
    {
        [System.IO.File]::Delete($file)
        continue
    }
    $ok++
    Write-Host ("OK {0}  {1}  {2} KB" -f $name, $u, [int]($len / 1024)) -ForegroundColor Green
}

Write-Host ("下载成功 {0} 张 → {1}" -f $ok, $OutDir) -ForegroundColor Cyan
