#!/bin/bash

# Get the directory where this script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Sync help.md to resources folder before building
echo "Syncing help.md to resources..."
cp -f "$SCRIPT_DIR/help.md" "$SCRIPT_DIR/resources/help.md"
if [ $? -ne 0 ]; then
    echo "WARNING: Failed to sync help files"
fi

# NOTE: removed aggressive deletion of .class files to avoid interfering with incremental builds
echo "Skipping class file cleanup to avoid build issues"

# Stop any running loghog instances
pkill -f "java.*loghog" 2>/dev/null || true

# Compile Java files (excluding test files)
echo "Compiling Java files..."
find . -name "*.java" ! -path "*/test/*" -print0 | xargs -0 javac -d .
if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi

# Create JAR file in top-level build directory
echo "Creating JAR file..."
mkdir -p "$SCRIPT_DIR/../build"
BUILD_TS="$(date +"%Y-%m-%d-%H_%M")"
JAR_NAME="loghog-$BUILD_TS.jar"
jar cvfm "$SCRIPT_DIR/../build/$JAR_NAME" "$SCRIPT_DIR/manifest.txt" \
    LogHog.class \
    main/LogTextEditor.class \
    gui/*.class \
    filehandling/*.class \
    clipboard/*.class \
    notepad/*.class \
    browser/*.class \
    encryption/*.class \
    markdown/*.class \
    main/*.class \
    services/*.class \
    utils/*.class \
    -C "$SCRIPT_DIR" resources/

INVENTORY_FILE="$SCRIPT_DIR/../build/component-inventory-$BUILD_TS.txt"
{
  echo "Build Timestamp: $BUILD_TS"
  echo "Artifact: $JAR_NAME"
  echo "Runtime: Pure JDK (no external runtime dependencies)"
  echo
  echo "Java Version:"
  java -version 2>&1
  echo
  echo "Source Inventory:"
  echo "Java Files: $(find . -name '*.java' ! -path '*/test/*' | wc -l | tr -d ' ')"
} > "$INVENTORY_FILE"

if [ $? -eq 0 ]; then
    echo "Production build completed: $SCRIPT_DIR/../build/$JAR_NAME"
else
    echo "JAR creation failed!"
    exit 1
fi
