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

REM Clean all .class files to ensure fresh compilation
echo Cleaning old .class files...
powershell -Command "Get-ChildItem -Path '%~dp0' -Recurse -Filter *.class | Remove-Item -Force"

powershell -Command "Get-Process javaw -ErrorAction SilentlyContinue | Where-Object { $_.CommandLine -like '*loghog*' } | Stop-Process -Force"
set "files="
pushd "%~dp0"
set "files="
for /f "delims=" %%i in ('dir /s /b *.java ^| findstr /v test') do set "files=!files! "%%i""
javac -d . %files%
if %errorlevel% neq 0 (
    popd
    exit /b %errorlevel%
)
REM Create the JAR file in the src/build directory (single artifact)
if not exist "%~dp0build" mkdir "%~dp0build"
REM Include compiled classes, resource directory and any top-level text resources
jar cvfm "%~dp0build\loghog.jar" manifest.txt LogHog.class main/LogTextEditor.class gui/*.class filehandling/*.class clipboard/*.class notepad/*.class browser/*.class encryption/*.class markdown/*.class main/*.class services/*.class utils/*.class resources/ *.txt resources/*
popd
echo Production build completed: %~dp0build\loghog.jar
pause