@echo off
REM Simple LogHog JUnit Test Runner (without coverage)
REM For quick test execution

echo Running LogHog JUnit Tests (without coverage)...

REM Set classpath to include main classes, test classes, and JUnit libraries
set CLASSPATH=%~dp0src;%~dp0src\test\java;%~dp0lib\junit-platform-console-standalone-1.10.2.jar

REM Run tests
java -cp %CLASSPATH% org.junit.platform.console.ConsoleLauncher --select-package test

echo.
echo Test execution completed.