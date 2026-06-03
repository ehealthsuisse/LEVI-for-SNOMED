# LEVI GUI - Quick Start Guide

## Prerequisites

Before you begin, ensure you have:
- ✅ Java 17 or higher installed ([Download here](https://adoptium.net/))
- ✅ MySQL database with SNOMED CT data
- ✅ A translation file (CSV, TSV, Excel, or JSON format)

## Installation

### Option 1: Download Executable JAR (Recommended)
1. Download `levi-gui-1.0.0.jar` from the releases page
2. Double-click the JAR file or run: `java -jar levi-gui-1.0.0.jar`

### Option 2: Build from Source
```bash
git clone https://github.com/gurtner-it/LEVI-for-SNOMED.git
cd LEVI-for-SNOMED/LEVI-GUI
mvn clean package
java -jar target/levi-gui-1.0.0.jar
```

## First Run

### Step 1: Configure Database Connection
When you first launch the application:

1. **Database URL**: Enter your MySQL connection string
   ```
   jdbc:mysql://localhost:3306/snomed
   ```

2. **Username**: Your database username (e.g., `root`)

3. **Password**: Your database password

4. Click **Test** button to verify connection
   - ✅ Green "Connected" = Success
   - ❌ Red "Disconnected" = Check credentials

### Step 2: Configure Settings

1. **Country Code**: Select your country from dropdown
   - `CH` - Switzerland (German, French, Italian)
   - `AT` - Austria (German)
   - `DE` - Germany (German)
   - `FR` - France (French)
   - `IT` - Italy (Italian)

2. **Eszett Transform**: ☑ Check this for CH/AT to convert ß → ss

3. **Regex Validation**: ☑ Check to enable term validation

### Step 3: Set File Paths

1. **Current File**: Click **Browse...** and select your translation file
   - Supported formats: CSV, TSV, Excel (.xlsx, .xls), JSON

2. **Previous File** (optional): For comparison operations

3. **Output Directory**: Where to save generated files

### Step 4: Save Configuration

Click **Save Config** to save your settings for next time.
The configuration is also auto-saved when you run a job.

## Running Your First Job

### Simple Translation Overview

1. Click the **Translation Overview** button (it turns green)
2. Click **▶ Start**
3. Watch the progress bar
4. View results in the **Statistics** tab

### Generate Complete Delta

1. Click the **Complete Delta** button
2. Click **▶ Start**
3. Monitor progress (can take a few minutes for large files)
4. Check the output directory for generated files:
   - `DeltaDescAdditions.tsv`
   - `DeltaDescChanges.tsv` (if changes exist)
   - `DeltaDescInactivations.tsv`

## Understanding the Jobs

### 1. Translation Overview
**What it does**: Creates a summary of all translations  
**Output**: `TranslationOverview.tsv`  
**Use when**: You want to see what's in your file

### 2. New Descriptions
**What it does**: Generates delta for new translations  
**Output**: `DeltaDescAdditions.tsv`, `DeltaDescChanges.tsv`  
**Use when**: Adding new terms to SNOMED

### 3. Inactivations
**What it does**: Generates delta for inactivating terms  
**Output**: `DeltaDescInactivations.tsv`  
**Use when**: Retiring obsolete translations

### 4. Complete Delta
**What it does**: Full delta (additions + changes + inactivations)  
**Output**: All delta files  
**Use when**: Complete update cycle

### 5. Eszett Check
**What it does**: Finds and replaces ß with ss in German extension  
**Output**: `EszettInactivations.tsv`, `EszettAdditions.tsv`  
**Use when**: Standardizing German text for CH/AT

### 6. Unpublished Translations
**What it does**: Finds terms in previous but not in current  
**Output**: `DeltaNotPublishedTranslations.tsv`  
**Use when**: Tracking what was removed

## Tips & Tricks

### Keyboard Shortcuts
- `Ctrl+S` / `Cmd+S` - Save configuration
- `Ctrl+O` / `Cmd+O` - Load configuration
- `Ctrl+Q` / `Cmd+Q` - Quit application

### Tooltips
Hover over any field to see helpful information about what it does.

### Validation
- Red border = Required field is missing
- Fix all red borders before starting a job

### Progress Monitoring
- **Progress Bar**: Shows completion percentage
- **Status Label**: Shows current operation
- **Runtime**: Shows elapsed time (HH:MM:SS)

### Canceling Jobs
Click **⏹ Cancel** to stop a running job at any time.

### Viewing Logs
Switch to the **Log** tab to see detailed execution logs.
Click **Save Log** to export for troubleshooting.

## Common Issues

### "Database connection failed"
**Solution**: 
- Check MySQL is running: `systemctl status mysql`
- Verify URL, username, and password
- Test connection using MySQL Workbench first
- Check firewall settings

### "File not found"
**Solution**:
- Use absolute paths (e.g., `/home/user/data/file.csv`)
- Ensure file exists and is readable
- Check file permissions
- Use **Browse...** button to select file

### "Cannot create output directory"
**Solution**:
- Ensure parent directory exists
- Check write permissions
- Use an existing directory
- Don't use special characters in path

### "Job failed: OutOfMemoryError"
**Solution**:
- Increase Java heap: `java -Xmx4g -jar levi-gui-1.0.0.jar`
- Split large files into smaller chunks
- Close other applications

### "No data in results"
**Solution**:
- Check that current file has data
- Verify file format is correct
- Ensure database has reference data
- Check log tab for details

## Language Selection

The UI automatically uses the language based on your country code:
- `CH`, `AT`, `DE` → German
- `FR` → French
- `IT` → Italian
- Default → English

Want to change? Modify `countryCodeCombo` in the configuration section.

## Configuration File Format

Your configuration is saved as JSON:

```json
{
  "version": "1.0",
  "database": {
    "url": "jdbc:mysql://localhost:3306/snomed",
    "username": "root",
    "password": "LEVI:encrypted_password_here"
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

Stored at: `~/.levi-last-config.json`

## Getting Help

### Documentation
- **README-GUI.md** - Full user manual
- **IMPLEMENTATION_SUMMARY.md** - Technical details
- **Main README** - LEVI core documentation

### Support
- Check the log tab for error details
- Export logs for bug reports
- Create an issue on GitHub
- Contact your system administrator

## Next Steps

Once you're comfortable with the basic workflow:
1. Set up scheduled jobs (external scheduler + CLI)
2. Create configuration templates for different scenarios
3. Integrate with your translation workflow
4. Explore advanced features

## Success Indicators

✅ Database status shows "Connected" in green  
✅ All required fields filled (no red borders)  
✅ Job completes with "Successful" status  
✅ Output files appear in output directory  
✅ Statistics show expected counts  

You're ready to use LEVI GUI for production workflows!

---

**Need more help?** See the full documentation in [README-GUI.md](README-GUI.md)
