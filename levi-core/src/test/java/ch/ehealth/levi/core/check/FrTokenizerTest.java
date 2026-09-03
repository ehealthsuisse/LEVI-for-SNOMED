package ch.ehealth.levi.core.check;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FrTokenizerTest {

    @Test
    public void testNfcNormalization() {
        String nfd = "e\u0301l\u00e9phant";
        assertEquals("\u00e9l\u00e9phant", FrTokenizer.normalize(nfd));
    }

    @Test
    public void testNfcNormalizationNull() {
        assertEquals("", FrTokenizer.normalize(null));
    }

    @Test
    public void testElisionPrefixDropped() {
        assertEquals(List.of("hypertension"), FrTokenizer.tokens("l'hypertension"));
        assertEquals(List.of("\u00e9paule"), FrTokenizer.tokens("l'\u00e9paule"));
        assertEquals(List.of("importe"), FrTokenizer.tokens("n'importe"));
        assertEquals(List.of("ici"), FrTokenizer.tokens("jusqu'ici"));
        assertEquals(List.of("est"), FrTokenizer.tokens("c'est"));
    }

    @Test
    public void testHyphenCompoundSplit() {
        assertEquals(List.of("cou", "de", "pied"), FrTokenizer.tokens("cou-de-pied"));
        assertEquals(List.of("p\u00e9ri", "anal"), FrTokenizer.tokens("p\u00e9ri-anal"));
    }

    @Test
    public void testCompoundTokenKeepsHyphen() {
        assertEquals(List.of("cou-de-pied"), FrTokenizer.compoundTokens("cou-de-pied"));
        assertEquals(List.of("p\u00e9ri-anal"), FrTokenizer.compoundTokens("p\u00e9ri-anal"));
    }

    @Test
    public void testAujourdhuiKeptWhole() {
        assertEquals(List.of("aujourd'hui"), FrTokenizer.tokens("aujourd'hui"));
        assertEquals(List.of("AUJOURD'HUI"), FrTokenizer.compoundTokens("AUJOURD'HUI"));
    }

    @Test
    public void testPunctuationStripped() {
        assertEquals(List.of("rouge", "gonfl\u00e9"), FrTokenizer.tokens("rouge,gonfl\u00e9"));
        assertEquals(List.of("oeuf"), FrTokenizer.tokens("(oeuf)"));
    }

    @Test
    public void testMultipleSpacesDetected() {
        assertTrue(FrTokenizer.hasMultipleSpaces("a  b"));
        assertTrue(FrTokenizer.hasMultipleSpaces("a\tb"));
        assertFalse(FrTokenizer.hasMultipleSpaces("a b"));
    }

    @Test
    public void testWords() {
        assertEquals(List.of("TDAH", "-", "trouble"), FrTokenizer.words("TDAH - trouble"));
    }

    @Test
    public void testEmptyAndNull() {
        assertTrue(FrTokenizer.tokens("").isEmpty());
        assertTrue(FrTokenizer.tokens(null).isEmpty());
        assertTrue(FrTokenizer.words(null).isEmpty());
    }
}
