#Requires -Version 7
<#
    启动 Minecraft **专用服务端**（开发环境），日志写进 logs\server-<版本>-<时间戳>.log。

    用途：不开客户端也能把数据包完整加载一遍，用来验证配方 / 标签 / 方块状态有没有写错。
    启动后它会一直挂着，验证完自己 Ctrl+C 或杀掉进程即可。

    用法：
        pwsh -File tools\start_server.ps1
#>
param(
    [string]$GameVersion = '1.21.1',
    [string]$JavaHome = 'H:\OpenJDK21'
)

$ErrorActionPreference = 'Stop'

$env:JAVA_HOME = $JavaHome
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$logDir = Join-Path $projectRoot 'logs'
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

# 同 start_client.ps1：把 Gradle 守护进程注册表挪到工程内，
# 避开用户目录里那份损坏过的 registry.bin。
$registryBase = Join-Path $projectRoot '.gradle-registry'
New-Item -ItemType Directory -Force -Path $registryBase | Out-Null
$env:GRADLE_OPTS = "-Dorg.gradle.daemon.registry.base=$registryBase"

$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$consoleLog = Join-Path $logDir "server-$GameVersion-$stamp.log"

Set-Location $projectRoot
Write-Host "日志输出：$consoleLog"
& .\gradlew.bat (':' + $GameVersion + ':runServer') --console=plain *>&1 | Tee-Object -FilePath $consoleLog
