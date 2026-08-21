package ch.ehealth.levi.core.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ehealth.levi.core.db.SctReleaseFileScanner.SctRelease;
import ch.ehealth.levi.core.compare.ProgressListener;

/**
 * Creates a MySQL/MariaDB database and imports SNOMED CT RF2 release data.
 *
 * <p>Mirrors the standalone SNOMED_Database project (CreateDatabaseAndImportData /
 * Create_AT_DB / CreateBetaDB / CreatePreProductionDB): creates the database,
 * the seven full-release tables, imports the International Edition plus the
 * extension language files, and finally adds the query-performance indexes.
 */
public class SctDatabaseCreator {

    private static final Logger logger = LoggerFactory.getLogger(SctDatabaseCreator.class);

    // ------------------------------------------------------------------
    // DDL (identical to SNOMED_Database)
    // ------------------------------------------------------------------

    private static final String CREATE_DATABASE_SQL =
            "DROP DATABASE IF EXISTS `%s`;\n"
            + "CREATE DATABASE `%s` /*!40100 DEFAULT CHARACTER SET utf8mb4 */;\n"
            + "USE `%s`;\n"
            + "SET GLOBAL net_write_timeout = 60;\n"
            + "SET GLOBAL net_read_timeout = 120;\n"
            + "SET GLOBAL sql_mode = '';\n"
            + "SET SESSION sql_mode = '';";

    private static final String CREATE_CONCEPT_TABLE =
            "DROP TABLE IF EXISTS `full_concept`;\n"
            + "CREATE TABLE `full_concept` (\n"
            + "    `id` BIGINT NOT NULL DEFAULT  0,\n"
            + "    `effectiveTime` DATETIME NOT NULL DEFAULT  '2000-01-31 00:00:00',\n"
            + "    `active` TINYINT NOT NULL DEFAULT  0,\n"
            + "    `moduleId` BIGINT NOT NULL DEFAULT  0,\n"
            + "    `definitionStatusId` BIGINT NOT NULL DEFAULT  0,\n"
            + "    PRIMARY KEY (`id`,`effectiveTime`))\n"
            + "    ENGINE=MyISAM DEFAULT CHARSET=utf8mb4;";

    private static final String CREATE_DESCRIPTION_TABLE =
            "DROP TABLE IF EXISTS `full_description`;\n"
            + "CREATE TABLE `full_description` (\n"
            + "    `id` BIGINT NOT NULL DEFAULT  0,\n"
            + "    `effectiveTime` DATETIME NOT NULL DEFAULT  '2000-01-31 00:00:00',\n"
            + "    `active` TINYINT NOT NULL DEFAULT  0,\n"
            + "    `moduleId` BIGINT NOT NULL DEFAULT  0,\n"
            + "    `conceptId` BIGINT NOT NULL DEFAULT  0,\n"
            + "    `languageCode` VARCHAR (3) NOT NULL DEFAULT '',\n"
            + "    `typeId` BIGINT NOT NULL DEFAULT  0,\n"
            + "    `term` TEXT NOT NULL,\n"
            + "    `caseSignificanceId` BIGINT NOT NULL DEFAULT  0,\n"
            + "    PRIMARY KEY (`id`,`effectiveTime`))\n"
            + "    ENGINE=MyISAM DEFAULT CHARSET=utf8mb4;";

    private static final String CREATE_RELATIONSHIP_TABLE =
            "DROP TABLE IF EXISTS `full_relationship`;\n"
            + "CREATE TABLE `full_relationship` (\n"
            + "    `id` BIGINT NOT NULL DEFAULT  0,\n"
            + "    `effectiveTime` DATETIME NOT NULL DEFAULT  '2000-01-31 00:00:00',\n"
            + "    `active` TINYINT NOT NULL DEFAULT  0,\n"
            + "    `moduleId` BIGINT NOT NULL DEFAULT  0,\n"
            + "    `sourceId` BIGINT NOT NULL DEFAULT  0,\n"
            + "    `destinationId` BIGINT NOT NULL DEFAULT  0,\n"
            + "    `relationshipGroup` INT NOT NULL DEFAULT 0,\n"
            + "    `typeId` BIGINT NOT NULL DEFAULT  0,\n"
            + "    `characteristicTypeId` BIGINT NOT NULL DEFAULT  0,\n"
            + "    `modifierId` BIGINT NOT NULL DEFAULT  0,\n"
            + "    PRIMARY KEY (`id`,`effectiveTime`))\n"
            + "    ENGINE=MyISAM DEFAULT CHARSET=utf8mb4;";

