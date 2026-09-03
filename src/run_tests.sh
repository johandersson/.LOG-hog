#!/bin/bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ROOT_DIR="${SCRIPT_DIR}/.."
TOOLS_DIR="${ROOT_DIR}/.tools"
JUNIT_VERSION="1.10.2"
JUNIT_JAR="${TOOLS_DIR}/junit-platform-console-standalone-${JUNIT_VERSION}.jar"
BUILD_DIR="${TOOLS_DIR}/testbuild"
JUNIT_URL="https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/${JUNIT_VERSION}/junit-platform-console-standalone-${JUNIT_VERSION}.jar"

mkdir -p "$TOOLS_DIR"

if [ ! -f "$JUNIT_JAR" ]; then
    echo "Downloading JUnit 5 console launcher..."
    if ! curl -fsSL "$JUNIT_URL" -o "$JUNIT_JAR"; then
        echo "Failed to download JUnit 5 console launcher. Re-run when online or place it at: $JUNIT_JAR"
        exit 1
    fi
else
    echo "Using cached JUnit 5 console launcher: $JUNIT_JAR"
fi

rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"

cd "$SCRIPT_DIR" || exit 1

echo "Compiling main sources..."
find . -name "*.java" ! -path "./test/*" -print0 | xargs -0 javac -encoding UTF-8 -cp ".:${JUNIT_JAR}" -d "$BUILD_DIR"
if [ $? -ne 0 ]; then
    echo "Main source compilation failed"
    exit 1
fi

if [ -d resources ]; then
    cp -R resources "$BUILD_DIR"/
fi

echo "Compiling test sources..."
find test -name "*.java" -print0 | xargs -0 javac -encoding UTF-8 -cp "${BUILD_DIR}:${JUNIT_JAR}" -d "$BUILD_DIR"
if [ $? -ne 0 ]; then
    echo "Test source compilation failed"
    exit 1
fi

echo "Running JUnit 5 tests..."
java -jar "$JUNIT_JAR" execute --class-path "$BUILD_DIR" --scan-class-path --disable-banner --details=tree
