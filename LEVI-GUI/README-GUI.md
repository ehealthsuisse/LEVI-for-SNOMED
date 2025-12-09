# LEVI GUI - Desktop Application for LEVI for SNOMED

## Overview

LEVI GUI is a JavaFX-based desktop application that provides a graphical user interface for the LEVI for SNOMED translation validation and delta generation tool.

## Features

- **Configuration Management**: Easy-to-use interface for managing database connections, file paths, and settings
- **Job Execution**: Run all LEVI workflows with a single click:
  - Translation Overview
  - New Descriptions (Additions)
  - Inactivations
  - Complete Delta
  - Eszett Check
  - Unpublished Translations
- **Progress Tracking**: Real-time progress bars and status updates
- **Results Display**: View job statistics and results
- **Multi-language Support**: German, English, French, and Italian
- **Configuration Persistence**: Save and load configurations as JSON files
- **Secure Password Storage**: AES-256 encryption for database passwords

## Prerequisites

- Java 17 or later
- Maven 3.6+
- SNOMED CT database (MySQL)

## Installation

### Quick Start - Double-Click to Launch! 🚀

After building, you can launch LEVI GUI with a **simple double-click**:

- **Windows**: Double-click `launch-levi-gui.bat`
- **Linux**: Double-click `launch-levi-gui.sh` (or run `./launch-levi-gui.sh`)
- **macOS**: Double-click `launch-levi-gui.command`

> 📖 For detailed installation instructions, see [INSTALLATION.md](INSTALLATION.md)

### Building from Source

```bash
cd LEVI-GUI
mvn clean package
```

This creates an executable JAR at `target/levi-gui-1.0.0.jar`

### Running the Application

**Option 1: Double-Click Launcher (Easiest!) ⭐**
- Windows: `launch-levi-gui.bat`
- Linux: `launch-levi-gui.sh`
- macOS: `launch-levi-gui.command`

**Option 2: Direct JAR Execution**
```bash
java -jar target/levi-gui-1.0.0.jar
```

**Option 3: Using Maven (Development)**
```bash
mvn javafx:run
```

### Creating Native Installers (Optional)

**For macOS:**
```bash
mvn clean package jpackage:jpackage -Pmac
```
Output: `target/installer/LEVI-GUI-1.0.0.dmg`

**For Windows:**
```bash
mvn clean package jpackage:jpackage -Pwin
```
Output: `target/installer/LEVI-GUI-1.0.0.exe`

**For Linux:**
```bash
mvn clean package jpackage:jpackage -Plinux
```
Output: `target/installer/levi-gui_1.0.0_amd64.deb`

## Usage

### First-Time Setup

1. **Database Configuration**
   - Enter your MySQL database URL (e.g., `jdbc:mysql://localhost:3306/snomed`)
   - Enter username and password
   - Click "Test" to verify the connection

2. **Settings**
   - Select your country code (CH, AT, DE, FR, IT)
   - Enable Eszett transformation if needed (recommended for CH/AT)
   - Enable Regex validation

3. **File Paths**
   - Select the current translation file (CSV, TSV, Excel, or JSON)
   - Optionally select a previous file (for comparison)
   - Select an output directory for generated files

4. **Save Configuration**
   - Click "Save Config" to save your settings for future use
   - Configuration is automatically saved after each use

### Running a Job

1. Select a job by clicking one of the job buttons
2. Click "▶ Start" to begin execution
3. Monitor progress in the progress bar
4. View results in the "Results" tab when complete

### Job Types

- **Translation Overview**: Creates an overview of all translations
- **New Descriptions**: Generates delta for new translations (additions)
- **Inactivations**: Generates delta for inactivations
- **Complete Delta**: Generates complete delta (additions, changes, reactivations, inactivations)
- **Eszett Check**: Checks and replaces ß → ss in German extension
- **Unpublished Translations**: Finds translations present in previous but not in current file

## Configuration File Format

Configurations are saved as JSON files:

```json
{
  "version": "1.0",
  "database": {
    "url": "jdbc:mysql://localhost:3306/snomed",
    "username": "root",
    "password": "LEVI:encrypted_base64_string"
  },
  "settings": {
    "countryCode": "CH",
    "transformEszett": true,
    "regexCheck": true
  },
  "paths": {
    "currentFile": "/path/to/current.csv",
    "previousFile": "/path/to/previous.csv",
    "outputDirectory": "/path/to/output"
  }
}
```

## Keyboard Shortcuts

- `Ctrl+S` / `Cmd+S`: Save configuration
- `Ctrl+O` / `Cmd+O`: Load configuration
- `Ctrl+Q` / `Cmd+Q`: Quit application

## Troubleshooting

### Application won't start
- Ensure Java 17 or later is installed
- Check that JavaFX libraries are included in the JAR

### Database connection fails
- Verify MySQL is running
- Check database URL, username, and password
- Ensure database exists and is accessible

### Job fails to run
- Check that all required fields are filled
- Verify the current file exists and is readable
- Ensure output directory is writable
- Check the log tab for detailed error messages

## Log Files

Log files are stored in the `logs/` directory:
- `levi-gui.log`: Current day's log
- Historical logs are kept for 30 days

## Support

For issues and questions, please refer to the main LEVI for SNOMED repository.

## License

This project is licensed under the MIT License.
