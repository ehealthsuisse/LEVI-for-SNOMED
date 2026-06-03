package ch.ehealth.levi.core.export;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ehealth.levi.core.compare.ChangeType;
import ch.ehealth.levi.core.io.FileWriterUtil;

/**
 * Groups SNOMED release concepts by their combination of change types and
 * writes aligned, optionally batch-split output files.
 *
 * <h3>Grouping logic</h3>
 * <p>Each concept is assigned to exactly one of 15 groups (G1–G15) based on
 * which {@link ChangeType}s apply to it.  The mapping is:</p>
 * <pre>
 *  G1  – CHANGE only
 *  G2  – ADDITION only
 *  G3  – INACTIVATION only
 *  G4  – REACTIVATION only
 *  G5  – CHANGE + ADDITION
 *  G6  – CHANGE + INACTIVATION+
 *  G7  – CHANGE + REACTIVATION
 *  G8  – ADDITION + INACTIVATION
 *  G9  – ADDITION + REACTIVATION
 *  G10 – INACTIVATION + REACTIVATION
 *  G11 – CHANGE + ADDITION + INACTIVATION
 *  G12 – CHANGE + ADDITION + REACTIVATION
 *  G13 – CHANGE + INACTIVATION + REACTIVATION
 *  G14 – ADDITION + INACTIVATION + REACTIVATION
 *  G15 – CHANGE + ADDITION + INACTIVATION + REACTIVATION
 * </pre>
 *
 * <h3>Aligned batch splitting</h3>
 * <p>If a group contains more than {@link #batchSize} distinct concept IDs the
 * list is split into chunks.  All files within the same group and batch always
 * contain the <em>identical</em> set of concept IDs, guaranteeing alignment.</p>
 *
 * <h3>File naming</h3>
 * <ul>
 *   <li>No split: {@code G5_changes.tsv}, {@code G5_additions.tsv}</li>
 *   <li>Split:    {@code G5_changes_batch1.tsv}, {@code G5_additions_batch1.tsv}, …</li>
 * </ul>
 */
public class BatchExportService {

    private static final Logger logger = LoggerFactory.getLogger(BatchExportService.class);

    /** Default maximum number of distinct concept IDs per output batch. */
    public static final int DEFAULT_BATCH_SIZE = 1000;

    /**
     * Column index of the concept ID in the <em>additions</em> delta list
     * (after header row).
     */
    private static final int ADDITION_CONCEPT_ID_COL = 0;

    /**
     * Column index of the concept ID in the <em>inactivations</em> delta list
     * (after header row): description ID (0), language code (1), concept ID (2).
     */
    private static final int INACTIVATION_CONCEPT_ID_COL = 2;

    /**
     * Ordered mapping from change-type combination to group name (G1 … G15).
     * Iteration order defines the canonical processing sequence.
     */
    private static final Map<EnumSet<ChangeType>, String> GROUPS;

    static {
        GROUPS = new LinkedHashMap<>();
        // Single-type groups
        GROUPS.put(EnumSet.of(ChangeType.CHANGE),                                                                                "G1");
        GROUPS.put(EnumSet.of(ChangeType.ADDITION),                                                                              "G2");
        GROUPS.put(EnumSet.of(ChangeType.INACTIVATION),                                                                          "G3");
        GROUPS.put(EnumSet.of(ChangeType.REACTIVATION),                                                                          "G4");
        // Two-type groups
        GROUPS.put(EnumSet.of(ChangeType.CHANGE,      ChangeType.ADDITION),                                                      "G5");
        GROUPS.put(EnumSet.of(ChangeType.CHANGE,      ChangeType.INACTIVATION),                                                  "G6");
        GROUPS.put(EnumSet.of(ChangeType.CHANGE,      ChangeType.REACTIVATION),                                                  "G7");
        GROUPS.put(EnumSet.of(ChangeType.ADDITION,    ChangeType.INACTIVATION),                                                  "G8");
        GROUPS.put(EnumSet.of(ChangeType.ADDITION,    ChangeType.REACTIVATION),                                                  "G9");
        GROUPS.put(EnumSet.of(ChangeType.INACTIVATION, ChangeType.REACTIVATION),                                                 "G10");
        // Three-type groups
        GROUPS.put(EnumSet.of(ChangeType.CHANGE,      ChangeType.ADDITION,    ChangeType.INACTIVATION),                         "G11");
        GROUPS.put(EnumSet.of(ChangeType.CHANGE,      ChangeType.ADDITION,    ChangeType.REACTIVATION),                         "G12");
        GROUPS.put(EnumSet.of(ChangeType.CHANGE,      ChangeType.INACTIVATION, ChangeType.REACTIVATION),                        "G13");
        GROUPS.put(EnumSet.of(ChangeType.ADDITION,    ChangeType.INACTIVATION, ChangeType.REACTIVATION),                        "G14");
        // Four-type group
        GROUPS.put(EnumSet.of(ChangeType.CHANGE, ChangeType.ADDITION, ChangeType.INACTIVATION, ChangeType.REACTIVATION),        "G15");
    }

