package ch.ehealth.levi.core.check;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Bootstraps an German lexicon from the SNOMED CT database. Extracts all
 * active German descriptions with their acceptability, tokenizes them,
 * computes token frequencies and categorizes tokens. High-frequency tokens
 * (&gt;= {@link #FREQ_THRESHOLD}) are valid vocabulary candidates; low-frequency
 * tokens (1-2) form a suspect list for manual curation.
 */
public final class DeLexiconBuilder {

    public static final long FREQ_THRESHOLD = 3;

    public static final String PREFERRED_ACCEPTABILITY = "900000000000548007";
    public static final String ACCEPTABLE_ACCEPTABILITY = "900000000000549004";

    // Category labels
    public static final String CAT_DE = "de";
    public static final String CAT_INN = "inn";
    public static final String CAT_LATIN = "latin";
    public static final String CAT_EPONYM = "eponym";
    public static final String CAT_TAXON = "taxon";
    public static final String CAT_ACRONYM = "acronym";
    public static final String CAT_UNKNOWN = "unknown";

    private static final Pattern INN_SUFFIX = Pattern.compile(
            ".*(?:abine|afil|amine|azole|ciclib|conazole|dronate|entan|grel|ine$|lukast|mab$|metacin|"
                    + "mycin|nib$|olol|onium|pamil|pril|profen|sartan|stat$|tecan|tinib|triptan|vir$|zolam)$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern LATIN_SUFFIX = Pattern.compile(
            ".*(?:[aei]us$|[aei]um$|itis$|oma$|osis$|iasis$|[ae]ae$|[aei]is$|[aeo]x$|or$|alis$|aris$|ans$|ens$|"
                    + "ite$|iasi$|oma$|opatia$|ectomia$|scopia$|grafia$|tomia$|stomia$|rragia$|rrea$|litica?$)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ACRONYM_RE = Pattern.compile("^[A-Z0-9]{2,}$");
    private static final Pattern CAPITALIZED = Pattern.compile("^[A-Z]");
    private static final Pattern LOWERCASE_LETTERS = Pattern.compile("^[\\p{L}]+$");

    private DeLexiconBuilder() {
    }

    /** A single German description as loaded from the database. */
    public record DeDescription(String term, String conceptId, String acceptability) {
    }

    /** Token frequency statistics computed from a collection of terms. */
    public record DeLexiconResult(
            List<DeDescription> descriptions,
            Map<String, Long> frequencies,
            Map<String, Long> termCounts,
            Map<String, Long> capitalizedCounts,
            Map<String, Long> lowercaseCounts,
            Map<String, String> categories,
            Map<String, String> originalForms,
            Map<String, DeDescription> examples) {

        /** Tokens appearing in at least {@link #FREQ_THRESHOLD} terms. */
        public Set<String> highFrequencyTokens() {
            Set<String> set = new TreeSet<>();
            frequencies.forEach((tok, freq) -> {
                if (freq >= FREQ_THRESHOLD) {
                    set.add(tok);
                }
            });
            return set;
        }

        /** Tokens appearing in 1-2 terms only (suspect list). */
        public Set<String> lowFrequencyTokens() {
            Set<String> set = new TreeSet<>();
            frequencies.forEach((tok, freq) -> {
                if (freq < FREQ_THRESHOLD) {
                    set.add(tok);
                }
            });
            return set;
        }
    }

    /**
     * Pure, DB-independent analysis of a collection of German terms.
     */
    public static DeLexiconResult analyze(Collection<String> terms) {
        List<DeDescription> descriptions = new ArrayList<>();
        for (String term : terms) {
            if (term != null && !term.isBlank()) {
                descriptions.add(new DeDescription(term, "", ""));
            }
        }
        return analyzeDescriptions(descriptions);
    }

    /**
     * Pure analysis of a collection of German descriptions.
     */
    public static DeLexiconResult analyzeDescriptions(Collection<DeDescription> descriptions) {
        Map<String, Long> frequencies = new HashMap<>();
        Map<String, Long> termCounts = new HashMap<>();
        Map<String, Long> capitalizedCounts = new HashMap<>();
        Map<String, Long> lowercaseCounts = new HashMap<>();
        Map<String, String> originalForms = new HashMap<>();
        Map<String, DeDescription> examples = new HashMap<>();
        Set<String> taxonLike = new HashSet<>();

        List<DeDescription> seenList = new ArrayList<>(descriptions);
        for (DeDescription desc : seenList) {
            if (desc == null || desc.term() == null || desc.term().isBlank()) {
                continue;
            }

            List<String> tokens = DeTokenizer.tokens(desc.term());
            Set<String> seen = new java.util.HashSet<>();
            for (int i = 0; i < tokens.size(); i++) {
                String token = tokens.get(i);
                if (token.length() < 2) {
                    continue;
                }
                String lower = token.toLowerCase();
                frequencies.merge(lower, 1L, Long::sum);
                originalForms.putIfAbsent(lower, token);
                if (seen.add(lower)) {
                    termCounts.merge(lower, 1L, Long::sum);
                }
                if (CAPITALIZED.matcher(token).find()) {
                    capitalizedCounts.merge(lower, 1L, Long::sum);
                    if (i + 1 < tokens.size() && LOWERCASE_LETTERS.matcher(tokens.get(i + 1)).matches()
                            && !CAPITALIZED.matcher(tokens.get(i + 1)).find()) {
                        taxonLike.add(lower);
                    }
                } else {
                    lowercaseCounts.merge(lower, 1L, Long::sum);
                }
                examples.putIfAbsent(lower, desc);
            }
        }

        Map<String, String> categories = new HashMap<>();
        for (String token : frequencies.keySet()) {
            String original = originalForms.get(token);
            long caps = capitalizedCounts.getOrDefault(token, 0L);
            long lowers = lowercaseCounts.getOrDefault(token, 0L);
            boolean mostlyCapitalized = caps > lowers && caps > 0;
            categories.put(token, classify(original, mostlyCapitalized, taxonLike.contains(token)));
        }

        return new DeLexiconResult(seenList, frequencies, termCounts, capitalizedCounts, lowercaseCounts,
                categories, originalForms, examples);
    }

    /**
     * Heuristic category for a token.
     */
    public static String classify(String originalToken, boolean mostlyCapitalized, boolean taxonLike) {
        if (originalToken != null && ACRONYM_RE.matcher(originalToken).matches()) {
            return CAT_ACRONYM;
        }
        if (taxonLike && mostlyCapitalized) {
            return CAT_TAXON;
        }
        if (mostlyCapitalized && originalToken != null) {
            return CAT_EPONYM;
        }
        if (INN_SUFFIX.matcher(originalToken == null ? "" : originalToken).matches()) {
            return CAT_INN;
        }
        if (LATIN_SUFFIX.matcher(originalToken == null ? "" : originalToken).matches()) {
            return CAT_LATIN;
        }
        return CAT_UNKNOWN;
    }

    /**
     * Loads the latest active German descriptions from the database.
     */
    public static DeLexiconResult build(Connection conn, String languageCode, String refsetId) throws SQLException {
        String sql = """
                    SELECT t.term, t.conceptId, l.acceptabilityId
                    FROM (
                        SELECT id, term, conceptId, languageCode, active,
                               ROW_NUMBER() OVER (PARTITION BY id ORDER BY effectiveTime DESC) AS rn
                        FROM full_description
                        WHERE languageCode = ?
                    ) t
                    LEFT JOIN full_refset_Language l
                      ON l.referencedComponentId = t.id
                     AND l.active = 1
                     AND l.refsetId = ?
                    WHERE t.rn = 1 AND t.active = 1
                """;

        List<DeDescription> descriptions = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, languageCode);
            ps.setString(2, refsetId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    descriptions.add(new DeDescription(
                            rs.getString("term"),
                            rs.getString("conceptId"),
                            rs.getString("acceptabilityId")));
                }
            }
        }
        return analyzeDescriptions(descriptions);
    }

    private static final Map<String, String> CATEGORY_FILES = Map.of(
            CAT_INN, "inn.txt",
            CAT_LATIN, "latin.txt",
            CAT_EPONYM, "eponyms.txt",
            CAT_TAXON, "taxons.txt",
            CAT_ACRONYM, "acronyms.txt");

    /**
     * Writes the categorized output files into {@code <outDir>/dictionary_<lang>/}.
     */
    public static Path writeOutputs(DeLexiconResult result, Path outDir, SpellingChecker spellcheck)
            throws IOException {
        return writeOutputs(result, outDir, "de", spellcheck);
    }

    public static Path writeOutputs(DeLexiconResult result, Path outDir, String language,
            SpellingChecker spellcheck) throws IOException {
        String lang = language == null || language.isBlank() ? "de" : language.toLowerCase();
        Path dictDir = outDir.resolve("dictionary_" + lang);
        Files.createDirectories(dictDir);
        Path allowPath = dictDir.resolve(lang + "_allowlist.txt");

        Map<String, List<String>> categoryFiles = new java.util.TreeMap<>();
        List<String> allowLines = new ArrayList<>();
        for (String token : result.highFrequencyTokens()) {
            if (spellcheck != null && spellcheck.isAvailable() && spellcheck.isCorrect(token)) {
                continue;
            }
            String category = result.categories().get(token);
            if (CAT_INN.equals(category) || CAT_LATIN.equals(category) || CAT_EPONYM.equals(category)
                    || CAT_TAXON.equals(category) || CAT_ACRONYM.equals(category)) {
                categoryFiles.computeIfAbsent(category, k -> new ArrayList<>()).add(token);
            } else {
                allowLines.add(token);
            }
        }
        Files.write(allowPath, allowLines, StandardCharsets.UTF_8);
        for (Map.Entry<String, List<String>> e : categoryFiles.entrySet()) {
            String suffix = CATEGORY_FILES.get(e.getKey());
            if (suffix == null) {
                continue;
            }
            boolean caseSensitive = CAT_EPONYM.equals(e.getKey()) || CAT_TAXON.equals(e.getKey());
            List<String> lines = caseSensitive
                    ? e.getValue().stream()
                            .map(t -> result.originalForms().getOrDefault(t, t))
                            .sorted()
                            .toList()
                    : e.getValue().stream().map(String::toLowerCase).sorted().toList();
            Files.write(dictDir.resolve(lang + "_" + suffix), lines, StandardCharsets.UTF_8);
        }

        Path suspectPath = dictDir.resolve(lang + "_suspects.tsv");
        List<String> suspectLines = new ArrayList<>();
        suspectLines.add("token\tfreq\tterms\tcategory\texampleTerm\tconceptId\tacceptability");
        for (String token : result.lowFrequencyTokens()) {
            DeDescription ex = result.examples().get(token);
            suspectLines.add(String.join("\t",
                    token,
                    String.valueOf(result.frequencies().get(token)),
                    String.valueOf(result.termCounts().get(token)),
                    result.categories().get(token),
                    ex == null ? "" : ex.term(),
                    ex == null ? "" : ex.conceptId(),
                    ex == null ? "" : ex.acceptability()));
        }
        suspectLines.sort(Comparator.naturalOrder());
        Files.write(suspectPath, suspectLines, StandardCharsets.UTF_8);

        Path statsPath = dictDir.resolve(lang + "_stats.txt");
        StringBuilder stats = new StringBuilder();
        stats.append("descriptions\t").append(result.descriptions().size()).append('\n');
        stats.append("uniqueTokens\t").append(result.frequencies().size()).append('\n');
        stats.append("highFrequencyTokens\t").append(result.highFrequencyTokens().size()).append('\n');
        stats.append("lowFrequencyTokens\t").append(result.lowFrequencyTokens().size()).append('\n');
        result.categories().entrySet().stream()
                .collect(java.util.stream.Collectors.groupingBy(Map.Entry::getValue, LinkedHashMap::new,
                        java.util.stream.Collectors.counting()))
                .forEach((cat, count) -> stats.append("category.").append(cat).append('\t').append(count).append('\n'));
        Files.write(statsPath, stats.toString().getBytes(StandardCharsets.UTF_8));

        return allowPath;
    }

    /**
     * CLI entry point:
     *
     * <pre>
     * DeLexiconBuilder &lt;dbUrl&gt; &lt;user&gt; &lt;password&gt; &lt;languageCode&gt; &lt;refsetId&gt; &lt;outDir&gt;
     * </pre>
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 6) {
            System.err.println(
                    "Usage: DeLexiconBuilder <dbUrl> <user> <password> <languageCode> <refsetId> <outDir>");
            System.exit(1);
        }
        String dbUrl = args[0];
        String user = args[1];
        String password = args[2];
        String languageCode = args[3];
        String refsetId = args[4];
        Path outDir = Path.of(args[5]);

        long start = System.currentTimeMillis();
        try (Connection conn = java.sql.DriverManager.getConnection(dbUrl, user, password)) {
            DeLexiconResult result = build(conn, languageCode, refsetId);
            Path allow = writeOutputs(result, outDir, languageCode, null);
            long seconds = (System.currentTimeMillis() - start) / 1000;
            System.out.println("Processed " + result.descriptions().size() + " descriptions in " + seconds + "s");
            System.out.println("Allow-list written to " + allow);
            System.out.println("Stats written to " + outDir.resolve("dictionary_" + languageCode)
                    + "/" + languageCode + "_stats.txt");
            System.out.println("Pass " + outDir + " as lexicon root to HunspellSpellingChecker.");
        }
    }
}