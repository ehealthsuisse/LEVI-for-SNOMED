package ch.ehealth.levi.core.check;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Italian translation rule checker, ported and adapted from the French
 * reference implementation. Covers general style, orthography, punctuation,
 * units, scientific notation, and hierarchy-specific naming conventions for
 * Italian SNOMED CT translations.
 */
public class ItalianTranslationRuleChecker implements TranslationRuleChecker {

    private static final Map<String, RuleMetadata> RULES = buildRules();
    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");

    // Fix patterns: false-positive reduction
    private static final Pattern CHEM_LOCANT = Pattern.compile("\\b[A-Z],[A-Z]'?");
    private static final Pattern CHEM_DIGIT = Pattern.compile("\\b\\d,\\d\\b");
    private static final Pattern KARYOTYPE = Pattern.compile("\\b\\d{1,2},[XY](?!\\w)");
    private static final Pattern CYTOBAND = Pattern.compile("\\b[0-9XY]+[pq][0-9]+(?:\\.[0-9]+)+\\b");
    private static final Pattern MT_MUTATION = Pattern.compile("(?:m\\.\\d+\\s*)?[ATCG]>[ATCG]");
    private static final Pattern SEROTYPE_L = Pattern.compile("\\b(?:sierotipo|serotype|tipo|gruppo|sierovar)\\s+\\d+[Ll]\\b");

    private final SpellingChecker spellingChecker;

    public ItalianTranslationRuleChecker() {
        this(new HunspellSpellingChecker("it"));
    }

    public ItalianTranslationRuleChecker(SpellingChecker spellingChecker) {
        this.spellingChecker = spellingChecker != null ? spellingChecker : new HunspellSpellingChecker("it");
    }

