package ch.ehealth.levi.core.check;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DeLexiconBuilderTest {

    @Test
    void analyzeEmpty() {
        var result = DeLexiconBuilder.analyze(List.of());
        assertEquals(0, result.frequencies().size());
        assertTrue(result.highFrequencyTokens().isEmpty());
        assertTrue(result.lowFrequencyTokens().isEmpty());
    }

    @Test
    void analyzeSingleTerm() {
        var result = DeLexiconBuilder.analyze(List.of("diabete mellito"));
        assertEquals(2, result.frequencies().size());
        assertTrue(result.frequencies().containsKey("diabete"));
        assertTrue(result.frequencies().containsKey("mellito"));
        assertEquals(2, result.termCounts().size());
    }

    @Test
    void analyzeFiltersShortTokens() {
        var result = DeLexiconBuilder.analyze(List.of("der Diabetes"));
        assertTrue(result.frequencies().containsKey("der"));
        assertEquals(2, result.frequencies().size());
    }

    @Test
    void analyzePreservesGermanCompoundWords() {
        var result = DeLexiconBuilder.analyze(List.of("Herzkrankheit"));
        assertTrue(result.frequencies().containsKey("herzkrankheit"));
    }

    @Test
    void analyzeTokenFrequency() {
        var result = DeLexiconBuilder.analyze(List.of(
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
        var result = DeLexiconBuilder.analyze(List.of(
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
        var result = DeLexiconBuilder.analyze(List.of(
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
        assertEquals(DeLexiconBuilder.CAT_ACRONYM, DeLexiconBuilder.classify("IRM", true, false));
        assertEquals(DeLexiconBuilder.CAT_ACRONYM, DeLexiconBuilder.classify("TC", true, false));
    }

    @Test
    void classifyTaxon() {
        assertEquals(DeLexiconBuilder.CAT_TAXON, DeLexiconBuilder.classify("Escherichia", true, true));
    }

    @Test
    void classifyEponym() {
        assertEquals(DeLexiconBuilder.CAT_EPONYM, DeLexiconBuilder.classify("Parkinson", true, false));
        assertEquals(DeLexiconBuilder.CAT_EPONYM, DeLexiconBuilder.classify("Crohn", true, false));
    }

    @Test
    void classifyInn() {
        assertEquals(DeLexiconBuilder.CAT_INN, DeLexiconBuilder.classify("imatinib", false, false));
        assertEquals(DeLexiconBuilder.CAT_INN, DeLexiconBuilder.classify("infliximab", false, false));
    }

    @Test
    void classifyLatin() {
        assertEquals(DeLexiconBuilder.CAT_LATIN, DeLexiconBuilder.classify("carcinoma", false, false));
        assertEquals(DeLexiconBuilder.CAT_LATIN, DeLexiconBuilder.classify("fibrosis", false, false));
    }

    @Test
    void classifyUnknown() {
        assertEquals(DeLexiconBuilder.CAT_UNKNOWN, DeLexiconBuilder.classify("fegato", false, false));
        assertEquals(DeLexiconBuilder.CAT_UNKNOWN, DeLexiconBuilder.classify("diabete", false, false));
    }

    @Test
    void classifyNull() {
        assertEquals(DeLexiconBuilder.CAT_UNKNOWN, DeLexiconBuilder.classify(null, false, false));
    }

    // ---- analyze() with categorization ----

    @Test
    void analyzeCategorizesAcronyms() {
        var result = DeLexiconBuilder.analyze(List.of("TC total body", "TAC cranio"));
        assertEquals(DeLexiconBuilder.CAT_ACRONYM, result.categories().get("tc"));
        assertEquals(DeLexiconBuilder.CAT_ACRONYM, result.categories().get("tac"));
    }

    @Test
    void analyzeCategorizesEponyms() {
        var result = DeLexiconBuilder.analyze(List.of("morbo di Parkinson", "morbo di Crohn"));
        assertEquals(DeLexiconBuilder.CAT_EPONYM, result.categories().get("parkinson"));
        assertEquals(DeLexiconBuilder.CAT_EPONYM, result.categories().get("crohn"));
    }

    @Test
    void originalFormsPreserved() {
        var result = DeLexiconBuilder.analyze(List.of("Morbo di Parkinson"));
        assertEquals("Parkinson", result.originalForms().get("parkinson"));
    }
}