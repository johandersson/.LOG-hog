@echo off
setlocal EnableExtensions EnableDelayedExpansion

REM Non-interactive by default. Set LOGHOG_PAUSE_ON_ERROR=1 to pause on build errors.
if not defined LOGHOG_PAUSE_ON_ERROR set "LOGHOG_PAUSE_ON_ERROR=0"

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

powershell -NoProfile -NonInteractive -Command "Get-Process javaw -ErrorAction SilentlyContinue | Where-Object { $_.CommandLine -like '*loghog*' } | Stop-Process -Force" >nul 2>&1
REM Ensure we run from the script directory so compiled classes end up where the jar expects them
pushd "%~dp0"
set "SOURCES_FILE=%TEMP%\loghog-javac-%RANDOM%%RANDOM%.args"
> "%SOURCES_FILE%" (
    for /f "delims=" %%i in ('dir /s /b *.java ^| findstr /v test') do (
        set "SRC_FILE=%%i"
        set "SRC_FILE=!SRC_FILE:\=/!"
        @echo !SRC_FILE!
    )
)
javac -encoding UTF-8 -d . @"%SOURCES_FILE%"
set "JAVAC_EXIT=%ERRORLEVEL%"
del /q "%SOURCES_FILE%" >nul 2>&1
if %JAVAC_EXIT% neq 0 (
    popd
    echo Compilation failed with errors. Please fix the issues and try again.
    if /I "%LOGHOG_PAUSE_ON_ERROR%"=="1" pause
    exit /b %JAVAC_EXIT%
)
REM Create the JAR file in the src/build directory (single artifact)
if not exist "%~dp0build" mkdir "%~dp0build"
REM Include compiled classes, resource directory and any top-level text resources
for /f %%i in ('powershell -NoProfile -Command "Get-Date -Format \"yyyy-MM-dd-HH_mm\""') do set "BUILD_TS=%%i"
if "%BUILD_TS%"=="" set "BUILD_TS=unknown"
set "JAR_NAME=loghog-%BUILD_TS%.jar"
jar cvfm "%~dp0build\%JAR_NAME%" manifest.txt LogHog.class main/LogTextEditor.class gui/*.class filehandling/*.class clipboard/*.class notepad/*.class browser/*.class encryption/*.class security/*.class markdown/*.class main/*.class services/*.class utils/*.class resources/ *.txt resources/*
if %errorlevel% neq 0 (
    popd
    echo JAR packaging failed.
    if /I "%LOGHOG_PAUSE_ON_ERROR%"=="1" pause
    exit /b %errorlevel%
)

set "INVENTORY_FILE=%~dp0build\component-inventory-%BUILD_TS%.txt"
echo Build Timestamp: %BUILD_TS%> "%INVENTORY_FILE%"
echo Artifact: %JAR_NAME%>> "%INVENTORY_FILE%"
echo Runtime: Pure JDK (no external runtime dependencies)>> "%INVENTORY_FILE%"
echo.>> "%INVENTORY_FILE%"
echo Java Version:>> "%INVENTORY_FILE%"
java -version 2>> "%INVENTORY_FILE%"
echo.>> "%INVENTORY_FILE%"
echo Source Inventory:>> "%INVENTORY_FILE%"
for /f %%c in ('dir /s /b *.java ^| find /c /v ""') do echo Java Files: %%c>> "%INVENTORY_FILE%"

set "RUN_BAT=%~dp0build\run-latest.bat"
> "%RUN_BAT%" echo @echo off
>> "%RUN_BAT%" echo setlocal
>> "%RUN_BAT%" echo java -jar "%%~dp0%JAR_NAME%"

set "RUN_SH=%~dp0build\run-latest.sh"
setlocal DisableDelayedExpansion
> "%RUN_SH%" echo #^!/usr/bin/env sh
>> "%RUN_SH%" echo SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
>> "%RUN_SH%" echo java -jar "$SCRIPT_DIR/%JAR_NAME%"
endlocal

popd
echo Production build completed: %~dp0build\%JAR_NAME%
echo Run with: java -jar "%~dp0build\%JAR_NAME%"
echo Or use: %~dp0build\run-latest.bat