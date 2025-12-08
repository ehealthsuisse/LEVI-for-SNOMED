@echo off
REM Launcher script for LEVI GUI on Windows
REM This script launches the LEVI GUI application

setlocal

set "SCRIPT_DIR=%~dp0"
set "JAR_FILE=%SCRIPT_DIR%target\levi-gui-1.0.0.jar"

REM Check if Java is installed
where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Error: Java is not installed or not in PATH
    echo Please install Java 17 or later from: https://adoptium.net/
    pause
    exit /b 1
)

REM Check if JAR file exists
if not exist "%JAR_FILE%" (
    echo Error: JAR file not found at %JAR_FILE%
    echo Please build the application first using: mvn clean package
    pause
    exit /b 1
)

REM Launch the application
echo Starting LEVI GUI...
start "LEVI GUI" javaw -jar "%JAR_FILE%"

REM Note: Using javaw instead of java to avoid console window
