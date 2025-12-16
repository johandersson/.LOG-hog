@echo off
setlocal enabledelayedexpansion
powershell -Command "Get-Process javaw -ErrorAction SilentlyContinue | Where-Object { $_.CommandLine -like '*loghog*' } | Stop-Process -Force"
set "files="
for /r %%i in (*.java) do set "files=!files! "%%i""
javac -cp "..\lib\junit-platform-console-standalone-1.10.2.jar" -d . %files%
if %errorlevel% neq 0 exit /b %errorlevel%
jar cvfm loghog.jar manifest.txt LogHog.class main/LogTextEditor.class gui/*.class filehandling/*.class clipboard/*.class notepad/*.class browser/*.class encryption/*.class markdown/*.class main/*.class utils/*.class resources/
echo Production build completed: loghog.jar
pause