param()

$p = Join-Path $env:TEMP 'spotbugs-4.7.3\loghog-classes'
if (-Not (Test-Path $p)) {
    Write-Host "Missing extracted folder: $p"
    exit 0
}
$files = Get-ChildItem -Path $p -Recurse -Filter *.class -File
Write-Host "Found class files count: $($files.Count)"
$files | Select-Object -First 50 | ForEach-Object { Write-Host $_.FullName }
