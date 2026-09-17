package ch.ehealth.levi.core.check;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * German translation rule checker implementing the D-A-CH editorial rules and
 * verifying common phrase translations.
 */
public final class GermanTranslationRuleChecker implements TranslationRuleChecker {

    private static final Map<String, RuleMetadata> RULES = buildRules();

    private static final Pattern ARTICLE_START = Pattern.compile(
            "^(der|die|das|den|dem|des|ein|eine|einen|einem|eines|am|im|zum|zur|an der|an dem)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern ADVERBIAL_START = Pattern.compile(
            "^(mindestens|höchstens|zumindest|gegebenenfalls)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern MULTIPLE_SPACES = Pattern.compile(" {2,}|\\t");
    private static final Pattern STRAIGHT_APOSTROPHE = Pattern.compile("'");
    private static final Pattern CURLY_APOSTROPHE = Pattern.compile("[’‘‛`´]");
    private static final Pattern PERIOD_AT_END = Pattern.compile("\\.$");
    private static final Pattern PAREN_SPACE = Pattern.compile("\\(\\s|\\s\\)");
    private static final Pattern DECIMAL_POINT = Pattern.compile("\\d\\.\\d");
    private static final Pattern THOUSAND_SEPARATOR = Pattern.compile("\\d{1,3}[.,]\\d{3}(?:[.,]\\d{3})+");
    private static final Pattern COMMA_SPACE_BEFORE = Pattern.compile("\\s,\\s*");
    private static final Pattern COMMA_NO_SPACE_AFTER = Pattern.compile(",(?=[A-Za-zÄÖÜäöüß])");
    private static final Pattern SI_WORD = Pattern.compile(
            "\\b(meter|metre|gram|gramme|liter|litre|centimeter|millimeter|milliliter)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern NUMBER_UNIT_NO_SPACE = Pattern.compile(
            "\\b(\\d+(?:[.,]\\d+)?)(mg|g|kg|ug|ng|mmol|umol|mol|cm|mm|m|L|l|ml|kPa|Pa|U|IU|%|‰)\\b");
    private static final Pattern NUMBER_UNIT_SPACE = Pattern.compile(
            "\\b\\d+(?:[.,]\\d+)? (mg|g|kg|ug|ng|mmol|umol|mol|cm|mm|m|L|ml|kPa|Pa|U|IU)\\b");
    private static final Pattern PERCENT_NO_SPACE = Pattern.compile("\\d+(?:[.,]\\d+)?%");
    private static final Pattern PERCENT_SPACE = Pattern.compile("\\d+(?:[.,]\\d+)? %");
    private static final Pattern MICRO_SYMBOL = Pattern.compile("µ");
    private static final Pattern MICRO_WITH_U = Pattern.compile("\\b\\d+ ?ug\\b");
    private static final Pattern SLASH_WITH_SPACES = Pattern.compile("\\s/\\s");
    private static final Pattern WORD_SLASH_WORD = Pattern.compile("[A-Za-zÄÖÜäöüß]+/[A-Za-zÄÖÜäöüß]+");
    private static final Pattern FRACTION = Pattern.compile("\\b[0-9]+/[0-9]+\\b");
    private static final Pattern COMPARISON_SYMBOL = Pattern.compile("[<>]");
    private static final Pattern EN_HISTORY = Pattern.compile("history", Pattern.CASE_INSENSITIVE);
    private static final Pattern EN_INPATIENT = Pattern.compile("inpatient", Pattern.CASE_INSENSITIVE);
    private static final Pattern EN_OUTPATIENT = Pattern.compile("outpatient", Pattern.CASE_INSENSITIVE);
    private static final Pattern GERMAN_HISTORY = Pattern.compile("Vorgeschichte:|Zustand nach|Anamnese",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern GERMAN_INPATIENT = Pattern.compile("stationär", Pattern.CASE_INSENSITIVE);
    private static final Pattern GERMAN_OUTPATIENT = Pattern.compile("ambulant", Pattern.CASE_INSENSITIVE);

    private final SpellingChecker spellingChecker;
    private final List<PhraseRule> phraseRules;

    public GermanTranslationRuleChecker() {
        this(new HunspellSpellingChecker("de"));
    }

    public GermanTranslationRuleChecker(SpellingChecker spellingChecker) {
        this.spellingChecker = spellingChecker != null ? spellingChecker : new HunspellSpellingChecker("de");
        this.phraseRules = loadPhraseRules();
    }

    private static Map<String, RuleMetadata> buildRules() {
        Map<String, RuleMetadata> map = new LinkedHashMap<>();
        map.put("ss1", new RuleMetadata("ss1", "error", 1, "4.1",
                "German PT/FSN must start uppercase unless symbol/code"));
        map.put("ss4", new RuleMetadata("ss4", "error", 1, "4.1",
                "Follow German syntax and avoid consecutive spaces"));
        map.put("ar2", new RuleMetadata("ar2", "error", 1, "4.3",
                "Remove definite/indefinite articles at term start"));
        map.put("se1", new RuleMetadata("se1", "warning", 1, "4.10",
                "Comma attaches to previous word and is followed by space"));
        map.put("se3", new RuleMetadata("se3", "warning", 1, "4.10",
                "Slash reserved for fractions (mg/L) or 'und/oder'"));
        map.put("se9", new RuleMetadata("se9", "error", 1, "4.10",
                "Spaces belong outside parentheses"));
        map.put("se10", new RuleMetadata("se10", "error", 1, "4.10",
                "Use straight apostrophe U+0027"));
        map.put("se11", new RuleMetadata("se11", "error", 1, "4.10",
                "No trailing period"));
        map.put("sc3", new RuleMetadata("sc3", "error", 1, "4.7",
                "Use decimal comma"));
        map.put("sc4", new RuleMetadata("sc4", "error", 1, "4.7",
                "Separate thousands with space"));
        map.put("sc6", new RuleMetadata("sc6", "error", 1, "4.7",
                "Write comparison operators out"));
        map.put("um1", new RuleMetadata("um1", "error", 1, "4.6",
                "Use SI abbreviations"));
        map.put("um2", new RuleMetadata("um2", "error", 1, "4.6",
                "Write 'Grad Celsius' in full"));
        map.put("um4", new RuleMetadata("um4", "warning", 1, "4.6",
                "Use uppercase L for litre"));
        map.put("um5", new RuleMetadata("um5", "error", 1, "4.6",
                "Insert space between number and unit"));
        map.put("um6", new RuleMetadata("um6", "error", 1, "4.6",
                "Insert space before %"));
        map.put("um7", new RuleMetadata("um7", "error", 1, "4.6",
                "Avoid Unicode superscripts"));
        map.put("um8", new RuleMetadata("um8", "error", 1, "4.6",
                "Use letter 'u' instead of µ"));
        map.put("hs1", new RuleMetadata("hs1", "error", 1, "4.21",
                "Translate 'history' as 'Vorgeschichte:' or 'Zustand nach'"));
        map.put("en4", new RuleMetadata("en4", "error", 1, "4.22",
                "Translate 'Inpatient [X]' with 'stationär' phrasing"));
        map.put("en5", new RuleMetadata("en5", "warning", 1, "4.22",
                "Translate 'Outpatient [X]' with 'ambulant' phrasing"));
        map.put("cp1", new RuleMetadata("cp1", "info", 1, "Common phrases",
                "English common phrase missing expected German rendering"));
        map.put("sp1", new RuleMetadata("sp1", "warning", 1, "Spelling",
                "Possible German spelling issue"));
        return map;
    }

    private static record PhraseRule(String english,
                                     Set<String> keywords,
                                     Set<String> germanVariants) {

        boolean matches(String englishText) {
            String lower = englishText.toLowerCase(Locale.ROOT);
            if (keywords.isEmpty()) {
                return lower.contains(english.toLowerCase(Locale.ROOT));
            }
            for (String kw : keywords) {
                if (!lower.contains(kw)) {
                    return false;
                }
            }
            return true;
        }

        boolean satisfiedBy(String germanLower) {
            for (String variant : germanVariants) {
                if (germanLower.contains(variant)) {
                    return true;
                }
            }
            return false;
        }
    }

    private List<PhraseRule> loadPhraseRules() {
        InputStream is = getClass().getResourceAsStream("/dach/common_phrases.tsv");
        if (is == null) {
            return Collections.emptyList();
        }
        List<PhraseRule> rules = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line = reader.readLine(); // header
            while ((line = reader.readLine()) != null) {
                String trimmed = line.strip();
                if (trimmed.isEmpty()) {
                    continue;
                }
                String[] cols = trimmed.split("\t");
                if (cols.length < 2) {
                    continue;
                }
                Set<String> keywords = parseKeywords(cols[0]);
                Set<String> variants = parseVariants(cols[1]);
                if (variants.isEmpty()) {
                    continue;
                }
                rules.add(new PhraseRule(cols[0], keywords, variants));
            }
        } catch (IOException ignored) {
        }
        return rules;
    }

    private Set<String> parseKeywords(String englishColumn) {
        java.util.LinkedHashSet<String> set = new java.util.LinkedHashSet<>();
        for (String part : englishColumn.split("\\|")) {
            String normalized = part.strip().toLowerCase(Locale.ROOT);
            if (!normalized.isEmpty()) {
                set.add(normalized);
            }
        }
        return set;
    }

    private Set<String> parseVariants(String germanColumn) {
        java.util.LinkedHashSet<String> set = new java.util.LinkedHashSet<>();
        for (String part : germanColumn.split("\\|")) {
            String normalized = part.strip().toLowerCase(Locale.ROOT);
            if (!normalized.isEmpty()) {
                set.add(normalized);
            }
        }
        return set;
    }

    @Override
    public String getLanguageCode() {
        return "de";
    }

    @Override
    public Map<String, RuleMetadata> getRules() {
        return RULES;
    }

    @Override
    public List<Finding> check(TranslationCheckContext ctx) {
        String rawTerm = ctx.term() == null ? "" : ctx.term();
        String term = DeTokenizer.normalize(rawTerm).strip();
        List<Finding> findings = new ArrayList<>();
        if (term.isBlank()) {
            return findings;
        }

        checkCapitalization(term, findings);
        checkArticlesAndWhitespace(term, findings);
        checkPunctuation(term, findings);
        checkUnits(term, findings);
        checkSlashUsage(term, findings);
        checkComparison(term, findings);
        checkSpelling(term, findings);
        applyContextualRules(ctx, term, findings);
        applyPhraseRules(ctx, term, findings);

        return findings;
    }

    private void checkCapitalization(String term, List<Finding> findings) {
        char first = term.charAt(0);
        if (Character.isLetter(first) && Character.isLowerCase(first)) {
            findings.add(find("ss1", "fail",
                    "German PT/FSN must start with uppercase letter"));
        }
    }

    private void checkArticlesAndWhitespace(String term, List<Finding> findings) {
        if (!ADVERBIAL_START.matcher(term).find() && ARTICLE_START.matcher(term).find()) {
            String article = term.split("\\s+")[0];
            findings.add(find("ar2", "fail",
                    "Remove article at term start ('" + article + "')"));
        }
        if (MULTIPLE_SPACES.matcher(term).find()) {
            findings.add(find("ss4", "fail",
                    "Multiple consecutive spaces"));
        }
    }

    private void checkPunctuation(String term, List<Finding> findings) {
        if (CURLY_APOSTROPHE.matcher(term).find()) {
            findings.add(find("se10", "fail",
                    "Use straight apostrophe U+0027"));
        } else if (STRAIGHT_APOSTROPHE.matcher(term).find()) {
            findings.add(find("se10", "pass",
                    "Straight apostrophe used"));
        }
        if (COMMA_SPACE_BEFORE.matcher(term).find()) {
            findings.add(find("se1", "fail",
                    "Remove space before comma"));
        }
        if (COMMA_NO_SPACE_AFTER.matcher(term).find()) {
            findings.add(find("se1", "fail",
                    "Insert space after comma"));
        }
        if (PAREN_SPACE.matcher(term).find()) {
            findings.add(find("se9", "fail",
                    "Spaces belong outside parentheses"));
        }
        if (PERIOD_AT_END.matcher(term).find()) {
            findings.add(find("se11", "fail",
                    "Remove trailing period"));
        }
        if (DECIMAL_POINT.matcher(term).find()) {
            findings.add(find("sc3", "fail",
                    "Use decimal comma"));
        }
        if (THOUSAND_SEPARATOR.matcher(term).find()) {
            findings.add(find("sc4", "fail",
                    "Use space as thousands separator"));
        }
    }

    private void checkUnits(String term, List<Finding> findings) {
        if (SI_WORD.matcher(term).find()) {
            findings.add(find("um1", "fail",
                    "Use SI abbreviations"));
        }
        if (MICRO_SYMBOL.matcher(term).find()) {
            findings.add(find("um8", "fail",
                    "Replace µ with letter 'u'"));
        } else if (MICRO_WITH_U.matcher(term).find()) {
            findings.add(find("um8", "pass",
                    "Micro symbol written as 'u'"));
        }
        if (NUMBER_UNIT_NO_SPACE.matcher(term).find()) {
            findings.add(find("um5", "fail",
                    "Insert space between number and unit"));
        } else if (NUMBER_UNIT_SPACE.matcher(term).find()) {
            findings.add(find("um5", "pass",
                    "Spacing between number and unit is correct"));
        }
        if (PERCENT_NO_SPACE.matcher(term).find()) {
            findings.add(find("um6", "fail",
                    "Add space before %"));
        } else if (PERCENT_SPACE.matcher(term).find()) {
            findings.add(find("um6", "pass",
                    "Space before % is correct"));
        }
    }

    private void checkSlashUsage(String term, List<Finding> findings) {
        if (SLASH_WITH_SPACES.matcher(term).find()) {
            findings.add(findWarning("se3", "uncertain",
                    "Remove spaces around '/'"));
        }
        if (WORD_SLASH_WORD.matcher(term).find() && !FRACTION.matcher(term).find()) {
            findings.add(findWarning("se3", "uncertain",
                    "Slash between words detected; consider rewriting as conjunction"));
        }
    }

    private void checkComparison(String term, List<Finding> findings) {
        if (COMPARISON_SYMBOL.matcher(term).find()) {
            findings.add(find("sc6", "fail",
                    "Write comparison operators out"));
        }
    }

    private void checkSpelling(String term, List<Finding> findings) {
        try {
            for (SpellingIssue issue : spellingChecker.check(term)) {
                if (issue.definiteTypo()) {
                    findings.add(findWarning("sp1", "fail",
                            "Spelling error: '" + issue.token() + "' → '" + issue.correction() + "'"));
                } else if (issue.correction() == null) {
                    findings.add(findInfo("sp1", "uncertain",
                            "Potential typo: '" + issue.token() + "'"));
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void applyContextualRules(TranslationCheckContext ctx, String term, List<Finding> findings) {
        String english = ((ctx.fsn() == null ? "" : ctx.fsn()) + " "
                + (ctx.pt() == null ? "" : ctx.pt())).toLowerCase(Locale.ROOT);
        if (english.isBlank()) {
            return;
        }
        if (EN_HISTORY.matcher(english).find() && !GERMAN_HISTORY.matcher(term).find()) {
            findings.add(find("hs1", "fail",
                    "Translate 'history' with 'Vorgeschichte:' or 'Zustand nach'"));
        }
        if (EN_INPATIENT.matcher(english).find() && !GERMAN_INPATIENT.matcher(term).find()) {
            findings.add(find("en4", "fail",
                    "Add 'stationär' phrasing for 'Inpatient' terms"));
        }
        if (EN_OUTPATIENT.matcher(english).find() && !GERMAN_OUTPATIENT.matcher(term).find()) {
            findings.add(find("en5", "uncertain",
                    "Add 'ambulant' phrasing for 'Outpatient' terms"));
        }
    }

    private void applyPhraseRules(TranslationCheckContext ctx, String term, List<Finding> findings) {
        if (phraseRules.isEmpty()) {
            return;
        }
        String english = ((ctx.fsn() == null ? "" : ctx.fsn()) + " "
                + (ctx.pt() == null ? "" : ctx.pt())).toLowerCase(Locale.ROOT);
        if (english.isBlank()) {
            return;
        }
        String germanLower = term.toLowerCase(Locale.ROOT);
        for (PhraseRule rule : phraseRules) {
            if (rule.matches(english) && !rule.satisfiedBy(germanLower)) {
                findings.add(findInfo("cp1", "uncertain",
                        "English phrase '" + rule.english + "' expects German variant(s): "
                                + String.join(", ", rule.germanVariants)));
            }
        }
    }

    private Finding find(String ruleId, String status, String message) {
        RuleMetadata metadata = RULES.getOrDefault(ruleId, new RuleMetadata(ruleId, "warning", 0, "", ""));
        return new Finding(ruleId, metadata.severity(), status, message, metadata.pdfPage(), metadata.section());
    }

    private Finding findWarning(String ruleId, String status, String message) {
        RuleMetadata metadata = RULES.getOrDefault(ruleId, new RuleMetadata(ruleId, "warning", 0, "", ""));
        return new Finding(ruleId, "warning", status, message, metadata.pdfPage(), metadata.section());
    }

    private Finding findInfo(String ruleId, String status, String message) {
        RuleMetadata metadata = RULES.getOrDefault(ruleId, new RuleMetadata(ruleId, "info", 0, "", ""));
        return new Finding(ruleId, "info", status, message, metadata.pdfPage(), metadata.section());
    }
}
