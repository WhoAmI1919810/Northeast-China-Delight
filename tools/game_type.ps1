#Requires -Version 7
<#
    往游戏窗口里"打字"（用于在聊天框里输命令做验证）。

    只覆盖命令里会用到的字符：a-z 0-9 空格 / : _ ~ { } [ ] " , -
    Shift = 0x10，按住 Shift 再敲符号键。

    用法：
      pwsh -File tools\game_type.ps1 -Text "setblock ~ ~ ~ air" -Enter
      pwsh -File tools\game_type.ps1 -Text "..."            # 只打字，不回车
#>
param(
    [Parameter(Mandatory = $true)][string]$Text,
    [switch]$Enter,
    [switch]$Post,
    [int]$DelayMs = 18,
    [string]$Title = '*Minecraft*'
)

if (-not ('Win32Type' -as [type])) {
    Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;
public class Win32Type {
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
    [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
    [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr hWnd, int cmd);
    [DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint pid);
    [DllImport("kernel32.dll")] public static extern uint GetCurrentThreadId();
    [DllImport("user32.dll")] public static extern bool AttachThreadInput(uint attach, uint attachTo, bool fAttach);
    [DllImport("user32.dll")] public static extern uint MapVirtualKey(uint code, uint mapType);
    [DllImport("imm32.dll")] public static extern IntPtr ImmAssociateContext(IntPtr hWnd, IntPtr hIMC);
    public static void DetachIme(IntPtr hWnd) { ImmAssociateContext(hWnd, IntPtr.Zero); }
    [DllImport("user32.dll")] public static extern void keybd_event(byte vk, byte scan, uint flags, UIntPtr extra);
    public static bool Focus(IntPtr hWnd) {
        ShowWindow(hWnd, 9);
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
    public static void Down(byte vk) { keybd_event(vk, (byte)MapVirtualKey(vk, 0), 0, UIntPtr.Zero); }
    public static void Up(byte vk) { keybd_event(vk, (byte)MapVirtualKey(vk, 0), 2, UIntPtr.Zero); }
}
'@
}

if (-not ('Win32Post' -as [type])) {
    Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;
public struct INPUT {
    public uint type;
    public ushort wVk, wScan, dwFlags;
    public uint time;
    public IntPtr dwExtraInfo;
    public ushort pad0, pad1;
}
public class Win32Post {
    [DllImport("user32.dll")] public static extern uint MapVirtualKey(uint code, uint mapType);
    [DllImport("user32.dll")] public static extern bool PostMessage(IntPtr hWnd, uint msg, IntPtr wParam, IntPtr lParam);
    public const uint WM_KEYDOWN = 0x0100;
    public const uint WM_KEYUP = 0x0101;
    public const uint WM_CHAR = 0x0102;
    [DllImport("user32.dll")] public static extern uint SendInput(uint n, INPUT[] inputs, int size);
    /** 用 SendInput 直接送 Unicode 字符：不经过键盘布局和输入法，最可靠 */
    public static void SendUnicode(IntPtr hWnd, int ch) {
        INPUT[] inputs = new INPUT[2];
        inputs[0].type = 1; inputs[0].wScan = (ushort)ch; inputs[0].dwFlags = 0x0004;
        inputs[1].type = 1; inputs[1].wScan = (ushort)ch; inputs[1].dwFlags = 0x0004 | 0x0002;
        SendInput(2, inputs, Marshal.SizeOf(typeof(INPUT)));
    }
    /** 直接投递字符（聊天框的输入走 charTyped ← WM_CHAR，这样才能绕开输入法） */
    public static void PostChar(IntPtr hWnd, int ch) {
        PostMessage(hWnd, WM_CHAR, (IntPtr)ch, (IntPtr)1);
    }
    /** 直接投递按键消息：绕过键盘驱动和中文输入法 */
    public static void PostKey(IntPtr hWnd, int vk) {
        int scan = (int)MapVirtualKey((uint)vk, 0);
        IntPtr down = (IntPtr)((scan << 16) | 1);
        IntPtr up = (IntPtr)((scan << 16) | 0xC0000001);
        PostMessage(hWnd, WM_KEYDOWN, (IntPtr)vk, down);
        System.Threading.Thread.Sleep(30);
        PostMessage(hWnd, WM_KEYUP, (IntPtr)vk, up);
    }
}
'@
}

$shift = 0x10
$map = @{}
# 注意：PowerShell 里 [char][int]'a' + $i 这种写法会被解析成数组加法，必须先算好再当键
for ($i = 0; $i -lt 26; $i++) {
    $code = 0x41 + $i
    $map[[char]$code] = @($code, $false)
}
for ($i = 0; $i -lt 10; $i++) {
    $code = 0x30 + $i
    $map[[char]$code] = @($code, $false)
}
$map[' ']  = @(0x20, $false)
$map['/']  = @(0xBF, $false)
$map[',']  = @(0xBC, $false)
$map['-']  = @(0xBD, $false)
$map['=']  = @(0xBB, $false)
$map[':']  = @(0xBA, $true)
$map['_']  = @(0xBD, $true)
$map['~']  = @(0xC0, $true)
$map['{']  = @(0xDB, $true)
$map['}']  = @(0xDD, $true)
$map['[']  = @(0xDB, $false)
$map[']']  = @(0xDD, $false)
$map['"']  = @(0xDE, $true)

$proc = Get-Process | Where-Object { $_.MainWindowTitle -like $Title } | Select-Object -First 1
if (-not $proc) { throw "没找到标题匹配 $Title 的窗口" }
[Win32Type]::Focus($proc.MainWindowHandle) | Out-Null
[Win32Type]::DetachIme($proc.MainWindowHandle)
Start-Sleep -Milliseconds 400

foreach ($ch in $Text.ToCharArray()) {
    if (-not $map.ContainsKey($ch)) {
        Write-Warning "跳过不支持的字符：$ch"
        continue
    }
    $vk = [byte]$map[$ch][0]
    $needShift = [bool]$map[$ch][1]
    if ($Post) {
        # 投递模式：SendInput + Unicode，输入法/键盘布局都拦不住
        [Win32Post]::SendUnicode($proc.MainWindowHandle, [int]$ch)
        Start-Sleep -Milliseconds $DelayMs
        continue
    }
    if ($needShift) { [Win32Type]::Down($shift); Start-Sleep -Milliseconds 10 }
    [Win32Type]::Down($vk)
    Start-Sleep -Milliseconds $DelayMs
    [Win32Type]::Up($vk)
    if ($needShift) { [Win32Type]::Up($shift) }
}

if ($Enter) {
    Start-Sleep -Milliseconds 120
    if ($Post) {
        [Win32Post]::PostKey($proc.MainWindowHandle, 0x0D)
    } else {
        [Win32Type]::Down(0x0D); Start-Sleep -Milliseconds 40; [Win32Type]::Up(0x0D)
    }
}
Write-Host ("已输入 {0} 个字符{1}" -f $Text.Length, $(if ($Enter) { ' 并回车' } else { '' })) -ForegroundColor Green
