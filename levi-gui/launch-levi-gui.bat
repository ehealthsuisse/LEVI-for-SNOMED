@echo off
REM Launcher script for LEVI GUI on Windows
REM This script launches the LEVI GUI application

setlocal enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"
set "JAR_FILE=%SCRIPT_DIR%target\levi-gui-2.1.0.jar"
set "FALLBACK_JAVAFX_LIB=C:\Program Files\Java\javafx-sdk-21.0.11\lib"

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

REM Determine JavaFX strategy:
REM 1. Check if JavaFX modules are already bundled in the JRE
REM 2. If not, check JAVAFX_HOME env var
REM 3. Fall back to hardcoded path

set "USE_MODULE_PATH=0"
set "JAVAFX_LIB="

java --list-modules 2>nul | findstr /I "javafx" >nul
if %ERRORLEVEL% EQU 0 (
    echo JavaFX detected in current Java runtime.
    set "USE_MODULE_PATH=0"
) else (
    if defined JAVAFX_HOME (
        if exist "%JAVAFX_HOME%\lib" (
            echo Using JavaFX from JAVAFX_HOME: %JAVAFX_HOME%
            set "JAVAFX_LIB=%JAVAFX_HOME%\lib"
            set "USE_MODULE_PATH=1"
        )
    )
    if not defined JAVAFX_LIB (
        if exist "%FALLBACK_JAVAFX_LIB%" (
            echo Using fallback JavaFX path: %FALLBACK_JAVAFX_LIB%
            set "JAVAFX_LIB=%FALLBACK_JAVAFX_LIB%"
            set "USE_MODULE_PATH=1"
        ) else (
            echo Warning: Could not locate a JavaFX installation.
            echo The application may fail to start unless JavaFX is bundled elsewhere.
        )
    )
)

REM Launch the application
echo Starting LEVI GUI...

if "%USE_MODULE_PATH%"=="1" (
    java --module-path "%JAVAFX_LIB%" --add-modules javafx.controls,javafx.fxml -jar "%JAR_FILE%"
) else (
    java -jar "%JAR_FILE%"
)

if %ERRORLEVEL% NEQ 0 (
    echo Error: Failed to start LEVI GUI.
    pause
    exit /b 1
)