package ch.ehealth.levi.core.compare;

/**
 * Represents the four possible change types a SNOMED concept can have
 * in a release delta.  A concept may carry more than one type simultaneously;
 * the combination determines its export group (G1 – G15).
 */
public enum ChangeType {

    CHANGE,
    ADDITION,
    INACTIVATION,
    REACTIVATION;

    /**
     * Returns the lower-case file-name token used in output file names.
     * Examples: {@code CHANGE} → {@code "changes"}, {@code ADDITION} → {@code "additions"}.
     */
    public String fileNameToken() {
        return name().toLowerCase() + "s";
    }
}
