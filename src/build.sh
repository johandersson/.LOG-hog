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

# Create JAR file
echo "Creating JAR file..."
jar cvfm loghog.jar "$SCRIPT_DIR/manifest.txt" \
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

if [ $? -eq 0 ]; then
    echo "Production build completed: loghog.jar"
else
    echo "JAR creation failed!"
    exit 1
fi
