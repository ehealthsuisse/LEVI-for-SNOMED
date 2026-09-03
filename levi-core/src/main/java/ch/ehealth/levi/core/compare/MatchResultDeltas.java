package ch.ehealth.levi.core.compare;

import java.util.ArrayList;
import java.util.List;

import ch.ehealth.levi.core.export.ResultCollector;

/**
 * Shared builder that converts {@link MatchResult}s into the LEVI delta
 * {@link List}&lt;{@link List}&lt;{@link String}&gt;&gt; row shape used by both
 * the flat {@link CsvExporter} files and the grouped/batched
 * {@link SnomedBatchExporter}. Centralising the row layout here removes the
 * previous duplication between those two exporters.
 */
public final class MatchResultDeltas {

    /** Number of empty language-refset/acceptability placeholder pairs appended by delta row builders. */
    private static final int PLACEHOLDER_PAIRS = 4;

    private MatchResultDeltas() {
        // utility class
    }

    /**
     * Builds the additions delta (rows for MISSING_IN_CH, TERM_ON_DIFFERENT_CONCEPT
     * and MISSING_IN_FR) including the header row at index 0.
     */
    public static List<List<String>> buildAdditions(List<MatchResult> results) {
        List<List<String>> delta = new ArrayList<>();
        delta.add(new ArrayList<>(DeltaColumns.ADDITIONS));
        for (MatchResult r : results) {
            delta.add(additionRow(r));
        }
        return delta;
    }

    /**
     * Builds the changes delta (rows for DIFFERENT_ACCEPTABILITY) including the
     * header row. Registers each description→concept mapping on
     * {@code resultCollector} so the batch exporter can resolve concept IDs.
     */
    public static List<List<String>> buildChanges(List<MatchResult> results) {
        return buildChanges(results, null);
    }

    public static List<List<String>> buildChanges(List<MatchResult> results, ResultCollector resultCollector) {
        List<List<String>> delta = new ArrayList<>();
        delta.add(new ArrayList<>(DeltaColumns.CHANGES));
        for (MatchResult r : results) {
            delta.add(changeRow(r));
            registerMapping(r, resultCollector);
        }
        return delta;
    }

    /**
     * Builds the inactivations delta (rows for ACTIVE_CH_INACTIVE_FR and
     * ACTIVE_CH_MISSING_IN_FR) including the header row.
     */
    public static List<List<String>> buildInactivations(List<MatchResult> results) {
        List<List<String>> delta = new ArrayList<>();
        delta.add(new ArrayList<>(DeltaColumns.INACTIVATIONS));
        for (MatchResult r : results) {
            delta.add(inactivationRow(r));
        }
        return delta;
    }

    /**
     * Builds the reactivations delta (rows for ACTIVE_FR_INACTIVE_CH) including
     * the header row. Registers each description→concept mapping on
     * {@code resultCollector} so the batch exporter can resolve concept IDs.
     */
    public static List<List<String>> buildReactivations(List<MatchResult> results) {
        return buildReactivations(results, null);
    }

    public static List<List<String>> buildReactivations(List<MatchResult> results, ResultCollector resultCollector) {
        List<List<String>> delta = new ArrayList<>();
        delta.add(new ArrayList<>(DeltaColumns.CHANGES));
        for (MatchResult r : results) {
            delta.add(reactivationRow(r));
            registerMapping(r, resultCollector);
        }
        return delta;
    }

    private static List<String> additionRow(MatchResult r) {
        List<String> row = new ArrayList<>();
        row.add(r.conceptId_FR() != null ? r.conceptId_FR() : safe(r.conceptId_CH()));
        row.add(safe(r.fsnTerm()));
        row.add(safe(r.preferredTerm()));
        row.add(safe(r.term()));
        row.add(safe(r.languageCode()));
        row.add(safe(r.caseSignificance()));
        row.add(safe(r.typeId()));
        row.add(safe(r.refsetId()));
        row.add(safe(r.acceptability_FR()));
        addPlaceholders(row);
        row.add(safe(r.note()));
        return row;
    }

    private static List<String> changeRow(MatchResult r) {
        List<String> row = new ArrayList<>();
        row.add(safe(r.descriptionId_CH()));
        row.add(safe(r.preferredTerm()));
        row.add(safe(r.term()));
        row.add(safe(r.caseSignificance()));
        row.add(safe(r.typeId()));
        row.add(safe(r.refsetId()));
        row.add(safe(r.acceptability_FR()));
        addPlaceholders(row);
        row.add(safe(r.note()));
        return row;
    }

    private static List<String> inactivationRow(MatchResult r) {
        List<String> row = new ArrayList<>();
        row.add(safe(r.descriptionId_CH()));
        row.add(safe(r.languageCode()));
        row.add(safe(r.conceptId_CH()));
        row.add("");
        row.add(safe(r.term()));
        row.add("");
        row.add("");
        row.add("");
        row.add("");
        row.add("");
        row.add(safe(r.note()));
        return row;
    }

    private static List<String> reactivationRow(MatchResult r) {
        List<String> row = new ArrayList<>();
        row.add(safe(r.descriptionId_CH()));
        row.add(safe(r.preferredTerm()));
        row.add(safe(r.term()));
        row.add(safe(r.caseSignificance()));
        row.add(safe(r.typeId()));
        row.add(safe(r.refsetId()));
        row.add(safe(r.acceptability_CH()));
        addPlaceholders(row);
        row.add("translation reactivated");
        return row;
    }

    /** Appends the shared empty language-refset/acceptability placeholder pairs. */
    private static void addPlaceholders(List<String> row) {
        for (int i = 0; i < PLACEHOLDER_PAIRS; i++) {
            row.add("");
            row.add("");
        }
    }

    private static void registerMapping(MatchResult r, ResultCollector resultCollector) {
        if (resultCollector == null) return;
        if (r.descriptionId_CH() != null && !r.descriptionId_CH().isEmpty()
                && r.conceptId_CH() != null && !r.conceptId_CH().isEmpty()) {
            resultCollector.addDescriptionToConceptMapping(r.descriptionId_CH(), r.conceptId_CH());
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
