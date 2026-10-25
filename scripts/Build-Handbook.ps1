$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$docsPath = Join-Path $projectRoot 'docs'
$chapters = Get-ChildItem -LiteralPath $docsPath -Filter '*.md' | Where-Object { $_.Name -match '^\d{2}-' } | Sort-Object Name
$parts = [System.Collections.Generic.List[string]]::new()
$parts.Add("# Banking Academy · 完整教学手册`n`n本文件由各章节合并生成。分章阅读入口为 [教学目录](README.md)。更新章节后运行 scripts/Build-Handbook.ps1 重新生成。`n`n项目版本：1.0.0。示例全部使用虚构教学数据。")
foreach ($chapter in $chapters) {
    $parts.Add("`n---`n")
    $parts.Add((Get-Content -LiteralPath $chapter.FullName -Raw).Trim())
}
Set-Content -LiteralPath (Join-Path $docsPath '完整教学手册.md') -Value ($parts -join "`n`n") -Encoding utf8
Write-Host '已生成 docs/完整教学手册.md'
