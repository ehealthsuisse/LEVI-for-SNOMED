package ch.ehealth.levi.core.check;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * French-aware text normalization and tokenization.
 *
 * <p>Applies Unicode NFC normalization, detects consecutive spaces, splits off
 * elision prefixes (l', d', s', n', m', t', c', j', qu', jusqu', lorsqu',
 * puisqu', quoiqu') and hyphenated compounds (cou-de-pied).</p>
 */
public final class FrTokenizer {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern MULTIPLE_SPACES = Pattern.compile("(?: {2,}|\\t)");
    private static final Pattern ELISION =
            Pattern.compile("^(?:l|d|s|n|m|t|c|j|qu|jusqu|lorsqu|puisqu|quoiqu)'");
    private static final Pattern TOKEN =
            Pattern.compile("[\\p{L}\\p{Nd}']+", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern COMPOUND_TOKEN =
            Pattern.compile("[\\p{L}\\p{Nd}'-]+", Pattern.UNICODE_CHARACTER_CLASS);

    private FrTokenizer() {
    }

    /** NFC-normalize the given text (no-op on null). */
    public static String normalize(String text) {
        return text == null ? "" : Normalizer.normalize(text, Normalizer.Form.NFC);
    }

    /** Split on whitespace, preserving original tokens (incl. punctuation). */
    public static List<String> words(String term) {
        List<String> result = new ArrayList<>();
        if (term == null || term.isBlank()) {
            return result;
        }
        for (String w : WHITESPACE.split(term.strip())) {
            if (!w.isEmpty()) {
                result.add(w);
            }
        }
        return result;
    }

    /**
     * Split into atomic word tokens for spelling checks: elision prefixes are
     * dropped and hyphenated compounds are split into their parts.
     */
    public static List<String> tokens(String term) {
        List<String> result = new ArrayList<>();
        for (String word : words(term)) {
            if ("aujourd'hui".equalsIgnoreCase(word)) {
                result.add(word);
                continue;
            }
            word = stripElision(word);
            for (String part : word.split("-")) {
                Matcher m = TOKEN.matcher(part);
                while (m.find()) {
                    result.add(m.group());
                }
            }
        }
        return result;
    }

    /**
     * Split keeping hyphenated compounds whole (for lexicon / frequency
     * lookups). Elision prefixes are still dropped.
     */
    public static List<String> compoundTokens(String term) {
        List<String> result = new ArrayList<>();
        for (String word : words(term)) {
            if ("aujourd'hui".equalsIgnoreCase(word)) {
                result.add(word);
                continue;
            }
            word = stripElision(word);
            Matcher m = COMPOUND_TOKEN.matcher(word);
            while (m.find()) {
                result.add(m.group());
            }
        }
        return result;
    }

    /** True if the text contains two or more consecutive spaces or tabs. */
    public static boolean hasMultipleSpaces(String term) {
        return term != null && MULTIPLE_SPACES.matcher(term).find();
    }

    private static String stripElision(String word) {
        Matcher el = ELISION.matcher(word);
        return el.find() ? word.substring(el.end()) : word;
    }
}
