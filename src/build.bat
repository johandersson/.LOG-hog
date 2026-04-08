@echo off
setlocal enabledelayedexpansion

REM Sync help.md to resources folder before building (prefer top-level help.md)
echo Syncing help.md to resources...
if exist "%~dp0..\help.md" (
    copy /Y "%~dp0..\help.md" "%~dp0resources\help.md" >nul
) else if exist "%~dp0help.md" (
    copy /Y "%~dp0help.md" "%~dp0resources\help.md" >nul
) else (
    echo WARNING: help.md not found in expected locations
)

REM NOTE: removed aggressive deletion of .class files because it interferes with incremental builds
echo Skipping class file cleanup to avoid build issues

powershell -Command "Get-Process javaw -ErrorAction SilentlyContinue | Where-Object { $_.CommandLine -like '*loghog*' } | Stop-Process -Force"
REM Ensure we run from the script directory so compiled classes end up where the jar expects them
pushd "%~dp0"
set "files="
pushd "%~dp0"
set "files="
for /f "delims=" %%i in ('dir /s /b *.java ^| findstr /v test') do set "files=!files! "%%i""
javac -d . %files%
if %errorlevel% neq 0 (
    popd
    REM PAUSE to allow user to see compilation errors before exiting
    echo Compilation failed with errors. Please fix the issues and try again.
    pause
    exit /b %errorlevel%
)
REM Create the JAR file in the src/build directory (single artifact)
if not exist "%~dp0build" mkdir "%~dp0build"
REM Include compiled classes, resource directory and any top-level text resources
REM Generate a date stamp for the build (YYYYMMDD) and include it in the jar filename
for /f %%i in ('powershell -NoProfile -Command "Get-Date -Format yyyyMMdd"') do set "BUILD_DATE=%%i"
set "JAR_NAME=loghog-%BUILD_DATE%.jar"
jar cvfm "%~dp0build\%JAR_NAME%" manifest.txt LogHog.class main/LogTextEditor.class gui/*.class filehandling/*.class clipboard/*.class notepad/*.class browser/*.class encryption/*.class markdown/*.class main/*.class services/*.class utils/*.class resources/ *.txt resources/*
popd
echo Production build completed: %~dp0build\%JAR_NAME%
pause