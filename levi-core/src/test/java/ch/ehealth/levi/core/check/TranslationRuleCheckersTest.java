package ch.ehealth.levi.core.check;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TranslationRuleCheckersTest {

    @Test
    public void testForLanguageFrReturnsWorkingChecker() {
        TranslationRuleChecker checker = TranslationRuleCheckers.forLanguage("fr");
        assertNotNull(checker);
        assertEquals("fr", checker.getLanguageCode());
        assertNotNull(checker.getRules());
        assertFalse(checker.getRules().isEmpty());
    }

    @Test
    public void testForLanguageReturnsFreshInstance() {
        TranslationRuleChecker first = TranslationRuleCheckers.forLanguage("fr");
        TranslationRuleChecker second = TranslationRuleCheckers.forLanguage("fr");
        assertNotNull(first);
        assertNotSame(first, second);
    }

    @Test
    public void testNullLanguageReturnsNull() {
        assertNull(TranslationRuleCheckers.forLanguage(null));
    }

    @Test
    public void testUnsupportedLanguageThrows() {
        assertThrows(UnsupportedOperationException.class, () -> TranslationRuleCheckers.forLanguage("xx"));
    }
}
