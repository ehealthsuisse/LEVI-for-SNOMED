package ch.ehealth.levi.core.compare;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import ch.ehealth.levi.core.export.BatchExportService;
import ch.ehealth.levi.core.export.ResultCollector;

/**
 * Groups SNOMED comparison results into the LEVI G1–G15 change-type groups with
 * optional batch splitting, reusing {@link BatchExportService}.
 *
 * <p>The MatchResult → delta-row conversion itself lives in
 * {@link MatchResultDeltas}, which is shared with the flat-file
 * {@link CsvExporter} so the two exporters cannot drift apart.</p>
 */
public class SnomedBatchExporter {

    private final BatchExportService batchExportService;

    public SnomedBatchExporter() {
        this(BatchExportService.DEFAULT_BATCH_SIZE);
    }

    public SnomedBatchExporter(int batchSize) {
        this.batchExportService = new BatchExportService(batchSize);
    }

    public void exportGrouped(
            List<MatchResult> results,
            ResultCollector resultCollector,
            String destination) throws IOException {

        List<MatchResult> additions = filter(results, MatchStatus.MISSING_IN_CH,
                MatchStatus.TERM_ON_DIFFERENT_CONCEPT, MatchStatus.MISSING_IN_FR);
        List<MatchResult> changes = filter(results, MatchStatus.DIFFERENT_ACCEPTABILITY);
        List<MatchResult> inactivations = filter(results, MatchStatus.ACTIVE_CH_INACTIVE_FR,
                MatchStatus.ACTIVE_CH_MISSING_IN_FR);
        List<MatchResult> reactivations = filter(results, MatchStatus.ACTIVE_FR_INACTIVE_CH);

        batchExportService.export(
            MatchResultDeltas.buildAdditions(additions),
            MatchResultDeltas.buildChanges(changes, resultCollector),
            MatchResultDeltas.buildInactivations(inactivations),
            MatchResultDeltas.buildReactivations(reactivations, resultCollector),
            resultCollector,
            destination
        );
    }

    private static List<MatchResult> filter(List<MatchResult> results, MatchStatus... statuses) {
        return results.stream()
                .filter(r -> matches(r, statuses))
                .collect(Collectors.toList());
    }

    private static boolean matches(MatchResult r, MatchStatus[] statuses) {
        for (MatchStatus s : statuses) {
            if (r.status() == s) return true;
        }
        return false;
    }
}
