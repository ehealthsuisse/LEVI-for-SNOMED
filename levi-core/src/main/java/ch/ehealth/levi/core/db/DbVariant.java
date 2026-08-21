package ch.ehealth.levi.core.db;

/**
 * The variant of the SNOMED CT database to create, mirroring the standalone
 * SNOMED_Database project scenarios:
 *
 * <ul>
 *   <li>{@link #PRODUCTION} – CH production (e.g. {@code SCT:CH_Jun26})</li>
 *   <li>{@link #BETA} – CH beta / daily build (e.g. {@code SCT:CH_Beta_Aug26})</li>
 *   <li>{@link #PRE_PRODUCTION} – CH pre-production (e.g. {@code SCT:CH_PreProdJun26})</li>
 *   <li>{@link #AT} – Austrian extension (e.g. {@code SCT:AT_Mar25})</li>
 * </ul>
 */
public enum DbVariant {

    PRODUCTION("CH"),
    BETA("CH"),
    PRE_PRODUCTION("CH"),
    AT("AT");

    private static final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    private final String defaultCountryCode;

    DbVariant(String defaultCountryCode) {
        this.defaultCountryCode = defaultCountryCode;
    }

    /** Default country code for this variant ("CH" except {@link #AT} which is "AT"). */
    public String getDefaultCountryCode() {
        return defaultCountryCode;
    }

    /**
     * Suggests a database name for this variant from the country code and the
     * international release date (yyyyMMdd), following the SNOMED_Database
     * naming convention with normalized 3-letter months, e.g.
     * ("CH", "20260601") &rarr; "SCT:CH_Jun26", ("CH", "20260801", BETA) &rarr;
     * "SCT:CH_Beta_Aug26", ("CH", "20260601", PRE_PRODUCTION) &rarr;
     * "SCT:CH_PreProdJun26", ("AT", "20250315", AT) &rarr; "SCT:AT_Mar25".
     */
    public String suggestDbName(String countryCode, String releaseDate) {
        String monthYear = "";
        if (releaseDate != null && releaseDate.length() >= 8) {
            int month = Integer.parseInt(releaseDate.substring(4, 6));
            int year = Integer.parseInt(releaseDate.substring(0, 4)) % 100;
            monthYear = MONTHS[month - 1] + String.format("%02d", year);
        }
        String cc = countryCode == null || countryCode.trim().isEmpty()
                ? defaultCountryCode : countryCode.trim().toUpperCase();
        switch (this) {
            case BETA:
                return "SCT:" + cc + "_Beta_" + monthYear;
            case PRE_PRODUCTION:
                return "SCT:" + cc + "_PreProd" + monthYear;
            default:
                return "SCT:" + cc + "_" + monthYear;
        }
    }

    /**
     * Detects the database variant from an extension release path or module
     * identifier, e.g. "..._PRODUCTION_...", "..._DAILYBUILD_BETA_...",
     * "..._PREPRODUCTION_..." or a module starting with "AT". Returns null when
     * nothing recognizable is found.
     */
    public static DbVariant detectVariant(String pathOrModule) {
        if (pathOrModule == null) {
            return null;
        }
        String upper = pathOrModule.toUpperCase();
        if (upper.contains("PREPRODUCTION") || upper.contains("PRE_PRODUCTION")) {
            return PRE_PRODUCTION;
        }
        if (upper.contains("BETA") || upper.contains("DAILYBUILD")) {
            return BETA;
        }
        if (upper.startsWith("AT") || upper.replace('\\', '/').contains("/AT")) {
            return AT;
        }
        if (upper.contains("PRODUCTION")) {
            return PRODUCTION;
        }
        return null;
    }
}