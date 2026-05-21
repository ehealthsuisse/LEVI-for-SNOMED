#!/bin/bash
# Launcher script for LEVI GUI on macOS
# This script launches the LEVI GUI application
# Double-click this file to run (make sure it's executable)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_FILE="$SCRIPT_DIR/target/levi-gui-1.0.0.jar"

# Check if Java is installed
if ! command -v java &> /dev/null; then
    osascript -e 'display dialog "Java is not installed. Please install Java 17 or later from https://adoptium.net/" buttons {"OK"} default button "OK" with icon stop'
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    osascript -e 'display dialog "Java 17 or later is required. Please install from https://adoptium.net/" buttons {"OK"} default button "OK" with icon stop'
    exit 1
fi

# Check if JAR file exists; if not, build automatically
if [ ! -f "$JAR_FILE" ]; then
    osascript -e 'display dialog "JAR not found. Building application with Maven — this may take a minute..." buttons {"OK"} default button "OK" with icon note'
    
    # Ensure Maven is available
    if ! command -v mvn &> /dev/null; then
        osascript -e 'display dialog "Maven (mvn) is not installed or not in PATH. Please install Maven 3.6+ and try again." buttons {"OK"} default button "OK" with icon stop'
        exit 1
    fi
    
    cd "$SCRIPT_DIR"
    if ! mvn clean package -q; then
        osascript -e 'display dialog "Build failed. Please check the terminal output for details." buttons {"OK"} default button "OK" with icon stop'
        exit 1
    fi
    cd - > /dev/null
fi

# Launch the application
java -jar "$JAR_FILE"
