# Generate sources.lst with production sources only (exclude test files/directories)
$root = Resolve-Path -Path "$PSScriptRoot\.." 
$sourcesFile = Join-Path -Path $PSScriptRoot -ChildPath 'sources.lst'
Get-ChildItem -Path $root -Recurse -Filter *.java | Where-Object {
    ($_.FullName -notmatch '\\(?i)test\\') -and ($_.Name -notlike '*Test.java')
} | ForEach-Object { $_.FullName } | Out-File -FilePath $sourcesFile -Encoding ascii
Write-Host "Wrote sources to: $sourcesFile (`$(Get-Content $sourcesFile | Measure-Object -Line).Lines lines)"