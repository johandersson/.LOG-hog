param()
$v = '8.2.1'
$zip = "dependency-check-$v-release.zip"
$dir = Join-Path $PSScriptRoot '..' | Resolve-Path | ForEach-Object { Join-Path $_ 'dependency-check' }
New-Item -ItemType Directory -Force -Path $dir | Out-Null
$out = Join-Path $dir $zip
$url = "https://github.com/jeremylong/DependencyCheck/releases/download/v$v/$zip"
Write-Host "Downloading $url to $out ..."
Invoke-WebRequest -Uri $url -OutFile $out -UseBasicParsing
Write-Host "Extracting $out to $dir"
Expand-Archive -Path $out -DestinationPath $dir -Force
$droot = Get-ChildItem -Path $dir -Directory | Where-Object { $_.Name -like 'dependency-check*' } | Select-Object -First 1
if ($droot -eq $null) { Write-Error 'Dependency-Check not found after extract'; exit 1 }
$exe = Join-Path $droot.FullName 'bin\dependency-check.bat'
if (-Not (Test-Path $exe)) { Write-Error 'dependency-check executable not found'; exit 1 }
Write-Host "Running $exe against src ..."
& $exe --project 'LogHog' --scan (Join-Path (Get-Location) 'src') --format HTML --out (Join-Path (Get-Location) 'build\dependency-check-report')
Write-Host "Done. Reports in build\dependency-check-report (if scan succeeded)"
