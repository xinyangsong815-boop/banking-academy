param([switch]$SkipBuild)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$backendPath = Join-Path $projectRoot 'backend'
$frontendPath = Join-Path $projectRoot 'frontend'
$artifactPath = Join-Path $projectRoot 'artifacts'
foreach ($commandName in @('java', 'node', 'pnpm')) {
    if (-not (Get-Command $commandName -ErrorAction SilentlyContinue)) { throw "缺少 $commandName，请先阅读 docs/01-快速开始.md" }
}
foreach ($portNumber in @(18080, 5173)) {
    if (Get-NetTCPConnection -LocalPort $portNumber -State Listen -ErrorAction SilentlyContinue) {
        throw "端口 $portNumber 已被占用，请停止已有学习服务后重试。"
    }
}
New-Item -ItemType Directory -Path $artifactPath -Force | Out-Null
Push-Location $frontendPath
try {
    if (-not (Test-Path 'node_modules')) {
        & pnpm install --frozen-lockfile
        if ($LASTEXITCODE -ne 0) { throw '前端依赖安装失败' }
    }
} finally { Pop-Location }
if (-not $SkipBuild) {
    Push-Location $backendPath
    try {
        & .\mvnw.cmd package
        if ($LASTEXITCODE -ne 0) { throw '后端测试或打包失败' }
    } finally { Pop-Location }
}
$jarPath = Join-Path $backendPath 'target\banking-academy-1.0.0.jar'
if (-not (Test-Path -LiteralPath $jarPath)) { throw '找不到后端安装包，请去掉 -SkipBuild 后重试' }
$javaPath = (Get-Command java).Source
if ($env:JAVA_HOME -and (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
    $javaPath = Join-Path $env:JAVA_HOME 'bin\java.exe'
} else {
    # Oracle 的 javapath 可能是启动代理；使用 java.home 定位实际 JVM，便于精确停止。
    $javaSettings = & $javaPath -XshowSettings:properties -version 2>&1 | Out-String
    $javaHomeMatch = [regex]::Match($javaSettings, '(?m)^\s*java\.home\s*=\s*(.+)$')
    if ($javaHomeMatch.Success) { $javaPath = Join-Path $javaHomeMatch.Groups[1].Value.Trim() 'bin\java.exe' }
}
$nodePath = (Get-Command node).Source
$vitePath = Join-Path $frontendPath 'node_modules\vite\bin\vite.js'
$backendProcess = Start-Process -FilePath $javaPath -ArgumentList @('-jar', ('"' + $jarPath + '"')) -WorkingDirectory $backendPath -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $artifactPath 'backend.log') -RedirectStandardError (Join-Path $artifactPath 'backend-error.log')
try {
    $frontendProcess = Start-Process -FilePath $nodePath -ArgumentList @(('"' + $vitePath + '"'), '--host', '127.0.0.1') -WorkingDirectory $frontendPath -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $artifactPath 'frontend.log') -RedirectStandardError (Join-Path $artifactPath 'frontend-error.log')
} catch {
    Stop-Process -Id $backendProcess.Id -ErrorAction SilentlyContinue
    throw
}
@{ backend = $backendProcess.Id; frontend = $frontendProcess.Id; root = $projectRoot } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $artifactPath 'processes.json') -Encoding utf8
$ready = $false
for ($attempt = 0; $attempt -lt 40; $attempt++) {
    try {
        $health = Invoke-RestMethod 'http://127.0.0.1:18080/api/health' -TimeoutSec 1
        $page = Invoke-WebRequest 'http://127.0.0.1:5173' -UseBasicParsing -TimeoutSec 1
        if ($health.data.status -eq 'UP' -and $page.StatusCode -eq 200) { $ready = $true; break }
    } catch { Start-Sleep -Milliseconds 500 }
}
if (-not $ready) { throw "服务未就绪，请查看 $artifactPath 下的日志，并运行 Stop-Learning.ps1 清理进程" }
Write-Host '学习平台已启动：http://127.0.0.1:5173'
Write-Host '演示账号：admin / teller / auditor，密码：Learn@123456'
Write-Host '停止服务：.\scripts\Stop-Learning.ps1'
