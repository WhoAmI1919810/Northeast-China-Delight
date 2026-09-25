#Requires -Version 7
<#
    给游戏窗口发鼠标点击（用于自动化验证渲染效果）。

    用法：
      pwsh -File tools\game_click.ps1 -X 960 -Y 504            # 单击屏幕坐标
      pwsh -File tools\game_click.ps1 -X 960 -Y 504 -Double    # 双击
      pwsh -File tools\game_click.ps1 -Focus                   # 只把游戏窗口切到前台
#>
param(
    [int]$X = 0,
    [int]$Y = 0,
    [switch]$Double,
    [switch]$Focus,
    [string]$Resize = '',
    [int]$MoveX = 0,
    [int]$MoveY = 0,
    [string]$Title = '*Minecraft*'
)

Add-Type -AssemblyName System.Drawing.Common

if (-not ('Win32Click' -as [type])) {
    Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;
public class Win32Click {
    [DllImport("user32.dll")] public static extern bool SetCursorPos(int x, int y);
    [DllImport("user32.dll")] public static extern void mouse_event(uint flags, uint dx, uint dy, uint data, UIntPtr extra);
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
    [DllImport("user32.dll")] public static extern bool MoveWindow(IntPtr hWnd, int X, int Y, int w, int h, bool repaint);
    [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
    [DllImport("user32.dll")] public static extern void keybd_event(byte vk, byte scan, uint flags, UIntPtr extra);
    [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr hWnd, int cmd);
    [DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint pid);
    [DllImport("kernel32.dll")] public static extern uint GetCurrentThreadId();
    [DllImport("user32.dll")] public static extern bool AttachThreadInput(uint attach, uint attachTo, bool fAttach);
    public const uint LEFTDOWN = 0x0002;
    public const uint LEFTUP = 0x0004;
    public const uint MOVE = 0x0001;
    /** 把窗口抢到前台。后台进程直接 SetForegroundWindow 会被系统拒绝，
     *  所以先补一次 ALT 的按下/抬起，让系统认为"本进程刚收到过用户输入"。 */
    public static bool Focus(IntPtr hWnd) {
        ShowWindow(hWnd, 9);
        if (GetForegroundWindow() == hWnd) return true;
        keybd_event(0x12, 0, 0, UIntPtr.Zero);
        keybd_event(0x12, 0, 2, UIntPtr.Zero);
        uint pid;
        uint target = GetWindowThreadProcessId(hWnd, out pid);
        uint self = GetCurrentThreadId();
        AttachThreadInput(self, target, true);
        bool ok = SetForegroundWindow(hWnd);
        AttachThreadInput(self, target, false);
        System.Threading.Thread.Sleep(200);
        return ok || GetForegroundWindow() == hWnd;
    }
    public static void MoveBy(int dx, int dy) {
        mouse_event(MOVE, (uint)dx, (uint)dy, 0, UIntPtr.Zero);
    }
    public static void Click(int x, int y) {
        SetCursorPos(x, y);
        System.Threading.Thread.Sleep(120);
        mouse_event(LEFTDOWN, 0, 0, 0, UIntPtr.Zero);
        System.Threading.Thread.Sleep(60);
        mouse_event(LEFTUP, 0, 0, 0, UIntPtr.Zero);
    }
}
'@
}

$proc = Get-Process | Where-Object { $_.MainWindowTitle -like $Title } | Select-Object -First 1
if (-not $proc) { throw "没找到标题匹配 $Title 的窗口" }
[Win32Click]::SetForegroundWindow($proc.MainWindowHandle) | Out-Null
Start-Sleep -Milliseconds 400

if ($Resize) {
    $wh = $Resize -split 'x'
    $w = [int]$wh[0]; $h = [int]$wh[1]
    [Win32Click]::MoveWindow($proc.MainWindowHandle, 40, 40, $w, $h, $true) | Out-Null
    Write-Host ("窗口已调整成 {0}x{1}" -f $w, $h) -ForegroundColor Green
    return
}

if ($MoveX -ne 0 -or $MoveY -ne 0) {
    # 只移动鼠标、不按键：用来转眼（是否生效取决于游戏是否用原始鼠标输入）
    [Win32Click]::MoveBy($MoveX, $MoveY)
    Write-Host ("已移动鼠标 ({0},{1})" -f $MoveX, $MoveY) -ForegroundColor Green
    return
}

if ($Focus) {
    Write-Host '已切到游戏窗口' -ForegroundColor Green
    return
}

[Win32Click]::Click($X, $Y)
if ($Double) {
    Start-Sleep -Milliseconds 150
    [Win32Click]::Click($X, $Y)
}
Write-Host ("已点击 ({0},{1}){2}" -f $X, $Y, $(if ($Double) { ' 两次' } else { '' })) -ForegroundColor Green
