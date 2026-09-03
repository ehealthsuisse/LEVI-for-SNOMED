package ch.ehealth.levi.core.check;

import dumonts.hunspell.Hunspell;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hunspell-based spelling checker with an Ebene-1 lexicon.
 *
 * <p>Language-specific data lives in a per-language subdirectory named
 * {@code dictionary_<lang>} (e.g. {@code dictionary_fr}), both as bundled
 * resources under {@code /spelling/dictionary_<lang>/} and as an optional
 * external lexicon root. The external root is either the language subfolder
 * itself or a parent directory containing {@code dictionary_<lang>/} (in which
 * case the checker resolves the subfolder automatically).</p>
 */
public class HunspellSpellingChecker implements SpellingChecker {

    private static final Pattern DIGITS_AND_SYMBOLS = Pattern.compile("[\\d.,%°µμg/]+");
    private static final Pattern DU_A_PATTERN = Pattern.compile("\\bdu\\s+à\\b");
    private static final Pattern TYPO_LINE = Pattern.compile("^(.+)=(.+)$");

    private static final Set<String> UNITS = Set.of(
            "mg", "g", "kg", "ug", "ng", "mL", "L", "cm", "mm",
            "kPa", "Pa", "mmol", "umol", "mol", "IU", "UI", "U", "%",
            "ml", "l");

    private final String language;
    private final Hunspell hunspell;
    private final boolean available;

    // Ebene 1 lexicon: categorized allow-lists
    private final Set<String> allowListCi = new HashSet<>();
    private final Set<String> allowListCs = new HashSet<>();
    private final Map<String, String> typoMap = new HashMap<>();

    public HunspellSpellingChecker() {
        this("fr");
    }

    public HunspellSpellingChecker(String language) {
        this(language, null, null, lexiconRootFromProperty());
    }

    public HunspellSpellingChecker(Path extraAllowListFile) {
        this("fr", extraAllowListFile, null, lexiconRootFromProperty());
    }

    public HunspellSpellingChecker(Path extraAllowListFile, Path extraTypoFile) {
        this("fr", extraAllowListFile, extraTypoFile, lexiconRootFromProperty());
    }

    /**
     * @param extraAllowListFile optional external case-insensitive allow-list
     * @param extraTypoFile      optional external typo map (typo=correction)
     * @param lexiconRoot        optional lexicon root directory: either the
     *                           {@code dictionary_<lang>} folder directly, or a
     *                           parent directory containing it (produced by
     *                           {@link FrLexiconBuilder})
     */
    public HunspellSpellingChecker(Path extraAllowListFile, Path extraTypoFile, Path lexiconRoot) {
        this("fr", extraAllowListFile, extraTypoFile, lexiconRoot);
    }

    public HunspellSpellingChecker(String language, Path extraAllowListFile, Path extraTypoFile, Path lexiconRoot) {
        this.language = language == null || language.isBlank() ? "fr" : language.toLowerCase();
        String dictionaryDir = dictionaryDirName();

        Hunspell h = null;
        boolean ok = false;
        try {
            h = Hunspell.forDictionaryInResources(this.language, "/spelling/" + dictionaryDir + "/");
            ok = true;
        } catch (Exception e) {
            // Hunspell not available, degrade gracefully
        }
        this.hunspell = h;
        this.available = ok;

        String resBase = "/spelling/" + dictionaryDir + "/";
        loadResourceLines(resBase + this.language + "_allowlist.txt", allowListCi, true);
        loadResourceLines(resBase + this.language + "_inn.txt", allowListCi, true);
        loadResourceLines(resBase + this.language + "_latin.txt", allowListCi, true);
        loadResourceLines(resBase + this.language + "_eponyms.txt", allowListCs, false);
        loadResourceLines(resBase + this.language + "_taxons.txt", allowListCs, false);
        loadTypoMapFromResource(resBase + this.language + "_typos.txt");

        if (extraAllowListFile != null && Files.exists(extraAllowListFile)) {
            loadPathLines(extraAllowListFile, allowListCi, true);
        }
        if (extraTypoFile != null && Files.exists(extraTypoFile)) {
            loadTypoMapFromPath(extraTypoFile);
        }
        if (lexiconRoot != null) {
            loadLexiconRoot(lexiconRoot);
        }
    }

    private String dictionaryDirName() {
        return "dictionary_" + language;
    }

    /**
     * Reads the optional {@code levi.spelling.lexiconDir} system property (a
     * lexicon root) so a generated lexicon (see {@link FrLexiconBuilder}) is
     * picked up by the default construction path, e.g. when the translation
     * check runs.
     */
    private static Path lexiconRootFromProperty() {
        String dir = System.getProperty("levi.spelling.lexiconDir");
        return dir == null || dir.isBlank() ? null : Path.of(dir);
    }

