package ch.ehealth.levi.core.db;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scans SNOMED CT RF2 release folders and detects the importable files
 * (concept, description per language, relationship, language refsets).
 *
 * <p>Release folders follow the official RF2 structure, e.g.:
 * <pre>
 *   &lt;root&gt;/Full/Terminology/sct2_Concept_Full_INT_20260601.txt
 *   &lt;root&gt;/Full/Terminology/sct2_Description_Full-de-ch_CH1000195_20260607.txt
 *   &lt;root&gt;/Full/Refset/Language/der2_cRefset_LanguageFull-en_INT_20260601.txt
 * </pre>
 */
public class SctReleaseFileScanner {

    /** Detected files for one release. */
    public static class SctRelease {
        private final String rootPath;
        private final DbCreateConfig.ReleaseType releaseType;
        private String moduleId;
        private String releaseDate;
        private File conceptFile;
        private File relationshipFile;
        private final Map<String, File> descriptionFiles = new LinkedHashMap<>();
        private final Map<String, File> languageRefsetFiles = new LinkedHashMap<>();

        SctRelease(String rootPath, DbCreateConfig.ReleaseType releaseType) {
            this.rootPath = rootPath;
            this.releaseType = releaseType;
        }

        public String getRootPath() {
            return rootPath;
        }

        public DbCreateConfig.ReleaseType getReleaseType() {
            return releaseType;
        }

        public String getModuleId() {
            return moduleId;
        }

        public String getReleaseDate() {
            return releaseDate;
        }

        public File getConceptFile() {
            return conceptFile;
        }

        public File getRelationshipFile() {
            return relationshipFile;
        }

        public Map<String, File> getDescriptionFiles() {
            return descriptionFiles;
        }

        public Map<String, File> getLanguageRefsetFiles() {
            return languageRefsetFiles;
        }

        public List<String> getLanguageCodes() {
            return new ArrayList<>(descriptionFiles.keySet());
        }
    }

    // sct2_Concept_Full_INT_20260601.txt
    private static final Pattern CONCEPT_PATTERN =
            Pattern.compile("sct2_Concept_" + typePrefix() + "_(.+?)_(\\d{8})\\.txt$");
    // sct2_Description_Full-de-ch_CH1000195_20260607.txt
    private static final Pattern DESCRIPTION_PATTERN =
            Pattern.compile("sct2_Description_" + typePrefix() + "-([a-z]{2}(?:-[a-z]{2})?)_(.+?)_(\\d{8})\\.txt$");
    // sct2_Relationship_Full_INT_20260601.txt
    private static final Pattern RELATIONSHIP_PATTERN =
            Pattern.compile("sct2_Relationship_" + typePrefix() + "_(.+?)_(\\d{8})\\.txt$");
    // der2_cRefset_LanguageFull-en_INT_20260601.txt
    private static final Pattern LANGUAGE_REFSET_PATTERN =
            Pattern.compile("der2_cRefset_Language" + typePrefix() + "-([a-z]{2}(?:-[a-z]{2})?)_(.+?)_(\\d{8})\\.txt$");

    private static String typePrefix() {
        return "(Full|Snapshot)";
    }

    private SctReleaseFileScanner() {
    }

    /**
     * Scans a release root folder, auto-detecting the release type (Full or
     * Snapshot) from the folder structure.
     *
     * @param releaseRootPath path to the release root (containing a Full/ or Snapshot/ subfolder)
     * @return detected release files
     * @throws IllegalArgumentException if the folder does not exist or no importable files are found
     */
    public static SctRelease scan(String releaseRootPath) {
        return scan(releaseRootPath, null);
    }

