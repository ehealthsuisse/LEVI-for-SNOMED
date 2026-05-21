#!/bin/bash
# Launcher script for LEVI GUI on Linux/macOS
# This script launches the LEVI GUI application

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_FILE="$SCRIPT_DIR/target/levi-gui-1.0.0.jar"

# Initialize SDKMAN if available
if [ -f "$HOME/.sdkman/bin/sdkman-init.sh" ]; then
    source "$HOME/.sdkman/bin/sdkman-init.sh"
fi

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "Error: Java is not installed or not in PATH"
    echo "Please install Java 17 with JavaFX from: https://sdkman.io/ (sdk install java 17.0.13.fx-zulu)"
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "Error: Java 17 or later is required"
    echo "Current Java version: $(java -version 2>&1 | head -n 1)"
    echo "Please install Java 17 with JavaFX from: https://sdkman.io/ (sdk install java 17.0.13.fx-zulu)"
    exit 1
fi

# Check if JavaFX is available (required for GUI)
if ! java --list-modules 2>/dev/null | grep -q javafx; then
    echo "Warning: JavaFX modules not detected in current Java installation"
    echo "The application may not start properly"
    echo "Recommended: Install Java with JavaFX using SDKMAN: sdk install java 17.0.13.fx-zulu"
fi

# Check if JAR file exists; if not, build automatically
if [ ! -f "$JAR_FILE" ]; then
    echo "JAR not found. Building application with Maven..."
    
    # Ensure Maven is available
    if ! command -v mvn &> /dev/null; then
        echo "Error: Maven (mvn) is not installed or not in PATH."
        echo "Please install Maven 3.6+ and try again."
        exit 1
    fi
    
    pushd "$SCRIPT_DIR" > /dev/null
    if ! mvn clean package; then
        echo "Error: Build failed. Please check the output above."
        popd > /dev/null
        exit 1
    fi
    popd > /dev/null
    echo "Build successful."
fi

# Launch the application
echo "Starting LEVI GUI..."
java -jar "$JAR_FILE"