    /**
     * Resolves the actual dictionary directory for this language from an
     * external root: prefers {@code <root>/dictionary_<lang>/}, falls back to
     * {@code <root>} directly (backward compatible with flat layouts).
     */
    private void loadLexiconRoot(Path root) {
        Path dir = root.resolve(dictionaryDirName());
        if (Files.isDirectory(dir)) {
            loadLexiconDir(dir);
        } else if (Files.isDirectory(root)) {
            loadLexiconDir(root);
        }
    }

    private void loadLexiconDir(Path dir) {
        loadPathLines(dir.resolve(language + "_allowlist.txt"), allowListCi, true);
        loadPathLines(dir.resolve(language + "_inn.txt"), allowListCi, true);
        loadPathLines(dir.resolve(language + "_latin.txt"), allowListCi, true);
        loadPathLines(dir.resolve(language + "_eponyms.txt"), allowListCs, false);
        loadPathLines(dir.resolve(language + "_taxons.txt"), allowListCs, false);
        Path typos = dir.resolve(language + "_typos.txt");
        if (Files.exists(typos)) {
            loadTypoMapFromPath(typos);
        }
    }

    private void loadResourceLines(String path, Set<String> target, boolean lowercase) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is != null) {
                loadLines(is, target, lowercase);
            }
        } catch (IOException e) {
            // ignore
        }
    }

    private void loadPathLines(Path file, Set<String> target, boolean lowercase) {
        if (!Files.exists(file)) {
            return;
        }
        try (InputStream is = Files.newInputStream(file)) {
            loadLines(is, target, lowercase);
        } catch (IOException e) {
            // ignore
        }
    }

    private void loadLines(InputStream is, Set<String> target, boolean lowercase) throws IOException {
        if (target == null) {
            return;
        }
        BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        r.lines().map(String::trim)
                .filter(l -> !l.isEmpty() && !l.startsWith("#"))
                .map(l -> lowercase ? l.toLowerCase() : l)
                .forEach(target::add);
    }

    private void loadTypoMapFromResource(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is != null) {
                loadTypoMap(is);
            }
        } catch (IOException e) {
            // ignore
        }
    }

    private void loadTypoMapFromPath(Path file) {
        try (InputStream is = Files.newInputStream(file)) {
            loadTypoMap(is);
        } catch (IOException e) {
            // ignore
        }
    }

    private void loadTypoMap(InputStream is) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        String line;
        while ((line = r.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            Matcher m = TYPO_LINE.matcher(line);
            if (m.matches()) {
                typoMap.put(m.group(1).toLowerCase(), m.group(2));
            }
        }
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    /** Tests membership in the Ebene 1 lexicon only (no Hunspell, no heuristics). */
    boolean isInLexicon(String token) {
        if (allowListCs.contains(token)) {
            return true;
        }
        return allowListCi.contains(token.toLowerCase());
    }

    @Override
    public boolean isCorrect(String token) {
        if (token == null || token.isBlank() || token.length() < 2) {
            return true;
        }
        if (DIGITS_AND_SYMBOLS.matcher(token).matches()) {
            return true;
        }
        if (isInLexicon(token)) {
            return true;
        }
        if (UNITS.contains(token)) {
            return true;
        }
        if (Character.isUpperCase(token.charAt(0))) {
            boolean allUpper = true;
            for (int i = 0; i < token.length(); i++) {
                char c = token.charAt(i);
                if (!Character.isUpperCase(c) && !Character.isDigit(c)) {
                    allUpper = false;
                    break;
                }
            }
            if (allUpper) {
                return true;
            }
        }
        if (available) {
            try {
                return hunspell.spell(token);
            } catch (Exception e) {
                return true;
            }
        }
        return true;
    }

    @Override
    public List<String> suggest(String token) {
        if (token == null || token.isBlank() || !available) {
            return List.of();
        }
        String[] arr = hunspell.suggest(token);
        return arr == null ? List.of() : List.of(arr);
    }

    @Override
    public List<SpellingIssue> check(String term) {
        List<SpellingIssue> result = new ArrayList<>();
        if (term == null || term.isBlank()) {
            return result;
        }
        String normalized = FrTokenizer.normalize(term);

        if ("fr".equals(language) && DU_A_PATTERN.matcher(normalized).find()) {
            result.add(new SpellingIssue("du \u00e0", "d\u00fb \u00e0", List.of("d\u00fb \u00e0"), true));
        }

        for (String token : FrTokenizer.tokens(normalized)) {
            if (token == null || token.isBlank() || token.length() < 2) {
                continue;
            }
            String lower = token.toLowerCase();
            String correction = typoMap.get(lower);
            if (correction != null) {
                result.add(new SpellingIssue(token, correction, List.of(correction), true));
                continue;
            }
            if (!isCorrect(token)) {
                List<String> suggestions = suggest(token);
                result.add(new SpellingIssue(token, null, suggestions, false));
            }
        }

        return result;
    }
}
