@echo off
REM Downloads OWASP Dependency-Check CLI zip and extracts it to tools\dependency-check
REM Edit DC_VERSION to the desired release if needed.
set DC_VERSION=8.2.1
set DC_ZIP=dependency-check-%DC_VERSION%-release.zip
set URL=https://github.com/jeremylong/DependencyCheck/releases/download/v%DC_VERSION%/%DC_ZIP%













pauseecho You can now run tools\dependency-check\dependency-check\bin\dependency-check.batpowershell -Command "Expand-Archive -Path 'tools\dependency-check\%DC_ZIP%' -DestinationPath 'tools\dependency-check' -Force"
necho Done.
necho Extracting archive...)  exit /b 1  echo Failed to download dependency-check. Exiting.if errorlevel 1 (powershell -Command "Try { Invoke-WebRequest -Uri '%URL%' -OutFile 'tools\dependency-check\%DC_ZIP%'; exit 0 } Catch { Write-Error 'Download failed. Please check the URL and network.'; exit 1 }"
necho Downloading %URL% ...mkdir tools\dependency-check 2>nulnmdir tools\dependency-check 2>nul