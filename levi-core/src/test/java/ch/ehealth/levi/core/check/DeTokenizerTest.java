package ch.ehealth.levi.core.check;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeTokenizerTest {

    @Test
    void normalizeNull() {
        assertEquals("", DeTokenizer.normalize(null));
    }

    @Test
    void normalizeNfc() {
        String composed = "\u00E4";
        assertEquals(composed, DeTokenizer.normalize(composed));
    }

    @Test
    void wordsSplitsWhitespace() {
        assertEquals(List.of("Diabetes", "mellitus"), DeTokenizer.words("Diabetes mellitus"));
    }

    @Test
    void wordsReturnsEmptyForNull() {
        assertTrue(DeTokenizer.words(null).isEmpty());
    }

    @Test
    void wordsReturnsEmptyForBlank() {
        assertTrue(DeTokenizer.words("   ").isEmpty());
    }

    // ---- tokens() ----

    @Test
    void tokensSimpleTerm() {
        assertEquals(List.of("Diabetes", "mellitus"), DeTokenizer.tokens("Diabetes mellitus"));
    }

    @Test
    void tokensHandlesGermanUmlauts() {
        // German compound words are single tokens (no space splitting)
        assertEquals(List.of("Ahnliche", "Arznei"), DeTokenizer.tokens("Ahnliche Arznei"));
    }

    @Test
    void tokensHandlesEszett() {
        assertEquals(List.of("Groe", "Korper"), DeTokenizer.tokens("Groe Korper"));
    }

    @Test
    void tokensStripsGenitiveS() {
        assertEquals(List.of("Muller", "Syndrom"), DeTokenizer.tokens("Muller's Syndrom"));
    }

    @Test
    void tokensSplitsHyphenatedCompounds() {
        List<String> result = DeTokenizer.tokens("Aedes-albopictus-Densovirus");
        assertTrue(result.contains("Aedes"));
        assertTrue(result.contains("albopictus"));
        assertTrue(result.contains("Densovirus"));
    }

    @Test
    void tokensSplitsGermanCompoundWithHyphen() {
        List<String> result = DeTokenizer.tokens("Angiotensin-converting-Enzym");
        assertTrue(result.contains("Angiotensin"));
        assertTrue(result.contains("converting"));
        assertTrue(result.contains("Enzym"));
    }

    @Test
    void tokensSplitsDurchkopplung() {
        List<String> result = DeTokenizer.tokens("Petit-mal-Anfall");
        assertTrue(result.contains("Petit"));
        assertTrue(result.contains("mal"));
        assertTrue(result.contains("Anfall"));
    }

    @Test
    void tokensPreservesUmlauts() {
        List<String> result = DeTokenizer.tokens("akutes Koronarsyndrom");
        assertTrue(result.contains("akutes"));
        assertTrue(result.contains("Koronarsyndrom"));
    }

    @Test
    void tokensHandlesCompoundWordWithoutHyphen() {
        List<String> result = DeTokenizer.tokens("Krankenschwester");
        assertEquals(List.of("Krankenschwester"), result);
    }

    // ---- compoundTokens() ----

    @Test
    void compoundTokensKeepsHyphenatedForms() {
        List<String> result = DeTokenizer.compoundTokens("Aedes-albopictus-Densovirus");
        assertTrue(result.contains("Aedes-albopictus-Densovirus"));
    }

    @Test
    void compoundTokensStripsGenitiveS() {
        assertEquals(List.of("Parkinson", "Krankheit"), DeTokenizer.compoundTokens("Parkinson's Krankheit"));
    }

    // ---- hasMultipleSpaces ----

    @Test
    void hasMultipleSpacesTrue() {
        assertTrue(DeTokenizer.hasMultipleSpaces("Diabetes  mellitus"));
    }

    @Test
    void hasMultipleSpacesFalse() {
        assertFalse(DeTokenizer.hasMultipleSpaces("Diabetes mellitus"));
    }

    @Test
    void hasMultipleSpacesFalseForNull() {
        assertFalse(DeTokenizer.hasMultipleSpaces(null));
    }
}