    private static Map<String, RuleMetadata> buildRules() {
        Map<String, RuleMetadata> m = new LinkedHashMap<>();
        m.put("ss1", new RuleMetadata("ss1", "error", 1, "4.1",
                "Termine in minuscole, tranne nomi propri, simboli, codici, sigle, acronimi o taxon."));
        m.put("ss4", new RuleMetadata("ss4", "error", 1, "4.1",
                "La terminologia medica rispetta la sintassi corrente dell'italiano."));
        m.put("ar2", new RuleMetadata("ar2", "error", 1, "4.3",
                "Nessun articolo all'inizio del termine. Esempio: sindrome epatorenale (non \"la sindrome epatorenale\")"));
        m.put("se1", new RuleMetadata("se1", "warning", 1, "4.10",
                "La virgola è attaccata alla parola che la precede e seguita da uno spazio."));
        m.put("se9", new RuleMetadata("se9", "error", 1, "4.10",
                "Gli spazi sono posti all'esterno delle parentesi, non all'interno."));
        m.put("se10", new RuleMetadata("se10", "error", 1, "4.10",
                "L'apostrofo retto verticale U+0027 è l'unico carattere consentito."));
        m.put("se11", new RuleMetadata("se11", "error", 1, "4.10",
                "Il punto (U+002E) non deve apparire in ultima posizione del termine, salvo eccezioni."));
        m.put("sc3", new RuleMetadata("sc3", "error", 1, "4.7",
                "Il separatore decimale in italiano è la virgola. Esempio: 1,5 mg"));
        m.put("sc4", new RuleMetadata("sc4", "error", 1, "4.7",
                "Il separatore delle migliaia è lo spazio. Esempio: 100 000 000"));
        m.put("sc6", new RuleMetadata("sc6", "error", 1, "4.7",
                "I simboli '<' (minore di) e '>' (maggiore di) sono sostituiti dall'espressione in chiaro."));
        m.put("um1", new RuleMetadata("um1", "error", 1, "4.6",
                "Le unità di misura sono abbreviate secondo le regole del SI (m = metro; s = secondo; Pa = pascal)."));
        m.put("um2", new RuleMetadata("um2", "error", 1, "4.6",
                "Le unità di temperatura sono espresse in forma sviluppata: gradi Celsius."));
        m.put("um4", new RuleMetadata("um4", "warning", 1, "4.6",
                "L'abbreviazione di \"litro\" è la lettera maiuscola L (mL, /L)."));
        m.put("um5", new RuleMetadata("um5", "error", 1, "4.6",
                "Uno spazio separa il numero dall'unità di misura. Esempio: 0,75 g"));
        m.put("um6", new RuleMetadata("um6", "error", 1, "4.6",
                "\"Percent\" e \"%\" si traducono con il simbolo %. Uno spazio separa la quantità dal simbolo. Esempio: 75 %"));
        m.put("um7", new RuleMetadata("um7", "error", 1, "4.6",
                "Gli esponenti non sono esplicitati da un simbolo particolare; il numero in esponente è accostato al simbolo (mm3)."));
        m.put("um8", new RuleMetadata("um8", "error", 1, "4.6",
                "La lettera µ che significa micro in un'unità è sostituita dalla lettera u. Esempio: umol/L"));
        m.put("sc2", new RuleMetadata("sc2", "error", 1, "4.7",
                "La scrittura dei numeri privilegia le cifre arabe (vitamina K2; diabete di tipo 1)."));
        m.put("sc5", new RuleMetadata("sc5", "error", 1, "4.7",
                "Gli indici non sono esplicitati da un simbolo particolare: il numero è accostato (IgA2)."));
        m.put("sc7", new RuleMetadata("sc7", "warning", 1, "4.7",
                "Il simbolo + è usato per codici o risultati di test (Na+; gruppo A+; glicosuria = +++)."));
        m.put("sc8", new RuleMetadata("sc8", "warning", 1, "4.7",
                "Un aggettivo numerale ordinale è espresso in lettere nel termine preferito (PT: quinta malattia; SYN: 5\u00AA malattia)."));
        m.put("ab1", new RuleMetadata("ab1", "error", 1, "4.5",
                "Evitare le abbreviazioni e scrivere le parole per intero (salvo abbreviazioni consolidate dall'uso)."));
        m.put("ab2", new RuleMetadata("ab2", "error", 1, "4.5",
                "L'acronimo è seguito dalla forma sviluppata con trattino semplice tra due spazi o tra parentesi."));
        m.put("ab3", new RuleMetadata("ab3", "error", 1, "4.5",
                "I sigle e acronimi comuni si lessicalizzano; si scrivono in minuscolo (laser)."));
        m.put("me1", new RuleMetadata("me1", "warning", 1, "4.14",
                "prodotto contenente sostanza (e sostanza), le sostanze sono elencate senza articolo, in ordine alfabetico."));
        m.put("me2", new RuleMetadata("me2", "warning", 1, "4.14",
                "prodotto contenente soltanto sostanza (e sostanza), senza articolo."));
        m.put("me3", new RuleMetadata("me3", "warning", 1, "4.14",
                "prodotto contenente precisamente {sostanza} {dosaggio} [e {sostanza} {dosaggio} ...] per {forma farmaceutica}."));
        m.put("ec1", new RuleMetadata("ec1", "error", 1, "4.15",
                "Il concetto 123038009 |Specimen (specimen)| ha come termine preferito italiano \"campione\"."));
        m.put("ec2", new RuleMetadata("ec2", "warning", 1, "4.15",
                "Il termine preferito impiega \"campione\"."));
        m.put("ec4", new RuleMetadata("ec4", "error", 1, "4.15",
                "I concetti con \"fluid sample\" seguono il pattern: campione di liquido [X]."));
        m.put("ec5", new RuleMetadata("ec5", "error", 1, "4.15",
                "Pattern: campione [sito] prelevato mediante [metodo]."));
        m.put("ec6", new RuleMetadata("ec6", "error", 1, "4.15",
                "Tradurre con \"campione proveniente da\" quando il campione proviene da un sistema eterogeneo, altrimenti \"campione di\"."));
        m.put("sb3", new RuleMetadata("sb3", "error", 1, "4.16",
                "Il concetto 65818007 |Stent (physical object)| ha come PT \"endoprotesi\" con sinonimo \"stent\"."));
        m.put("pa3", new RuleMetadata("pa3", "error", 1, "4.13",
                "injury -> \"lesione\" se la pelle è coinvolta, altrimenti \"trauma\" o \"lesione traumatica\"."));
        m.put("pr2", new RuleMetadata("pr2", "warning", 1, "4.17",
                "Template: procedure non chirurgiche -> \"procedura\"; chirurgiche -> \"intervento chirurgico\"."));
        m.put("hs1", new RuleMetadata("hs1", "error", 1, "4.21",
                "\"history\" si traduce con \"anamnesi\" al singolare (anamnesi familiare di asma)."));
        m.put("en4", new RuleMetadata("en4", "error", 1, "4.22",
                "\"Inpatient [X]\" si traduce con \"[X] per paziente ricoverato\"."));
        m.put("en5", new RuleMetadata("en5", "warning", 1, "4.22",
                "\"Outpatient [X]\" si traduce con \"[X] ambulatoriale\" o \"[X] in regime ambulatoriale\"."));
        m.put("co1", new RuleMetadata("co1", "error", 1, "4.12",
                "Il PT del concetto 404684003 |Clinical finding (finding)| è \"reperto clinico\"."));
        m.put("co2", new RuleMetadata("co2", "error", 1, "4.12",
                "Il termine \"finding\" si traduce con \"reperto\", \"riscontro\" o viene omesso se ridondante."));
        m.put("ss5", new RuleMetadata("ss5", "warning", 1, "4.1",
                "Plurale regolare dei vocaboli di origine latina: minimi, massimi, stimoli."));
        return m;
    }

