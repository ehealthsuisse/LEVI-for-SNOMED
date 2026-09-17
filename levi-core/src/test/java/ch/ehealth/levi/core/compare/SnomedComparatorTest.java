package ch.ehealth.levi.core.compare;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SnomedComparatorTest {

    private SnomedComparator comparator;

    @BeforeEach
    void setUp() {
        comparator = new SnomedComparator();
    }

    private static DescriptionWithAcceptability desc(
            String conceptId, String term, String active, String languageCode) {
        return new DescriptionWithAcceptability(
                conceptId, "D-" + conceptId, term,
                "900000000000013009", languageCode,
                "900000000000448009", "Preferred", "2021000195106", active);
    }

    private static DescriptionWithAcceptability desc(
            String conceptId, String term, String active, String languageCode,
            String acceptability, String refsetId, String descId) {
        return new DescriptionWithAcceptability(
                conceptId, descId, term,
                "900000000000013009", languageCode,
                "900000000000448009", acceptability, refsetId, active);
    }

    // ── Identical ───────────────────────────────────────────────

    @Test
    void identicalTermsProduceIdenticalStatus() {
        var fr = List.of(desc("C1", "term", "1", "fr"));
        var ch = List.of(desc("C1", "term", "1", "fr"));

        var results = comparator.compare(fr, ch);
        var statuses = results.stream().map(MatchResult::status).toList();
        assertTrue(statuses.contains(MatchStatus.IDENTICAL));
    }

    // ── Active status mismatches ────────────────────────────────

    @Test
    void frActiveChInactiveProducesActiveFrInactiveCh() {
        var fr = List.of(desc("C1", "term", "1", "fr"));
        var ch = List.of(desc("C1", "term", "0", "fr"));

        var results = comparator.compare(fr, ch);
        assertEquals(MatchStatus.ACTIVE_FR_INACTIVE_CH, results.get(0).status());
    }

    @Test
    void frInactiveChActiveProducesActiveChInactiveFr() {
        var fr = List.of(desc("C1", "term", "0", "fr"));
        var ch = List.of(desc("C1", "term", "1", "fr"));

        var results = comparator.compare(fr, ch);
        assertEquals(MatchStatus.ACTIVE_CH_INACTIVE_FR, results.get(0).status());
    }

    @Test
    void bothInactiveProducesNoResult() {
        var fr = List.of(desc("C1", "term", "0", "fr"));
        var ch = List.of(desc("C1", "term", "0", "fr"));

        var results = comparator.compare(fr, ch);
        assertTrue(results.isEmpty());
    }

    // ── Different acceptability ──────────────────────────────────

    @Test
    void sameTermDifferentAcceptabilityProducesDiffAcceptability() {
        var fr = List.of(desc("C1", "term", "1", "fr", "Preferred", "11000315107", "D1"));
        var ch = List.of(desc("C1", "term", "1", "fr", "Acceptable", "2021000195106", "D2"));

        var results = comparator.compare(fr, ch);
        assertEquals(MatchStatus.DIFFERENT_ACCEPTABILITY, results.get(0).status());
    }

    // ── Missing in CH ───────────────────────────────────────────

    @Test
    void termInFrButNotInChProducesMissingInCh() {
        var fr = List.of(desc("C1", "term", "1", "fr"));
        var ch = List.<DescriptionWithAcceptability>of();

        var results = comparator.compare(fr, ch);
        assertEquals(MatchStatus.MISSING_IN_CH, results.get(0).status());
    }

    // ── Missing in FR (active CH only) ──────────────────────────

    @Test
    void activeChTermNotInFrProducesActiveChMissingInFr() {
        var fr = List.<DescriptionWithAcceptability>of();
        var ch = List.of(desc("C1", "term", "1", "fr"));

        var results = comparator.compare(fr, ch);
        assertEquals(1, results.size());
        assertEquals(MatchStatus.ACTIVE_CH_MISSING_IN_FR, results.get(0).status());
        assertEquals("Term+ConceptId not found in FR file", results.get(0).note());
    }

    @Test
    void inactiveChTermNotInFrIsSkipped() {
        var fr = List.<DescriptionWithAcceptability>of();
        var ch = List.of(desc("C1", "term", "0", "fr"));

        var results = comparator.compare(fr, ch);
        assertTrue(results.isEmpty(), "Inactive CH descriptions missing in FR should not be reported");
    }

    // ── TERM_ON_DIFFERENT_CONCEPT (FR→CH) ───────────────────────

    @Test
    void termInChUnderDifferentConceptProducesTermOnDifferentConcept_fromFr() {
        var fr = List.of(desc("C1", "same term", "1", "fr"));
        var ch = List.of(desc("C2", "same term", "1", "fr"));

        var results = comparator.compare(fr, ch);
        assertEquals(MatchStatus.TERM_ON_DIFFERENT_CONCEPT, results.get(0).status());
        assertTrue(results.get(0).note().contains("CH"));
    }

    // ── TERM_ON_DIFFERENT_CONCEPT (CH→FR) ───────────────────────

    @Test
    void termInFrUnderDifferentConceptProducesTermOnDifferentConcept_fromCh() {
        var fr = List.of(desc("C1", "same term", "1", "fr"));
        var ch = List.of(desc("C2", "same term", "1", "fr"));

        var results = comparator.compare(fr, ch);

        // Both FR and CH loops see this term mismatch.
        // FR loop: FR(C1, "same term") not in CH by (C1, term) but term exists in CH under C2
        // CH loop: CH(C2, "same term") not in FR by (C2, term) but term exists in FR under C1
        long termDiffCount = results.stream()
                .filter(r -> r.status() == MatchStatus.TERM_ON_DIFFERENT_CONCEPT)
                .count();
        assertEquals(2, termDiffCount, "Both loops should detect term on different concept");
    }

    @Test
    void activeChTermExistsInFrUnderDifferentConceptProducesTermOnDifferentConcept() {
        var fr = List.of(desc("C99", "médicament", "1", "fr"));
        var ch = List.of(desc("C50", "médicament", "1", "fr"));

        var results = comparator.compare(fr, ch);
        var statuses = results.stream().map(MatchResult::status).toList();
        assertTrue(statuses.contains(MatchStatus.TERM_ON_DIFFERENT_CONCEPT),
                "Active CH term existing in FR under different concept should be TERM_ON_DIFFERENT_CONCEPT");
        assertFalse(statuses.contains(MatchStatus.ACTIVE_CH_MISSING_IN_FR),
                "Should not be MISSING_IN_FR since term exists in FR");
    }

    @Test
    void inactiveChTermExistsInFrUnderDifferentConcept_noChResult() {
        var fr = List.of(desc("C99", "médicament", "1", "fr"));
        var ch = List.of(desc("C50", "médicament", "0", "fr"));

        var results = comparator.compare(fr, ch);
        // FR loop still produces MISSING_IN_CH for FR(C99, "médicament"),
        // but the CH loop should NOT produce any result for the inactive CH entry.
        long chOriginResults = results.stream()
                .filter(r -> r.conceptId_CH() != null && r.conceptId_FR() == null)
                .count();
        assertEquals(0, chOriginResults,
                "Inactive CH descriptions should not produce any results from the CH loop");
    }

    // ── Unicode normalisation (NFC vs NFD) ──────────────────────

    @Test
    void nfdTermMatchesNfcTerm() {
        // é as NFD (e + U+0301 combining acute) vs é as NFC (U+00E9)
        String nfc = "café";       // U+00E9
        String nfd = "cafe\u0301"; // e + combining acute accent

        var fr = List.of(desc("C1", nfc, "1", "fr"));
        var ch = List.of(desc("C1", nfd, "1", "fr"));

        var results = comparator.compare(fr, ch);
        assertTrue(results.stream().anyMatch(r -> r.status() == MatchStatus.IDENTICAL),
                "NFC and NFD forms of same term should match as IDENTICAL");
    }

    // ── Whitespace trimming ──────────────────────────────────────

    @Test
    void trimmedTermsMatchUntrimmed() {
        var fr = List.of(desc("C1", "  term  ", "1", "fr"));
        var ch = List.of(desc("C1", "term", "1", "fr"));

        var results = comparator.compare(fr, ch);
        assertTrue(results.stream().anyMatch(r -> r.status() == MatchStatus.IDENTICAL),
                "Terms differing only by leading/trailing whitespace should match");
    }

    // ── Case sensitivity preserved ──────────────────────────────

    @Test
    void caseDifferenceDoesNotMatch() {
        var fr = List.of(desc("C1", "Term", "1", "fr"));
        var ch = List.of(desc("C1", "term", "1", "fr"));

        var results = comparator.compare(fr, ch);
        assertFalse(results.stream().anyMatch(r -> r.status() == MatchStatus.IDENTICAL),
                "Terms differing in case should NOT match as identical");
    }

    // ── Mixed scenario ──────────────────────────────────────────

    @Test
    void mixedScenarioWithBothDirections() {
        var fr = List.of(
                desc("C1", "france term", "1", "fr"),
                desc("C2", "shared term", "1", "fr"));
        var ch = List.of(
                desc("C2", "shared term", "1", "fr"),
                desc("C3", "swiss term", "1", "fr"),
                desc("C4", "inactive old", "0", "fr"));

        var results = comparator.compare(fr, ch);
        var byStatus = results.stream()
                .collect(Collectors.groupingBy(MatchResult::status, Collectors.counting()));

        assertEquals(1L, byStatus.getOrDefault(MatchStatus.MISSING_IN_CH, 0L),
                "C1 should be missing in CH");
        assertEquals(1L, byStatus.getOrDefault(MatchStatus.MISSING_IN_FR, 0L)
                + byStatus.getOrDefault(MatchStatus.ACTIVE_CH_MISSING_IN_FR, 0L),
                "C3 should be missing in FR");
        assertEquals(0L, byStatus.getOrDefault(MatchStatus.TERM_ON_DIFFERENT_CONCEPT, 0L),
                "No term-on-different-concept in this scenario");
    }

    // ── Non-breaking space ──────────────────────────────────────

    @Test
    void nbspTermMatchesNormalSpace() {
        String withNbsp = "term\u00A0after";
        String withSpace = "term after";

        // Current normalize() does trim + NFC but does NOT replace NBSP.
        // If this test fails, it means NBSP handling was NOT added —
        // which is fine per requirements (trim+NFC only).
        var fr = List.of(desc("C1", withNbsp, "1", "fr"));
        var ch = List.of(desc("C1", withSpace, "1", "fr"));

        var results = comparator.compare(fr, ch);
        // NBSP is NOT regular whitespace; trim() won't remove it,
        // so these may or may not match depending on future normalization.
        // This test documents the current behavior.
        boolean matched = results.stream().anyMatch(r -> r.status() == MatchStatus.IDENTICAL);
        // Document: current behavior may differ; this test captures it
        // regardless of outcome. Remove or adjust if NBSP normalization is added.
        assertFalse(matched,
                "NBSP vs normal space are NOT equal under current trim+NFC normalization");
    }

    // ── verify helper for language code ──────────────────────────

    @Test
    void nonLowercaseFrLanguageCodeProducesMissingInFr() {
        var fr = List.<DescriptionWithAcceptability>of();
        var ch = List.of(desc("C1", "term", "1", "FR"));

        var results = comparator.compare(fr, ch);
        assertEquals(1, results.size());
        assertEquals(MatchStatus.MISSING_IN_FR, results.get(0).status(),
                "Non-lowercase FR language code should fall through to MISSING_IN_FR, not ACTIVE_CH_MISSING_IN_FR");
    }
}
