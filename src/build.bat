@echo off
setlocal enabledelayedexpansion

REM Close any running LogHog Java processes to prevent JAR file locking
echo Checking for running LogHog processes...
powershell -Command "Get-Process -Name javaw -ErrorAction SilentlyContinue | Where-Object { $_.CommandLine -like '*loghog*' } | Stop-Process -Force"
powershell -Command "Get-Process -Name java -ErrorAction SilentlyContinue | Where-Object { $_.CommandLine -like '*loghog*' } | Stop-Process -Force"
echo LogHog processes terminated (if any were running).

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
    del /f /q loghog.jar 2>nul
    if exist loghog.jar (
        echo JAR file is locked, will create temporary JAR and rename...
        set TEMP_JAR=1
    )
)
set "files="
for /f "delims=" %%i in ('dir /s /b *.java ^| findstr /v test') do set "files=!files! "%%i""
javac -d . %files%
if %errorlevel% neq 0 exit /b %errorlevel%

if defined TEMP_JAR (
    jar cvfm loghog_temp.jar %~dp0manifest.txt LogHog.class main/LogTextEditor.class gui/*.class filehandling/*.class clipboard/*.class notepad/*.class browser/*.class encryption/*.class markdown/*.class main/*.class services/*.class utils/*.class -C %~dp0 resources/
    if exist loghog.jar (
        echo Original JAR is locked, keeping both versions...
        echo New JAR created as: loghog_temp.jar
        echo You can manually replace loghog.jar with loghog_temp.jar when ready
    ) else (
        move loghog_temp.jar loghog.jar
    )
) else (
    jar cvfm loghog.jar %~dp0manifest.txt LogHog.class main/LogTextEditor.class gui/*.class filehandling/*.class clipboard/*.class notepad/*.class browser/*.class encryption/*.class markdown/*.class main/*.class services/*.class utils/*.class -C %~dp0 resources/
)
echo Production build completed: loghog.jar
pause