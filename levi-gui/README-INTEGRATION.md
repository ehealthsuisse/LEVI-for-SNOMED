# LEVI GUI Integration with LEVI Core

## Purpose

This document explains how LEVI-GUI interacts with LEVI core classes (`translation.check.*`) and how file paths are passed from the GUI into the logic process.

## High-Level Flow

1. User enters values in the GUI (database, locale, current file, previous file, output directory).
2. `MainController` writes those values into `AppConfig`.
3. `ConfigService.toConf()` converts `AppConfig` to LEVI core `Conf`.
4. `MainController` starts selected jobs through `JobService`.
5. `JobService` creates a `CompareManager` and calls the corresponding LEVI core method.
6. LEVI core reads input files and writes output files using the paths from `Conf`.

## Core Integration Points

### 1) GUI -> AppConfig

`MainController.updateConfigFromUI()` copies field values into `AppConfig`, including:
- `paths.currentFile`
- `paths.previousFile`
- `paths.outputDirectory`

Relevant class:
- `LEVI-GUI/src/main/java/ch/ehealth/levi/gui/controller/MainController.java`

### 2) AppConfig -> Conf

`ConfigService.toConf()` maps GUI model values into LEVI core `Conf`:
- `conf.setFilePathCurrent(currentFile)`
- `conf.setFilePathPrevious(previousFile)`
- `conf.setDestination(outputDirectory)`

It also maps DB and locale settings (`countryCode` plus optional `selectedLanguageCode`).

Relevant class:
- `LEVI-GUI/src/main/java/ch/ehealth/levi/gui/service/ConfigService.java`

### 3) Job Dispatch into LEVI Core

`MainController.runNextJob(...)` creates one `Conf` per job run and dispatches by job type to `JobService`.

`JobService` then invokes `CompareManager` methods from LEVI core.

Relevant classes:
- `LEVI-GUI/src/main/java/ch/ehealth/levi/gui/controller/MainController.java`
- `LEVI-GUI/src/main/java/ch/ehealth/levi/gui/service/JobService.java`

## How Paths Are Used Per Job

The three path values in `Conf` are:
- `filePathCurrent`
- `filePathPrevious`
- `destination`

Defined in:
- `LEVI/src/translation/check/Conf.java`

Job usage in `JobService` and `CompareManager`:

- `overview`
  - Uses: `filePathCurrent`, `destination`
  - Core call: `runTranslationOverview(current, destination)`

- `desc-add`
  - Uses: `filePathCurrent`, `destination`
  - Core call: `runDeltaDescAdditions(current, destination)`

- `desc-inact`
  - Uses: `filePathCurrent`, `destination`
  - Core call: `runDeltaDescInactivations(current, destination)`

- `translate-delta`
  - Uses: `filePathCurrent`, `destination`
  - Core call: `runGenerateDelta(current, destination)`

- `eszett-check`
  - Uses: `destination`
  - Core call: `runCheckEszettInExtension(destination)`

- `not-published`
  - Uses: `filePathCurrent`, `filePathPrevious`, `destination`
  - Core call: `runDeltaNotPublishedTranslations(current, previous, destination)`
  - Optimization: if previous job already loaded current data, GUI can reuse a preloaded `CompareManager` and call `runDeltaNotPublishedTranslationsReusingCurrent(previous, destination)`.

## Input File Reading in Core

`CompareManager` delegates file loading to `FileReaderUtil.readFile(path)`.

`FileReaderUtil` selects parser/loader by extension and content type:
- Excel: `.xlsx`, `.xls`
- FHIR JSON: `.json`
- CSV/TSV variants (supported naming conventions)

Relevant classes:
- `LEVI/src/translation/check/CompareManager.java`
- `LEVI/src/translation/check/FileReaderUtil.java`

## Validation Behavior in GUI

Before start, `ConfigService.validateConfig()` checks:
- database name is present
- current file path is present and exists
- output directory is present (and can be created)

Current behavior does not enforce `previousFile` for all runs. This is fine for jobs that do not need it, but `not-published` may fail at runtime if `previousFile` is empty/invalid.

Relevant class:
- `LEVI-GUI/src/main/java/ch/ehealth/levi/gui/service/ConfigService.java`

## Logging and Interactive Input Bridge

- Logs from `translation.check.*` are forwarded into the GUI log panel via `GuiLogAppender`.
- During job execution, GUI redirects `System.in` to `GuiInputStream` so LEVI core prompts can be answered from the GUI input bar.

Relevant classes:
- `LEVI-GUI/src/main/java/ch/ehealth/levi/gui/util/GuiLogAppender.java`
- `LEVI-GUI/src/main/java/ch/ehealth/levi/gui/util/GuiInputStream.java`
- `LEVI-GUI/src/main/java/ch/ehealth/levi/gui/controller/MainController.java`
