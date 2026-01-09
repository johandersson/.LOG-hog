@echo off
setlocal enabledelayedexpansion

REM Sync help.md to resources folder before building
echo Syncing help.md to resources...
copy /Y help.md resources\help.md >nul
if %errorlevel% neq 0 (
    echo WARNING: Failed to sync help files
)

powershell -Command "Get-Process javaw -ErrorAction SilentlyContinue | Where-Object { $_.CommandLine -like '*loghog*' } | Stop-Process -Force"
set "files="
for /f "delims=" %%i in ('dir /s /b *.java ^| findstr /v test') do set "files=!files! "%%i""
javac -d . %files%
if %errorlevel% neq 0 exit /b %errorlevel%
jar cvfm loghog.jar manifest.txt LogHog.class main/LogTextEditor.class gui/*.class filehandling/*.class clipboard/*.class notepad/*.class browser/*.class encryption/*.class markdown/*.class main/*.class services/*.class utils/*.class resources/
echo Production build completed: loghog.jar
pause