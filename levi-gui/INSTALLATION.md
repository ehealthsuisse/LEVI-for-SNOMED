# LEVI GUI - Installation & Launch Guide

## Quick Start - Clickable Launchers

After building the application, you can launch it with a **double-click** using the provided launcher scripts:

### 🪟 Windows
**Double-click**: `launch-levi-gui.bat`

### 🐧 Linux
**Double-click**: `launch-levi-gui.sh` (if your file manager supports it)  
**Or from terminal**: `./launch-levi-gui.sh`

### 🍎 macOS
**Double-click**: `launch-levi-gui.command`

> **Note**: On first use on macOS, you may need to:
> 1. Right-click → "Open" (instead of double-clicking)
> 2. Click "Open" in the security dialog
> 3. Or run: `chmod +x launch-levi-gui.command` in Terminal

---

## Prerequisites

Before running LEVI GUI, ensure you have:

✅ **Java 17 or later** installed  
Download from: https://adoptium.net/

✅ **SNOMED CT MySQL Database** (for actual use)

---

## Installation Options

### Option 1: Pre-built JAR (Recommended for Quick Start)

1. **Download** or **build** the application:
   ```bash
   cd LEVI-GUI
   mvn clean package
   ```

2. **Double-click** the appropriate launcher for your OS:
   - Windows: `launch-levi-gui.bat`
   - Linux: `launch-levi-gui.sh`
   - macOS: `launch-levi-gui.command`

3. **Done!** The application will start automatically.

### Option 2: Direct JAR Execution

If you prefer to run the JAR directly:

```bash
cd LEVI-GUI
java -jar target/levi-gui-1.0.0.jar
```

### Option 3: Maven Run (Development)

For development or testing:

```bash
cd LEVI-GUI
mvn javafx:run
```

### Option 4: Native Installers (Advanced)

Create platform-specific installers that integrate with your OS:

#### macOS (.dmg)
```bash
cd LEVI-GUI
mvn clean package jpackage:jpackage -Pmac
```
Output: `target/installer/LEVI-GUI-1.0.0.dmg`

**To install**:
1. Open the `.dmg` file
2. Drag "LEVI for SNOMED" to Applications
3. Launch from Applications folder or Launchpad

#### Windows (.exe)
```bash
cd LEVI-GUI
mvn clean package jpackage:jpackage -Pwin
```
Output: `target/installer/LEVI-GUI-1.0.0.exe`

**To install**:
1. Double-click the `.exe` installer
2. Follow the installation wizard
3. Launch from Start Menu or Desktop shortcut

#### Linux (.deb)
```bash
cd LEVI-GUI
mvn clean package jpackage:jpackage -Plinux
```
Output: `target/installer/levi-gui_1.0.0_amd64.deb`

**To install**:
```bash
sudo dpkg -i target/installer/levi-gui_1.0.0_amd64.deb
```
Or double-click the `.deb` file if your system supports it.

---

## Verifying Installation

### Check Java Version

**Windows**:
```cmd
java -version
```

**Linux/macOS**:
```bash
java -version
```

You should see output like:
```
openjdk version "17.0.x" or higher
```

If Java is not installed or version is too old, download from: https://adoptium.net/

### Test the Application

1. Launch using one of the methods above
2. You should see the LEVI GUI window open
3. If you see the configuration screen, installation was successful!

---

## Troubleshooting

### "Java not found" Error

**Problem**: The launcher script cannot find Java.

**Solution**:
1. Install Java 17 or later from https://adoptium.net/
2. Verify installation: `java -version`
3. Restart your terminal/command prompt
4. Try launching again

### "JAR file not found" Error

**Problem**: The application hasn't been built yet.

**Solution**:
```bash
cd LEVI-GUI
mvn clean package
```

### Permission Denied (Linux/macOS)

**Problem**: Launcher script is not executable.

**Solution**:
```bash
chmod +x launch-levi-gui.sh     # Linux
chmod +x launch-levi-gui.command # macOS
```

### macOS Security Warning

**Problem**: macOS blocks the application because it's from an unidentified developer.

**Solution**:
1. Right-click the launcher → "Open"
2. Click "Open" in the security dialog
3. Or go to System Preferences → Security & Privacy → Allow

### Windows SmartScreen Warning

**Problem**: Windows SmartScreen blocks the launcher.

**Solution**:
1. Click "More info"
2. Click "Run anyway"
3. Or create native installer for better integration

### Application Starts but Shows Blank Window

**Problem**: JavaFX libraries may not be properly loaded.

**Solution**:
1. Rebuild the application: `mvn clean package`
2. Check that you have Java 17+ (not just Java 8)
3. Try running with Maven: `mvn javafx:run`

### Database Connection Fails

**Problem**: Cannot connect to SNOMED database.

**Solution**:
1. Verify MySQL is running
2. Check database URL format: `jdbc:mysql://localhost:3306/snomed`
3. Verify username and password
4. Click "Test" button to diagnose the issue

---

## Uninstallation

### For JAR-based Installation (Options 1-3)
Simply delete the `LEVI-GUI` folder. No system files are modified.

### For Native Installers (Option 4)

**Windows**:
- Go to Settings → Apps → Find "LEVI-GUI" → Uninstall

**macOS**:
- Drag "LEVI for SNOMED" from Applications to Trash

**Linux**:
```bash
sudo dpkg -r levi-gui
```

---

## File Locations

### Application Files
- **JAR file**: `LEVI-GUI/target/levi-gui-1.0.0.jar` (55 MB)
- **Launcher scripts**: `LEVI-GUI/launch-levi-gui.*`
- **Configuration**: `~/.levi-last-config.json` (created on first use)
- **Logs**: `LEVI-GUI/logs/` (created when running)

### What Gets Created
When you run the application, it creates:
- **Configuration file**: `~/.levi-last-config.json` in your home directory
- **Log files**: In `LEVI-GUI/logs/` directory
- **No registry entries** (Windows)
- **No system files** modified

---

## System Requirements

### Minimum
- **OS**: Windows 10+, macOS 12+, Ubuntu 20.04+
- **Java**: 17 or later
- **RAM**: 2 GB
- **Disk**: 200 MB for application + space for data files
- **Display**: 1024x768 (recommended: 1200x800 or higher)

### Recommended
- **RAM**: 4 GB or more
- **Java**: Latest LTS version (21+)
- **Display**: 1920x1080 or higher
- **SSD**: For faster data processing

---

## Next Steps

After installation:

1. **First-time setup**: See [QUICKSTART.md](QUICKSTART.md)
2. **User guide**: See [README-GUI.md](README-GUI.md)
3. **Configuration**: Set up database connection and file paths
4. **Run your first job**: Start with "Translation Overview"

---

## Support

For issues:
- Check [QUICKSTART.md](QUICKSTART.md) for common problems
- Review log files in `LEVI-GUI/logs/`
- Create an issue on GitHub
- See [README-GUI.md](README-GUI.md) for detailed documentation

---

## License

This project is licensed under the MIT License. See the main repository for details.

---

**Enjoy using LEVI GUI for SNOMED CT translation validation!** 🎉
