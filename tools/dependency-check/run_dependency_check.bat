@echo off
REM Runs OWASP Dependency-Check CLI against the repository (uses extracted repo from download script)
setlocal
set DC_ROOT=tools\dependency-check\dependency-check
if not exist "%DC_ROOT%\bin\dependency-check.bat" (
  echo Dependency-Check not found. Run tools\dependency-check\download_dependency_check.bat first.
  exit /b 1
)









pause)  echo dependency-check finished successfully. Reports in build\dependency-check-report\) else (  echo dependency-check reported issues or failed. Check output and exit code.if errorlevel 1 ("%DC_ROOT%\bin\dependency-check.bat" --project "LogHog" --scan "%cd%\src" --format ALL --out "%cd%\build\dependency-check-report"necho Running OWASP Dependency-Check (this may take several minutes)...