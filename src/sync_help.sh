#!/bin/bash
# Sync help.md to resources/help.md
# Run this script whenever you modify help.md to keep both files in sync

echo "Syncing help.md to resources folder..."
cp -f help.md resources/help.md
if [ $? -eq 0 ]; then
    echo "Successfully synced help.md to resources/help.md"
else
    echo "ERROR: Failed to sync help files"
    exit 1
fi
