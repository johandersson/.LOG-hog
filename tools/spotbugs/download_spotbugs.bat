@echo off
REM Downloads SpotBugs CLI zip and extracts it to tools\spotbugs
set SB_VERSION=4.7.3
set SB_ZIP=spotbugs-%SB_VERSION%.zip
set URL=https://github.com/spotbugs/spotbugs/releases/download/%SB_VERSION%/%SB_ZIP%





echo Done. Run tools\spotbugs\run_spotbugs.bat to execute spotbugs.powershell -Command "Expand-Archive -Path 'tools\spotbugs\%SB_ZIP%' -DestinationPath 'tools\spotbugs' -Force"powershell -Command "Invoke-WebRequest -Uri '%URL%' -OutFile 'tools\spotbugs\%SB_ZIP%'"
necho Extracting...nmkdir tools\spotbugs 2>nul