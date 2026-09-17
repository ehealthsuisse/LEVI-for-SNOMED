package ch.ehealth.levi.core.check;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ItLexiconBuilderTest {

    @Test
    void analyzeEmpty() {
        var result = ItLexiconBuilder.analyze(List.of());
        assertEquals(0, result.frequencies().size());
        assertTrue(result.highFrequencyTokens().isEmpty());
        assertTrue(result.lowFrequencyTokens().isEmpty());
    }

    @Test
    void analyzeSingleTerm() {
        var result = ItLexiconBuilder.analyze(List.of("diabete mellito"));
        assertEquals(2, result.frequencies().size());
        assertTrue(result.frequencies().containsKey("diabete"));
        assertTrue(result.frequencies().containsKey("mellito"));
        assertEquals(2, result.termCounts().size());
    }

    @Test
    void analyzeFiltersShortTokens() {
        var result = ItLexiconBuilder.analyze(List.of("il diabete"));
        // "il" is length 2, should be included
        assertTrue(result.frequencies().containsKey("il"));
        assertEquals(2, result.frequencies().size());
    }

    @Test
    void analyzeStripsItalianElision() {
        var result = ItLexiconBuilder.analyze(List.of("dell'orecchio"));
        assertTrue(result.frequencies().containsKey("orecchio"));
        assertFalse(result.frequencies().containsKey("dell'orecchio"));
        assertFalse(result.frequencies().containsKey("dell"));
    }

    @Test
    void analyzeTokenFrequency() {
        var result = ItLexiconBuilder.analyze(List.of(
                "diabete mellito",
                "diabete insipido",
                "fegato"));
        assertEquals(1L, result.frequencies().get("mellito"));
        assertEquals(1L, result.frequencies().get("insipido"));
        assertEquals(2L, result.frequencies().get("diabete"));
        assertEquals(1L, result.frequencies().get("fegato"));
    }

    @Test
    void highFrequencyTokensThreshold() {
        var result = ItLexiconBuilder.analyze(List.of(
                "diabete mellito",
                "diabete insipido",
                "diabete gestazionale",
                "fegato",
                "fegato"));
        Set<String> high = result.highFrequencyTokens();
        assertTrue(high.contains("diabete"));  // freq=3
        assertFalse(high.contains("mellito"));  // freq=1
        // fegato appears in 2 terms but freq=2 -> < threshold
        assertFalse(high.contains("fegato"));
    }

    @Test
    void lowFrequencyTokensOneOrTwo() {
        var result = ItLexiconBuilder.analyze(List.of(
                "diabete mellito",
                "fegato"));
        Set<String> low = result.lowFrequencyTokens();
        assertTrue(low.contains("mellito"));
        assertTrue(low.contains("fegato"));
        assertTrue(low.contains("diabete"));
    }

    // ---- classify() ----

    @Test
    void classifyAcronym() {
        assertEquals(ItLexiconBuilder.CAT_ACRONYM, ItLexiconBuilder.classify("IRM", true, false));
        assertEquals(ItLexiconBuilder.CAT_ACRONYM, ItLexiconBuilder.classify("TC", true, false));
    }

    @Test
    void classifyTaxon() {
        assertEquals(ItLexiconBuilder.CAT_TAXON, ItLexiconBuilder.classify("Escherichia", true, true));
    }

    @Test
    void classifyEponym() {
        assertEquals(ItLexiconBuilder.CAT_EPONYM, ItLexiconBuilder.classify("Parkinson", true, false));
        assertEquals(ItLexiconBuilder.CAT_EPONYM, ItLexiconBuilder.classify("Crohn", true, false));
    }

    @Test
    void classifyInn() {
        assertEquals(ItLexiconBuilder.CAT_INN, ItLexiconBuilder.classify("imatinib", false, false));
        assertEquals(ItLexiconBuilder.CAT_INN, ItLexiconBuilder.classify("infliximab", false, false));
    }

    @Test
    void classifyLatin() {
        assertEquals(ItLexiconBuilder.CAT_LATIN, ItLexiconBuilder.classify("carcinoma", false, false));
        assertEquals(ItLexiconBuilder.CAT_LATIN, ItLexiconBuilder.classify("fibrosis", false, false));
    }

    @Test
    void classifyUnknown() {
        assertEquals(ItLexiconBuilder.CAT_UNKNOWN, ItLexiconBuilder.classify("fegato", false, false));
        assertEquals(ItLexiconBuilder.CAT_UNKNOWN, ItLexiconBuilder.classify("diabete", false, false));
    }

    @Test
    void classifyNull() {
        assertEquals(ItLexiconBuilder.CAT_UNKNOWN, ItLexiconBuilder.classify(null, false, false));
    }

    // ---- analyze() with categorization ----

    @Test
    void analyzeCategorizesAcronyms() {
        var result = ItLexiconBuilder.analyze(List.of("TC total body", "TAC cranio"));
        assertEquals(ItLexiconBuilder.CAT_ACRONYM, result.categories().get("tc"));
        assertEquals(ItLexiconBuilder.CAT_ACRONYM, result.categories().get("tac"));
    }

    @Test
    void analyzeCategorizesEponyms() {
        var result = ItLexiconBuilder.analyze(List.of("morbo di Parkinson", "morbo di Crohn"));
        assertEquals(ItLexiconBuilder.CAT_EPONYM, result.categories().get("parkinson"));
        assertEquals(ItLexiconBuilder.CAT_EPONYM, result.categories().get("crohn"));
    }

    @Test
    void originalFormsPreserved() {
        var result = ItLexiconBuilder.analyze(List.of("Morbo di Parkinson"));
        assertEquals("Parkinson", result.originalForms().get("parkinson"));
    }
}