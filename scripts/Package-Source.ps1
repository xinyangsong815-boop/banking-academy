param([string]$OutputPath)
$ErrorActionPreference = 'Stop'
$projectRoot = [System.IO.Path]::GetFullPath((Split-Path -Parent $PSScriptRoot))
if (-not $OutputPath) { $OutputPath = Join-Path (Split-Path -Parent $projectRoot) 'banking-academy-学习源码.zip' }
$OutputPath = [System.IO.Path]::GetFullPath($OutputPath)
if (Test-Path -LiteralPath $OutputPath) { throw '输出文件已存在，请指定新的 -OutputPath，避免覆盖已有资料。' }
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$excludedDirectories = @('node_modules', 'target', 'dist', 'data', 'artifacts', '.git', '.idea', '.vscode')
$directories = [System.Collections.Generic.Stack[string]]::new()
$directories.Push($projectRoot)
$archive = [System.IO.Compression.ZipFile]::Open($OutputPath, [System.IO.Compression.ZipArchiveMode]::Create)
try {
    while ($directories.Count -gt 0) {
        $directoryPath = $directories.Pop()
        foreach ($entry in Get-ChildItem -LiteralPath $directoryPath -Force) {
            if ($entry.Attributes -band [System.IO.FileAttributes]::ReparsePoint) { continue }
            if ($entry.PSIsContainer) {
                if ($entry.Name -notin $excludedDirectories) { $directories.Push($entry.FullName) }
                continue
            }
            if ($entry.Name -eq '.env' -or $entry.Extension -in @('.log', '.tsbuildinfo', '.zip')) { continue }
            $relativePath = $entry.FullName.Substring($projectRoot.Length + 1).Replace('\', '/')
            [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive, $entry.FullName,
                ('banking-academy/' + $relativePath), [System.IO.Compression.CompressionLevel]::Optimal) | Out-Null
        }
    }
} finally { $archive.Dispose() }
Write-Host "已生成源码包：$OutputPath"
