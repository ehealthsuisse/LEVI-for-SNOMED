#!/bin/bash
# Launcher script for LEVI GUI on Linux/macOS
# This script launches the LEVI GUI application

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_FILE="$SCRIPT_DIR/target/levi-gui-1.0.0.jar"

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "Error: Java is not installed or not in PATH"
    echo "Please install Java 17 or later from: https://adoptium.net/"
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "Error: Java 17 or later is required"
    echo "Current Java version: $(java -version 2>&1 | head -n 1)"
    echo "Please install Java 17 or later from: https://adoptium.net/"
    exit 1
fi

# Check if JAR file exists
if [ ! -f "$JAR_FILE" ]; then
    echo "Error: JAR file not found at $JAR_FILE"
    echo "Please build the application first using: mvn clean package"
    exit 1
fi

# Launch the application
echo "Starting LEVI GUI..."
java -jar "$JAR_FILE"
