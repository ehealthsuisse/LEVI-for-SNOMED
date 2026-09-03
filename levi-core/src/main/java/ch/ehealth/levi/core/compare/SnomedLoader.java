package ch.ehealth.levi.core.compare;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SnomedLoader {

    public record Description(String id, String active, String conceptId, String languageCode, String typeId, String term, String caseSignificanceId) {}
    public record LanguageRefset(String referencedComponentId, String acceptabilityId, String refsetId, String active) {}

    /**
     * Loads an RF2 sct2_Description file.
     * Expected columns: id, effectiveTime, active, moduleId, conceptId,
     *                   languageCode, typeId, term, caseSignificanceId.
     */
    public Map<String, Description> loadDescriptions(Path path) throws IOException {
        Map<String, Description> map = new HashMap<>();
        // Stream lines (buffered) so we do not hold the whole (large RF2) file
        // in memory as a List, unlike Files.readAllLines.
        try (var lines = Files.lines(path)) {
            lines.skip(1) // skip header
                 .forEach(line -> {
                     String[] parts = line.split("\t");
                     if (parts.length >= 9) {
                         Description d = new Description(
                                 parts[0], parts[2], parts[4], parts[5],
                                 parts[6], parts[7], parts[8]);
                         map.put(d.id(), d);
                     }
                 });
        }
        return map;
    }

    /**
     * Loads an RF2 der2_cRefset_Language file.
     * Expected columns: id, effectiveTime, active, moduleId, refsetId,
     *                   referencedComponentId, acceptabilityId.
     * The map is keyed by referencedComponentId (= description id).
     */
    public Map<String, LanguageRefset> loadLanguageRefset(Path path) throws IOException {
        Map<String, LanguageRefset> map = new HashMap<>();
        try (var lines = Files.lines(path)) {
            lines.skip(1) // skip header
                 .forEach(line -> {
                     String[] parts = line.split("\t");
                     if (parts.length >= 7) {
                         LanguageRefset lr = new LanguageRefset(
                                 parts[5], parts[6], parts[4], parts[2]);
                         map.put(lr.referencedComponentId(), lr);
                     }
                 });
        }
        return map;
    }

    public List<DescriptionWithAcceptability> enrich(Map<String, Description> descriptions, Map<String, LanguageRefset> languageRefsets) {
        List<DescriptionWithAcceptability> enriched = new ArrayList<>();
        for (var entry : descriptions.entrySet()) {
            Description desc = entry.getValue();
            LanguageRefset refset = languageRefsets.get(desc.id());
            if (refset != null) {
                String acceptability = mapAcceptability(refset.acceptabilityId());
                enriched.add(new DescriptionWithAcceptability(
                    desc.conceptId(),
                    desc.id(),
                    desc.term(),
                    desc.typeId(),
                    desc.languageCode(),
                    desc.caseSignificanceId(),
                    acceptability,
                    refset.refsetId(),
                    desc.active()
                ));
            }
        }
        return enriched;
    }

    private String mapAcceptability(String acceptabilityId) {
        return switch (acceptabilityId) {
            case "900000000000548007" -> "Preferred";
            case "900000000000549004" -> "Acceptable";
            default -> acceptabilityId;
        };
    }
}