@echo off
REM Runs SpotBugs CLI on compiled classes in the repository root and writes report to build\spotbugs-report
setlocal
set SB_ROOT=tools\spotbugs
for /f "delims=" %%D in ('dir /b /ad %SB_ROOT%') do set SB_DIR=%SB_ROOT%\%%D
if not exist "%SB_DIR%\bin\spotbugs.bat" (
  echo SpotBugs not found. Run tools\spotbugs\download_spotbugs.bat first.
  exit /b 1
)












"%SB_DIR%\bin\spotbugs.bat" -textui -effort:max -low -output build\spotbugs-report\spotbugs.xml %CLASSPATH%
necho SpotBugs finished. Report: build\spotbugs-report\spotbugs.xml
nexit /b 0  exit /b 1
n)
necho Class directories: %CLASSPATH%  echo No .class files found to analyze. Ensure the project is built first.
n:run
nif "%CLASSPATH%"=="" (
n:addpath
nset p=%~1
nrem remove trailing backslash
nif "%p:~-1%"=="\" set p=%p:~0,-1%
necho adding %p%
necho %CLASSPATH% | findstr /c:";%p%" >nul || set CLASSPATH=%CLASSPATH%;%p%
ngoto :eof)
ngoto :run  call :addpath "%%~dpF"  set FILE=%%~dpF
necho Running SpotBugs on compiled classes (searching for .class files under project)...
nrem Collect class directories to scan
nset CLASSPATH=
nfor /f "delims=" %%F in ('dir /s /b *.class') do (mkdir build\spotbugs-report 2>nulnrem Create output directory