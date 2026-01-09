#!/bin/bash

# Sync help.md to resources folder before building
echo "Syncing help.md to resources..."
cp -f help.md resources/help.md
if [ $? -ne 0 ]; then
    echo "WARNING: Failed to sync help files"
fi

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
jar cvfm loghog.jar manifest.txt \
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
    resources/

if [ $? -eq 0 ]; then
    echo "Production build completed: loghog.jar"
else
    echo "JAR creation failed!"
    exit 1
fi
