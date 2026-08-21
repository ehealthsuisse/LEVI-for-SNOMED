package ch.ehealth.levi.core.check;

import java.util.Locale;

/**
 * Factory for language-specific translation rule checkers. French is
 * implemented; German and Italian checkers can be plugged in here later.
 */
public final class TranslationRuleCheckers {

    private static final TranslationRuleChecker FRENCH = new FrenchTranslationRuleChecker();

    private TranslationRuleCheckers() {
    }

    public static TranslationRuleChecker forLanguage(String languageCode) {
        if (languageCode == null) {
            return null;
        }
        switch (languageCode.toLowerCase(Locale.ROOT)) {
            case "fr":
                return FRENCH;
            default:
                throw new UnsupportedOperationException(
                        "No translation rule checker implemented for language '" + languageCode + "'");
        }
    }
}