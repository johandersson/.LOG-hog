@echo off
setlocal enabledelayedexpansion
set "files="
for /r %%i in (*.java) do set "files=!files! "%%i""
javac -d . %files%
if %errorlevel% neq 0 exit /b %errorlevel%
jar cvfm loghog-test.jar manifest.txt main/LogTextEditor.class gui/*.class filehandling/*.class clipboard/*.class notepad/*.class browser/*.class encryption/*.class markdown/*.class main/*.class utils/*.class resources/
echo Test build completed: loghog-test.jar
pause