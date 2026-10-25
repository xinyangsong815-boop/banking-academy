$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$statePath = Join-Path $projectRoot 'artifacts\processes.json'
if (-not (Test-Path -LiteralPath $statePath)) { Write-Host '没有由启动脚本记录的服务。'; exit 0 }
$state = Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
if ($state.root -ne $projectRoot) { throw '进程记录与当前项目目录不匹配' }
foreach ($processId in @($state.backend, $state.frontend)) {
    $owned = Get-CimInstance Win32_Process -Filter "ProcessId = $processId" -ErrorAction SilentlyContinue
    # PID 可能已被系统复用，必须核对命令中的项目绝对路径。
    if ($owned -and $owned.CommandLine -and $owned.CommandLine.Contains($projectRoot)) {
        Stop-Process -Id $processId -ErrorAction SilentlyContinue
    }
}
Remove-Item -LiteralPath $statePath
Write-Host '已停止本项目记录的服务。'
