#Requires -Version 7
<#
    往游戏里敲一串命令（自动化验证用）。

    和 game_key.ps1 / game_type.ps1 的区别：那两个是"发单个按键 / 打字"，这个把
    「打开输入框 → 打字 → 回车」整套做掉，而且**默认走投递模式**（不抢前台焦点）——
    开发时窗口常被终端挡着，直接投消息给游戏窗口反而更稳。

    用法：
      pwsh -File tools\game_commands.ps1 -CommandsFile cmds.txt
      pwsh -File tools\game_commands.ps1 -Commands 'say hi'
      pwsh -File tools\game_commands.ps1 -CommandsFile cmds.txt -Post:$false   # 抢前台模式

    cmds.txt：一行一条，空行和 # 开头的行跳过。命令要自己带开头的斜杠。

    注意：投递模式下，游戏自己的消息循环会把开输入框那次按键 TranslateMessage 成一个字符
    塞进输入框，所以脚本会先退两格清掉它；空框上退格是空操作，不会误删。
#>
param(
    [string[]]$Commands,
    [string]$CommandsFile,
    [string]$Title = '*Minecraft*',
    [int]$CharDelayMs = 12,
    [int]$AfterMs = 1200,
    [bool]$Post = $true
)

if ($CommandsFile) {
    $Commands = Get-Content $CommandsFile | Where-Object { $_.Trim() -and -not $_.TrimStart().StartsWith('#') }
}
if (-not $Commands) { throw "没有要发送的命令（用 -Commands 或 -CommandsFile）" }

Add-Type -AssemblyName System.Drawing.Common
Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;
using System.Text;
public class GameDrive {
    [StructLayout(LayoutKind.Sequential)] public struct RECT { public int L,T,R,B; }
    [StructLayout(LayoutKind.Sequential)]
    public struct INPUT { public uint type; public ushort wVk, wScan, dwFlags; public uint time; public IntPtr extra; public ushort pad0, pad1; }

    [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr h, out RECT r);
    [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
    [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr h, int c);
    [DllImport("user32.dll")] public static extern bool SetCursorPos(int x, int y);
    [DllImport("user32.dll")] public static extern void mouse_event(uint f, uint dx, uint dy, uint d, UIntPtr e);
    [DllImport("user32.dll")] public static extern void keybd_event(byte vk, byte scan, uint f, UIntPtr e);
    [DllImport("user32.dll")] public static extern uint MapVirtualKey(uint code, uint type);
    [DllImport("user32.dll")] public static extern bool PostMessage(IntPtr h, uint msg, IntPtr w, IntPtr l);
    [DllImport("user32.dll")] public static extern uint SendInput(uint n, INPUT[] inputs, int size);
    [DllImport("user32.dll", CharSet=CharSet.Unicode)] public static extern int GetWindowText(IntPtr h, StringBuilder s, int n);
    /** 关掉这个窗口的输入法上下文，否则字母会被中文输入法吃掉 */
    [DllImport("imm32.dll")] public static extern IntPtr ImmAssociateContext(IntPtr h, IntPtr imc);
    public static void DetachIme(IntPtr h) { ImmAssociateContext(h, IntPtr.Zero); }
    public static string TitleOf(IntPtr h) { var sb = new StringBuilder(256); GetWindowText(h, sb, 256); return sb.ToString(); }

    public const uint WM_KEYDOWN = 0x0100, WM_KEYUP = 0x0101, WM_CHAR = 0x0102;
    /** 点一下标题栏：Windows 上最可靠的"把窗口提到前台" */
    public static void FocusByTitleBar(IntPtr h) {
        ShowWindow(h, 9);
        RECT r; GetWindowRect(h, out r);
        SetCursorPos(r.L + 220, r.T + 12);
        System.Threading.Thread.Sleep(120);
        mouse_event(0x0002, 0, 0, 0, UIntPtr.Zero);
        System.Threading.Thread.Sleep(60);
        mouse_event(0x0004, 0, 0, 0, UIntPtr.Zero);
        System.Threading.Thread.Sleep(200);
    }
    public static void Tap(byte vk) {
        byte scan = (byte)MapVirtualKey(vk, 0);
        keybd_event(vk, scan, 0, UIntPtr.Zero);
        System.Threading.Thread.Sleep(50);
        keybd_event(vk, scan, 2, UIntPtr.Zero);
    }
    public static void TypeChar(char ch) {
        INPUT[] inputs = new INPUT[2];
        inputs[0].type = 1; inputs[0].wScan = ch; inputs[0].dwFlags = 0x0004;
        inputs[1].type = 1; inputs[1].wScan = ch; inputs[1].dwFlags = 0x0004 | 0x0002;
        SendInput(2, inputs, Marshal.SizeOf(typeof(INPUT)));
    }
    public static void PostKey(IntPtr h, byte vk) {
        int scan = (int)MapVirtualKey(vk, 0);
        PostMessage(h, WM_KEYDOWN, (IntPtr)vk, (IntPtr)((scan << 16) | 1));
        System.Threading.Thread.Sleep(40);
        PostMessage(h, WM_KEYUP, (IntPtr)vk, (IntPtr)((scan << 16) | 0xC0000001));
    }
    public static void PostChar(IntPtr h, char ch) { PostMessage(h, WM_CHAR, (IntPtr)ch, (IntPtr)1); }
}
'@

$proc = Get-Process | Where-Object { $_.MainWindowTitle -like $Title } | Select-Object -First 1
if (-not $proc) { throw "没找到标题匹配 $Title 的窗口" }
$h = $proc.MainWindowHandle

if (-not $Post) {
    [GameDrive]::FocusByTitleBar($h)
    [GameDrive]::DetachIme($h)
    $fg = [GameDrive]::GetForegroundWindow()
    if ($fg -ne $h) { throw "窗口没抢到前台（当前前台是 " + [GameDrive]::TitleOf($fg) + "）" }
} else {
    Write-Host '投递模式：不抢前台，直接把按键消息投给游戏窗口' -ForegroundColor DarkGray
}

foreach ($cmd in $Commands) {
    if ($Post) { [GameDrive]::PostKey($h, 0x54) } else { [GameDrive]::Tap(0x54) }   # T 打开输入框
    Start-Sleep -Milliseconds 400
    if ($Post) {
        [GameDrive]::PostKey($h, 0x08)   # 退格：清掉 TranslateMessage 塞进来的那个字符
        Start-Sleep -Milliseconds 80
        [GameDrive]::PostKey($h, 0x08)
        Start-Sleep -Milliseconds 120
    }
    foreach ($ch in $cmd.ToCharArray()) {
        if ($Post) { [GameDrive]::PostChar($h, $ch) } else { [GameDrive]::TypeChar($ch) }
        Start-Sleep -Milliseconds $CharDelayMs
    }
    Start-Sleep -Milliseconds 120
    if ($Post) { [GameDrive]::PostKey($h, 0x0D) } else { [GameDrive]::Tap(0x0D) }   # 回车
    Write-Host "已发送：$cmd"
    Start-Sleep -Milliseconds $AfterMs
}
