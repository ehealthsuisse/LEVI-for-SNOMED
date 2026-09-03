package ch.ehealth.levi.core.compare;

import java.util.List;

/**
 * Single source of truth for the column layouts (headers) of the four LEVI
 * delta output files (additions, changes, inactivations, reactivations).
 *
 * <p>These headers were previously re-declared verbatim in several places
 * ({@link Comparator}, {@link CompareManager#runTranslationCheck},
 * {@link CsvExporter} and {@link SnomedBatchExporter}). Centralising them here
 * guarantees the layout stays consistent across the whole pipeline.</p>
 */
public final class DeltaColumns {

    private DeltaColumns() {
        // utility class
    }

    /** Column count of the base additions header (without the regex columns). */
    public static final int ADDITION_BASE_COLUMNS = 18;

    /** Additions header used when regex checking is disabled. */
    public static final List<String> ADDITIONS = List.of(
        "Concept ID", "GB/US FSN Term (For reference only)", "Preferred Term (For reference only)",
        "Translated Term", "Language Code", "Case significance", "TypeId",
        "Language reference set", "Acceptability",
        "Language reference set", "Acceptability",
        "Language reference set", "Acceptability",
        "Language reference set", "Acceptability",
        "Language reference set", "Acceptability",
        "Notes"
    );

    /** Additions header with the five extra regex-check columns appended. */
    public static final List<String> ADDITIONS_WITH_REGEX = List.of(
        "Concept ID", "GB/US FSN Term (For reference only)", "Preferred Term (For reference only)",
        "Translated Term", "Language Code", "Case significance", "TypeId",
        "Language reference set", "Acceptability",
        "Language reference set", "Acceptability",
        "Language reference set", "Acceptability",
        "Language reference set", "Acceptability",
        "Language reference set", "Acceptability",
        "Notes",
        "Quotes", "SoftHyphen", "SpaceAroundSlash", "Apostrophe", "Upper/lower case"
    );

    /** Changes header. */
    public static final List<String> CHANGES = List.of(
        "Description ID", "Preferred Term (For reference only)", "Term (For reference only)",
        "Case significance", "Type",
        "Language reference set", "Acceptability",
        "Language reference set", "Acceptability",
        "Language reference set", "Acceptability",
        "Language reference set", "Acceptability",
        "Language reference set", "Acceptability",
        "Notes"
    );

    /** Inactivations header. */
    public static final List<String> INACTIVATIONS = List.of(
        "Description ID", "Language Code", "Concept ID",
        "Preferred Term (For reference only)", "Term (For reference only)",
        "Inactivation Reason",
        "Association Target ID 1", "Association Target ID 2",
        "Association Target ID 3", "Association Target ID 4",
        "Notes"
    );

    /**
     * Returns the additions header appropriate for the given regex setting.
     *
     * @param regexEnabled whether the regex check columns should be included
     * @return the additions header list
     */
    public static List<String> additionsHeader(boolean regexEnabled) {
        return regexEnabled ? ADDITIONS_WITH_REGEX : ADDITIONS;
    }
}
