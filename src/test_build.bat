@echo off
setlocal enabledelayedexpansion
if not defined LOGHOG_PAUSE_ON_ERROR set "LOGHOG_PAUSE_ON_ERROR=0"
set "files="
for /r %%i in (*.java) do set "files=!files! "%%i""
javac -encoding UTF-8 -cp "src/lib/*" -d . %files%
if %errorlevel% neq 0 exit /b %errorlevel%
jar cvfm loghog-test.jar manifest.txt main/LogTextEditor.class gui/*.class filehandling/*.class clipboard/*.class notepad/*.class browser/*.class encryption/*.class markdown/*.class main/*.class utils/*.class resources/
echo Test build completed: loghog-test.jar
if /I "%LOGHOG_PAUSE_ON_ERROR%"=="1" pause