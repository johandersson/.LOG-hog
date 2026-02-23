@echo off
setlocal enabledelayedexpansion

REM Sync help.md to resources folder before building
echo Syncing help.md to resources...
copy /Y %~dp0help.md %~dp0resources\help.md >nul
if %errorlevel% neq 0 (
    echo WARNING: Failed to sync help files
)

REM NOTE: removed aggressive deletion of .class files because it interferes with incremental builds
echo Skipping class file cleanup to avoid build issues

powershell -Command "Get-Process javaw -ErrorAction SilentlyContinue | Where-Object { $_.CommandLine -like '*loghog*' } | Stop-Process -Force"
REM Ensure we run from the script directory so compiled classes end up where the jar expects them
pushd "%~dp0"
set "files="
for /f "delims=" %%i in ('dir /s /b *.java ^| findstr /v test') do set "files=!files! "%%i""
javac -d . %files%
if %errorlevel% neq 0 (
    popd
    exit /b %errorlevel%
)
REM Ensure build output directory exists and create the JAR in src\build
if not exist build mkdir build
if exist build\loghog.jar del /F /Q build\loghog.jar
jar cvfm build\loghog.jar manifest.txt LogHog.class main/LogTextEditor.class gui/*.class filehandling/*.class clipboard/*.class notepad/*.class browser/*.class encryption/*.class markdown/*.class main/*.class services/*.class utils/*.class -C . resources
popd
echo Production build completed: src\build\loghog.jar
pause