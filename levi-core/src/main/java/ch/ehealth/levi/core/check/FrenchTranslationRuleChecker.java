package ch.ehealth.levi.core.check;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * French translation rule checker, ported from the reference implementation
 * {@code review_fr.py} (Guide éditorial de traduction Snomed France v2.29).
 *
 * <p>Every rule is deterministic and operates on the French term only; the
 * English FSN/PT fields of the context are carried along and available to
 * future hierarchy-aware checks.</p>
 */
public class FrenchTranslationRuleChecker implements TranslationRuleChecker {

    // ------------------------------------------------------------------
    // Rule registry (source: RULES dict in review_fr.py)
    // ------------------------------------------------------------------
    private static final Map<String, RuleMetadata> RULES = buildRules();
    private static final Pattern UPPERCASE = Pattern.compile("[A-ZÀ-Ý]");

    // Fix patterns: false-positive reduction
    private static final Pattern ADVERBIAL_START = Pattern.compile("^(au moins|au plus|au maximum|au-delà)\\b");
    private static final Pattern CHEM_LOCANT = Pattern.compile("\\b[A-Z],[A-Z]'?");
    private static final Pattern CHEM_DIGIT = Pattern.compile("\\b\\d,\\d\\b");
    private static final Pattern KARYOTYPE = Pattern.compile("\\b\\d{1,2},[XY](?!\\w)");
    private static final Pattern CYTOBAND = Pattern.compile("\\b[0-9XY]+[pq][0-9]+(?:\\.[0-9]+)+\\b");
    private static final Pattern MT_MUTATION = Pattern.compile("(?:m\\.\\d+\\s*)?[ATCG]>[ATCG]");
    private static final Pattern LATIN_FOETUS = Pattern.compile("\\b[A-Z][a-zà-ÿéèêë]+ (?:foetus|foetal)s?\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SEROTYPE_L = Pattern.compile("\\b(?:sérotype|serotype|type|groupe|sérovar|sérogroupe)\\s+\\d+[Ll]\\b");

    private final SpellingChecker spellingChecker;

    public FrenchTranslationRuleChecker() {
        this(new HunspellSpellingChecker());
    }

    public FrenchTranslationRuleChecker(SpellingChecker spellingChecker) {
        this.spellingChecker = spellingChecker != null ? spellingChecker : new HunspellSpellingChecker();
    }

    private static Map<String, RuleMetadata> buildRules() {
        Map<String, RuleMetadata> m = new LinkedHashMap<>();
        m.put("ss1", new RuleMetadata("ss1", "error", 17, "4.1",
                "Terme - y compris la première lettre - en minuscules et insensible à la casse. Exceptions : noms propres, symboles, codes, sigles, acronymes ou taxons."));
        m.put("ss4", new RuleMetadata("ss4", "error", 18, "4.1",
                "La terminologie médicale respecte la syntaxe courante du français."));
        m.put("ar2", new RuleMetadata("ar2", "error", 20, "4.3",
                "Pas d'article en début de terme. Exemple : syndrome hépatorénal (et non « le syndrome hépatorénal »)"));
        m.put("se1", new RuleMetadata("se1", "warning", 25, "4.10",
                "la virgule est collée au mot qui la précède et suivie d'un caractère espace"));
        m.put("se2", new RuleMetadata("se2", "error", 25, "4.10",
                "La barre oblique sans espace est utilisée dans l'expression « et/ou »"));
        m.put("se7", new RuleMetadata("se7", "error", 26, "4.10",
                "Le tiret simple (UTF8 0x2D) encadré par deux espaces ne sert que pour introduire la forme développée d'un sigle ou d'un acronyme"));
        m.put("se9", new RuleMetadata("se9", "error", 26, "4.10",
                "Les deux caractères espaces sont placés à l'extérieur des parenthèses, et non à l'intérieur"));
        m.put("se10", new RuleMetadata("se10", "error", 26, "4.10",
                "Le caractère retenu est exclusivement l'apostrophe neutre verticale : ' encodé U+0027"));
        m.put("se11", new RuleMetadata("se11", "error", 26, "4.10",
                "le caractère point (UTF8 0x2E) ne doit pas apparaître en dernière position d'un terme, sauf exception"));
        m.put("or1", new RuleMetadata("or1", "warning", 19, "4.2",
                "Positionner le tréma sur la lettre à prononcer. Exemple : otite aigüe (et non pas aiguë)"));
        m.put("or3", new RuleMetadata("or3", "warning", 19, "4.2",
                "Dans les mots composés, on privilégie désormais la soudure au lieu du trait d'union (posttraumatique ; préopératoire ; contrindication ; intracrânien)"));
        m.put("or6", new RuleMetadata("or6", "warning", 20, "4.2",
                "Le consensus actuel du groupe maintient l'usage de l'ancienne orthographe concernant le traitement de l'accent circonflexe sur les voyelles i et u"));
        m.put("ab1", new RuleMetadata("ab1", "error", 21, "4.5",
                "Eviter les abréviations et écrire les mots en entier (sauf pour les abréviations incontournables consacrées par l'usage)"));
        m.put("ab2", new RuleMetadata("ab2", "error", 21, "4.5",
                "l'acronyme est suivi de sa forme développée introduite par un tiret simple encadré de deux espaces si l'acronyme est seul dans le terme, ou une paire de parenthèses si l'acronyme est accompagné d'autres mots"));
        m.put("ab3", new RuleMetadata("ab3", "error", 22, "4.5",
                "Les sigles et acronymes courants se lexicalisent ; ils s'écrivent alors en minuscules et prennent le pluriel comme des noms. Exemples : laser ; sidéen(ne)"));
        m.put("um1", new RuleMetadata("um1", "error", 22, "4.6",
                "Les unités de mesure sont abrégées conformément aux règles du SI (m = mètre ; s = seconde ; Pa = pascal ; m3 = mètre cube)"));
        m.put("um2", new RuleMetadata("um2", "error", 22, "4.6",
                "Les unités de température sont exprimées en forme développée - degrés Celsius"));
        m.put("um3", new RuleMetadata("um3", "error", 22, "4.6",
                "Les degrés d'angle sont exprimés en forme développée sauf lorsque le contexte ne laisse aucune ambigüité sur la nature de l'unité"));
        m.put("um4", new RuleMetadata("um4", "warning", 22, "4.6",
                "L'abréviation de l'unité de mesure « litre » est la lettre majuscule L"));
        m.put("um5", new RuleMetadata("um5", "error", 22, "4.6",
                "un caractère espace sépare le nombre de l'unité de mesure qui le qualifie. Exemple : 0,75 g"));
        m.put("um6", new RuleMetadata("um6", "error", 23, "4.6",
                "L'anglais « percent » et « % » se traduisent par le symbole %. Un caractère espace sépare la quantité du symbole. Exemple : 75 %"));
        m.put("um7", new RuleMetadata("um7", "error", 23, "4.6",
                "Les exposants ne sont pas explicités par un symbole particulier ; le nombre en exposant est accolé au symbole (mm3). Si l'exposant s'applique à un nombre on utilise ^ (10^3)"));
        m.put("um8", new RuleMetadata("um8", "error", 23, "4.6",
                "La lettre µ signifiant micro dans une unité est remplacée par la lettre u. Exemple : umol/L"));
        m.put("sc2", new RuleMetadata("sc2", "error", 23, "4.7",
                "L'écriture des chiffres privilégie les chiffres arabes à moins que l'usage médical n'ait consacré un autre système (vitamine K2 ; diabète de type 1 ; Chiari type II ; facteur VI)"));
        m.put("sc3", new RuleMetadata("sc3", "error", 23, "4.7",
                "Le séparateur décimal en français est la virgule. Exemple : 1,5 mg"));
        m.put("sc4", new RuleMetadata("sc4", "error", 23, "4.7",
                "Le séparateur des milliers est l'espace. Exemple : 100 000 000"));
        m.put("sc5", new RuleMetadata("sc5", "error", 23, "4.7",
                "Les indices ne sont pas explicités par un symbole particulier ; le nombre en indice est accolé au symbole (IgA2). Il n'y a pas d'indice suivant un nombre (sinon caractère _)"));
        m.put("sc6", new RuleMetadata("sc6", "error", 23, "4.7",
                "Les symboles de comparaison '<' (inférieur à) et '>' (supérieur à) sont remplacés par l'expression en clair"));
        m.put("sc7", new RuleMetadata("sc7", "warning", 24, "4.7",
                "Le symbole + (plus) est utilisé pour participer à un code ou quantifier le résultat d'un test (Na+ ; groupe A+ ; glycosurie = +++)"));
        m.put("sc8", new RuleMetadata("sc8", "warning", 24, "4.7",
                "Un adjectif numéral ordinal est exprimé en toutes lettres dans le terme préféré du français commun ; un synonyme acceptable peut l'abréger (PT : cinquième maladie ; SYN : 5e maladie ; 1er ; 2d)"));
        m.put("ll1", new RuleMetadata("ll1", "error", 25, "4.9",
                "Les lettres ligaturées doivent être employées à bon escient et avec le bon codage : æ, Æ, œ, Œ. Exemples : fœtal ; œsophage ; nævus"));
        m.put("se3", new RuleMetadata("se3", "error", 25, "4.10",
                "La barre oblique sans espace est utilisée comme barre de fraction. Exemples : mg/L ; citrate de sodium 1/4"));
        m.put("se4", new RuleMetadata("se4", "error", 25, "4.10",
                "Une alternative marquée par la barre oblique dans le terme anglais est à remplacer par la conjonction de coordination « ou » dans la traduction française"));
        m.put("se5", new RuleMetadata("se5", "warning", 26, "4.10",
                "Le double point collé sans espace peut servir à introduire un sous-type dans une taxonomie (Escherichia coli de sérotype O103:H11)"));
        m.put("se6", new RuleMetadata("se6", "warning", 26, "4.10",
                "Le double point encadré par deux espaces peut être utilisé en remplacement d'une préposition pour introduire une précision en style télégraphique"));
        m.put("se8", new RuleMetadata("se8", "error", 26, "4.10",
                "Le tiret simple (UTF8 0x2D) est aussi le trait d'union qui assemble les mots composés (voir règle or3)"));
        m.put("me1", new RuleMetadata("me1", "warning", 34, "4.14",
                "produit contenant substance (et substance (et substance) …), les substances étant listées sans article, dans l'ordre alphabétique"));
        m.put("me2", new RuleMetadata("me2", "warning", 34, "4.14",
                "produit contenant seulement substance (et substance), les substances étant listées sans article"));
        m.put("me3", new RuleMetadata("me3", "warning", 35, "4.14",
                "produit contenant précisément {substance} {dosage} [et {substance} {dosage} …] par {forme fabriquée}"));
        m.put("me4", new RuleMetadata("me4", "error", 35, "4.14",
                "La caractéristique de libération n'est pas reprise dans le terme préféré français lorsque sa valeur est « libération conventionnelle (ou classique) », car il s'agit de la valeur par défaut"));
        m.put("ec1", new RuleMetadata("ec1", "error", 35, "4.15",
                "Le concept 123038009 |Specimen (specimen)| a pour terme préféré français « échantillon »"));
        m.put("ec2", new RuleMetadata("ec2", "warning", 35, "4.15",
                "Le terme préféré des concepts de cette hiérarchie emploie le nom « échantillon » ou le nom « spécimen »"));
        m.put("ec4", new RuleMetadata("ec4", "error", 36, "4.15",
                "Les concepts contenant « fluid sample » sont traduits suivant ce patron : échantillon de liquide [X]"));
        m.put("ec5", new RuleMetadata("ec5", "error", 36, "4.15",
                "Les concepts exprimant une méthode de prélèvement sont traduits suivant le patron [« échantillon » | « spécimen »] [site] prélevé par [méthode]"));
        m.put("ec6", new RuleMetadata("ec6", "error", 37, "4.15",
                "Traduire par « échantillon provenant de » lorsque l'échantillon provient d'un système ou est hétérogène, et par « échantillon de » lorsqu'on parle de la matière elle-même"));
        m.put("sb1", new RuleMetadata("sb1", "error", 38, "4.16",
                "tube sous vide <caractéristiques> pour prélèvement <adjectif du milieu biologique> (Evacuated blood collection tube)"));
        m.put("sb2", new RuleMetadata("sb2", "error", 38, "4.16",
                "support sous vide <caractéristiques> pour prélèvement <adjectif du milieu biologique> (Evacuated specimen container)"));
        m.put("sb3", new RuleMetadata("sb3", "error", 38, "4.16",
                "Le concept 65818007 |Stent (physical object)| et ses descendants comportent le mot « endoprothèse » dans leur terme préféré français, et peuvent comporter un synonyme acceptable français employant le mot « stent »"));
        m.put("pa3", new RuleMetadata("pa3", "error", 30, "4.13",
                "injury -> « blessure » si la peau est impliquée, sinon « traumatisme », « lésion traumatique » ou « écrasement » pour crushing injury"));
        m.put("pa31", new RuleMetadata("pa31", "warning", 31, "4.13",
                "Les concepts [Pressure injury of X] traduisent pressure injury par « escarre »"));
        m.put("pr2", new RuleMetadata("pr2", "warning", 39, "4.17",
                "Template de traduction du nom « procedure » pour les procédures non chirurgicales : PT = procédure ; pour les procédures chirurgicales : PT = intervention chirurgicale"));
        m.put("hs1", new RuleMetadata("hs1", "error", 51, "4.21",
                "Le mot « history » se traduit par « antécédent » au singulier par défaut (antécédent familial d'asthme)"));
        m.put("en4", new RuleMetadata("en4", "error", 52, "4.22",
                "L'expression « Inpatient [X] » se traduit par « [X] pour patient(e) hospitalisé(e) »"));
        m.put("en5", new RuleMetadata("en5", "warning", 52, "4.22",
                "L'expression « Outpatient [X] » se traduit par « [X] de soins ambulatoires » ou « [X] ambulatoire »"));
        m.put("or2", new RuleMetadata("or2", "warning", 19, "4.2",
                "Mettre la graphie en accord avec la prononciation lorsque é se prononce è (évènement). Un synonyme acceptable peut être ajouté dans l'ancienne orthographe"));
        m.put("or4", new RuleMetadata("or4", "error", 20, "4.2",
                "Le trait d'union est conservé avec les préfixes demi-, mi-, semi-, ex-, sous-, vice-, non-"));
        m.put("or5", new RuleMetadata("or5", "error", 20, "4.2",
                "Le trait d'union (U+002D) coordonnant deux noms propres ou géographiques est conservé pour marquer l'égalité des deux noms"));
        m.put("co5", new RuleMetadata("co5", "error", 29, "4.12",
                "Les groupes sanguins sont exprimés sous trois formes – longue, courte et intermédiaire, donc par trois synonymes. Le terme préféré est la forme longue : groupe sanguin A Rh(D) positif"));
        m.put("ar6", new RuleMetadata("ar6", "error", 21, "4.3",
                "Lorsque le terme désigne un dispositif ou un produit ciblant une partie du corps, celle-ci ne requiert pas d'article (prothèse de hanche ; sonde nasale ; pommade à lèvres)"));
        m.put("ar7", new RuleMetadata("ar7", "error", 21, "4.3",
                "Lorsque le terme anglais spécifie « all » ou « both » on utilise l'article défini pluriel (tous les doigts ; les deux oreilles)"));
        m.put("ss5", new RuleMetadata("ss5", "error", 18, "4.1",
                "Pluriel régulier des mots d'origine latine conformément à la nouvelle orthographe (des minimums, des stimulus)"));
        m.put("ss6", new RuleMetadata("ss6", "warning", 18, "4.1",
                "Le terme préféré du français commun ajoute la terminaison féminine entre parenthèses (amputé(e)). On n'écrira pas « conjoint.e » mais « conjoint(e) »"));
        m.put("ss7", new RuleMetadata("ss7", "error", 19, "4.1",
                "Si l'entité provient de Body structure, le mot « structure » est à omettre : fracture de l'ulna (et non « fracture de la structure osseuse de l'ulna »)"));
        return m;
    }

    // ------------------------------------------------------------------
    // Compiled patterns (ported from review_fr.py)
    // ------------------------------------------------------------------
    private static final Pattern ARTICLE_START = Pattern.compile("^(le|la|les|un|une|des|du|au|aux|l'|de la|de l')\\b");
    private static final Pattern SPACE_BEFORE_COMMA = Pattern.compile("\\s,");
    private static final Pattern COMMA_NO_SPACE = Pattern.compile(",(?=[A-Za-zÀ-ÿ])");
    private static final Pattern DECIMAL_POINT = Pattern.compile("\\d\\.\\d");
    private static final Pattern COMPARISON_NOTATION = Pattern.compile(">\\d+<");
    private static final Pattern MICRO_NUMBER = Pattern.compile("\\b\\d+\\s*ug\\b");
    private static final Pattern LOWER_LITRE = Pattern.compile("(?<=/)[a-zµ]*l\\b|\\b\\d+(?:[.,]\\d+)?\\s*[a-zµ]{1,3}l\\b|\\b\\d+\\s*l\\b");
    private static final Pattern UPPER_LITRE = Pattern.compile("mL\\b|/L\\b");
    private static final Pattern NUMBER_UNIT_NO_SPACE = Pattern.compile("\\b(\\d+(?:[.,]\\d+)?)(mg|g|kg|ug|ng|mcg|cm|mm|mL|ml|l|L|kPa|Pa|mmol|umol|mol|IU|UI)\\b");
    private static final Pattern NUMBER_UNIT_SPACE = Pattern.compile("\\b\\d+(?:[.,]\\d+)? (mg|g|kg|ug|mL|L|cm|mm)\\b");
    private static final Pattern PERCENT_NO_SPACE = Pattern.compile("\\d+(?:[.,]\\d+)?%");
    private static final Pattern PERCENT_SPACE = Pattern.compile("\\d+(?:[.,]\\d+)? %");
    private static final Pattern OLD_TREMA = Pattern.compile("aiguë|ciguë|ambiguë|exiguë|contiguë");
    private static final Pattern NEW_TREMA = Pattern.compile("aigüe");
    private static final Pattern SOLDABLE_HYPHEN = Pattern.compile(
            "post-opératoire|pré-opératoire|post-partum|contre-indication|pré-natal|intra-crânien|péri-anal|post-traumatique");
    private static final Pattern OLD_SPELLING = Pattern.compile("brulure|piqure");
    private static final Pattern ET_OU_BAD = Pattern.compile("\\bet\\s+/\\s+ou\\b|\\bet/\\s+ou\\b|\\bet\\s+/ou\\b");
    private static final Pattern ET_OU_GOOD = Pattern.compile("\\bet/ou\\b");
    private static final Pattern PAREN_BAD = Pattern.compile("\\(\\s|\\s\\)|\\(\\)");
    private static final Pattern TAXON_PATTERN = Pattern.compile(
            "sérotype|spp\\.|enterica|variant|influenzae|pneumoniae|^[A-Z][a-z]+ [a-z]+ [a-z]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern ACRONYM = Pattern.compile("^(?=.{2,20}$)(?=.*[A-Z])(?=.*[A-Za-z])[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*$");
    private static final Pattern SUBSP = Pattern.compile("\\bsubsp\\b\\.?", Pattern.CASE_INSENSITIVE);
    private static final Pattern ACRONYM_EXPANDED = Pattern.compile("[A-ZÀ-Ý]{2,} \\([a-zà-ÿéèêë]|[A-ZÀ-Ý]{2,} - ");
    private static final Pattern LASER_UPPER = Pattern.compile("\\bLASER\\b");
    private static final Pattern UNIT_EN = Pattern.compile("\\b(?:gram|milligram|kilogram|milliliter|centimeter|meter)s?\\b");
    private static final Pattern DEGREES_CELSIUS = Pattern.compile("°C|º C");
    private static final Pattern DEGREES_BARE = Pattern.compile("\\d+\\s*°(?!C)");
    private static final Pattern SUPERSCRIPT = Pattern.compile("[\u00B2\u00B3\u2070\u2074\u2075\u2076\u2077\u2078\u2079]");
    private static final Pattern EXPONENT_CARET = Pattern.compile("\\d+\\^");
    private static final Pattern ROMAN_NUMERAL = Pattern.compile("\\b(?:II|III|IV|VI|VII|VIII|IX|XI|XII)\\b");
    private static final Pattern THOUSAND_SEPARATOR = Pattern.compile("\\d{1,3}[.,]\\d{3}[.,]\\d{3}\\b");
    private static final Pattern UNDERSCORE_INDEX = Pattern.compile("[A-Za-z0-9]_[A-Za-z0-9]");
    private static final Pattern ADJACENT_INDEX = Pattern.compile("\\bIg[A-Za-z]?\\d+\\b");
    private static final Pattern ORDINAL_EME = Pattern.compile("\\d+ème\\b");
    private static final Pattern ORDINAL_ABBR = Pattern.compile("\\b\\d+(?:er|re|e|d|de)\\b");
    private static final Pattern TAXONOMY_WORD = Pattern.compile("\\bsérotype\\b|\\bsous-type\\b|\\bsérovar\\b|\\bsérogroupe\\b");
    private static final Pattern LIGATURE_MISSING = Pattern.compile(
            "\\b(?:coeur|coeurs|soeur|soeurs|noeud|noeuds|moeurs|oe[dé]me|oe[dé]mes|oe[dé]mateux|oesophage|oesophagite|oesophagiens?|foet[ae]l|foetus)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern LIGATURE_USED = Pattern.compile("[œæŒÆ]");
    private static final Pattern SLASH_SPACED = Pattern.compile("[0-9A-Za-zÀ-ÿ] / [0-9A-Za-zÀ-ÿ]");
    private static final Pattern ET_OU_SPACED = Pattern.compile("\\bet / ou\\b");
    private static final Pattern FRACTION_UNIT = Pattern.compile("(?:mg/L|mmol/L|umol/L|g/L|\\d/\\d)\\b");
    private static final Pattern SLASH_WORD = Pattern.compile("(?<![A-Za-z0-9])[a-zà-ÿ]{2,}/(?:[a-zà-ÿ]{2,})(?![A-Za-z0-9])");
    private static final Pattern COLON_TAXONOMY = Pattern.compile("(?<=[A-Za-zÀ-ÿ]):[A-Za-z0-9À-ÿ]");
    private static final Pattern COLON_ONE_SIDE = Pattern.compile("(?<=[A-Za-zÀ-ÿ0-9]):(?= )|(?<= ):(?=[A-Za-zÀ-ÿ0-9])");
    private static final Pattern COMPOUND_HYPHEN = Pattern.compile("[a-zà-ÿ]{2,}-[a-zà-ÿ]{2,}");
    private static final Pattern PREFIX_OK = Pattern.compile("\\b(?:demi|mi|semi|ex|sous|vice|non)-[a-zà-ÿ]");
    private static final Pattern PREFIX_SOLID = Pattern.compile("\\b(?:demi|semi)[a-zà-ÿ]{3,}");
    private static final Pattern PROPER_HYPHEN = Pattern.compile("[A-ZÀ-Ý][a-zà-ÿéèêë]{1,20}-[A-ZÀ-Ý][a-zà-ÿéèêë]{1,20}");
    private static final Pattern DRUG_PRODUCT = Pattern.compile("produit contenant|vaccin contenant");
    private static final Pattern ARTICLE_AFTER_CONTAINING = Pattern.compile("contenant (de la|de l'|du |des |de )");
    private static final Pattern UNIQUEMENT = Pattern.compile("contenant uniquement");
    private static final Pattern SEULEMENT = Pattern.compile("contenant seulement");
    private static final Pattern COORDINATION = Pattern.compile("\\bet\\b|\\+");
    private static final Pattern PRECISEMENT = Pattern.compile("produit contenant précisément");
    private static final Pattern PA31_TRIGGER = Pattern.compile("escarre|décubitus");
    private static final Pattern PA3_TRIGGER = Pattern.compile("lésion traumatique|blessure|écrasement");
    private static final Pattern SB3_TRIGGER = Pattern.compile("endoprothèse|stent");
    private static final Pattern EC2_TRIGGER = Pattern.compile("échantillon|spécimen|prélèvement|écouvillonnage");
    private static final Pattern PR2_TRIGGER = Pattern.compile("\\bprocédure\\b|\\bintervention\\b|\\bopération\\b|\\bchirurgie\\b");
    private static final Pattern EVENEMENT = Pattern.compile("évènement");
    private static final Pattern EVENEMENT_OLD = Pattern.compile("événement");
    private static final Pattern HISTORY_SING = Pattern.compile("\\bantécédent de\\b|\\bantécédent familial\\b|\\bantécédent personnel\\b");
    private static final Pattern HISTORY_PLUR = Pattern.compile("\\bantécédents\\b");
    private static final Pattern INPATIENT = Pattern.compile("patient(\\(e\\))? hospitalisé(\\(e\\))?");
    private static final Pattern AMBULATOIRE = Pattern.compile("\\bambulatoire\\b");
    private static final Pattern BLOOD_GROUP = Pattern.compile("^groupe sanguin ");
    private static final Pattern BLOOD_GROUP_SHORT = Pattern.compile("groupe (?!sanguin\\b)");
    private static final Pattern BLOOD_GROUP_DETAIL = Pattern.compile("\\b(?:positif|négatif)\\b|Rh\\([D]\\)|RH\\s*[+-]|\\+b");
    private static final Pattern DEVICE_PLUS_ARTICLE = Pattern.compile(
            "\\b(?:prothèse|sonde|pommade|cathéter|endoprothèse|implant|valve|drain|canule|attelle|orthèse|broche|clip)s?\\b (?:de la |du |de l'|des )[a-zà-ÿ]");
    private static final Pattern DEVICE_DE = Pattern.compile(
            "\\b(?:prothèse|sonde|pommade|cathéter|endoprothèse|implant|valve|drain|canule|attelle|orthèse|broche|clip)s?\\b de [a-zà-ÿ]");
    private static final Pattern AR7_TRIGGER = Pattern.compile("tous les|toutes les|les deux");
    private static final Pattern LATIN_PLURAL = Pattern.compile("\\b(?:minima|maxima|stimuli)\\b");
    private static final Pattern INCLUSIVE_DOT = Pattern.compile("\\b[a-zà-ÿ]{2,}\\.[a-zà-ÿéèêë]");
    private static final Pattern VAR_PREFIX = Pattern.compile("\\bvar\\.");
    private static final Pattern PAREN_ENDING = Pattern.compile("\\b\\w+\\((?:e|ne|ère|trice|euse|le|te)\\)", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern LIBERATION_DEFAULT = Pattern.compile("libération (conventionnelle|classique)(?!\\s+et\\b)");
    private static final Pattern DOSAGE_FORM = Pattern.compile(
            "\\b(?:comprimé|gélule|tablette|capsule|pommade|suppositoire|granulé|granules|solution|suspension|sirop|crème|gel|pansement|injectable|perfusion)\\b");
    private static final Pattern LIQUIDE_SAMPLE = Pattern.compile("(?:échantillon|spécimen) de liquide");
    private static final Pattern PROVENANT_DE = Pattern.compile("provenant de|provenant d'");
    private static final Pattern PRELEVE_PAR = Pattern.compile("prélevé par");
    private static final Pattern TUBE_SOUS_VIDE = Pattern.compile("tube sous vide");
    private static final Pattern SUPPORT_SOUS_VIDE = Pattern.compile("support sous vide");
    private static final Pattern POUR_PRELEVEMENT = Pattern.compile("pour prélèvement");

    private static final Set<String> UNITS = Set.of(
            "mg", "g", "kg", "ug", "ng", "ml", "cm", "mm", "kpa", "pa", "mmol", "umol", "mol", "l", "m", "s", "h", "j", "u");

    private static final List<String> PREFIX_WHITELIST = List.of(
            "demie", "demiurge", "semelle", "semence", "semaine", "semestre", "semis",
            "seminal", "seminale", "seminalis", "semialdéhyde", "semialdéhydes");

    private static final String TYPE_PREFERRED = "Preferred";

    @Override
    public String getLanguageCode() {
        return "fr";
    }

    @Override
    public Map<String, RuleMetadata> getRules() {
        return RULES;
    }

    @Override
    public List<Finding> check(TranslationCheckContext ctx) {
        String rawTerm = ctx.term() == null ? "" : ctx.term();
        String term = FrTokenizer.normalize(rawTerm).strip();
        String cid = ctx.conceptId() == null ? "" : ctx.conceptId();
        String acc = ctx.acceptability();
        List<Finding> findings = new ArrayList<>();

        boolean isAdverbial = ADVERBIAL_START.matcher(term).find();
        if (ARTICLE_START.matcher(term).find() && !isAdverbial) {
            String article = term.split("\\s+")[0];
            findings.add(find("ar2", "fail", "Article en début de terme (« " + article + " ») à supprimer"));
        }

        boolean curlApostrophe = term.chars().anyMatch(c -> "’‘‚‛`".indexOf(c) >= 0);
        if (curlApostrophe) {
            findings.add(find("se10", "fail", "Apostrophe typographique détectée ; utiliser uniquement l'apostrophe droite U+0027"));
        } else if (term.contains("'")) {
            findings.add(find("se10", "pass", "Apostrophe droite U+0027 conforme"));
        }

        if (term.stripTrailing().endsWith(".") && !Pattern.compile("(?:spp\\.|sp\\.)$").matcher(term).find()) {
            findings.add(find("se11", "fail", "Point final non autorisé en fin de terme (sauf exception)"));
        }

        if (FrTokenizer.hasMultipleSpaces(term)) {
            findings.add(find("ss4", "fail", "Espaces multiples consécutifs ; utiliser un seul espace entre les mots"));
        }

        boolean salm = Pattern.compile("Salmonella\\b|\\d+[a-z]?\\d*:\\w").matcher(term).find();
        if (!salm) {
            String se1work = term;
            if (CHEM_LOCANT.matcher(term).find() || CHEM_DIGIT.matcher(term).find() || KARYOTYPE.matcher(term).find()) {
                se1work = term
                        .replaceAll("\\b[A-Z],[A-Z]'?", "")
                        .replaceAll("\\b\\d,\\d\\b", "")
                        .replaceAll("\\b\\d{1,2},[XY](?!\\w)", "")
                        .replaceAll("[Ss]érotype:", "");
            }
            if (SPACE_BEFORE_COMMA.matcher(se1work).find()) {
                findings.add(find("se1", "fail", "Espace avant une virgule : coller la virgule au mot précédent"));
            } else if (COMMA_NO_SPACE.matcher(se1work).find()) {
                findings.add(find("se1", "fail", "Virgule non suivie d'un espace dans une énumération"));
            }
        }

        String sc3work = CYTOBAND.matcher(term).find() ? term.replaceAll("\\b[0-9XY]+[pq][0-9]+(?:\\.[0-9]+)+\\b", "00") : term;
        if (DECIMAL_POINT.matcher(sc3work).find()) {
            findings.add(find("sc3", "fail", "Séparateur décimal par point ; utiliser la virgule française"));
        }
        String sc6work = term;
        if (MT_MUTATION.matcher(sc6work).find()) {
            sc6work = sc6work.replaceAll("(?:m\\.\\d+\\s*)?[ATCG]>[ATCG]", "A G");
        }
        if ((sc6work.contains("<") || sc6work.contains(">")) && !COMPARISON_NOTATION.matcher(sc6work).find()) {
            findings.add(find("sc6", "fail", "Symbole de comparaison « < » ou « > » ; écrire « inférieur à » / « supérieur à » en clair"));
        } else if (COMPARISON_NOTATION.matcher(sc6work).find()) {
            findings.add(find("sc6", "pass", "« >n< » noté en exposant/indice (notation chimique), pas un symbole de comparaison"));
        }
        if (term.contains("µ")) {
            findings.add(find("um8", "fail", "« µ » détecté ; remplacer par « u » (ex. ug)"));
        } else if (MICRO_NUMBER.matcher(term).find()) {
            findings.add(find("um8", "pass", "Micro noté « u » (ug) conformément à la règle"));
        }

        String um4work = term;
        if (SEROTYPE_L.matcher(um4work).find()) {
            um4work = um4work.replaceAll("\\b(?:sérotype|serotype|type|groupe|sérovar|sérogroupe)\\s+\\d+[Ll]\\b", "0");
        }
        if (LOWER_LITRE.matcher(um4work).find()) {
            findings.add(find("um4", "fail", "« l » minuscule pour litre ; utiliser la majuscule L (mL, /L)"));
        } else if (UPPER_LITRE.matcher(um4work).find()) {
            findings.add(find("um4", "pass", "Litre noté avec L majuscule"));
        }

        String um5work = term;
        if (SEROTYPE_L.matcher(um5work).find()) {
            um5work = um5work.replaceAll("\\b(?:sérotype|serotype|type|groupe|sérovar|sérogroupe)\\s+\\d+[Ll]\\b", "0");
        }
        if (NUMBER_UNIT_NO_SPACE.matcher(um5work).find()) {
            findings.add(find("um5", "fail", "Espace manquant entre le nombre et l'unité de mesure"));
        } else if (NUMBER_UNIT_SPACE.matcher(um5work).find()) {
            findings.add(find("um5", "pass", "Espace correct entre nombre et unité"));
        }
        if (PERCENT_NO_SPACE.matcher(term).find()) {
            findings.add(find("um6", "fail", "Espace manquant avant le symbole %"));
        } else if (PERCENT_SPACE.matcher(term).find()) {
            findings.add(find("um6", "pass", "Espace correct avant %"));
        }

        if (OLD_TREMA.matcher(term).find()) {
            findings.add(find("or1", "fail", "« aiguë » : placer le tréma sur la voyelle prononcée « aigüe »"));
        } else if (NEW_TREMA.matcher(term).find()) {
            findings.add(find("or1", "pass", "Tréma positionné conformément à la règle (« aigüe »)"));
        }

        if (SOLDABLE_HYPHEN.matcher(term).find()) {
            if (TYPE_PREFERRED.equals(acc)) {
                findings.add(find("or3", "fail",
                        "Trait d'union dans un mot composé à souder (ex. posttraumatique, postopératoire, contrindication)"));
            } else {
                findings.add(find("or3", "pass",
                        "Ancienne graphie avec trait d'union admise pour un synonyme acceptable"));
            }
        }

        if (OLD_SPELLING.matcher(term).find()) {
            if (TYPE_PREFERRED.equals(acc)) {
                findings.add(find("or6", "fail",
                        "Terme préféré en nouvelle orthographe (« brulure »/« piqure ») ; l'orthographe ancienne « brûlure »/« piqûre » est maintenue pour le terme préféré"));
            } else {
                findings.add(find("or6", "pass",
                        "« brulure »/« piqure » acceptable en synonyme (nouvelle orthographe autorisée)"));
            }
        }

        if (ET_OU_BAD.matcher(term).find()) {
            findings.add(find("se2", "fail", "Espaces autour de la barre oblique dans « et/ou »"));
        } else if (ET_OU_GOOD.matcher(term).find()) {
            findings.add(find("se2", "pass", "« et/ou » sans espace conforme"));
        }

        if (PAREN_BAD.matcher(term).find()) {
            findings.add(find("se9", "fail", "Espace à l'intérieur des parenthèses ; les espaces se placent à l'extérieur"));
        } else if (term.contains("(")) {
            findings.add(find("se9", "pass", "Parenthèses correctement espacées"));
        }

        if (term.contains(" - ")) {
            String left = term.split(" - ", 2)[0].trim();
            if (ACRONYM.matcher(left).matches()) {
                findings.add(find("se7", "pass", "Tiret simple encadré de deux espaces utilisé pour développer un sigle/acronyme"));
            } else {
                findings.add(find("se7", "fail", "Tiret encadré de deux espaces réservé à la forme développée d'un sigle/acronyme"));
            }
        }

        // ---------- spelling checks (ss4) ----------
        try {
            List<SpellingIssue> issues = spellingChecker.check(term);
            for (SpellingIssue si : issues) {
                if (si.definiteTypo()) {
                    String msg = "Faute d'orthographe : « " + si.token() + " » → « " + si.correction() + " »";
                    findings.add(findWarning("ss4", "fail", msg));
                } else if (si.correction() == null) {
                    String sugPart = si.suggestions().isEmpty() ? "" : " (suggestions : " + String.join(", ", si.suggestions()) + ")";
                    String msg = "Mot peut-être mal orthographié : « " + si.token() + " »" + sugPart;
                    findings.add(findInfo("ss4", "uncertain", msg));
                }
            }
        } catch (Exception e) {
            // spelling check failure is not critical
        }

        // ---------- medicament hierarchy (me1/me2/me3) ----------
        if (DRUG_PRODUCT.matcher(term).find()) {
            if (ARTICLE_AFTER_CONTAINING.matcher(term).find()) {
                if (TYPE_PREFERRED.equals(acc)) {
                    findings.add(find("me1", "fail", "Article après « contenant » ; les substances sont listées sans article"));
                } else {
                    findings.add(findInfo("me1", "uncertain",
                            "Article après « contenant » ; acceptable, règle me1 non stricte pour les synonymes"));
                }
            }
            if (UNIQUEMENT.matcher(term).find()) {
                findings.add(findInfo("me2", "uncertain",
                        "« uniquement » employé ; la forme prescrite est « produit contenant seulement substance »"));
            }
            if (SEULEMENT.matcher(term).find()) {
                findings.add(find("me2", "pass", "« contenant seulement » conforme à la règle me2"));
            }
            if (COORDINATION.matcher(term).find()) {
                findings.add(findInfo("me1", "uncertain",
                        "Vérifier l'ordre alphabétique et la coordination des substances"));
            }
            if (PRECISEMENT.matcher(term).find()) {
                findings.add(findInfo("me3", "uncertain",
                        "Terme de médicament virtuel : vérifier le patron PT (ingrédients …, forme) vs synonyme « produit contenant précisément … par forme »"));
            }
        }

        // ---------- hierarchy-specific contexts (human review) ----------
        if (PA31_TRIGGER.matcher(term).find()) {
            findings.add(findInfo("pa31", "uncertain",
                    "Vérifier la traduction de pressure injury par « escarre » (et absence du mot « décubitus »)"));
        }
        if (PA3_TRIGGER.matcher(term).find()) {
            findings.add(findInfo("pa3", "uncertain",
                    "Vérifier le choix blessure / traumatisme / lésion traumatique / écrasement selon l'implication de la peau"));
        }
        if (SB3_TRIGGER.matcher(term).find()) {
            findings.add(findInfo("sb3", "uncertain",
                    "Vérifier l'emploi d'« endoprothèse » (PT) et la présence du synonyme « stent »"));
        }
        if (EC2_TRIGGER.matcher(term).find()) {
            findings.add(findInfo("ec2", "uncertain",
                    "Vérifier échantillon/specimen et les patrons échantillon de / provenant de / prélevé par"));
        }
        if (PR2_TRIGGER.matcher(term).find()) {
            findings.add(findInfo("pr2", "uncertain",
                    "Vérifier le template « procédure » vs « intervention chirurgicale » selon le caractère chirurgical"));
        }

        // ---------- orthographe 1990 (or2/or4/or5) ----------
        if (EVENEMENT.matcher(term).find()) {
            findings.add(find("or2", "pass", "Graphie « évènement » (nouvelle orthographe) conforme"));
        }
        if (EVENEMENT_OLD.matcher(term).find()) {
            if (TYPE_PREFERRED.equals(acc)) {
                findings.add(find("or2", "fail",
                        "Terme préféré en ancienne graphie « événement » ; la nouvelle orthographe « évènement » est privilégiée"));
            } else {
                findings.add(find("or2", "pass", "« événement » acceptable en synonyme (ancienne orthographe autorisée)"));
            }
        }

        if (PREFIX_OK.matcher(term).find()) {
            findings.add(find("or4", "pass", "Trait d'union conservé avec le préfixe (demi-, mi-, semi-, ex-, sous-, vice-, non-)"));
        }
        Matcher prefixSolid = PREFIX_SOLID.matcher(term);
        while (prefixSolid.find()) {
            String w = prefixSolid.group();
            if (PREFIX_WHITELIST.contains(w)) {
                continue;
            }
            if (w.endsWith("us") || w.endsWith("um") || w.endsWith("ae") || w.endsWith("is")) {
                continue;
            }
            findings.add(findWarning("or4", "fail",
                    "Trait d'union manquant avec le préfixe « " + w + " » (ex. semi-conducteur)"));
            break;
        }

        if (PROPER_HYPHEN.matcher(term).find()) {
            findings.add(find("or5", "pass", "Trait d'union coordonnant deux noms propres conservé (ex. Epstein-Barr)"));
        }

        // ---------- sigles et acronymes (ab1/ab2/ab3) ----------
        if (SUBSP.matcher(term).find()) {
            findings.add(find("ab1", "fail", "Abréviation « subsp. » détectée ; écrire « subspecies » en entier"));
        }
        if (ACRONYM_EXPANDED.matcher(term).find()) {
            findings.add(find("ab2", "pass", "Sigle/acronyme suivi de sa forme développée (tiret encadré d'espaces ou parenthèses)"));
        }
        if (LASER_UPPER.matcher(term).find()) {
            findings.add(find("ab3", "fail", "Sigle lexicalisé en majuscules « LASER » ; écrire en minuscules « laser »"));
        }

        // ---------- unités (um1/um2/um3/um7) ----------
        if (UNIT_EN.matcher(term).find()) {
            findings.add(find("um1", "fail", "Mot anglais d'unité détecté ; utiliser l'abréviation SI (ex. mg, g, mL)"));
        }
        if (DEGREES_CELSIUS.matcher(term).find()) {
            findings.add(find("um2", "fail", "Température notée « °C » ; utiliser la forme développée « degrés Celsius »"));
        }
        if (DEGREES_BARE.matcher(term).find()) {
            findings.add(findWarning("um3", "uncertain",
                    "Degrés notés par le symbole « ° » ; vérifier la forme développée « degrés »"));
        }
        if (SUPERSCRIPT.matcher(term).find()) {
            findings.add(find("um7", "fail", "Caractère exposant Unicode ; utiliser la notation accolée (mm3) ou ^ pour les nombres"));
        } else if (EXPONENT_CARET.matcher(term).find()) {
            findings.add(find("um7", "pass", "Exposant appliqué à un nombre noté avec le séparateur ^"));
        }

        // ---------- symboles scientifiques (sc2/sc4/sc5/sc7/sc8) ----------
        if (ROMAN_NUMERAL.matcher(term).find()) {
            findings.add(find("sc2", "pass", "Chiffres romains admis pour l'usage médical (type II, facteur VI), exception sc2"));
        }
        if (THOUSAND_SEPARATOR.matcher(term).find()) {
            findings.add(find("sc4", "fail", "Séparateur des milliers par point/virgule ; utiliser l'espace (100 000 000)"));
        }
        if (UNDERSCORE_INDEX.matcher(term).find()) {
            findings.add(find("sc5", "fail", "Caractère « _ » pour indice ; la notation accolée est requise (IgA2)"));
        } else if (ADJACENT_INDEX.matcher(term).find()) {
            findings.add(find("sc5", "pass", "Indice/accolade de symbole conforme (IgA2)"));
        }
        if (term.contains("+")) {
            findings.add(find("sc7", "pass", "Symbole + utilisé dans un code / un résultat de test (Na+, groupe A+, +++)"));
        }
        if (ORDINAL_EME.matcher(term).find()) {
            findings.add(findWarning("sc8", "fail",
                    "Abréviation ordinale « ème » ; convention académique : nombre suffixé de e (1er, 1re, 2d, 2e)"));
        } else if (ORDINAL_ABBR.matcher(term).find() && !TAXONOMY_WORD.matcher(term).find()) {
            if (TYPE_PREFERRED.equals(acc)) {
                findings.add(find("sc8", "fail",
                        "Ordinal abrégé dans le terme préféré ; l'écrire en toutes lettres (cinquième, premier)"));
            } else {
                findings.add(find("sc8", "pass",
                        "Abréviation ordinale acceptable en synonyme (5e maladie, 1er métacarpien, 2d)"));
            }
        }

        // ---------- ligatures (ll1) ----------
        String ll1work = LATIN_FOETUS.matcher(term).find()
                ? term.replaceAll("\\b[A-Z][a-zà-ÿéèêë]+ (?:foetus|foetal)s?\\b", "")
                : term;
        if (LIGATURE_MISSING.matcher(ll1work).find()) {
            findings.add(find("ll1", "fail",
                    "Ligature non employée (ex. « coeur »/« oesophage ») ; utiliser æ/œ (fœtal, œsophage, nævus)"));
        } else if (LIGATURE_USED.matcher(term).find()) {
            findings.add(find("ll1", "pass", "Ligatures æ/œ employées avec le bon codage"));
        }

        // ---------- ponctuation (se3/se4/se5/se6/se8) ----------
        if (SLASH_SPACED.matcher(term).find() && !ET_OU_SPACED.matcher(term).find()) {
            findings.add(findWarning("se3", "uncertain",
                    "Barre oblique entourée d'espaces ; vérifier barre de fraction sans espace (mg/L) ou alternative à traduire par « ou »"));
        } else if (FRACTION_UNIT.matcher(term).find()) {
            findings.add(find("se3", "pass", "Barre de fraction sans espace conforme (mg/L, 1/4)"));
        }

        Matcher slashWord = SLASH_WORD.matcher(term);
        while (slashWord.find()) {
            String[] parts = slashWord.group().split("/");
            String left = parts[0];
            String right = parts[1];
            if (UNITS.contains(left) || UNITS.contains(right) || ("et".equals(left) && "ou".equals(right))) {
                continue;
            }
            findings.add(findWarning("se4", "uncertain",
                    "Barre oblique entre deux mots (« " + slashWord.group()
                            + " ») ; vérifier qu'il ne s'agit pas d'une alternative à traduire par « ou »"));
            break;
        }

        if (COLON_TAXONOMY.matcher(term).find()) {
            findings.add(find("se5", "pass", "Double point collé utilisé pour un sous-type taxonomique (sérotype O103:H11)"));
        }
        if (term.contains(" : ")) {
            findings.add(find("se6", "pass", "Double point encadré d'espaces : précision en style télégraphique conforme (antécédents : goutte)"));
        }
        if (COLON_ONE_SIDE.matcher(term).find()) {
            findings.add(find("se6", "fail",
                    "Espace d'un seul côté du double point ; l'espacement « : » est encadré de deux espaces ou collé (sous-type)"));
        }

        if (COMPOUND_HYPHEN.matcher(term).find()) {
            findings.add(find("se8", "pass", "Trait d'union assemblant un mot composé conforme (cf. or3)"));
        }

        // ---------- medicaments (me4) et échantillons (ec1/ec4/ec5/ec6) ----------
        if (TYPE_PREFERRED.equals(acc) && LIBERATION_DEFAULT.matcher(term).find() && DOSAGE_FORM.matcher(term).find()) {
            findings.add(find("me4", "fail",
                    "« libération conventionnelle/classique » reprise dans un terme préféré de forme fabriquée ; à omettre (valeur par défaut)"));
        }

        if ("123038009".equals(cid)) {
            String t = term.strip().toLowerCase();
            if ("specimen".equals(t) || "spécimen".equals(t)) {
                findings.add(find("ec1", "fail", "Terme préféré du concept Specimen (123038009) doit être « échantillon »"));
            } else if ("échantillon".equals(t)) {
                findings.add(find("ec1", "pass", "« échantillon » conforme pour le concept Specimen"));
            }
        }

        if (LIQUIDE_SAMPLE.matcher(term).find()) {
            if (PROVENANT_DE.matcher(term).find()) {
                findings.add(find("ec4", "fail",
                        "« échantillon de liquide … provenant de » : ne pas ajouter « provenant de » (implicite)"));
            } else {
                findings.add(find("ec4", "pass", "Patron « échantillon de liquide [X] » conforme à ec4"));
            }
        }
        if (PRELEVE_PAR.matcher(term).find()) {
            findings.add(find("ec5", "pass", "Patron « prélevé par [méthode] » conforme à ec5"));
        }
        if (PROVENANT_DE.matcher(term).find()) {
            findings.add(find("ec6", "pass", "« provenant de » utilisé conformément à ec6"));
        }

        // ---------- objets physiques (sb1/sb2) ----------
        if (TUBE_SOUS_VIDE.matcher(term).find()) {
            if (POUR_PRELEVEMENT.matcher(term).find()) {
                findings.add(find("sb1", "pass", "Patron « tube sous vide … pour prélèvement [milieu] » conforme"));
            } else {
                findings.add(findWarning("sb1", "uncertain",
                        "« tube sous vide » sans « pour prélèvement [adjectif] » ; vérifier le patron sb1"));
            }
        }
        if (SUPPORT_SOUS_VIDE.matcher(term).find()) {
            if (POUR_PRELEVEMENT.matcher(term).find()) {
                findings.add(find("sb2", "pass", "Patron « support sous vide … pour prélèvement [milieu] » conforme"));
            } else {
                findings.add(findWarning("sb2", "uncertain",
                        "« support sous vide » sans « pour prélèvement [adjectif] » ; vérifier le patron sb2"));
            }
        }

        // ---------- history / inpatient / groupes sanguins (hs1/en4/en5/co5) ----------
        if (HISTORY_SING.matcher(term).find()) {
            findings.add(find("hs1", "pass", "« antécédent » au singulier par défaut conforme"));
        }
        if (HISTORY_PLUR.matcher(term).find()) {
            findings.add(findInfo("hs1", "uncertain",
                    "« antécédents » au pluriel ; le défaut est le singulier « antécédent » (sauf « antécédents familiaux inconnus »)"));
        }
        if (INPATIENT.matcher(term).find()) {
            findings.add(find("en4", "pass", "Patron « pour patient(e) hospitalisé(e) » conforme à en4"));
        }
        if (AMBULATOIRE.matcher(term).find()) {
            findings.add(find("en5", "pass", "« ambulatoire » / « soins ambulatoires » conforme à en5"));
        }
        if (BLOOD_GROUP.matcher(term).find()) {
            findings.add(find("co5", "pass",
                    "Forme longue du groupe sanguin utilisée comme terme préféré (groupe sanguin A Rh(D) positif)"));
        } else if (TYPE_PREFERRED.equals(acc) && BLOOD_GROUP_SHORT.matcher(term).find() && BLOOD_GROUP_DETAIL.matcher(term).find()) {
            findings.add(findWarning("co5", "uncertain",
                    "Groupe sanguin : vérifier les trois formes (longue/intermédiaire/courte) et la forme longue en terme préféré"));
        }

        // ---------- articles dispositifs (ar6/ar7) ----------
        if (DEVICE_PLUS_ARTICLE.matcher(term).find()) {
            findings.add(findWarning("ar6", "uncertain",
                    "Article entre un dispositif et la partie du corps ciblée ; per ar6 l'article est omis (prothèse de hanche, sonde nasale)"));
        } else if (DEVICE_DE.matcher(term).find()) {
            findings.add(find("ar6", "pass", "Aucun article entre le dispositif et la partie du corps (ex. prothèse de hanche)"));
        }
        if (AR7_TRIGGER.matcher(term).find()) {
            findings.add(find("ar7", "pass", "Article défini pluriel utilisé (tous les doigts / les deux oreilles) conforme à ar7"));
        }

        // ---------- autres règles 4.1 (ss5/ss6/ss7) ----------
        if (LATIN_PLURAL.matcher(term).find()) {
            findings.add(findWarning("ss5", "uncertain",
                    "Pluriel d'origine latine (« minima »/« maxima »/« stimuli ») ; la nouvelle orthographe préfère « minimums », « stimulus »"));
        }
        if (INCLUSIVE_DOT.matcher(term).find() && !VAR_PREFIX.matcher(term).find()) {
            findings.add(findWarning("ss6", "fail",
                    "Typographie inclusive par point (« conjoint.e ») ; utiliser la terminaison entre parenthèses « conjoint(e) »"));
        } else if (PAREN_ENDING.matcher(term).find()) {
            findings.add(find("ss6", "pass", "Terminaison féminine entre parenthèses conforme à ss6 (amputé(e))"));
        }

        if (TAXON_PATTERN.matcher(term).find() && UPPERCASE.matcher(term).find()) {
            findings.add(find("ss1", "pass", "Majuscules justifiées (taxon / nom propre / sigle), exception ss1"));
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
        RuleMetadata r = RULES.getOrDefault(ruleId, new RuleMetadata(ruleId, "warning", 0, "", ""));
        return new Finding(ruleId, "info", status, message, r.pdfPage(), r.section());
    }
}