    /**
     * Scans a release root folder for the given release type.
     *
     * @param releaseRootPath path to the release root
     * @param type            expected release type, or null to auto-detect
     * @return detected release files
     * @throws IllegalArgumentException if the folder does not exist or no importable files are found
     */
    public static SctRelease scan(String releaseRootPath, DbCreateConfig.ReleaseType type) {
        File root = new File(releaseRootPath);
        if (!root.isDirectory()) {
            throw new IllegalArgumentException(
                    "Release folder does not exist: " + releaseRootPath);
        }

        DbCreateConfig.ReleaseType resolvedType = type;
        File typeDir;
        if (resolvedType == null) {
            typeDir = null;
            for (DbCreateConfig.ReleaseType candidate : DbCreateConfig.ReleaseType.values()) {
                File dir = new File(root, candidate.getFolderName());
                if (dir.isDirectory()) {
                    resolvedType = candidate;
                    typeDir = dir;
                    break;
                }
            }
            if (typeDir == null) {
                throw new IllegalArgumentException(
                        "No 'Full' or 'Snapshot' folder found in: " + releaseRootPath);
            }
        } else {
            typeDir = new File(root, type.getFolderName());
            if (!typeDir.isDirectory()) {
                throw new IllegalArgumentException(
                        "Release type folder '" + type.getFolderName()
                                + "' not found in: " + releaseRootPath);
            }
        }

        File terminologyDir = new File(typeDir, "Terminology");
        File languageRefsetDir = new File(typeDir, "Refset/Language");

        SctRelease release = new SctRelease(releaseRootPath, resolvedType);
        boolean foundAnything = false;

        if (terminologyDir.isDirectory()) {
            for (File f : listFiles(terminologyDir)) {
                String name = f.getName();
                Matcher m = CONCEPT_PATTERN.matcher(name);
                if (m.matches() && release.conceptFile == null) {
                    release.conceptFile = f;
                    foundAnything = true;
                    continue;
                }
                m = DESCRIPTION_PATTERN.matcher(name);
                if (m.matches()) {
                    String lang = m.group(2);
                    release.descriptionFiles.putIfAbsent(lang, f);
                    foundAnything = true;
                    continue;
                }
                m = RELATIONSHIP_PATTERN.matcher(name);
                if (m.matches() && release.relationshipFile == null) {
                    release.relationshipFile = f;
                    foundAnything = true;
                }
            }
        }

        if (languageRefsetDir.isDirectory()) {
            for (File f : listFiles(languageRefsetDir)) {
                Matcher m = LANGUAGE_REFSET_PATTERN.matcher(f.getName());
                if (m.matches()) {
                    String lang = m.group(2);
                    release.languageRefsetFiles.putIfAbsent(lang, f);
                    foundAnything = true;
                }
            }
        }

        if (!foundAnything) {
            throw new IllegalArgumentException(
                    "No SNOMED CT release files found in: " + releaseRootPath);
        }

        // Derive module id and release date from the concept file (or any matched file).
        File anchor = release.conceptFile != null ? release.conceptFile
                : release.relationshipFile != null ? release.relationshipFile
                : release.descriptionFiles.isEmpty() ? null : release.descriptionFiles.values().iterator().next();
        if (anchor != null) {
            String name = anchor.getName();
            Matcher m = CONCEPT_PATTERN.matcher(name);
            if (m.matches()) {
                // type, module, date
                release.moduleId = m.group(2);
                release.releaseDate = m.group(3);
            } else {
                m = RELATIONSHIP_PATTERN.matcher(name);
                if (m.matches()) {
                    release.moduleId = m.group(2);
                    release.releaseDate = m.group(3);
                } else {
                    m = DESCRIPTION_PATTERN.matcher(name);
                    if (m.matches()) {
                        // type, language, module, date
                        release.moduleId = m.group(3);
                        release.releaseDate = m.group(4);
                    }
                }
            }
        }

        return release;
    }

    private static File[] listFiles(File dir) {
        File[] files = dir.listFiles();
        return files != null ? files : new File[0];
    }

    /**
     * Suggests a database name from the country code and the international
     * release date (yyyyMMdd), mirroring the SNOMED_Database naming convention,
     * e.g. ("CH", "20260601") -&gt; "SCT:CH_Jun26".
     */
    public static String suggestDbName(String countryCode, String releaseDate) {
        return DbVariant.PRODUCTION.suggestDbName(countryCode, releaseDate);
    }

    /**
     * Suggests a database name for the given database variant, e.g.
     * ("CH", "20260801", BETA) -&gt; "SCT:CH_Beta_Aug26".
     */
    public static String suggestDbName(String countryCode, String releaseDate, DbVariant variant) {
        DbVariant v = variant == null ? DbVariant.PRODUCTION : variant;
        return v.suggestDbName(countryCode, releaseDate);
    }

    /**
     * Detects the database variant from an extension release path or module
     * identifier, e.g. "..._PRODUCTION_...", "..._DAILYBUILD_BETA_...",
     * "..._PREPRODUCTION_..." or a module starting with "AT". Returns null when
     * nothing recognizable is found.
     */
    public static DbVariant detectVariant(String pathOrModule) {
        return DbVariant.detectVariant(pathOrModule);
    }

    /**
     * Detects the country code from an extension module identifier, e.g.
     * "CH1000195" -&gt; "CH", "AT1000234" -&gt; "AT". Falls back to "CH" when the
     * prefix is not a known country code.
     */
    public static String detectCountry(String moduleId) {
        if (moduleId == null || moduleId.isEmpty()) {
            return "CH";
        }
        String upper = moduleId.trim().toUpperCase();
        for (String cc : new String[]{"CH", "AT", "DE", "FR", "IT", "GB", "US", "AU", "BE", "NL"}) {
            if (upper.startsWith(cc)) {
                return cc;
            }
        }
        return "CH";
    }
}