    private static final String CREATE_SIMPLE_REFSET_TABLE =
            "DROP TABLE IF EXISTS `full_refset_Simple`;\n"
            + "CREATE TABLE `full_refset_Simple` (\n"
            + "  `id` char(36) NOT NULL DEFAULT '',\n"
            + "  `effectiveTime` DATETIME NOT NULL DEFAULT  '2000-01-31 00:00:00',\n"
            + "  `active` TINYINT NOT NULL DEFAULT  0,\n"
            + "  `moduleId` BIGINT NOT NULL DEFAULT  0,\n"
            + "  `refsetId` BIGINT NOT NULL DEFAULT  0,\n"
            + "  `referencedComponentId` BIGINT NOT NULL DEFAULT  0,\n"
            + "  PRIMARY KEY (`id`,`effectiveTime`))\n"
            + "  ENGINE=MyISAM DEFAULT CHARSET=utf8mb4;";

    private static final String CREATE_EXTENDED_MAP_REFSET_TABLE =
            "DROP TABLE IF EXISTS `full_refset_ExtendedMap`;\n"
            + "CREATE TABLE `full_refset_ExtendedMap` (\n"
            + "  `id` char(36) NOT NULL DEFAULT '',\n"
            + "  `effectiveTime` DATETIME NOT NULL DEFAULT  '2000-01-31 00:00:00',\n"
            + "  `active` TINYINT NOT NULL DEFAULT  0,\n"
            + "  `moduleId` BIGINT NOT NULL DEFAULT  0,\n"
            + "  `refsetId` BIGINT NOT NULL DEFAULT  0,\n"
            + "  `referencedComponentId` BIGINT NOT NULL DEFAULT  0,\n"
            + "  `mapGroup` INT NOT NULL DEFAULT 0,\n"
            + "  `mapPriority` INT NOT NULL DEFAULT 0,\n"
            + "  `mapRule` TEXT NOT NULL,\n"
            + "  `mapAdvice` TEXT NOT NULL,\n"
            + "  `mapTarget` VARCHAR (200) NOT NULL DEFAULT '',\n"
            + "  `correlationId` BIGINT NOT NULL DEFAULT  0,\n"
            + "  `mapCategoryId` BIGINT NOT NULL DEFAULT  0,\n"
            + "  PRIMARY KEY (`id`,`effectiveTime`))\n"
            + "  ENGINE=MyISAM DEFAULT CHARSET=utf8mb4;";

    private static final String CREATE_LANGUAGE_REFSET_TABLE =
            "DROP TABLE IF EXISTS `full_refset_Language`;\n"
            + "CREATE TABLE `full_refset_Language` (\n"
            + "  `id` char(36) NOT NULL DEFAULT '',\n"
            + "  `effectiveTime` DATETIME NOT NULL DEFAULT  '2000-01-31 00:00:00',\n"
            + "  `active` TINYINT NOT NULL DEFAULT  0,\n"
            + "  `moduleId` BIGINT NOT NULL DEFAULT  0,\n"
            + "  `refsetId` BIGINT NOT NULL DEFAULT  0,\n"
            + "  `referencedComponentId` BIGINT NOT NULL DEFAULT  0,\n"
            + "  `acceptabilityId` BIGINT NOT NULL DEFAULT  0,\n"
            + "  PRIMARY KEY (`id`,`effectiveTime`))\n"
            + "  ENGINE=MyISAM DEFAULT CHARSET=utf8mb4;";

    private static final String CREATE_MODULE_DEPENDENCY_REFSET_TABLE =
            "DROP TABLE IF EXISTS `full_refset_ModuleDependency`;\n"
            + "CREATE TABLE `full_refset_ModuleDependency` (\n"
            + "  `id` char(36) NOT NULL DEFAULT '',\n"
            + "  `effectiveTime` DATETIME NOT NULL DEFAULT  '2000-01-31 00:00:00',\n"
            + "  `active` TINYINT NOT NULL DEFAULT  0,\n"
            + "  `moduleId` BIGINT NOT NULL DEFAULT  0,\n"
            + "  `refsetId` BIGINT NOT NULL DEFAULT  0,\n"
            + "  `referencedComponentId` BIGINT NOT NULL DEFAULT  0,\n"
            + "  `sourceEffectiveTime` DATETIME NOT NULL DEFAULT  '2000-01-31 00:00:00',\n"
            + "  `targetEffectiveTime` DATETIME NOT NULL DEFAULT  '2000-01-31 00:00:00',\n"
            + "  PRIMARY KEY (`id`,`effectiveTime`))\n"
            + "  ENGINE=MyISAM DEFAULT CHARSET=utf8mb4;";

