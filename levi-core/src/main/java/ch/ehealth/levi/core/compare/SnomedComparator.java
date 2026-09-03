package ch.ehealth.levi.core.compare;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class SnomedComparator {

    record ConceptTermKey(String conceptId, String term) {}

    public List<MatchResult> compare(
            List<DescriptionWithAcceptability> frList,
            List<DescriptionWithAcceptability> chList) {

        List<MatchResult> results = new ArrayList<>();

        // Index: conceptId + term -> entry
        Map<ConceptTermKey, DescriptionWithAcceptability> frByConceptTerm =
            frList.stream().collect(Collectors.toMap(
                d -> new ConceptTermKey(d.conceptId(), d.term()),
                d -> d, (a, b) -> a));

        Map<ConceptTermKey, DescriptionWithAcceptability> chByConceptTerm =
            chList.stream().collect(Collectors.toMap(
                d -> new ConceptTermKey(d.conceptId(), d.term()),
                d -> d, (a, b) -> a));

        // Index: term -> set of conceptIds (for TERM_ON_DIFFERENT_CONCEPT)
        Map<String, Set<String>> chTermToConcepts = new HashMap<>();
        for (var ch : chList) {
            chTermToConcepts
                .computeIfAbsent(ch.term(), k -> new HashSet<>())
                .add(ch.conceptId());
        }

        // ── Iterate FR ──────────────────────────────────────────────
        for (var fr : frList) {
            var key = new ConceptTermKey(fr.conceptId(), fr.term());
            var ch  = chByConceptTerm.get(key);

            if (ch == null) {
                // Not found in CH by conceptId+term
                Set<String> otherConcepts = chTermToConcepts
                    .getOrDefault(fr.term(), Set.of())
                    .stream()
                    .filter(cid -> !cid.equals(fr.conceptId()))
                    .collect(Collectors.toSet());

                if (!otherConcepts.isEmpty()) {
                    results.add(buildResult(fr, null, MatchStatus.TERM_ON_DIFFERENT_CONCEPT,
                        "⚠️ Term exists in CH under different concept(s): " + otherConcepts));
                } else {
                    results.add(buildResult(fr, null, MatchStatus.MISSING_IN_CH, ""));
                }
                continue;
            }

            // Found → check active status
            boolean frActive = "1".equals(fr.active());
            boolean chActive = "1".equals(ch.active());

            if (frActive && !chActive) {
                results.add(buildResult(fr, ch, MatchStatus.ACTIVE_FR_INACTIVE_CH,
                    "FR active, CH inactive – consider reactivating in CH"));
                continue;
            }
            if (!frActive && chActive) {
                results.add(buildResult(fr, ch, MatchStatus.ACTIVE_CH_INACTIVE_FR,
                    "CH active, FR inactive – consider deactivating in CH"));
                continue;
            }
            if (!frActive && !chActive) {
                // both inactive → identical, no action needed
                continue;
            }

            // Both active → check acceptability
            if (!Objects.equals(fr.acceptability(), ch.acceptability())) {
                results.add(buildResult(fr, ch, MatchStatus.DIFFERENT_ACCEPTABILITY, ""));
            } else {
                results.add(buildResult(fr, ch, MatchStatus.IDENTICAL, ""));
            }
        }

        // ── Iterate CH to find MISSING_IN_FR ────────────────────────
        Map<String, Set<String>> frTermToConcepts = new HashMap<>();
        for (var fr : frList) {
            frTermToConcepts
                .computeIfAbsent(fr.term(), k -> new HashSet<>())
                .add(fr.conceptId());
        }

        for (var ch : chList) {
            var key = new ConceptTermKey(ch.conceptId(), ch.term());
            if (!frByConceptTerm.containsKey(key)) {
                // Only flag active CH French descriptions as candidates for inactivation
                if ("1".equals(ch.active()) && "fr".equals(ch.languageCode())) {
                    results.add(buildResult(null, ch, MatchStatus.ACTIVE_CH_MISSING_IN_FR,
                        "Term+ConceptId not found in FR file"));
                } else {
                    results.add(buildResult(null, ch, MatchStatus.MISSING_IN_FR, "Term+ConceptId active in CH but not found in FR file"));
                }
            }
        }

        return results;
    }

    private MatchResult buildResult(
            DescriptionWithAcceptability fr,
            DescriptionWithAcceptability ch,
            MatchStatus status,
            String note) {
        return new MatchResult(
            fr != null ? fr.conceptId()      : null,
            ch != null ? ch.conceptId()      : null,
            fr != null ? fr.descriptionId()  : null,
            ch != null ? ch.descriptionId()  : null,
            fr != null ? fr.term()           : ch.term(),
            null, // preferredTerm – enrich separately if needed
            null, // fsnTerm – enrich separately if needed
            fr != null ? fr.typeId()         : ch.typeId(),
            fr != null ? fr.languageCode()   : ch.languageCode(),
            fr != null ? fr.caseSignificance(): ch.caseSignificance(),
            fr != null ? fr.refsetId()       : ch.refsetId(),
            fr != null ? fr.acceptability()  : null,
            ch != null ? ch.acceptability()  : null,
            fr != null ? fr.active()         : null,
            ch != null ? ch.active()         : null,
            status,
            note
        );
    }
}