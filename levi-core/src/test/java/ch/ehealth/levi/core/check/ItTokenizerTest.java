package ch.ehealth.levi.core.check;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ItTokenizerTest {

    @Test
    void normalizeNull() {
        assertEquals("", ItTokenizer.normalize(null));
    }

    @Test
    void normalizeNfc() {
        // é composed (NFC) vs decomposed (NFD); NFC is the identity for composed chars
        String composed = "\u00E9"; // é precomposed
        assertEquals(composed, ItTokenizer.normalize(composed));
    }

    @Test
    void wordsSplitsWhitespace() {
        assertEquals(List.of("malattia", "del", "fegato"), ItTokenizer.words("malattia del fegato"));
    }

    @Test
    void wordsReturnsEmptyForNull() {
        assertTrue(ItTokenizer.words(null).isEmpty());
    }

    @Test
    void wordsReturnsEmptyForBlank() {
        assertTrue(ItTokenizer.words("   ").isEmpty());
    }

    // ---- token() with elision ----

    @Test
    void tokensStripsItalianElisionPrefix() {
        assertEquals(List.of("orecchio"), ItTokenizer.tokens("l'orecchio"));
    }

    @Test
    void tokensStripsDellElision() {
        assertEquals(List.of("orecchio"), ItTokenizer.tokens("dell'orecchio"));
    }

    @Test
    void tokensStripsNellElision() {
        assertEquals(List.of("acqua"), ItTokenizer.tokens("nell'acqua"));
    }

    @Test
    void tokensStripsSullElision() {
        assertEquals(List.of("anca"), ItTokenizer.tokens("sull'anca"));
    }

    @Test
    void tokensStripsAllElision() {
        assertEquals(List.of("ospedale"), ItTokenizer.tokens("all'ospedale"));
    }

    @Test
    void tokensStripsDallElision() {
        assertEquals(List.of("inizio"), ItTokenizer.tokens("dall'inizio"));
    }

    @Test
    void tokensStripsQuestElision() {
        assertEquals(List.of("articolazione"), ItTokenizer.tokens("quest'articolazione"));
    }

    @Test
    void tokensStripsGlElision() {
        assertEquals(List.of("italiani"), ItTokenizer.tokens("gl'italiani"));
    }

    @Test
    void tokensStripsUnElision() {
        assertEquals(List.of("anamnesi"), ItTokenizer.tokens("un'anamnesi"));
    }

    @Test
    void tokensDoesNotStripNonElisionWords() {
        assertEquals(List.of("diabete"), ItTokenizer.tokens("diabete"));
    }

    @Test
    void tokensSplitsHyphenatedWords() {
        List<String> result = ItTokenizer.tokens("non-invasiva");
        // The hyphen splits the word into atomic tokens: "non", "invasiva"
        assertTrue(result.contains("non"));
        assertTrue(result.contains("invasiva"));
    }

    @Test
    void tokensHandlesCompoundWithElision() {
        List<String> result = ItTokenizer.tokens("dell'orecchio");
        assertEquals(List.of("orecchio"), result);
    }

    @Test
    void tokensFullTerm() {
        List<String> result = ItTokenizer.tokens("malattia dell'orecchio non-invasiva");
        assertTrue(result.contains("malattia"));
        assertTrue(result.contains("orecchio"));
        assertTrue(result.contains("non"));
        assertTrue(result.contains("invasiva"));
        assertFalse(result.contains("dell"));
        assertFalse(result.contains("dell'orecchio"));
    }

    @Test
    void tokensHandlesMultiWordWithMultipleElisions() {
        List<String> result = ItTokenizer.tokens("l'orecchio e dell'osso temporale");
        assertTrue(result.contains("orecchio"));
        assertTrue(result.contains("e"));
        assertTrue(result.contains("osso"));
        assertTrue(result.contains("temporale"));
        assertFalse(result.contains("l'"));
        assertFalse(result.contains("dell'"));
    }

    // ---- compoundTokens() ----

    @Test
    void compoundTokensKeepsHyphenatedForms() {
        List<String> result = ItTokenizer.compoundTokens("non-invasiva");
        assertEquals(List.of("non-invasiva"), result);
    }

    @Test
    void compoundTokensStripsElision() {
        assertEquals(List.of("orecchio"), ItTokenizer.compoundTokens("l'orecchio"));
    }

    // ---- hasMultipleSpaces ----

    @Test
    void hasMultipleSpacesTrue() {
        assertTrue(ItTokenizer.hasMultipleSpaces("malattia  del fegato"));
    }

    @Test
    void hasMultipleSpacesFalse() {
        assertFalse(ItTokenizer.hasMultipleSpaces("malattia del fegato"));
    }

    @Test
    void hasMultipleSpacesFalseForNull() {
        assertFalse(ItTokenizer.hasMultipleSpaces(null));
    }
}