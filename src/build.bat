@echo off
setlocal enabledelayedexpansion

REM Sync help.md to resources folder before building
echo Syncing help.md to resources...
copy /Y %~dp0help.md %~dp0resources\help.md >nul
if %errorlevel% neq 0 (
    echo WARNING: Failed to sync help files
)

REM Clean all .class files to ensure fresh compilation
echo Cleaning old .class files...
powershell -Command "Get-ChildItem -Path '%~dp0' -Recurse -Filter *.class | Remove-Item -Force"

REM Delete existing JAR file if it exists
if exist loghog.jar (
    echo Deleting existing JAR file...
    del /f /q loghog.jar
)
set "files="
for /f "delims=" %%i in ('dir /s /b *.java ^| findstr /v test') do set "files=!files! "%%i""
javac -d . %files%
if %errorlevel% neq 0 exit /b %errorlevel%
jar cvfm loghog.jar %~dp0manifest.txt LogHog.class main/LogTextEditor.class gui/*.class filehandling/*.class clipboard/*.class notepad/*.class browser/*.class encryption/*.class markdown/*.class main/*.class services/*.class utils/*.class -C %~dp0 resources/
echo Production build completed: loghog.jar
pause