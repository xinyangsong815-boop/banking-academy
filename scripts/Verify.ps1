$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location (Join-Path $projectRoot 'backend')
try { & .\mvnw.cmd test; if ($LASTEXITCODE -ne 0) { throw '后端验证失败' } } finally { Pop-Location }
Push-Location (Join-Path $projectRoot 'frontend')
try {
    & pnpm install --frozen-lockfile
    if ($LASTEXITCODE -ne 0) { throw '前端依赖安装失败' }
    foreach ($taskName in @('format:check', 'test', 'build')) {
        & pnpm run $taskName
        if ($LASTEXITCODE -ne 0) { throw "前端 $taskName 失败" }
    }
} finally { Pop-Location }
Write-Host '后端测试、架构约束、前端格式检查、测试及构建全部通过。'