    private static final String[] INDEX_SQL = {
            "CREATE INDEX idx_fd_languageCode ON full_description(languageCode);",
            "CREATE INDEX idx_fd_conceptId ON full_description(conceptId);",
            "CREATE INDEX idx_fc_id ON full_concept(id);",
            "CREATE INDEX idx_fr_referencedComponentId ON full_refset_Language(referencedComponentId);",
            "CREATE INDEX idx_fd_concept_lang_term_eff ON full_description (conceptId, languageCode, term (200), effectiveTime);",
            "CREATE INDEX idx_fc_id_eff ON full_concept (id, effectiveTime);",
            "CREATE INDEX idx_fd_active_lang ON full_description (active, languageCode, conceptId);",
            "CREATE INDEX idx_frl_refsetId ON full_refset_Language(refsetId);",
            "CREATE INDEX idx_frl_refset_component_eff ON full_refset_Language(refsetId, referencedComponentId, effectiveTime);",
            "CREATE INDEX idx_fd_id ON full_description(id);"
    };

    private SctDatabaseCreator() {
    }

    /**
     * Creates the database, tables, imports all release data and adds indexes.
     *
     * @param config   the database creation configuration
     * @param listener progress listener, may be null
     * @throws Exception if any step fails
     */
    public static void create(DbCreateConfig config, ProgressListener listener)
            throws Exception {
        report(listener, "dbcreate.step.connect");
        try (Connection serverConn = DriverManager.getConnection(
                config.getServerUrl(), config.getDbUser(), config.getDbPassword())) {

            report(listener, "dbcreate.step.create_database", config.getDbName());
            try (Statement stmt = serverConn.createStatement()) {
                stmt.executeUpdate(String.format(CREATE_DATABASE_SQL,
                        config.getDbName(), config.getDbName(), config.getDbName()));
            }
            logger.info("Database '{}' created", config.getDbName());

            try (Connection dbConn = DriverManager.getConnection(
                    config.getDatabaseUrl(), config.getDbUser(), config.getDbPassword())) {
                createTables(dbConn, listener);

                // Scan release folders for the requested release type
                SctRelease intl = SctReleaseFileScanner.scan(
                        config.getIntlReleasePath(), config.getReleaseType());
                logger.info("International release detected: module={} date={}",
                        intl.getModuleId(), intl.getReleaseDate());

                importInternational(dbConn, intl, config, listener);

                if (config.getExtensionReleasePath() != null
                        && !config.getExtensionReleasePath().isEmpty()) {
                    SctRelease ext = SctReleaseFileScanner.scan(
                            config.getExtensionReleasePath(), config.getReleaseType());
                    logger.info("Extension release detected: module={} date={}",
                            ext.getModuleId(), ext.getReleaseDate());
                    importExtension(dbConn, ext, config, listener);
                }

                addIndexes(dbConn, listener);
            }
        }
        report(listener, "dbcreate.step.done", config.getDbName());
    }

    private static void createTables(Connection dbConn, ProgressListener listener)
            throws SQLException {
        String[] tables = {
                CREATE_CONCEPT_TABLE, CREATE_DESCRIPTION_TABLE, CREATE_RELATIONSHIP_TABLE,
                CREATE_SIMPLE_REFSET_TABLE, CREATE_EXTENDED_MAP_REFSET_TABLE,
                CREATE_LANGUAGE_REFSET_TABLE, CREATE_MODULE_DEPENDENCY_REFSET_TABLE
        };
        for (int i = 0; i < tables.length; i++) {
            report(listener, "dbcreate.step.table", String.valueOf(i + 1), String.valueOf(tables.length));
            try (Statement stmt = dbConn.createStatement()) {
                stmt.executeUpdate(tables[i]);
            }
        }
        logger.info("All 7 tables created");
    }

