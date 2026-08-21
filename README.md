# LEVI for SNOMED

![Java](https://img.shields.io/badge/Java-17%2B-orange)
![License](https://img.shields.io/badge/License-MIT-green)
![Release](https://img.shields.io/github/v/release/eHealth-Suisse/LEVI-for-SNOMED)

**LEVI for SNOMED** – **L**anguage and **E**xtension **V**alidation & **I**mport for SNOMED CT

## Description

LEVI for SNOMED is a comprehensive tool that acts as a bridge between the SNOMED International Authoring Tool and TermMed's Termspace. It facilitates the validation, comparison, and import of translation content into SNOMED CT extensions for German, French, and Italian. 

The tool is available in two interfaces:
- **Desktop GUI** (recommended for most users) - Easy-to-use graphical interface
- **Command-line** (for advanced users and automation) - Full control via command-line arguments

The tool supports multiple input formats including CSV, TSV, Excel, and FHIR JSON, and ensures consistency and accuracy in translation submissions.

## Why LEVI?

LEVI is designed for **national SNOMED CT release centers** and **translation teams** who maintain language extensions for SNOMED CT. It solves the critical problem of managing, validating, and preparing translation content for import into the SNOMED International Authoring Platform. By automating validation, comparison, and delta generation, LEVI reduces manual effort and ensures data integrity in translation workflows.

## Table of Contents

- [Features](#features)
- [Getting Started](#getting-started)
- [Option A: Desktop GUI](#option-a-desktop-gui-recommended)
- [Option B: Command-Line Interface](#option-b-command-line-interface-advanced-users)
- [Project Structure](#project-structure)
- [Architecture](#architecture)
- [Workflow](#workflow)
- [Input and Output Formats](#input-and-output-formats)
- [Configuration](#configuration)
- [Dependencies](#dependencies-levi-core)
- [Known Issues](#known-issues--limitations)
- [Contributing](#contributing)
- [License](#license)
- [Support](#support)

## Features

### General Features
- Validation of translation terms before import
- Comparison with existing SNOMED extensions
- Import template generation for new or updated translations
- Detection and preparation of inactivation entries
- Regex-based language checks (quotes, soft hyphens, spaces, capitalization)
- Support for CSV, TSV, Excel, and FHIR JSON files
- FHIR ValueSet expansion (extracts designations per language)
- Configurable language filter (de/fr/it/all)
- Handling of both "current" and "previous" releases
- Multi-language support (German, French, Italian, English)

### GUI Features
- One-click job execution
- Real-time progress tracking
- Configuration management and persistence
- Secure password storage (AES-256 encryption)
- Database connection testing
- Database dropdown (auto-lists available databases)
- Preflight validation of country code / DB refset configuration
- GitHub upload of generated delta files (JGit)
- Full internationalization with runtime language switching (EN/DE/FR/IT)
- Detailed statistics and result viewing
- **SNOMED database creation** (Database Setup tab) – build a query-ready SNOMED CT database from RF2 release files (International Edition + extension), with automatic file detection, Full/Snapshot support and a Production / Beta / Pre-Production / AT variant selector
- **XAMPP server control** (Database Setup tab) – start/stop XAMPP from inside LEVI and monitor MySQL reachability

## Getting Started

### Prerequisites

#### For End Users

- **Java 17 or later** – [Download](https://adoptium.net/)
- **MySQL database** – With SNOMED CT data (set up via [SNOMED_Database](https://github.com/eHealth-Suisse/SNOMED_Database))

#### For Developers

- **Java 17 or later** – [Download](https://adoptium.net/)
- **Maven 3.6 or later** – [Download](https://maven.apache.org/download.cgi)
- **Git** – [Download](https://git-scm.com/)
- **MySQL database** – With SNOMED CT data (set up via [SNOMED_Database](https://github.com/eHealth-Suisse/SNOMED_Database))

The SNOMED International Edition and National Extensions can be obtained via [**MLDS from SNOMED International**](https://mlds.ihtsdotools.org/), once a license has been granted.

---

## Option A: Desktop GUI (Recommended)

### Quick Start – Just Download and Run!

The easiest way to get started:

1. **Download** the pre-built JAR from the [Releases](https://github.com/eHealth-Suisse/LEVI-for-SNOMED/releases) page
2. **Double-click** to launch:
   - **Windows**: `launch-levi-gui.bat`
   - **Linux**: `launch-levi-gui.sh`
   - **macOS**: `launch-levi-gui.command`

Or run directly:
```bash
java -jar levi-gui-2.1.0.jar
```

### Building the GUI from Source

```bash
cd levi-gui
mvn clean package
```

Then run:
```bash
java -jar target/levi-gui-2.1.0.jar
```

### GUI First-Time Setup

#### Step 1: Database Connection
When you first launch LEVI GUI, configure your database:

1. **DB URL**: Enter your MySQL connection string
   ```
   jdbc:mysql://localhost:3306/snomed
   ```
2. **Database**: Select a database from the dropdown (lists all databases on the server) or type one manually. Use the reload button to refresh the list.
3. **Username**: Your database username (e.g., `root`)
4. **Password**: Your database password
5. Click **Test** to verify the connection

#### Step 2: Configure Settings

1. **Country Code**: Select your country
   - `CH` – Switzerland (German, French, Italian)
   - `DE` – Germany (German)
   - `AT` – Austria (German)
   - `FR` – France (French)
   - `IT` – Italy (Italian)

2. **Language Filter**: Select the language to process (`de`, `fr`, `it`, or `all`)
3. **Eszett Transform**: Check for CH/AT (converts ß → ss)
4. **Regex Validation**: Check to enable term validation

LEVI runs a **preflight check** against the database whenever the country code or database changes. If the configured language refsets don't match the selected SNOMED extension, the Start button stays disabled and the status bar shows a descriptive error.

#### Step 3: Set File Paths

1. **Current File**: Select your translation file (CSV, TSV, Excel, JSON)
2. **Previous File** (optional): For comparison-based operations
3. **Output Directory**: Where generated files will be saved

#### Step 4: GitHub Upload (optional)

If you want generated delta files to be pushed to a GitHub repository automatically:

1. Enter the **Repository URL** and **GitHub Token** (stored AES-256 encrypted)
2. Enable **Auto-upload after successful jobs**
3. Generated files are cloned, committed, and pushed after each successful job

#### Step 5: Save Configuration

Click **Save Config** to save settings for next time. Your configuration is also auto-saved after each job runs. Use the **Language** menu to switch the GUI language (EN/DE/FR/IT) at any time – your preference is remembered between sessions.

### Using the GUI

The GUI provides six main jobs:

| Job | Purpose | Output |
|-----|---------|--------|
| **Translation Overview** | Creates a summary of all translations | TranslationOverview.tsv |
| **New Descriptions** | Generates delta for new translations | DeltaDescAdditions.tsv |
| **Inactivations** | Generates delta for inactivations | DeltaDescInactivations.tsv |
| **Complete Delta** | Full delta (additions, changes, inactivations) | Multiple TSV files |
| **Eszett Check** | Checks and replaces ß → ss in German | Modified extension file |
| **Unpublished** | Finds translations in previous but not current | UnpublishedTranslations.tsv |

#### Running a Job

1. Click a job button (it will turn green) ✓
2. Click **▶ Start** to begin execution
3. Monitor the progress bar
4. View results in the **Statistics** tab when complete
5. Find generated files in your Output Directory

#### Understanding the Results

The Statistics tab shows:
- ✅ Number of additions, changes, inactivations, reactivations
- ⚠️ Error and warning counts
- ⏱️ Total runtime
- 📊 Detailed result summary

#### Database Setup & XAMPP

The **Database Setup** tab bundles two maintenance features:

**1. XAMPP Server control**

Instead of running `sudo /opt/lampp/lampp start` in a terminal before launching LEVI, you can control XAMPP from the GUI:

1. **XAMPP script**: Path to the control script (`/opt/lampp/lampp` on Linux, default detected)
2. **Sudo password**: Entered in the GUI and piped to `sudo -S` for the single executed command. It is **never stored** in the config file and **never logged**.
3. Press **Start XAMPP** (or **Stop XAMPP**) and check the status label.

The status label auto-detects whether MySQL/MariaDB is reachable via a JDBC ping (no root privileges required) and shows **MySQL running** or **MySQL not running**.

**2. SNOMED CT Database Creation**

Creates a complete SNOMED CT database from the RF2 Full/Snapshot release files, exactly like the standalone [SNOMED_Database](https://github.com/eHealth-Suisse/SNOMED_Database) tool:

1. Set the **International release** folder (the root of `SnomedCT_InternationalRF2_...`, containing `Full/` or `Snapshot/`)
2. Set the **Extension release** folder (root of `SnomedCT_ManagedServiceCH_...` for CH, or the AT extension) – optional but recommended
3. Select the **Database variant**: Production (full), Beta (daily build), Pre-Production or AT – determines how the suggested DB name is built and which country defaults are used
4. Click **Scan Release Files** – LEVI detects the release files, the release date, the module ID, the variant and the languages, and auto-fills the **DB name**, **country**, **variant** and **release type**
5. Optionally adjust the **New DB name**, **Material release** (Full/Snapshot) and **Country**
6. Click **Create Database**

Suggested DB names follow the SNOMED_Database convention (3-letter months):
`SCT:CH_Jun26` (Production), `SCT:CH_Beta_Aug26` (Beta), `SCT:CH_PreProdJun26` (Pre-Production), `SCT:AT_Mar25` (AT).

The tool:
- Drops and recreates the database (same behaviour as SNOMED_Database)
- Creates the 7 full-release tables (`full_concept`, `full_description`, `full_relationship`, `full_refset_Simple`, `full_refset_ExtendedMap`, `full_refset_Language`, `full_refset_ModuleDependency`)
- Imports the International Edition (concepts, English descriptions, relationships, English language refset)
- Imports all extension language files found (e.g. `de-ch`, `fr-ch`, `it-ch`, `en` for CH)
- Adds the 10 query-performance indexes

All variants of SNOMED_Database (CH production, AT, CH beta/daily build, CH pre-production) are supported through a **Database variant** selector – they differ only in the extension folder and module ID, which the scan detects automatically.

> **Note:** Generating the database can take a long time (the International Edition download is several GB). The selected database and its data will be completely dropped and recreated.

#### Configuration File Format

Configurations are saved as JSON files for easy reuse:

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
    "languageCodeFilter": "all",
    "transformEszett": true,
    "regexCheck": true
  },
  "paths": {
    "currentFile": "/path/to/current.csv",
    "previousFile": "/path/to/previous.csv",
    "outputDirectory": "/path/to/output"
  },
  "github": {
    "repoUrl": "https://github.com/user/levi-delta.git",
    "branch": "results",
    "token": "LEVI:encrypted_base64_string",
    "autoUpload": true
  },
  "dbSetup": {
    "intlReleasePath": "/home/user/Downloads/SnomedCT_InternationalRF2_PRODUCTION_20260601T120000Z",
    "extensionReleasePath": "/home/user/Downloads/SnomedCT_ManagedServiceCH_PRODUCTION_CH1000195_20260607T120000Z",
    "dbNameToCreate": "SCT:CH_Jun26",
    "dbVariant": "PRODUCTION",
    "releaseType": "FULL",
    "countryCode": "CH"
  },
  "xampp": {
    "lamppPath": "/opt/lampp/lampp"
  }
}
```

---

## Option B: Command-Line Interface (Advanced Users)

### Building the Core CLI from Source

```bash
cd levi-core
mvn clean package
```

This creates a JAR in `target/`:
```
target/levi-core-2.1.0-jar-with-dependencies.jar
```

### Running CLI Tasks

Navigate to the `target/` directory and run:

```bash
java -jar levi-core-2.1.0-jar-with-dependencies.jar ^
  --task=overview ^
  --country=DE ^
  --current="C:\path\to\current.xlsx" ^
  --dest="C:\path\to\output"
```

#### Available CLI Tasks

| Task | Description |
|------|-------------|
| `overview` | Translation Overview |
| `desc-add` | Delta Description Additions |
| `desc-inact` | Delta Description Inactivations |
| `translate-delta` | Generate Complete Translation Delta |
| `eszett-check` | Check and Transform Eszett (ß) |
| `not-published` | Find Unpublished Translations |

#### All Available CLI Arguments

| Argument | Required | Description |
|----------|----------|-------------|
| `--task=` | Yes | Task to execute (see table above) |
| `--country=` | Yes | Country code (DE, FR, IT, CH, AT) |
| `--current=` | Yes | Path to the current input file |
| `--previous=` | No | Path to the previous input file (for comparison) |
| `--dest=` | Yes | Output destination folder |
| `--dbUrl=` | Yes | JDBC database URL (e.g., `jdbc:mysql://localhost:3306/snomed`) |
| `--dbUser=` | Yes | Database username |
| `--dbPassword=` | Yes | Database password |
| `--transformEszett=` | No | `true`/`false` – transform Eszett (ß) to ss |
| `--regex=` | No | `true`/`false` – enable regex validation |

#### Example CLI Commands

**Translation Overview:**
```bash
java -jar levi-core-2.1.0-jar-with-dependencies.jar ^
  --task=overview ^
  --country=DE ^
  --current="C:\translations\current.xlsx" ^
  --dest="C:\output" ^
  --dbUrl="jdbc:mysql://localhost:3306/snomed" ^
  --dbUser=root ^
  --dbPassword=secret
```

**Generate Complete Delta with Eszett Transformation:**
```bash
java -jar levi-core-2.1.0-jar-with-dependencies.jar ^
  --task=translate-delta ^
  --country=CH ^
  --current="C:\translations\current.xlsx" ^
  --previous="C:\translations\previous.xlsx" ^
  --dest="C:\output" ^
  --dbUrl="jdbc:mysql://localhost:3306/snomed" ^
  --dbUser=root ^
  --dbPassword=secret ^
  --transformEszett=true ^
  --regex=true
```

#### IDE Setup (Development)

For development in Eclipse or IntelliJ:

1. Import the `levi-core` project
2. Open `Conf.java` and set your database connection details
3. In `Main.java`, uncomment your desired method:
   ```java
   compareManager.runTranslationOverview(conf.getFilePathCurrent(), conf.getDestination());
   ```
4. Run `Main.java` as a Java Application

---

## Project Structure

LEVI for SNOMED consists of two main modules:

```
LEVI-for-SNOMED/
├── levi-core/              # Core translation validation engine
│   ├── src/
│   │   ├── main/java/...   # Business logic for translation processing
│   │   └── test/java/...   # Unit tests
│   └── pom.xml
│
├── levi-gui/               # JavaFX desktop application
│   ├── src/
│   │   ├── main/java/...   # GUI components and controllers
│   │   └── resources/      # FXML layouts, CSS, i18n files
│   ├── launch-*.sh/.bat    # Quick-start launchers
│   └── pom.xml
│
├── pom.xml                 # Parent POM
└── README.md               # This file
```

### Module Descriptions

- **`levi-core`**: The core translation processing engine
  - Handles file reading, validation, comparison, and delta generation
  - Supports CSV, TSV, Excel, and JSON formats
  - Database operations for SNOMED CT lookups
  - Can be used as a library or executed via CLI

- **`levi-gui`**: JavaFX-based desktop application
  - User-friendly interface for levi-core functionality
  - Configuration management and persistence
  - Progress tracking and real-time feedback
  - Multi-language support (German, English, French, Italian)

---

## Architecture

LEVI consists of several modular components. For detailed information about the architecture, class diagram, and component descriptions, see [ARCHITECTURE.md](ARCHITECTURE.md).

## Workflow

### GUI Workflow
1. User launches LEVI GUI application
2. User configures database connection and file paths
3. User selects a job (Translation Overview, Delta Generation, etc.)
4. Application processes files, validates translations, and generates results
5. User views statistics and results in the GUI
6. Generated files are saved to the output directory

### CLI Workflow
1. User specifies task and parameters via command-line arguments
2. Core engine reads input files
3. `FileReaderUtil` routes to appropriate processor based on file type
4. Processor validates data and populates `ResultCollector`
5. Optional regex validation is applied
6. Results are written to output files
7. Process exits with status code

## Input and Output Formats

### Supported Input Formats

- **CSV** – Comma-separated values (UTF-8)
- **TSV** – Tab-separated values (UTF-8)
- **Excel** – .xlsx and .xls formats
- **FHIR JSON** – ValueSet JSON according to FHIR specification

### Output Files

Generated files depend on the job selected:

- `TranslationOverview.tsv` – Summary of all translations
- `DeltaDescAdditions.tsv` – New translation terms
- `DeltaDescChanges.tsv` – Modified translations
- `DeltaDescInactivations.tsv` – Terms to inactivate
- `DeltaDescReactivations.tsv` – Terms to reactivate
- `UnpublishedTranslations.tsv` – Translations in previous but not current

All output files are UTF-8 encoded TSV format suitable for import into the SNOMED International Authoring Platform.

### Batch Grouping

When running the `translate-delta` task, output files can be grouped by combinations of change types:

| Group | Change Types | Group | Change Types |
|-------|-------------|-------|-------------|
| G1 | CHANGE | G8 | ADDITION + INACTIVATION |
| G2 | ADDITION | G9 | ADDITION + REACTIVATION |
| G3 | INACTIVATION | G10 | INACTIVATION + REACTIVATION |
| G4 | REACTIVATION | G11 | CHANGE + ADDITION + INACTIVATION |
| G5 | CHANGE + ADDITION | G12 | CHANGE + ADDITION + REACTIVATION |
| G6 | CHANGE + INACTIVATION | G13 | CHANGE + INACTIVATION + REACTIVATION |
| G7 | CHANGE + REACTIVATION | G14 | ADDITION + INACTIVATION + REACTIVATION |
|  |  | G15 | CHANGE + ADDITION + INACTIVATION + REACTIVATION |

Large groups are automatically split into batches (default: 1,000 concepts per batch). Output filenames reflect the grouping: `G5_changes.tsv`, `G5_additions_batch1.tsv`, etc. Only applicable change types produce files for each group.

---

## Additional Documentation

- [GUI Quick Start Guide](levi-gui/QUICKSTART.md) – Get started with the GUI in 5 minutes
- [UI Description](levi-gui/UI_DESCRIPTION.md) – Detailed UI component descriptions
- [Installation Guide](levi-gui/INSTALLATION.md) – Advanced installation options
- [Integration Guide](levi-gui/README-INTEGRATION.md) – Integration with other systems

---

## Configuration

### Database Configuration
- JDBC connection string
- Database username and password
- Connection is tested before any operations

### Application Settings
- Country code (determines language rules)
- Language filter (de/fr/it/all)
- Eszett transformation toggle (ß → ss for German)
- Regex validation toggle (validates term format)
- GUI language (EN/DE/FR/IT, switched at runtime)

### GitHub Upload
- Repository URL and GitHub token (AES-256 encrypted)
- Optional auto-upload of generated delta files after successful jobs

### File Paths
- Input file (current translations)
- Optional comparison file (previous translations)
- Output directory for generated files

---

## Dependencies (levi-core)

* **Apache Commons Lang 3**

  * For utilities like `Pair`
* **OpenCSV**

  * `CSVReader`, `CSVParserBuilder`, `CSVReaderBuilder`
* **Apache POI**

  * For Excel file support (`HSSFWorkbook`, `XSSFWorkbook`, `Sheet`, etc.)
* **JDBC (java.sql.\*)**

  * For database interaction
* **Java Core Libraries**

  * `java.io`, `java.nio.file`, `java.util`, `java.util.stream`, etc.

## Known Issues / Limitations

- Large files (>100k rows) may require 8GB+ RAM
- No built-in multithreading (single-threaded processing)
- Language code must be manually entered for some file formats if not present
- Tested primarily against TermMed SNOMED extension setups

---

## Contributing

Contributions are welcome! Please:
1. Fork the repository
2. Create a feature branch
3. Submit a pull request with a clear description

For major features, please discuss via an issue first.

## License

This project is licensed under the MIT License. See LICENSE file for details.

## Support

- **Documentation**: See the [levi-gui](levi-gui/) directory for GUI-specific docs
- **Issues**: Report bugs via GitHub Issues
- **Questions**: Consult the documentation files in each module

---

**Last Updated**: 2026-08-17  
**Version**: 2.1.0 (with GUI)
