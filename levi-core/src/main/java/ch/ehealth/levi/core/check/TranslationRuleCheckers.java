package ch.ehealth.levi.core.check;

import java.util.Locale;

/**
 * Factory for language-specific translation rule checkers. French is
 * implemented; German and Italian checkers can be plugged in here later.
 *
 * <p>A fresh checker instance is returned per call so that the spelling
 * checker (and its lexicon directory, configurable at runtime) is created at
 * the time of use instead of being cached as a static singleton.</p>
 */
public final class TranslationRuleCheckers {

    private TranslationRuleCheckers() {
    }

    public static TranslationRuleChecker forLanguage(String languageCode) {
        if (languageCode == null) {
            return null;
        }
        switch (languageCode.toLowerCase(Locale.ROOT)) {
            case "fr":
                return new FrenchTranslationRuleChecker();
            default:
                throw new UnsupportedOperationException(
                        "No translation rule checker implemented for language '" + languageCode + "'");
        }
    }
}