    // Compiled patterns
    private static final Pattern ARTICLE_START = Pattern.compile(
            "^(il|lo|la|i|gli|le|un|uno|una|l')\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern ADVERBIAL_START = Pattern.compile(
            "^(almeno|al massimo|al di là|oltre)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SPACE_BEFORE_COMMA = Pattern.compile("\\s,");
    private static final Pattern COMMA_NO_SPACE = Pattern.compile(",(?=[A-Za-zÀ-ÖØ-öø-ÿ])");
    private static final Pattern DECIMAL_POINT = Pattern.compile("\\d\\.\\d");
    private static final Pattern COMPARISON_NOTATION = Pattern.compile(">\\d+<");
    private static final Pattern MICRO_NUMBER = Pattern.compile("\\b\\d+\\s*ug\\b");
    private static final Pattern LOWER_LITRE = Pattern.compile(
            "(?<=/)[a-zµ]*l\\b|\\b\\d+(?:[.,]\\d+)?\\s*[a-zµ]{1,3}l\\b|\\b\\d+\\s*l\\b");
    private static final Pattern UPPER_LITRE = Pattern.compile("mL\\b|/L\\b");
    private static final Pattern NUMBER_UNIT_NO_SPACE = Pattern.compile(
            "\\b(\\d+(?:[.,]\\d+)?)(mg|g|kg|ug|ng|mcg|cm|mm|mL|ml|l|L|kPa|Pa|mmol|umol|mol|IU|UI)\\b");
    private static final Pattern NUMBER_UNIT_SPACE = Pattern.compile(
            "\\b\\d+(?:[.,]\\d+)? (mg|g|kg|ug|mL|L|cm|mm)\\b");
    private static final Pattern PERCENT_NO_SPACE = Pattern.compile("\\d+(?:[.,]\\d+)?%");
    private static final Pattern PERCENT_SPACE = Pattern.compile("\\d+(?:[.,]\\d+)? %");
    private static final Pattern TAXON_PATTERN = Pattern.compile(
            "sierotipo|spp\\.|enterica|variant|influenzae|pneumoniae|^[A-Z][a-z]+ [a-z]+ [a-z]+",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ACRONYM = Pattern.compile(
            "^(?=.{2,20}$)(?=.*[A-Z])(?=.*[A-Za-z])[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*$");
    private static final Pattern SUBSP = Pattern.compile("\\bsubsp\\b\\.?", Pattern.CASE_INSENSITIVE);
    private static final Pattern ACRONYM_EXPANDED = Pattern.compile(
            "[A-Z]{2,} \\([a-z]|[A-Z]{2,} - ");
    private static final Pattern LASER_UPPER = Pattern.compile("\\bLASER\\b");
    private static final Pattern UNIT_EN = Pattern.compile(
            "\\b(?:gram|milligram|kilogram|milliliter|centimeter|meter)s?\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern DEGREES_CELSIUS = Pattern.compile("°C|º C");
    private static final Pattern DEGREES_BARE = Pattern.compile("\\d+\\s*°(?!C)");
    private static final Pattern SUPERSCRIPT = Pattern.compile(
            "[\u00B2\u00B3\u2070\u2074\u2075\u2076\u2077\u2078\u2079]");
    private static final Pattern EXPONENT_CARET = Pattern.compile("\\d+\\^");
    private static final Pattern ROMAN_NUMERAL = Pattern.compile("\\b(?:II|III|IV|VI|VII|VIII|IX|XI|XII)\\b");
    private static final Pattern THOUSAND_SEPARATOR = Pattern.compile("\\d{1,3}[.,]\\d{3}[.,]\\d{3}\\b");
    private static final Pattern UNDERSCORE_INDEX = Pattern.compile("[A-Za-z0-9]_[A-Za-z0-9]");
    private static final Pattern ADJACENT_INDEX = Pattern.compile("\\bIg[A-Za-z]?\\d+\\b");
    private static final Pattern ORDINAL_ABBR = Pattern.compile("\\b\\d+(?:º|\\u00AA|°|a|o|er|e|d|de)\\b");
    private static final Pattern TAXONOMY_WORD = Pattern.compile(
            "\\bsierotipo\\b|\\bsottotipo\\b|\\bsierovar\\b|\\bgruppo\\b|\\btipo\\b");
    private static final Pattern SLASH_SPACED = Pattern.compile("[0-9A-Za-zÀ-ÖØ-öø-ÿ] / [0-9A-Za-zÀ-ÖØ-öø-ÿ]");
    private static final Pattern ET_OU = Pattern.compile("\\be / o\\b|\\be/ o\\b|\\be /o\\b");
    private static final Pattern ET_OU_GOOD = Pattern.compile("\\be/o\\b");
    private static final Pattern FRACTION_UNIT = Pattern.compile("(?:mg/L|mmol/L|umol/L|g/L|\\d/\\d)\\b");
    private static final Pattern SLASH_WORD = Pattern.compile(
            "(?<![A-Za-z0-9])[a-z]{2,}/(?:[a-z]{2,})(?![A-Za-z0-9])");
    private static final Pattern COLON_TAXONOMY = Pattern.compile("(?<=[A-Za-z]):[A-Za-z0-9]");
    private static final Pattern COLON_DOUBLE_SPACE = Pattern.compile(" : ");
    private static final Pattern COLON_ONE_SIDE = Pattern.compile(
            "(?<=[A-Za-z0-9]):(?= )|(?<= ):(?=[A-Za-z0-9])");
    private static final Pattern COMPOUND_HYPHEN = Pattern.compile("[a-z]{2,}-[a-z]{2,}");
    private static final Pattern PAREN_BAD = Pattern.compile("\\(\\s|\\s\\)|\\(\\)");
    private static final Pattern PROPER_HYPHEN = Pattern.compile(
            "[A-Z][a-zéèêëàìòù]{1,20}-[A-Z][a-zéèêëàìòù]{1,20}");
    private static final Pattern DRUG_PRODUCT = Pattern.compile(
            "prodotto contenente|vaccino contenente");
    private static final Pattern ARTICLE_AFTER_CONTAINING = Pattern.compile(
            "contenente (il|lo|la|i|gli|le|del|dello|della|dei|degli|delle|di )");
    private static final Pattern UNIQUEMENT = Pattern.compile("contenente unicamente");
    private static final Pattern SEULEMENT = Pattern.compile("contenente soltanto");
    private static final Pattern COORDINATION = Pattern.compile("\\be\\b|\\+");
    private static final Pattern PRECISEMENT = Pattern.compile("prodotto contenente precisamente");
    private static final Pattern PA3_TRIGGER = Pattern.compile(
            "lesione traumatica|trauma|ferita|contusione|lacerazione");
    private static final Pattern SB3_TRIGGER = Pattern.compile("endoprotesi|stent");
    private static final Pattern EC2_TRIGGER = Pattern.compile(
            "campione|prelievo|biopsia|tampone|aspirato");
    private static final Pattern PR2_TRIGGER = Pattern.compile(
            "\\bprocedura\\b|\\bintervento\\b|\\boperazione\\b|\\bchirurgia\\b");
    private static final Pattern HISTORY_SING = Pattern.compile(
            "\\banamnesi di\\b|\\banamnesi familiare\\b|\\banamnesi personale\\b");
    private static final Pattern INPATIENT = Pattern.compile(
            "paziente ricoverato|ricoverato");
    private static final Pattern AMBULATORIALE = Pattern.compile("\\bambulatoriale\\b");
    private static final Pattern BLOOD_GROUP = Pattern.compile("^gruppo sanguigno |^gruppo sanguigno ");
    private static final Pattern LIQUIDO_SAMPLE = Pattern.compile("campione di liquido");
    private static final Pattern PROVENIENTE_DA = Pattern.compile("proveniente da|proveniente d'");
    private static final Pattern PRELEVATO_MEDIANTE = Pattern.compile("prelevato mediante|prelevato con");
    private static final Pattern ITALIAN_ELISION = Pattern.compile(
            "\\b(dell|nell|sull|all|dall|quest|gl|un|dov|ond|cod|senz|tutt|quant|com)'",
            Pattern.CASE_INSENSITIVE);

    private static final Set<String> UNITS = Set.of(
            "mg", "g", "kg", "ug", "ng", "ml", "cm", "mm", "kpa", "pa", "mmol",
            "umol", "mol", "l", "m", "s", "h", "j", "u");

    private static final String TYPE_PREFERRED = "Preferred";

    @Override
    public String getLanguageCode() {
        return "it";
    }

    @Override
    public Map<String, RuleMetadata> getRules() {
        return RULES;
    }

    @Override
    public List<Finding> check(TranslationCheckContext ctx) {
        String rawTerm = ctx.term() == null ? "" : ctx.term();
        String term = ItTokenizer.normalize(rawTerm).strip();
        String cid = ctx.conceptId() == null ? "" : ctx.conceptId();
        String acc = ctx.acceptability();
        List<Finding> findings = new ArrayList<>();

        // ---------- ss1: case + ar2: article ----------
        boolean isAdverbial = ADVERBIAL_START.matcher(term).find();
        if (ARTICLE_START.matcher(term).find() && !isAdverbial) {
            String article = term.split("\\s+")[0];
            findings.add(find("ar2", "fail",
                    "Articolo all'inizio del termine (\u00AB " + article + " \u00BB) da eliminare"));
        }

        // ---------- se10: apostrophe ----------
        boolean curlApostrophe = term.chars().anyMatch(c -> "\u2019\u2018\u201A\u201B\u2018\u2019".indexOf(c) >= 0);
        if (curlApostrophe) {
            findings.add(find("se10", "fail",
                    "Apostrofo tipografico rilevato; utilizzare solo l'apostrofo dritto U+0027"));
        } else if (term.contains("'")) {
            findings.add(find("se10", "pass",
                    "Apostrofo dritto U+0027 conforme"));
        }

        // ---------- se11: period at end ----------
        if (term.stripTrailing().endsWith(".")
                && !Pattern.compile("(?:spp\\.|sp\\.)$").matcher(term).find()) {
            findings.add(find("se11", "fail",
                    "Punto finale non autorizzato in fondo al termine (salvo eccezioni)"));
        }

        // ---------- ss4: multiple spaces ----------
        if (ItTokenizer.hasMultipleSpaces(term)) {
            findings.add(find("ss4", "fail",
                    "Spazi multipli consecutivi; utilizzare un solo spazio tra le parole"));
        }

        // ---------- se1: comma spacing ----------
        boolean salm = Pattern.compile("Salmonella\\b|\\d+[a-z]?\\d*:\\w").matcher(term).find();
        if (!salm) {
            String se1work = term;
            if (CHEM_LOCANT.matcher(term).find() || CHEM_DIGIT.matcher(term).find()
                    || KARYOTYPE.matcher(term).find()) {
                se1work = term
                        .replaceAll("\\b[A-Z],[A-Z]'?", "")
                        .replaceAll("\\b\\d,\\d\\b", "")
                        .replaceAll("\\b\\d{1,2},[XY](?!\\w)", "")
                        .replaceAll("[Ss]ierotipo:", "");
            }
            if (SPACE_BEFORE_COMMA.matcher(se1work).find()) {
                findings.add(find("se1", "fail",
                        "Spazio prima di una virgola: attaccare la virgola alla parola precedente"));
            } else if (COMMA_NO_SPACE.matcher(se1work).find()) {
                findings.add(find("se1", "fail",
                        "Virgola non seguita da uno spazio in un'enumerazione"));
            }
        }

        // ---------- sc3: decimal ----------
        String sc3work = CYTOBAND.matcher(term).find()
                ? term.replaceAll("\\b[0-9XY]+[pq][0-9]+(?:\\.[0-9]+)+\\b", "00")
                : term;
        if (DECIMAL_POINT.matcher(sc3work).find()) {
            findings.add(find("sc3", "fail",
                    "Separatore decimale con punto; utilizzare la virgola italiana"));
        }

        // ---------- sc6: comparison symbols ----------
        String sc6work = term;
        if (MT_MUTATION.matcher(sc6work).find()) {
            sc6work = sc6work.replaceAll("(?:m\\.\\d+\\s*)?[ATCG]>[ATCG]", "A G");
        }
        if ((sc6work.contains("<") || sc6work.contains(">"))
                && !COMPARISON_NOTATION.matcher(sc6work).find()) {
            findings.add(find("sc6", "fail",
                    "Simbolo di confronto \u00AB < \u00BB o \u00AB > \u00BB;"
                    + " scrivere \u00AB minore di \u00BB / \u00AB maggiore di \u00BB per esteso"));
        } else if (COMPARISON_NOTATION.matcher(sc6work).find()) {
            findings.add(find("sc6", "pass",
                    "\u00AB >n< \u00BB notato come esponente/indice (notazione chimica), non un simbolo di confronto"));
        }

        // ---------- um8: µ ----------
        if (term.contains("\u00B5")) {
            findings.add(find("um8", "fail",
                    "\u00AB \u00B5 \u00BB rilevato; sostituire con \u00AB u \u00BB (es. ug)"));
        } else if (MICRO_NUMBER.matcher(term).find()) {
            findings.add(find("um8", "pass",
                    "Micro notato \u00AB u \u00BB (ug) conformemente alla regola"));
        }

        // ---------- um4: litre ----------
        String um4work = term;
        if (SEROTYPE_L.matcher(um4work).find()) {
            um4work = um4work.replaceAll(
                    "\\b(?:sierotipo|serotype|tipo|gruppo|sierovar)\\s+\\d+[Ll]\\b", "0");
        }
        if (LOWER_LITRE.matcher(um4work).find()) {
            findings.add(find("um4", "fail",
                    "\u00AB l \u00BB minuscolo per litro; utilizzare la maiuscola L (mL, /L)"));
        } else if (UPPER_LITRE.matcher(um4work).find()) {
            findings.add(find("um4", "pass",
                    "Litro notato con L maiuscola"));
        }

        // ---------- um5: space number-unit ----------
        String um5work = term;
        if (SEROTYPE_L.matcher(um5work).find()) {
            um5work = um5work.replaceAll(
                    "\\b(?:sierotipo|serotype|tipo|gruppo|sierovar)\\s+\\d+[Ll]\\b", "0");
        }
        if (NUMBER_UNIT_NO_SPACE.matcher(um5work).find()) {
            findings.add(find("um5", "fail",
                    "Spazio mancante tra il numero e l'unit\u00E0 di misura"));
        } else if (NUMBER_UNIT_SPACE.matcher(um5work).find()) {
            findings.add(find("um5", "pass",
                    "Spazio corretto tra numero e unit\u00E0"));
        }

        // ---------- um6: percent ----------
        if (PERCENT_NO_SPACE.matcher(term).find()) {
            findings.add(find("um6", "fail",
                    "Spazio mancante prima del simbolo %"));
        } else if (PERCENT_SPACE.matcher(term).find()) {
            findings.add(find("um6", "pass",
                    "Spazio corretto prima di %"));
        }

        // ---------- se9: parentheses ----------
        if (PAREN_BAD.matcher(term).find()) {
            findings.add(find("se9", "fail",
                    "Spazio all'interno delle parentesi; gli spazi si mettono all'esterno"));
        } else if (term.contains("(")) {
            findings.add(find("se9", "pass",
                    "Parentesi correttamente spaziate"));
        }

        // ---------- spelling checks (ss4) ----------
        try {
            List<SpellingIssue> issues = spellingChecker.check(term);
            for (SpellingIssue si : issues) {
                if (si.definiteTypo()) {
                    String msg = "Errore ortografico: \u00AB " + si.token() + " \u00BB \u2192 \u00AB "
                            + si.correction() + " \u00BB";
                    findings.add(findWarning("ss4", "fail", msg));
                } else if (si.correction() == null) {
                    String sugPart = si.suggestions().isEmpty() ? ""
                            : " (suggerimenti: " + String.join(", ", si.suggestions()) + ")";
                    String msg = "Parola forse errata: \u00AB " + si.token() + " \u00BB" + sugPart;
                    findings.add(findInfo("ss4", "uncertain", msg));
                }
            }
        } catch (Exception e) {
            // spelling check failure is not critical
        }

        // ---------- drug hierarchy (me1/me2/me3) ----------
        if (DRUG_PRODUCT.matcher(term).find()) {
            if (ARTICLE_AFTER_CONTAINING.matcher(term).find()) {
                if (TYPE_PREFERRED.equals(acc)) {
                    findings.add(find("me1", "fail",
                            "Articolo dopo \u00AB contenente \u00BB; le sostanze sono elencate senza articolo"));
                } else {
                    findings.add(findInfo("me1", "uncertain",
                            "Articolo dopo \u00AB contenente \u00BB; accettabile, regola me1 non stretta per sinonimi"));
                }
            }
            if (UNIQUEMENT.matcher(term).find()) {
                findings.add(findInfo("me2", "uncertain",
                        "\u00AB unicamente \u00BB usato; la forma prescritta \u00E8 \u00AB prodotto contenente soltanto sostanza \u00BB"));
            }
            if (SEULEMENT.matcher(term).find()) {
                findings.add(find("me2", "pass",
                        "\u00AB contenente soltanto \u00BB conforme alla regola me2"));
            }
            if (COORDINATION.matcher(term).find()) {
                findings.add(findInfo("me1", "uncertain",
                        "Verificare l'ordine alfabetico e il coordinamento delle sostanze"));
            }
            if (PRECISEMENT.matcher(term).find()) {
                findings.add(findInfo("me3", "uncertain",
                        "Termine di farmaco virtuale: verificare il pattern PT (ingredienti ... forma)"));
            }
        }

        // ---------- hierarchy-specific triggers (informational) ----------
        if (PA3_TRIGGER.matcher(term).find()) {
            findings.add(findInfo("pa3", "uncertain",
                    "Verificare la scelta di lesione/trauma/ferita in base al coinvolgimento cutaneo"));
        }
        if (SB3_TRIGGER.matcher(term).find()) {
            findings.add(findInfo("sb3", "uncertain",
                    "Verificare l'uso di \u00AB endoprotesi \u00BB (PT) e la presenza del sinonimo \u00AB stent \u00BB"));
        }
        if (EC2_TRIGGER.matcher(term).find()) {
            findings.add(findInfo("ec2", "uncertain",
                    "Verificare campione e i pattern: campione di / proveniente da / prelevato mediante"));
        }
        if (PR2_TRIGGER.matcher(term).find()) {
            findings.add(findInfo("pr2", "uncertain",
                    "Verificare \u00AB procedura \u00BB vs \u00AB intervento chirurgico \u00BB in base al carattere chirurgico"));
        }

        // ---------- or5: proper noun hyphen ----------
        if (PROPER_HYPHEN.matcher(term).find()) {
            findings.add(find("ss1", "pass",
                    "Trattino tra nomi propri conservato (es. Epstein-Barr)"));
        }

        // ---------- ab1/ab2/ab3 ----------
        if (SUBSP.matcher(term).find()) {
            findings.add(find("ab1", "fail",
                    "Abbreviazione \u00AB subsp. \u00BB rilevata; scrivere \u00AB subspecies \u00BB per esteso"));
        }
        if (ACRONYM_EXPANDED.matcher(term).find()) {
            findings.add(find("ab2", "pass",
                    "Sigla/acronimo seguito dalla forma sviluppata"));
        }
        if (LASER_UPPER.matcher(term).find()) {
            findings.add(find("ab3", "fail",
                    "Sigla lessicalizzata in maiuscolo \u00AB LASER \u00BB; scrivere in minuscolo \u00AB laser \u00BB"));
        }

        // ---------- um1/um2/um3/um7 ----------
        if (UNIT_EN.matcher(term).find()) {
            findings.add(find("um1", "fail",
                    "Parola inglese di unit\u00E0 rilevata; usare l'abbreviazione SI (es. mg, g, mL)"));
        }
        if (DEGREES_CELSIUS.matcher(term).find()) {
            findings.add(find("um2", "fail",
                    "Temperatura notata \u00AB \u00B0C \u00BB; usare la forma estesa \u00AB gradi Celsius \u00BB"));
        }
        if (DEGREES_BARE.matcher(term).find()) {
            findings.add(findWarning("um3", "uncertain",
                    "Gradi notati con il simbolo \u00AB \u00B0 \u00BB; verificare la forma estesa \u00AB gradi \u00BB"));
        }
        if (SUPERSCRIPT.matcher(term).find()) {
            findings.add(find("um7", "fail",
                    "Carattere esponente Unicode; usare la notazione accostata (mm3) o ^ per i numeri"));
        } else if (EXPONENT_CARET.matcher(term).find()) {
            findings.add(find("um7", "pass",
                    "Esponente applicato a un numero notato con il separatore ^"));
        }

        // ---------- sc2/sc4/sc5/sc7/sc8 ----------
        if (ROMAN_NUMERAL.matcher(term).find()) {
            findings.add(find("sc2", "pass",
                    "Numeri romani ammessi per uso medico (tipo II, fattore VI)"));
        }
        if (THOUSAND_SEPARATOR.matcher(term).find()) {
            findings.add(find("sc4", "fail",
                    "Separatore delle migliaia con punto/virgola; usare lo spazio (100 000 000)"));
        }
        if (UNDERSCORE_INDEX.matcher(term).find()) {
            findings.add(find("sc5", "fail",
                    "Carattere \u00AB _ \u00BB per indice; la notazione accostata \u00E8 richiesta (IgA2)"));
        } else if (ADJACENT_INDEX.matcher(term).find()) {
            findings.add(find("sc5", "pass",
                    "Indice accostato conforme (IgA2)"));
        }
        if (term.contains("+")) {
            findings.add(find("sc7", "pass",
                    "Simbolo + usato in un codice/risultato di test (Na+, gruppo A+, +++)"));
        }
        if (ORDINAL_ABBR.matcher(term).find() && !TAXONOMY_WORD.matcher(term).find()) {
            if (TYPE_PREFERRED.equals(acc)) {
                findings.add(find("sc8", "fail",
                        "Ordinale abbreviato nel termine preferito; scrivere in lettere (quinta, primo)"));
            } else {
                findings.add(find("sc8", "pass",
                        "Abbreviazione ordinale accettabile in sinonimo (5\u00AA malattia, 1\u00BA metacarpo)"));
            }
        }

        // ---------- se3/se4: slash ----------
        boolean slashFlag = false;
        if (SLASH_SPACED.matcher(term).find() && !ET_OU.matcher(term).find()) {
            findings.add(findWarning("se3", "uncertain",
                    "Barra obliqua circondata da spazi; verificare barra di frazione senza spazi (mg/L)"));
            slashFlag = true;
        }
        if (!slashFlag && FRACTION_UNIT.matcher(term).find()) {
            findings.add(find("se3", "pass",
                    "Barra di frazione senza spazi conforme (mg/L, 1/4)"));
        }

        if (ET_OU.matcher(term).find()) {
            findings.add(find("se3", "fail",
                    "Spazi intorno alla barra in \u00AB e/o \u00BB; scrivere \u00AB e/o \u00BB senza spazi"));
        } else if (ET_OU_GOOD.matcher(term).find()) {
            findings.add(find("se3", "pass",
                    "\u00AB e/o \u00BB senza spazi conforme"));
        }

        Matcher slashWord = SLASH_WORD.matcher(term);
        while (slashWord.find()) {
            String[] parts = slashWord.group().split("/");
            String left = parts[0];
            String right = parts[1];
            if (UNITS.contains(left) || UNITS.contains(right)
                    || ("e".equals(left) && "o".equals(right))) {
                continue;
            }
            findings.add(findWarning("se3", "uncertain",
                    "Barra tra due parole (\u00AB " + slashWord.group()
                            + " \u00BB); verificare se si tratta di un'alternativa da tradurre con \u00AB o \u00BB"));
            break;
        }

        // ---------- se5/se6: colon ----------
        if (COLON_TAXONOMY.matcher(term).find()) {
            findings.add(find("ss4", "pass",
                    "Due punti attaccati usati per un sottotipo tassonomico (sierotipo O103:H11)"));
        }
        if (COLON_DOUBLE_SPACE.matcher(term).find()) {
            findings.add(find("ss4", "pass",
                    "Due punti tra due spazi: precisione in stile telegrafico conforme"));
        }
        if (COLON_ONE_SIDE.matcher(term).find()) {
            findings.add(find("ss4", "fail",
                    "Spazio da un solo lato dei due punti; lo spazio \u00E8 bilaterale o assente"));
        }

        // ---------- se8: compound hyphen ----------
        if (COMPOUND_HYPHEN.matcher(term).find()) {
            findings.add(find("ss4", "pass",
                    "Trattino che unisce un composto conforme"));
        }

        // ---------- ec1: specimen concept ----------
        if ("123038009".equals(cid)) {
            String t = term.strip().toLowerCase();
            if ("specimen".equals(t) || "specimen (specimen)".equals(t)) {
                findings.add(find("ec1", "fail",
                        "Il PT del concetto Specimen (123038009) deve essere \u00AB campione \u00BB"));
            } else if ("campione".equals(t)) {
                findings.add(find("ec1", "pass",
                        "\u00AB campione \u00BB conforme per il concetto Specimen"));
            }
        }

        // ---------- ec4/ec5/ec6 ----------
        if (LIQUIDO_SAMPLE.matcher(term).find()) {
            if (PROVENIENTE_DA.matcher(term).find()) {
                findings.add(find("ec4", "fail",
                        "\u00AB campione di liquido ... proveniente da \u00BB: non aggiungere \u00AB proveniente da \u00BB (implicito)"));
            } else {
                findings.add(find("ec4", "pass",
                        "Pattern \u00AB campione di liquido [X] \u00BB conforme a ec4"));
            }
        }
        if (PRELEVATO_MEDIANTE.matcher(term).find()) {
            findings.add(find("ec5", "pass",
                    "Pattern \u00AB prelevato mediante [metodo] \u00BB conforme a ec5"));
        }
        if (PROVENIENTE_DA.matcher(term).find()) {
            findings.add(find("ec6", "pass",
                    "\u00AB proveniente da \u00BB usato conformemente a ec6"));
        }

        // ---------- history / inpatient / outpatient ----------
        if (HISTORY_SING.matcher(term).find()) {
            findings.add(find("hs1", "pass",
                    "\u00AB anamnesi \u00BB al singolare per default conforme"));
        }
        if (INPATIENT.matcher(term).find()) {
            findings.add(find("en4", "pass",
                    "Pattern \u00AB per paziente ricoverato \u00BB conforme a en4"));
        }
        if (AMBULATORIALE.matcher(term).find()) {
            findings.add(find("en5", "pass",
                    "\u00AB ambulatoriale \u00BB conforme a en5"));
        }

        // ---------- ss1: taxon exception ----------
        if (TAXON_PATTERN.matcher(term).find() && UPPERCASE.matcher(term).find()) {
            findings.add(find("ss1", "pass",
                    "Maiuscole giustificate (taxon / nome proprio / sigla), eccezione ss1"));
        }

        // ---------- ss5: latin plurals ----------
        if (Pattern.compile("\\b(?:minimi|massimi|stimoli)\\b").matcher(term).find()) {
            findings.add(find("ss5", "pass",
                    "Plurale italiano regolare dei termini latini (minimi, massimi, stimoli)"));
        }

        return findings;
    }

    private Finding find(String ruleId, String status, String message) {
        RuleMetadata r = RULES.getOrDefault(ruleId, new RuleMetadata(ruleId, "warning", 0, "", ""));
        return new Finding(ruleId, r.severity(), status, message, r.pdfPage(), r.section());
    }

    private Finding findWarning(String ruleId, String status, String message) {
        RuleMetadata r = RULES.getOrDefault(ruleId, new RuleMetadata(ruleId, "warning", 0, "", ""));
        return new Finding(ruleId, "warning", status, message, r.pdfPage(), r.section());
    }

    private Finding findInfo(String ruleId, String status, String message) {
        RuleMetadata r = RULES.getOrDefault(ruleId, new RuleMetadata(ruleId, "info", 0, "", ""));
        return new Finding(ruleId, "info", status, message, r.pdfPage(), r.section());
    }
}