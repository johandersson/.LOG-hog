@echo off
REM Sync help.md to resources\help.md
REM Run this script whenever you modify help.md to keep both files in sync

echo Syncing help.md to resources folder...
copy /Y help.md resources\help.md >nul
if %errorlevel% equ 0 (
    echo Successfully synced help.md to resources\help.md
) else (
    echo ERROR: Failed to sync help files
    exit /b 1
)
