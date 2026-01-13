@echo off
setlocal enabledelayedexpansion

echo Checking for running LogHog processes...

set "found=0"
for /f "tokens=2 delims==" %%i in ('wmic process where "(name=''java.exe'' or name=''javaw.exe'') and commandline like ''%%LogHog%%''" get processid /value 2^>nul ^| find "="') do (
    set "pid=%%i"
    echo Found LogHog process with PID !pid!
    set "found=1"
)

if !found!==1 (
    echo Killing LogHog processes...
    for /f "tokens=2 delims==" %%i in ('wmic process where "(name=''java.exe'' or name=''javaw.exe'') and commandline like ''%%LogHog%%''" get processid /value 2^>nul ^| find "="') do (
        taskkill /PID %%i /F ^>nul 2^>^&1
    )
    echo LogHog processes terminated.
)

echo.
echo Syncing help.md to resources...
copy /Y help.md resources\help.md ^>nul 2^>^&1

echo Cleaning old .class files...
del /s /q *.class 2^>nul

if exist loghog.jar (
    echo Deleting existing JAR file...
    del /f /q loghog.jar 2^>nul
)

echo Compiling Java files...
for /f "delims=" %%i in ('dir /s /b *.java ^| findstr /v test') do javac -d . "%%i"

if %errorlevel% neq 0 (
    echo Compilation failed.
    pause
    exit /b 1
)

echo Creating JAR file...
jar cvfm loghog.jar manifest.txt LogHog.class main/LogTextEditor.class gui/*.class filehandling/*.class clipboard/*.class notepad/*.class browser/*.class encryption/*.class markdown/*.class main/*.class services/*.class utils/*.class -C . resources/ ^>nul 2^>^&1

if %errorlevel% neq 0 (
    echo JAR creation failed.
    pause
    exit /b 1
)

echo.
echo Build completed successfully: loghog.jar
pause
