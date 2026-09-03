package ch.ehealth.levi.core.compare;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ehealth.levi.core.io.FileWriterUtil;

/**
 * Writes the LexSync-SCT style flat comparison output files. The standard
 * delta row layouts are produced by {@link MatchResultDeltas} (shared with
 * {@link SnomedBatchExporter}) to avoid duplicating the same conversion.
 */
public class CsvExporter {

    private static final Logger logger = LoggerFactory.getLogger(CsvExporter.class);
    private final FileWriterUtil writer = new FileWriterUtil();

    public void exportDescriptionChangesDelta(
            List<MatchResult> results, String outputPath) throws IOException {

        var relevant = results.stream().filter(r ->
            r.status() == MatchStatus.DIFFERENT_ACCEPTABILITY).toList();

        List<List<String>> rows = MatchResultDeltas.buildChanges(relevant);
        writer.writeToFile(outputPath, rows);
        logger.info("{} -> {} rows", outputPath, relevant.size());
    }

    public void exportNewDescriptionsDelta(
            List<MatchResult> results, String outputPath) throws IOException {

        var relevant = results.stream().filter(r ->
            r.status() == MatchStatus.MISSING_IN_CH            ||
            r.status() == MatchStatus.TERM_ON_DIFFERENT_CONCEPT||
            r.status() == MatchStatus.MISSING_IN_FR
        ).toList();

        List<List<String>> rows = MatchResultDeltas.buildAdditions(relevant);
        writer.writeToFile(outputPath, rows);
        logger.info("{} -> {} rows", outputPath, relevant.size());
    }

    public void exportActiveCHInactiveFRDelta(
            List<MatchResult> results, String outputPath) throws IOException {

        var inactiveFR = results.stream()
            .filter(r -> r.status() == MatchStatus.ACTIVE_CH_INACTIVE_FR)
            .toList();

        var missingFR = results.stream()
            .filter(r -> r.status() == MatchStatus.ACTIVE_CH_MISSING_IN_FR)
            .toList();

        writeInactivationFile(inactiveFR, outputPath.replace(".tsv", "_INACTIVE_IN_FR_Inactivations.tsv"));
        writeAdditionFile(missingFR, outputPath.replace(".tsv", "_MISSING_IN_FR.tsv"));
    }

    private void writeAdditionFile(
            List<MatchResult> rows, String outputPath) throws IOException {

        List<String> header = List.of(
            "Concept ID", "Language Code", "Term", "Case Significance ID", "Type ID", "Acceptability"
        );
        List<List<String>> data = new ArrayList<>();
        data.add(header);

        for (var r : rows) {
            data.add(List.of(
                safe(r.conceptId_CH()),
                safe(r.languageCode()),
                safe(r.term()),
                "", "", ""
            ));
        }
        writer.writeToFile(outputPath, data);
        logger.info("{} -> {} rows", outputPath, rows.size());
    }

    private void writeInactivationFile(
            List<MatchResult> rows, String outputPath) throws IOException {

        List<List<String>> data = MatchResultDeltas.buildInactivations(rows);
        writer.writeToFile(outputPath, data);
        logger.info("{} -> {} rows", outputPath, rows.size());
    }

    public void exportReactivationDelta(
            List<MatchResult> results, String outputPath) throws IOException {

        var relevant = results.stream().filter(r ->
            r.status() == MatchStatus.ACTIVE_FR_INACTIVE_CH
        ).toList();

        List<List<String>> rows = MatchResultDeltas.buildReactivations(relevant);
        writer.writeToFile(outputPath, rows);
        logger.info("{} -> {} rows", outputPath, relevant.size());
    }

    public void printSummary(List<MatchResult> results) {
        System.out.println("\n=== Summary ===");
        for (MatchStatus status : MatchStatus.values()) {
            long count = results.stream().filter(r -> r.status() == status).count();
            if (count > 0) System.out.printf("%-35s: %d%n", status, count);
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
