# Changelog

All notable changes to LEVI for SNOMED are documented in this file.
Versioning follows [Semantic Versioning](https://semver.org/).

## [2.1.0] - 2026-08-17

### Added
- GitHub upload service (JGit): configure a repository URL and encrypted GitHub token, and automatically clone, commit, and push generated delta files after successful jobs
- Full GUI internationalization with runtime language switching (EN/DE/FR/IT), persisted between sessions
- Configurable language filter (`de`/`fr`/`it`/`all`), replacing the interactive console prompts
- Database dropdown in the GUI that lists available databases on the server (with reload button)
- Preflight validation of the country code / DB refset configuration, blocking the Start button until it passes
- FHIR ValueSet expansion: extract designations per language from FHIR ValueSet JSON files
- Tooltips for GitHub configuration fields

### Changed
- All GUI strings, dialogs, progress messages, and statistics moved to i18n resource bundles
- GitHub upload controls moved to a dedicated GitHub settings section; removed the blocking console-input prompt from the log tab
- Launch scripts improved: macOS dock icon, JavaFX auto-detection, and quiet Maven builds
- CLI default entry point now runs the translation overview task
- Maven dependencies updated (Mockito, surefire JVM arguments for Java 17+)

### Fixed
- Case-sensitive term comparison in the DB inactivation query (uses MySQL `BINARY`)
- Eszett check only matches German ß-counterparts and uses the correct column indices
- Not-published job: correct statistics, output filenames (stray backslash removed), and job queue ordering (always runs first)
- Job queue reordering when the not-published job is not queued
- `searchEszett()` now filters by the configured refset IDs instead of joining all language refsets
- Consistent identity key normalization for previous/current/inactivation term matching
- Proper error handling for password and token decryption in the config service
- Fixed `mainClass` path in the GUI `pom.xml`

### Known Issues
- The GUI may lag during heavy jobs; an analysis and recommended fixes are documented in `gui-lag.md` (investigation only, no changes applied)

[2.1.0]: https://github.com/eHealth-Suisse/LEVI-for-SNOMED/releases/tag/v2.1.0
