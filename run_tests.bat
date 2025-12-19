@echo off
REM LogHog JUnit Test Runner
REM Runs all JUnit tests with coverage

echo Running LogHog JUnit Tests...

REM Set classpath to include main classes, test classes, and JUnit libraries
set CLASSPATH=%~dp0src;%~dp0src\test\java;%~dp0src\lib\*

REM Run tests (JaCoCo agent removed - using CoverageAnalyzer instead)
java -cp %CLASSPATH% org.junit.platform.console.ConsoleLauncher --scan-classpath

echo.
echo Test execution completed.
echo.

REM Generate actual coverage report using CoverageAnalyzer
echo.
echo ========================================
echo         CODE COVERAGE ANALYSIS
echo ========================================
echo.
cd /d "%~dp0"
java -cp "src\lib\*;." CoverageAnalyzer