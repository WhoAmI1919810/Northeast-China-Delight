#Requires -Version 7
<#
    给游戏窗口发一个按键（自动化截屏用，比如 F1 隐藏 HUD）。

    用法： pwsh -File tools\game_key.ps1 -Key F1
#>
param(
    [Parameter(Mandatory = $true)][string]$Key,
    [switch]$Down,
    [switch]$Up,
    [string]$Title = '*Minecraft*'
)

if (-not ('Win32Key' -as [type])) {
    Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;
public class Win32Key {
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
    [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
    [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr hWnd, int cmd);
    [DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint pid);
    [DllImport("kernel32.dll")] public static extern uint GetCurrentThreadId();
    [DllImport("user32.dll")] public static extern bool AttachThreadInput(uint attach, uint attachTo, bool fAttach);
    [DllImport("user32.dll")] public static extern void keybd_event(byte vk, byte scan, uint flags, UIntPtr extra);
    [DllImport("user32.dll")] public static extern uint MapVirtualKey(uint code, uint mapType);
    [DllImport("imm32.dll")] public static extern IntPtr ImmAssociateContext(IntPtr hWnd, IntPtr hIMC);
    /** 关掉这个窗口的输入法上下文：否则字母键会被中文输入法吃掉（F 键、ESC 不受影响） */
    public static void DetachIme(IntPtr hWnd) { ImmAssociateContext(hWnd, IntPtr.Zero); }
    /** 把窗口切到前台，尽量保证真的拿到焦点（否则按键会被系统丢掉） */
    public static bool Focus(IntPtr hWnd) {
        ShowWindow(hWnd, 9); // SW_RESTORE
        if (GetForegroundWindow() == hWnd) return true;
        uint pid;
        uint target = GetWindowThreadProcessId(hWnd, out pid);
        uint self = GetCurrentThreadId();
        AttachThreadInput(self, target, true);
        bool ok = SetForegroundWindow(hWnd);
        AttachThreadInput(self, target, false);
        System.Threading.Thread.Sleep(120);
        return ok || GetForegroundWindow() == hWnd;
    }
    public static void Hold(byte vk) { keybd_event(vk, 0, 0, UIntPtr.Zero); }
    public static void Release(byte vk) { keybd_event(vk, 0, 2, UIntPtr.Zero); }
    public static void Tap(byte vk) {
        byte scan = (byte)MapVirtualKey(vk, 0);
        keybd_event(vk, scan, 0, UIntPtr.Zero);
        System.Threading.Thread.Sleep(90);
        keybd_event(vk, scan, 2, UIntPtr.Zero);
    }
}
'@
}

$map = @{
    'F1' = 0x70; 'F2' = 0x71; 'F3' = 0x72; 'F5' = 0x73
    'E' = 0x45; 'ESC' = 0x1B; 'T' = 0x54; 'ENTER' = 0x0D; 'SPACE' = 0x20
    'SHIFT' = 0x10; 'CTRL' = 0x11; 'W' = 0x57; 'A' = 0x41; 'S' = 0x53; 'D' = 0x44
    # 常用按键：JEI 的 R（配方）/ U（用途）、背包、数字栏、Q 丢弃、F 换副手
    'R' = 0x52; 'U' = 0x55; 'Q' = 0x51; 'F' = 0x46; 'H' = 0x48; 'B' = 0x42
    'PLUS' = 0xBB; 'MINUS' = 0xBD
    'TAB' = 0x09; 'BACKSPACE' = 0x08; 'DELETE' = 0x2E
}
# 数字键 1~9：切快捷栏、给 JEI 的配方页翻页都用得上（0 也一起给上）
for ($i = 0; $i -le 9; $i++) { $map["$i"] = 0x30 + $i }
$vk = $map[$Key.ToUpper()]
if (-not $vk) { throw "没定义这个键：$Key" }

$proc = Get-Process | Where-Object { $_.MainWindowTitle -like $Title } | Select-Object -First 1
if (-not $proc) { throw "没找到标题匹配 $Title 的窗口" }
[Win32Key]::Focus($proc.MainWindowHandle) | Out-Null
[Win32Key]::DetachIme($proc.MainWindowHandle)
Start-Sleep -Milliseconds 400
if ($Down) {
    [Win32Key]::Hold([byte]$vk)
    Write-Host "已按下 $Key" -ForegroundColor Green
    return
}
if ($Up) {
    [Win32Key]::Release([byte]$vk)
    Write-Host "已松开 $Key" -ForegroundColor Green
    return
}
[Win32Key]::Tap([byte]$vk)
Write-Host "已发送按键 $Key" -ForegroundColor Green