    private final int batchSize;
    private final FileWriterUtil writer;

    public BatchExportService() {
        this(DEFAULT_BATCH_SIZE, new FileWriterUtil());
    }

    public BatchExportService(int batchSize) {
        this(batchSize, new FileWriterUtil());
    }

    /**
     * @param batchSize maximum number of concept IDs per batch (must be &gt;= 1)
     * @param writer    file writer to use for output
     */
    public BatchExportService(int batchSize, FileWriterUtil writer) {
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must be >= 1, got: " + batchSize);
        }
        this.batchSize = batchSize;
        this.writer    = writer;
    }

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Performs the full group-based aligned batch export.
     *
     * <p>Column conventions for the delta lists (each list has a header row at
     * index 0 that is included in every output file):</p>
     * <ul>
     *   <li>{@code additionsDelta}    – concept ID at column 0</li>
     *   <li>{@code changesDelta}      – description ID at column 0 (concept ID
     *       resolved via {@code resultCollector})</li>
     *   <li>{@code inactivationsDelta} – concept ID at column 2</li>
     *   <li>{@code reactivationsDelta} – description ID at column 0 (concept ID
     *       resolved via {@code resultCollector})</li>
     * </ul>
     *
     * <p>Passing {@code null} for a delta list is equivalent to passing an
     * empty list; no output file will be written for that change type.</p>
     *
     * @param additionsDelta     delta rows for additions (may be {@code null})
     * @param changesDelta       delta rows for changes   (may be {@code null})
     * @param inactivationsDelta delta rows for inactivations (may be {@code null})
     * @param reactivationsDelta delta rows for reactivations (may be {@code null})
     * @param resultCollector    used to resolve description ID → concept ID for
     *                           changes and reactivations
     * @param destination        output directory path
     * @throws IOException if any output file cannot be written
     */
    public void export(
            List<List<String>> additionsDelta,
            List<List<String>> changesDelta,
            List<List<String>> inactivationsDelta,
            List<List<String>> reactivationsDelta,
            ResultCollector resultCollector,
            String destination) throws IOException {

        // Step 1 – build concept matrix
        Map<String, EnumSet<ChangeType>> matrix = buildConceptMatrix(
                additionsDelta, changesDelta, inactivationsDelta, reactivationsDelta,
                resultCollector);
        logger.info("Concept matrix built: {} distinct concept IDs.", matrix.size());

        // Step 2 – group concepts by their change-type combination
        Map<EnumSet<ChangeType>, List<String>> groups = groupConcepts(matrix);
        logger.info("{} non-empty concept groups identified.", groups.size());

        // Steps 3 + 4 – for each group write aligned, optionally split, files
        for (Map.Entry<EnumSet<ChangeType>, List<String>> entry : groups.entrySet()) {
            EnumSet<ChangeType> combination = entry.getKey();
            List<String>        conceptIds  = entry.getValue();
            String              groupName   = GROUPS.get(combination);

            if (groupName == null) {
                logger.warn("Unmapped change-type combination {}; skipping {} concept(s).",
                        combination, conceptIds.size());
                continue;
            }

            writeGroupFiles(groupName, combination, conceptIds,
                    additionsDelta, changesDelta, inactivationsDelta, reactivationsDelta,
                    resultCollector, destination);
        }
    }

    // =========================================================================
    // Step 1 – build concept matrix
    // =========================================================================

    /**
     * Scans all delta lists and builds a {@code Map<conceptId, EnumSet<ChangeType>>}.
     */
    private Map<String, EnumSet<ChangeType>> buildConceptMatrix(
            List<List<String>> additionsDelta,
            List<List<String>> changesDelta,
            List<List<String>> inactivationsDelta,
            List<List<String>> reactivationsDelta,
            ResultCollector rc) {

        // LinkedHashMap preserves insertion order → deterministic group ordering
        Map<String, EnumSet<ChangeType>> matrix = new LinkedHashMap<>();

        // ADDITION – concept ID at column 0
        if (additionsDelta != null) {
            for (int i = 1; i < additionsDelta.size(); i++) {
                String conceptId = safeGet(additionsDelta.get(i), ADDITION_CONCEPT_ID_COL);
                if (conceptId != null && !conceptId.isEmpty()) {
                    matrix.computeIfAbsent(conceptId, k -> EnumSet.noneOf(ChangeType.class))
                          .add(ChangeType.ADDITION);
                }
            }
        }

        // CHANGE – description ID at column 0; concept ID resolved via ResultCollector
        if (changesDelta != null) {
            for (int i = 1; i < changesDelta.size(); i++) {
                String descId    = safeGet(changesDelta.get(i), 0);
                String conceptId = (descId != null) ? rc.getConceptIdForDescription(descId) : null;
                if (conceptId != null && !conceptId.isEmpty()) {
                    matrix.computeIfAbsent(conceptId, k -> EnumSet.noneOf(ChangeType.class))
                          .add(ChangeType.CHANGE);
                } else {
                    logger.warn("No concept ID mapping for description ID '{}' in changes delta (row {}).",
                            descId, i);
                }
            }
        }

        // INACTIVATION – concept ID at column 2
        if (inactivationsDelta != null) {
            for (int i = 1; i < inactivationsDelta.size(); i++) {
                String conceptId = safeGet(inactivationsDelta.get(i), INACTIVATION_CONCEPT_ID_COL);
                if (conceptId != null && !conceptId.isEmpty()) {
                    matrix.computeIfAbsent(conceptId, k -> EnumSet.noneOf(ChangeType.class))
                          .add(ChangeType.INACTIVATION);
                }
            }
        }

        // REACTIVATION – description ID at column 0; concept ID resolved via ResultCollector
        if (reactivationsDelta != null) {
            for (int i = 1; i < reactivationsDelta.size(); i++) {
                String descId    = safeGet(reactivationsDelta.get(i), 0);
                String conceptId = (descId != null) ? rc.getConceptIdForDescription(descId) : null;
                if (conceptId != null && !conceptId.isEmpty()) {
                    matrix.computeIfAbsent(conceptId, k -> EnumSet.noneOf(ChangeType.class))
                          .add(ChangeType.REACTIVATION);
                } else {
                    logger.warn("No concept ID mapping for description ID '{}' in reactivations delta (row {}).",
                            descId, i);
                }
            }
        }

        return matrix;
    }

    // =========================================================================
    // Step 2 – group concepts
    // =========================================================================

    /**
     * Partitions the concept matrix into groups keyed by change-type combination.
     * Each concept ID appears in exactly one group.
     */
    private Map<EnumSet<ChangeType>, List<String>> groupConcepts(
            Map<String, EnumSet<ChangeType>> matrix) {

        Map<EnumSet<ChangeType>, List<String>> result = new LinkedHashMap<>();
        for (Map.Entry<String, EnumSet<ChangeType>> entry : matrix.entrySet()) {
            result.computeIfAbsent(entry.getValue(), k -> new ArrayList<>())
                  .add(entry.getKey());
        }
        return result;
    }

    // =========================================================================
    // Steps 3 + 4 – write group files with optional batch splitting
    // =========================================================================

    private void writeGroupFiles(
            String groupName,
            EnumSet<ChangeType> combination,
            List<String> groupConceptIds,
            List<List<String>> additionsDelta,
            List<List<String>> changesDelta,
            List<List<String>> inactivationsDelta,
            List<List<String>> reactivationsDelta,
            ResultCollector rc,
            String destination) throws IOException {

        // Split concept ID list into chunks (splitting always by concept ID, never by line)
        List<List<String>> batches  = splitIntoChunks(groupConceptIds, batchSize);
        boolean            useSuffix = batches.size() > 1;

        for (int batchIdx = 0; batchIdx < batches.size(); batchIdx++) {
            Set<String> batchConceptSet = new HashSet<>(batches.get(batchIdx));
            String      suffix          = useSuffix ? "_batch" + (batchIdx + 1) : "";

            for (ChangeType type : combination) {
                List<List<String>> delta = selectDelta(
                        type, additionsDelta, changesDelta, inactivationsDelta, reactivationsDelta);

                if (delta == null || delta.size() <= 1) {
                    // No data rows for this change type (acceptance criterion: no file written)
                    continue;
                }

                List<List<String>> filtered = filterByConceptId(delta, type, batchConceptSet, rc);
                if (filtered.size() <= 1) {
                    // Only header, no matching rows – skip
                    continue;
                }

                String fileName = destination + File.separator
                        + groupName + "_" + type.fileNameToken() + suffix + ".tsv";
                writer.writeToFile(fileName, filtered);
                logger.info("Group {}, batch {}/{}: wrote {} data rows to '{}'.",
                        groupName, batchIdx + 1, batches.size(), filtered.size() - 1, fileName);
            }
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Returns the subset of {@code delta} rows (plus the header) whose concept ID
     * is contained in {@code conceptIdSet}.
     */
    private List<List<String>> filterByConceptId(
            List<List<String>> delta,
            ChangeType type,
            Set<String> conceptIdSet,
            ResultCollector rc) {

        List<List<String>> result = new ArrayList<>();
        result.add(delta.get(0)); // always include header

        for (int i = 1; i < delta.size(); i++) {
            List<String> row       = delta.get(i);
            String       conceptId = extractConceptId(row, type, rc);
            if (conceptId != null && conceptIdSet.contains(conceptId)) {
                result.add(row);
            }
        }
        return result;
    }

    /**
     * Extracts the concept ID from a delta row according to the column conventions
     * for each {@link ChangeType}.
     */
    private String extractConceptId(List<String> row, ChangeType type, ResultCollector rc) {
        switch (type) {
            case ADDITION:
                return safeGet(row, ADDITION_CONCEPT_ID_COL);
            case INACTIVATION:
                return safeGet(row, INACTIVATION_CONCEPT_ID_COL);
            case CHANGE:
            case REACTIVATION: {
                String descId = safeGet(row, 0);
                return (descId != null) ? rc.getConceptIdForDescription(descId) : null;
            }
            default:
                throw new IllegalStateException("Unhandled ChangeType: " + type);
        }
    }

    /** Returns the delta list corresponding to the given {@link ChangeType}. */
    private List<List<String>> selectDelta(
            ChangeType type,
            List<List<String>> additionsDelta,
            List<List<String>> changesDelta,
            List<List<String>> inactivationsDelta,
            List<List<String>> reactivationsDelta) {
        switch (type) {
            case ADDITION:    return additionsDelta;
            case CHANGE:      return changesDelta;
            case INACTIVATION: return inactivationsDelta;
            case REACTIVATION: return reactivationsDelta;
            default:          throw new IllegalStateException("Unhandled ChangeType: " + type);
        }
    }

    /**
     * Splits {@code list} into consecutive sub-lists each of at most {@code size}
     * elements.  Returns a list with a single empty list when {@code list} is empty.
     */
    static <T> List<List<T>> splitIntoChunks(List<T> list, int size) {
        List<List<T>> chunks = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            chunks.add(new ArrayList<>(list.subList(i, Math.min(i + size, list.size()))));
        }
        if (chunks.isEmpty()) {
            chunks.add(new ArrayList<>());
        }
        return chunks;
    }

    /** Null-safe column accessor; returns {@code null} if index is out of bounds. */
    private static String safeGet(List<String> row, int index) {
        return (row != null && index < row.size()) ? row.get(index) : null;
    }
}
