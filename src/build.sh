#!/bin/bash

# Get the directory where this script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
BUILD_DIR="$SCRIPT_DIR/../build"
SOURCES_FILE=""
if SOURCES_FILE="$(mktemp -t loghog-javac.XXXXXX 2>/dev/null)"; then
    :
elif SOURCES_FILE="$(mktemp "${TMPDIR:-/tmp}/loghog-javac.XXXXXX" 2>/dev/null)"; then
    :
else
    echo "Failed to create temporary source list file"
    exit 1
fi
cleanup() { rm -f "$SOURCES_FILE"; }
trap cleanup EXIT

# Sync help.md to resources folder before building
echo "Syncing help.md to resources..."
if [ -f "$SCRIPT_DIR/../help.md" ]; then
    cp -f "$SCRIPT_DIR/../help.md" "$SCRIPT_DIR/resources/help.md"
elif [ -f "$SCRIPT_DIR/help.md" ]; then
    cp -f "$SCRIPT_DIR/help.md" "$SCRIPT_DIR/resources/help.md"
else
    echo "WARNING: help.md not found in expected locations"
fi
if [ $? -ne 0 ]; then
    echo "WARNING: Failed to sync help files"
fi

# NOTE: removed aggressive deletion of .class files to avoid interfering with incremental builds
echo "Skipping class file cleanup to avoid build issues"

# Stop any running loghog instances
PIDS="$(pgrep -f "java.*loghog" 2>/dev/null || true)"
if [ -n "$PIDS" ]; then
    while IFS= read -r pid; do
        [ -n "$pid" ] && kill "$pid" 2>/dev/null || true
    done <<< "$PIDS"
fi

# Compile Java files (excluding test files)
echo "Compiling Java files..."
(
    cd "$SCRIPT_DIR" || exit 1
    find . -name "*.java" ! -path "*/test/*" -print | sort > "$SOURCES_FILE"
)
if [ ! -s "$SOURCES_FILE" ]; then
    echo "Compilation failed: no source files found"
    exit 1
fi

(
    cd "$SCRIPT_DIR" || exit 1
    javac -encoding UTF-8 -d . @"$SOURCES_FILE"
)
if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi

# Create JAR file in top-level build directory
echo "Creating JAR file..."
mkdir -p "$BUILD_DIR"
BUILD_TS="$(date +"%Y-%m-%d-%H_%M")"
JAR_NAME="loghog-$BUILD_TS.jar"
CLASS_FILES=()
while IFS= read -r classFile; do
    CLASS_FILES+=("$classFile")
done < <(
    cd "$SCRIPT_DIR" || exit 1
    find . -name "*.class" ! -name "*Test.class" ! -path "./test/*" -print \
        | sed 's|^\./||' \
        | sort
)
if [ "${#CLASS_FILES[@]}" -eq 0 ]; then
    echo "JAR creation failed: no compiled classes found"
    exit 1
fi

(
    cd "$SCRIPT_DIR" || exit 1
    jar cvfm "$BUILD_DIR/$JAR_NAME" "$SCRIPT_DIR/manifest.txt" \
        "${CLASS_FILES[@]}" \
        -C "$SCRIPT_DIR/.." LICENSE.md \
        -C "$SCRIPT_DIR" resources/
)

INVENTORY_FILE="$BUILD_DIR/component-inventory-$BUILD_TS.txt"
{
  echo "Build Timestamp: $BUILD_TS"
  echo "Artifact: $JAR_NAME"
  echo "Runtime: Pure JDK (no external runtime dependencies)"
  echo
  echo "Java Version:"
  java -version 2>&1
  echo
  echo "Source Inventory:"
  echo "Java Files: $(cd "$SCRIPT_DIR" && find . -name '*.java' ! -path '*/test/*' | wc -l | tr -d ' ')"
} > "$INVENTORY_FILE"

if [ $? -eq 0 ]; then
    RUN_SH="$BUILD_DIR/run-latest.sh"
    RUN_BAT="$BUILD_DIR/run-latest.bat"

    {
        echo "#!/usr/bin/env sh"
        echo "SCRIPT_DIR=\"\$(CDPATH= cd -- \"\$(dirname -- \"\$0\")\" && pwd)\""
        echo "java -jar \"\$SCRIPT_DIR/$JAR_NAME\""
    } > "$RUN_SH"
    chmod +x "$RUN_SH"

    {
        echo "@echo off"
        echo "setlocal"
        echo "java -jar \"%~dp0$JAR_NAME\""
    } > "$RUN_BAT"

    echo "Production build completed: $BUILD_DIR/$JAR_NAME"
    echo "Run with: java -jar \"$BUILD_DIR/$JAR_NAME\""
    echo "Or use: $RUN_SH (Linux/macOS) or $RUN_BAT (Windows)"
else
    echo "JAR creation failed!"
    exit 1
fi
