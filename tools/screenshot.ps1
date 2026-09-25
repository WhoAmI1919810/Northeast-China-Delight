#Requires -Version 7
<#
    抓游戏窗口截图（用于验证建模/贴图在游戏内到底长什么样）。

    用法：
      pwsh -File tools\screenshot.ps1                       # 抓 Minecraft 窗口
      pwsh -File tools\screenshot.ps1 -Out shot.png -Title "*Minecraft*"
      pwsh -File tools\screenshot.ps1 -FullScreen           # 抓整个屏幕
#>
param(
    [string]$Out = "$PSScriptRoot\..\game_shot.png",
    [string]$Title = '*Minecraft*',
    [switch]$FullScreen,
    [int]$DelayMs = 0
)

Add-Type -AssemblyName System.Drawing.Common

if ($DelayMs -gt 0) { Start-Sleep -Milliseconds $DelayMs }

if (-not ('Win32Native' -as [type])) {
    Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;
public class Win32Native {
    [StructLayout(LayoutKind.Sequential)]
    public struct RECT { public int Left; public int Top; public int Right; public int Bottom; }
    [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr hWnd, out RECT rect);
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
}
'@
}

$rect = $null
if (-not $FullScreen) {
    $proc = Get-Process | Where-Object { $_.MainWindowTitle -like $Title } | Select-Object -First 1
    if (-not $proc) { throw "没找到标题匹配 $Title 的窗口" }
    $r = New-Object Win32Native+RECT
    [Win32Native]::GetWindowRect($proc.MainWindowHandle, [ref]$r) | Out-Null
    $rect = @{ X = $r.Left; Y = $r.Top; W = ($r.Right - $r.Left); H = ($r.Bottom - $r.Top) }
    Write-Host ("窗口: {0}  {1}x{2} @ {3},{4}" -f $proc.MainWindowTitle, $rect.W, $rect.H, $rect.X, $rect.Y) -ForegroundColor DarkGray
} else {
    $vs = [System.Windows.Forms.SystemInformation]::VirtualScreen
    $rect = @{ X = $vs.X; Y = $vs.Y; W = $vs.Width; H = $vs.Height }
}

$bmp = New-Object System.Drawing.Bitmap ($rect.W, $rect.H)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.CopyFromScreen($rect.X, $rect.Y, 0, 0, (New-Object System.Drawing.Size ($rect.W, $rect.H)))
$g.Dispose()
$dir = Split-Path -Parent $Out
if ($dir -and -not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
$bmp.Save($Out, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Host "已保存截图 -> $Out" -ForegroundColor Green
