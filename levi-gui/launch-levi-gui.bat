@echo off
REM Launcher script for LEVI GUI on Windows
REM This script launches the LEVI GUI application

setlocal

set "SCRIPT_DIR=%~dp0"
set "JAR_FILE=%SCRIPT_DIR%target\levi-gui-1.0.0.jar"
set "JAVAFX_LIB=C:\Program Files\Java\javafx-sdk-21.0.11\lib"

REM Check if Java is installed
where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Error: Java is not installed or not in PATH
    echo Please install Java 17 or later from: https://adoptium.net/
    pause
    exit /b 1
)

REM Check if JAR file exists; if not, build automatically
if not exist "%JAR_FILE%" (
    echo JAR not found. Building application with Maven...
    
    where mvn >nul 2>nul
    if %ERRORLEVEL% NEQ 0 (
        echo Error: Maven ^(mvn^) is not installed or not in PATH.
        echo Please install Maven 3.6+ and try again.
        pause
        exit /b 1
    )
    
    pushd "%SCRIPT_DIR%"
    call mvn clean package
    if %ERRORLEVEL% NEQ 0 (
        echo Error: Build failed. Please check the output above.
        popd
        pause
        exit /b 1
    )
    popd
    echo Build successful.
)

REM Launch the application
echo Starting LEVI GUI...
javaw --module-path "%JAVAFX_LIB%" --add-modules javafx.controls,javafx.fxml -jar "%JAR_FILE%"

if %ERRORLEVEL% NEQ 0 (
    echo Error: Failed to start LEVI GUI.
    pause
    exit /b 1
)

REM Note: Using javaw instead of java to avoid console window
