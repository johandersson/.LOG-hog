@echo off
REM Simple LogHog JUnit Test Runner (without coverage)
REM For quick test execution

echo Running LogHog JUnit Tests (without coverage)...

REM Set classpath to include main classes, test classes, and JUnit libraries
set CLASSPATH=%~dp0src;%~dp0src\test\java;%~dp0src\lib\*

REM Run tests
java -cp %CLASSPATH% org.junit.platform.console.ConsoleLauncher --scan-classpath

echo.
echo Test execution completed.