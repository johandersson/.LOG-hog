@echo off
REM LogHog JUnit Test Runner
REM Runs all JUnit tests with coverage

echo Running LogHog JUnit Tests...

REM Set classpath to include main classes, test classes, and JUnit libraries
set CLASSPATH=%~dp0src;%~dp0src\test\java;%~dp0lib\jacoco-agent-0.8.13-runtime.jar;%~dp0lib\junit-platform-console-standalone-1.10.2.jar

REM Run tests with JaCoCo coverage
java -javaagent:%~dp0lib\jacoco-agent-0.8.13-runtime.jar=destfile=%~dp0jacoco.exec -cp %CLASSPATH% org.junit.platform.console.ConsoleLauncher --select-package test

echo.
echo Test execution completed.
echo Coverage report saved to jacoco.exec
echo.

REM Generate HTML coverage report (optional)
echo Generating HTML coverage report...
java -jar %~dp0lib\jacoco-cli-0.8.13.jar report %~dp0jacoco.exec --classfiles %~dp0src --sourcefiles %~dp0src --html %~dp0coverage

echo.
echo HTML coverage report generated in 'coverage' directory
echo.