    private static void importInternational(Connection dbConn, SctRelease intl,
                                            DbCreateConfig config, ProgressListener listener)
            throws SQLException {
        String date = config.getIntlReleaseDate() != null ? config.getIntlReleaseDate()
                : intl.getReleaseDate();

        // Concepts
        requireFile(intl.getConceptFile(), "concept", "International Edition");
        load(dbConn, listener, intl.getConceptFile(), "full_concept",
                "`id`,`effectiveTime`,`active`,`moduleId`,`definitionStatusId`",
                "dbcreate.import.concept", "International", date);

        // English descriptions
        File descEn = intl.getDescriptionFiles().get("en");
        requireFile(descEn, "English description", "International Edition");
        load(dbConn, listener, descEn, "full_description",
                "`id`,`effectiveTime`,`active`,`moduleId`,`conceptId`,`languageCode`,`typeId`,`term`,`caseSignificanceId`",
                "dbcreate.import.description", "en (International)", date);

        // Relationships
        requireFile(intl.getRelationshipFile(), "relationship", "International Edition");
        load(dbConn, listener, intl.getRelationshipFile(), "full_relationship",
                "`id`,`effectiveTime`,`active`,`moduleId`,`sourceId`,`destinationId`,`relationshipGroup`,`typeId`,`characteristicTypeId`,`modifierId`",
                "dbcreate.import.relationship", "International", date);

        // English language refset
        File refsetEn = intl.getLanguageRefsetFiles().get("en");
        requireFile(refsetEn, "English language refset", "International Edition");
        load(dbConn, listener, refsetEn, "full_refset_Language",
                "`id`,`effectiveTime`,`active`,`moduleId`,`refsetId`,`referencedComponentId`,`acceptabilityId`",
                "dbcreate.import.langrefset", "en (International)", date);
    }

    private static void importExtension(Connection dbConn, SctRelease ext,
                                        DbCreateConfig config, ProgressListener listener)
            throws SQLException {
        String module = config.getExtensionModuleId() != null ? config.getExtensionModuleId()
                : ext.getModuleId();

        requireFile(ext.getConceptFile(), "concept", "Extension");
        load(dbConn, listener, ext.getConceptFile(), "full_concept",
                "`id`,`effectiveTime`,`active`,`moduleId`,`definitionStatusId`",
                "dbcreate.import.concept", "Extension", module);

        for (Map.Entry<String, File> e : ext.getDescriptionFiles().entrySet()) {
            load(dbConn, listener, e.getValue(), "full_description",
                    "`id`,`effectiveTime`,`active`,`moduleId`,`conceptId`,`languageCode`,`typeId`,`term`,`caseSignificanceId`",
                    "dbcreate.import.description", e.getKey() + " (Extension)", module);
        }

        requireFile(ext.getRelationshipFile(), "relationship", "Extension");
        load(dbConn, listener, ext.getRelationshipFile(), "full_relationship",
                "`id`,`effectiveTime`,`active`,`moduleId`,`sourceId`,`destinationId`,`relationshipGroup`,`typeId`,`characteristicTypeId`,`modifierId`",
                "dbcreate.import.relationship", "Extension", module);

        for (Map.Entry<String, File> e : ext.getLanguageRefsetFiles().entrySet()) {
            load(dbConn, listener, e.getValue(), "full_refset_Language",
                    "`id`,`effectiveTime`,`active`,`moduleId`,`refsetId`,`referencedComponentId`,`acceptabilityId`",
                    "dbcreate.import.langrefset", e.getKey() + " (Extension)", module);
        }
    }

    private static void load(Connection dbConn, ProgressListener listener, File file,
                             String table, String columns, String progressKey,
                             String edition, String releaseId) throws SQLException {
        String path = file.getAbsolutePath().replace('\\', '/');
        report(listener, progressKey, file.getName(), table);
        logger.info("Importing {} into {} from {}", file.getName(), table, path);
        String sql = "LOAD DATA LOCAL INFILE '" + path + "'\n"
                + "INTO TABLE `" + table + "`\n"
                + "LINES TERMINATED BY '\\r\\n'\n"
                + " IGNORE 1 LINES\n"
                + "(" + columns + ");";
        try (Statement stmt = dbConn.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    private static void addIndexes(Connection dbConn, ProgressListener listener)
            throws SQLException {
        report(listener, "dbcreate.step.indexes");
        try (Statement stmt = dbConn.createStatement()) {
            for (String idx : INDEX_SQL) {
                stmt.executeUpdate(idx);
            }
        }
        logger.info("All 10 indexes created");
    }

    private static void requireFile(File file, String what, String edition) {
        if (file == null || !file.isFile()) {
            throw new IllegalArgumentException(
                    "Missing " + what + " file for " + edition + ". Run a scan first.");
        }
    }

    private static void report(ProgressListener listener, String key, Object... args) {
        if (listener != null) {
            listener.onProgress(key, args);
        }
    }
}
