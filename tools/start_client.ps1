#Requires -Version 7
<#
    启动 Minecraft 客户端（开发环境），并把日志统一写进工程根目录的 logs\ 文件夹。

    - 控制台输出（Gradle + 启动器 + 游戏日志）：logs\client-<版本>-<时间戳>.log
    - 游戏自身的 latest.log，退出后归档为：logs\minecraft-<版本>-<时间戳>.log
    - 游戏目录仍在 run\<版本>\（存档、配置），全部位于工程目录内

    用法：
        pwsh -File tools\start_client.ps1
        pwsh -File tools\start_client.ps1 -GameVersion 1.21.1
#>
param(
    [string]$GameVersion = '1.21.1',
    [string]$JavaHome = $env:JAVA_HOME
)

$ErrorActionPreference = 'Stop'
if (-not $JavaHome) { throw '请先设置 JAVA_HOME（JDK 21），或用 -JavaHome 指定 JDK 目录。' }

$env:JAVA_HOME = $JavaHome
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$logDir = Join-Path $projectRoot 'logs'
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

# 用户目录里的 Gradle 守护进程注册表（%USERPROFILE%\.gradle\daemon\<版本>\registry.bin）
# 曾经损坏过一次，会让启动直接报「Could not write cache value to ... registry.bin」。
# 把注册表挪到工程内，和那份解耦（目录已加进 .gitignore）。
$registryBase = Join-Path $projectRoot '.gradle-registry'
New-Item -ItemType Directory -Force -Path $registryBase | Out-Null
$env:GRADLE_OPTS = "-Dorg.gradle.daemon.registry.base=$registryBase"

$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$consoleLog = Join-Path $logDir "client-$GameVersion-$stamp.log"
# 注意：不能写成 ":$GameVersion:runClient" —— PowerShell 会把 $GameVersion:runClient
# 当成「作用域变量」解析，任务名就丢了。
$task = ':' + $GameVersion + ':runClient'

Set-Location $projectRoot
Write-Host "日志输出：$consoleLog"
& .\gradlew.bat $task --console=plain *>&1 | Tee-Object -FilePath $consoleLog

# 游戏自身的 latest.log 下次启动会被覆盖，这里归档一份
$gameLog = Join-Path $projectRoot "run\$GameVersion\logs\latest.log"
if (Test-Path $gameLog) {
    $archive = Join-Path $logDir "minecraft-$GameVersion-$stamp.log"
    Copy-Item -LiteralPath $gameLog -Destination $archive -Force
    Write-Host "游戏日志归档：$archive"
}
