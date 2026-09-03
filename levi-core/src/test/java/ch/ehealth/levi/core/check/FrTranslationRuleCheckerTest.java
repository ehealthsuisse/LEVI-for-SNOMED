package ch.ehealth.levi.core.check;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class FrTranslationRuleCheckerTest {

    private final FrenchTranslationRuleChecker checker = new FrenchTranslationRuleChecker();

    private List<Finding> check(String term) {
        return check(stub(term));
    }

    private List<Finding> check(TranslationCheckContext ctx) {
        return checker.check(ctx);
    }

    private TranslationCheckContext stub(String term) {
        return new TranslationCheckContext("123", "", "", term, "fr", "", "900000000000013009", "Acceptable");
    }

    private void assertHas(List<Finding> findings, String ruleId, String status) {
        assertTrue(findings.stream().anyMatch(f -> f.ruleId().equals(ruleId) && f.status().equals(status)),
                "expected finding " + ruleId + ":" + status + " but got " + findings);
    }

    private void assertNotHas(List<Finding> findings, String ruleId, String status) {
        assertFalse(findings.stream().anyMatch(f -> f.ruleId().equals(ruleId) && f.status().equals(status)),
                "did not expect finding " + ruleId + ":" + status + " but got " + findings);
    }

    // ---------------- Registry ----------------

    @Test
    public void testRegistryContainsExpectedRules() {
        Map<String, RuleMetadata> rules = checker.getRules();
        assertTrue(rules.size() >= 60, "expected at least 60 rules, got " + rules.size());
        for (String id : List.of("ar2", "se1", "or1", "me1", "pa31", "ec2", "pr2", "sb3", "hs1", "co5", "ll1", "um4")) {
            assertTrue(rules.containsKey(id), "missing rule " + id);
        }
    }

    @Test
    public void testRegistryMetadata() {
        RuleMetadata r = checker.getRules().get("me1");
        assertEquals("warning", r.severity());
        assertEquals(34, r.pdfPage());
        assertEquals("4.14", r.section());
        assertFalse(r.quote().isEmpty());
    }

    // ---------------- ar2: no article at start ----------------

    @Test
    public void testAr2ArticleAtStartFails() {
        assertHas(check("le syndrome hépatorénal"), "ar2", "fail");
        assertHas(check("une rétinopathie diabétique"), "ar2", "fail");
    }

    @Test
    public void testAr2NoArticlePasses() {
        assertNotHas(check("syndrome hépatorénal"), "ar2", "fail");
    }

    // ---------------- se1: comma rules ----------------

    @Test
    public void testSe1SpaceBeforeCommaFails() {
        assertHas(check("rouge , gonflé"), "se1", "fail");
    }

    @Test
    public void testSe1CommaWithoutFollowingSpaceFails() {
        assertHas(check("rouge,gonflé"), "se1", "fail");
    }

    @Test
    public void testSe1CorrectCommaPasses() {
        assertNotHas(check("rouge, gonflé"), "se1", "fail");
    }

    @Test
    public void testSe1SeroTypeColonException() {
        assertNotHas(check("Escherichia coli de sérotype O103:H11"), "se1", "fail");
    }

    // ---------------- se2: et/ou ----------------

    @Test
    public void testSe2SpacedEtOuFails() {
        assertHas(check("et / ou"), "se2", "fail");
    }

    @Test
    public void testSe2CorrectEtOuPasses() {
        assertHas(check("et/ou"), "se2", "pass");
    }

    // ---------------- se7: dash with two spaces only for acronym expansion ----------------

    @Test
    public void testSe7AcronymExpansionPasses() {
        assertHas(check("IRM - imagerie par résonance magnétique"), "se7", "pass");
    }

    @Test
    public void testSe7NonAcronymFails() {
        assertHas(check("hypertension artérielle - compliquée"), "se7", "fail");
    }

    // ---------------- se9: spaces outside parentheses ----------------

    @Test
    public void testSe9SpaceInsideParensFails() {
        assertHas(check("test ( avec espace)"), "se9", "fail");
        assertHas(check("(espace ) test"), "se9", "fail");
    }

    @Test
    public void testSe9CorrectParensPasses() {
        assertHas(check("test (sans espace)"), "se9", "pass");
    }

    // ---------------- se10: apostrophe ----------------

    @Test
    public void testSe10CurlyApostropheFails() {
        assertHas(check("l\u2019hypertension"), "se10", "fail");
    }

    @Test
    public void testSe10StraightApostrophePasses() {
        assertHas(check("l'hypertension"), "se10", "pass");
    }

    // ---------------- se11: trailing dot ----------------

    @Test
    public void testSe11TrailingDotFails() {
        assertHas(check("hypertension artérielle."), "se11", "fail");
    }

    @Test
    public void testSe11SppExceptionPasses() {
        assertNotHas(check("Streptococcus spp."), "se11", "fail");
    }

    // ---------------- or1: tréma ----------------

    @Test
    public void testOr1OldTremFails() {
        assertHas(check("otite aiguë"), "or1", "fail");
    }

    @Test
    public void testOr1NewTremPasses() {
        assertHas(check("otite aigüe"), "or1", "pass");
    }

    // ---------------- or3: soudures ----------------

    @Test
    public void testOr3HyphenFailsPreferred() {
        TranslationCheckContext ctx = new TranslationCheckContext("123", "", "", "post-opératoire", "fr", "",
                "900000000000013009", "Preferred");
        assertHas(check(ctx), "or3", "fail");
    }

    @Test
    public void testOr3HyphenAcceptablePasses() {
        assertHas(check("post-opératoire"), "or3", "pass");
        assertHas(check("contre-indication"), "or3", "pass");
    }

    @Test
    public void testOr3SouduresPasses() {
        assertNotHas(check("postopératoire"), "or3", "fail");
        assertNotHas(check("contrindication"), "or3", "fail");
    }

    // ---------------- or6: brûlure / piqûre ----------------

    @Test
    public void testOr6PreferredNewSpellingFails() {
        TranslationCheckContext ctx = new TranslationCheckContext("123", "", "", "brulure cutanée", "fr", "",
                "900000000000013009", "Preferred");
        assertHas(check(ctx), "or6", "fail");
    }

    @Test
    public void testOr6SynonymNewSpellingPasses() {
        assertHas(check("brulure cutanée"), "or6", "pass");
    }

    // ---------------- or2: évènement ----------------

    @Test
    public void testOr2EvenementNewPasses() {
        assertHas(check("évènement"), "or2", "pass");
    }

    @Test
    public void testOr2PreferredOldSpellingFails() {
        TranslationCheckContext ctx = new TranslationCheckContext("123", "", "", "événement", "fr", "",
                "900000000000013009", "Preferred");
        assertHas(check(ctx), "or2", "fail");
    }

    // ---------------- or4: prefixes with hyphen ----------------

    @Test
    public void testOr4CorrectHyphenPasses() {
        assertHas(check("semi-conducteur"), "or4", "pass");
    }

    @Test
    public void testOr4MissingHyphenFails() {
        assertHas(check("semiconductor"), "or4", "fail");
    }

    // ---------------- or5: proper names hyphen ----------------

    @Test
    public void testOr5ProperNameHyphenPasses() {
        assertHas(check("maladie d'Epstein-Barr"), "or5", "pass");
    }

    // ---------------- ab1: subsp ----------------

    @Test
    public void testAb1SubspFails() {
        assertHas(check("subsp. Diarizonae"), "ab1", "fail");
    }

    // ---------------- ab2: acronym expansion ----------------

    @Test
    public void testAb2ExpansionPasses() {
        assertHas(check("IRM (imagerie par résonance magnétique)"), "ab2", "pass");
    }

    // ---------------- ab3: LASER ----------------

    @Test
    public void testAb3LaserFails() {
        assertHas(check("thérapie LASER"), "ab3", "fail");
    }

    // ---------------- um1: SI abbreviations ----------------

    @Test
    public void testUm1EnglishUnitFails() {
        assertHas(check("5 grams de substance"), "um1", "fail");
    }

    // ---------------- um2: °C ----------------

    @Test
    public void testUm2DegreesCelsiusFails() {
        assertHas(check("fièvre à 40 °C"), "um2", "fail");
    }

    // ---------------- um3: bare degrees ----------------

    @Test
    public void testUm3BareDegreeUncertain() {
        assertHas(check("rotation de 90°"), "um3", "uncertain");
    }

    // ---------------- um4: litre ----------------

    @Test
    public void testUm4LowercaseLitreFails() {
        assertHas(check("5 ml de sang"), "um4", "fail");
    }

    @Test
    public void testUm4CapitalLitrePasses() {
        assertHas(check("5 mL de sang"), "um4", "pass");
    }

    // ---------------- um5: space between number and unit ----------------

    @Test
    public void testUm5MissingSpaceFails() {
        assertHas(check("0,75g"), "um5", "fail");
    }

    @Test
    public void testUm5CorrectSpacePasses() {
        assertHas(check("0,75 g"), "um5", "pass");
    }

    // ---------------- um6: percent ----------------

    @Test
    public void testUm6MissingSpacePercentFails() {
        assertHas(check("75%"), "um6", "fail");
    }

    @Test
    public void testUm6CorrectSpacePercentPasses() {
        assertHas(check("75 %"), "um6", "pass");
    }

    // ---------------- um7: exponents ----------------

    @Test
    public void testUm7UnicodeSuperscriptFails() {
        assertHas(check("10\u00B2 mm"), "um7", "fail");
    }

    @Test
    public void testUm7CaretPasses() {
        assertHas(check("10^3 cellules"), "um7", "pass");
    }

    // ---------------- um8: micro ----------------

    @Test
    public void testUm8MicroSymbolFails() {
        assertHas(check("25 µg"), "um8", "fail");
    }

    @Test
    public void testUm8MicroUsesU() {
        assertHas(check("25 ug"), "um8", "pass");
    }

    // ---------------- sc2: roman numerals ----------------

    @Test
    public void testSc2RomanNumeralPasses() {
        assertHas(check("diabète de type II"), "sc2", "pass");
    }

    // ---------------- sc3: decimal point ----------------

    @Test
    public void testSc3DecimalPointFails() {
        assertHas(check("1.5 mg"), "sc3", "fail");
    }

    @Test
    public void testSc3DecimalCommaPasses() {
        assertNotHas(check("1,5 mg"), "sc3", "fail");
    }

    // ---------------- sc4: thousands separator ----------------

    @Test
    public void testSc4PointThousandFails() {
        assertHas(check("100.000.000 cellules"), "sc4", "fail");
    }

    // ---------------- sc5: indices ----------------

    @Test
    public void testSc5UnderscoreIndexFails() {
        assertHas(check("IgA_2"), "sc5", "fail");
    }

    @Test
    public void testSc5AdjacentIndexPasses() {
        assertHas(check("Immunoglobuline IgA2"), "sc5", "pass");
    }

    // ---------------- sc6: comparison symbols ----------------

    @Test
    public void testSc6ComparisonFails() {
        assertHas(check("taux < 5 mmol/L"), "sc6", "fail");
    }

    @Test
    public void testSc6ChemicalNotationPasses() {
        assertHas(check("thromboxane B>2<"), "sc6", "pass");
    }

    // ---------------- sc7: plus ----------------

    @Test
    public void testSc7PlusPasses() {
        assertHas(check("groupe A+"), "sc7", "pass");
    }

    // ---------------- sc8: ordinals ----------------

    @Test
    public void testSc8EmeAbbreviationFails() {
        assertHas(check("5ème maladie"), "sc8", "fail");
    }

    @Test
    public void testSc8PreferredAbbrevFails() {
        TranslationCheckContext ctx = new TranslationCheckContext("123", "", "", "premier 5e métacarpien", "fr", "",
                "900000000000013009", "Preferred");
        assertHas(check(ctx), "sc8", "fail");
    }

    @Test
    public void testSc8SynonymAbbrevPasses() {
        assertHas(check("5e maladie"), "sc8", "pass");
    }

    // ---------------- ll1: ligatures ----------------

    @Test
    public void testLl1MissingLigatureFails() {
        assertHas(check("oesophage"), "ll1", "fail");
    }

    @Test
    public void testLl1LigatureUsedPasses() {
        assertHas(check("œsophage"), "ll1", "pass");
    }

    // ---------------- se3: fraction slash ----------------

    @Test
    public void testSe3FractionPasses() {
        assertHas(check("glucose mg/L"), "se3", "pass");
    }

    // ---------------- se4: slash as alternative ----------------

    @Test
    public void testSe4SlashWordUncertain() {
        assertHas(check("rouge/bleu"), "se4", "uncertain");
    }

    @Test
    public void testSe4UnitSlashIgnored() {
        assertNotHas(check("mg/L"), "se4", "uncertain");
    }

    // ---------------- se5: colon taxonomy ----------------

    @Test
    public void testSe5ColonTaxonomyPasses() {
        assertHas(check("sérotype B:K1"), "se5", "pass");
    }

    // ---------------- se6: colon spacing ----------------

    @Test
    public void testSe6OneSidedSpacingFails() {
        assertHas(check("antécédents :goutte"), "se6", "fail");
    }

    @Test
    public void testSe6TwoSpacedPasses() {
        assertHas(check("antécédents : goutte"), "se6", "pass");
    }

    // ---------------- se8: compound hyphen ----------------

    @Test
    public void testSe8CompoundHyphenPasses() {
        assertHas(check("syndrome d'immunodéficience acquise via porte-voix"), "se8", "pass");
    }

    // ---------------- me1: substances without article ----------------

    @Test
    public void testMe1ArticleAfterContainingFailsPreferred() {
        TranslationCheckContext ctx = new TranslationCheckContext("123", "", "", "produit contenant du paracétamol", "fr", "",
                "900000000000013009", "Preferred");
        assertHas(check(ctx), "me1", "fail");
    }

    @Test
    public void testMe1ArticleAfterContainingAcceptableUncertain() {
        assertHas(check("produit contenant du paracétamol"), "me1", "uncertain");
    }

    @Test
    public void testMe1CoordinationUncertain() {
        assertHas(check("produit contenant paracétamol et ibuprofène"), "me1", "uncertain");
    }

    // ---------------- me2: seulement ----------------

    @Test
    public void testMe2SeulementPasses() {
        assertHas(check("produit contenant seulement paracétamol"), "me2", "pass");
    }

    @Test
    public void testMe2UniquementUncertain() {
        assertHas(check("produit contenant uniquement paracétamol"), "me2", "uncertain");
    }

    // ---------------- me3: précisément ----------------

    @Test
    public void testMe3PrecisementUncertain() {
        assertHas(check("produit contenant précisément quinagolide"), "me3", "uncertain");
    }

    // ---------------- me4: libération conventionnelle ----------------

    @Test
    public void testMe4ConventionalReleaseFails() {
        TranslationCheckContext ctx = new TranslationCheckContext("123", "", "",
                "comprimé à libération conventionnelle", "fr", "", "900000000000013009", "Preferred");
        assertHas(check(ctx), "me4", "fail");
    }

    @Test
    public void testMe4SynonymNotChecked() {
        assertNotHas(check("comprimé à libération conventionnelle"), "me4", "fail");
    }

    // ---------------- ec1: Specimen ----------------

    @Test
    public void testEc1SpecimenEnglishFails() {
        TranslationCheckContext ctx = new TranslationCheckContext("123038009", "", "", "specimen", "fr", "",
                "900000000000013009", "Preferred");
        assertHas(check(ctx), "ec1", "fail");
    }

    @Test
    public void testEc1EchantillonPasses() {
        TranslationCheckContext ctx = new TranslationCheckContext("123038009", "", "", "échantillon", "fr", "",
                "900000000000013009", "Acceptable");
        assertHas(check(ctx), "ec1", "pass");
    }

    // ---------------- ec4: échantillon de liquide ----------------

    @Test
    public void testEc4ProvenantDeFails() {
        assertHas(check("échantillon de liquide provenant de la vessie"), "ec4", "fail");
    }

    @Test
    public void testEc4CorrectPasses() {
        assertHas(check("échantillon de liquide céphalorachidien"), "ec4", "pass");
    }

    // ---------------- ec5: prélevé par ----------------

    @Test
    public void testEc5PreleveParPasses() {
        assertHas(check("échantillon de sang prélevé par ponction"), "ec5", "pass");
    }

    // ---------------- ec6: provenant de ----------------

    @Test
    public void testEc6ProvenantDePasses() {
        assertHas(check("échantillon provenant de la trachée"), "ec6", "pass");
    }

    // ---------------- sb1/sb2 ----------------

    @Test
    public void testSb1TubeSousVideNeedsPourPrelevement() {
        assertHas(check("tube sous vide"), "sb1", "uncertain");
    }

    @Test
    public void testSb1WithPourPrelevementPasses() {
        assertHas(check("tube sous vide pour prélèvement veineux"), "sb1", "pass");
    }

    @Test
    public void testSb2SupportSousVideNeedsPourPrelevement() {
        assertHas(check("support sous vide"), "sb2", "uncertain");
    }

    // ---------------- sb3/pa3/pa31/pr2/ec2: human review ----------------

    @Test
    public void testSb3StentUncertain() {
        assertHas(check("endoprothèse coronaire"), "sb3", "uncertain");
        assertHas(check("stent coronaire"), "sb3", "uncertain");
    }

    @Test
    public void testPa31EscarreUncertain() {
        assertHas(check("escarre du talon"), "pa31", "uncertain");
    }

    @Test
    public void testPa3BlessureUncertain() {
        assertHas(check("blessure de la cheville"), "pa3", "uncertain");
    }

    @Test
    public void testPr2ProcedureUncertain() {
        assertHas(check("procédure de diagnostic"), "pr2", "uncertain");
    }

    @Test
    public void testEc2EchantillonUncertain() {
        assertHas(check("échantillon de sang"), "ec2", "uncertain");
    }

    // ---------------- hs1: antécédent ----------------

    @Test
    public void testHs1SingularPasses() {
        assertHas(check("antécédent familial d'asthme"), "hs1", "pass");
    }

    @Test
    public void testHs1PluralUncertain() {
        assertHas(check("antécédents de diabète"), "hs1", "uncertain");
    }

    // ---------------- en4/en5 ----------------

    @Test
    public void testEn4InpatientPasses() {
        assertHas(check("soins pour patient hospitalisé"), "en4", "pass");
    }

    @Test
    public void testEn5OutpatientPasses() {
        assertHas(check("consultation ambulatoire"), "en5", "pass");
    }

    // ---------------- co5: blood groups ----------------

    @Test
    public void testCo5LongFormPasses() {
        assertHas(check("groupe sanguin A Rh(D) positif"), "co5", "pass");
    }

    @Test
    public void testCo5ShortFormUncertain() {
        TranslationCheckContext ctx = new TranslationCheckContext("123", "", "", "groupe A positif", "fr", "",
                "900000000000013009", "Preferred");
        assertHas(check(ctx), "co5", "uncertain");
    }

    // ---------------- ar6/ar7 ----------------

    @Test
    public void testAr6DeviceWithArticleUncertain() {
        assertHas(check("prothèse de la hanche"), "ar6", "uncertain");
    }

    @Test
    public void testAr6DeviceWithoutArticlePasses() {
        assertHas(check("prothèse de hanche"), "ar6", "pass");
    }

    @Test
    public void testAr7AllBothPasses() {
        assertHas(check("tous les doigts"), "ar7", "pass");
        assertHas(check("les deux oreilles"), "ar7", "pass");
    }

    // ---------------- ss5/ss6 ----------------

    @Test
    public void testSs5LatinPluralUncertain() {
        assertHas(check("plusieurs stimuli"), "ss5", "uncertain");
    }

    @Test
    public void testSs6InclusiveDotFails() {
        assertHas(check("amputé.e"), "ss6", "fail");
    }

    @Test
    public void testSs6ParenEndingPasses() {
        assertHas(check("amputé(e)"), "ss6", "pass");
    }

    // ---------------- ss1: taxon exception ----------------

    @Test
    public void testSs1TaxonUpperCasePasses() {
        assertHas(check("Streptococcus pneumoniae"), "ss1", "pass");
    }

    // ---------------- ss4: spelling ----------------

    @Test
    public void testSs4ChirugieFails() {
        assertHas(check("chirugie thoracique"), "ss4", "fail");
    }

    @Test
    public void testSc3CytobandNotFailing() {
        assertNotHas(check("17p11.2"), "sc3", "fail");
        assertNotHas(check("Xp11.22"), "sc3", "fail");
    }

    @Test
    public void testMultipleSpacesFails() {
        assertHas(check("hypertension  artérielle"), "ss4", "fail");
    }

    @Test
    public void testNfcNormalizedBeforeChecks() {
        assertHas(check("otite aigue\u0308"), "or1", "fail");
    }

    @Test
    public void testSc3DecimalStillFails() {
        assertHas(check("1.5 mg"), "sc3", "fail");
    }

    @Test
    public void testSe1ChemicalLocantSkipped() {
        assertNotHas(check("N,N'-dimethylpiperazine"), "se1", "fail");
        assertNotHas(check("45,X"), "se1", "fail");
    }

    @Test
    public void testSe1CommaNoSpaceStillFails() {
        assertHas(check("rouge,gonfle"), "se1", "fail");
    }

    @Test
    public void testSc6MtDNANotFailing() {
        assertNotHas(check("m.1555 A>G"), "sc6", "fail");
        assertNotHas(check("A>G"), "sc6", "fail");
    }

    @Test
    public void testSc6ComparisonStillFails() {
        assertHas(check("taux < 5"), "sc6", "fail");
    }

    @Test
    public void testLl1LatinFoetusPasses() {
        assertNotHas(check("Tritrichomonas foetus"), "ll1", "fail");
    }

    @Test
    public void testLl1OesophageStillFails() {
        assertHas(check("oesophage"), "ll1", "fail");
    }

    @Test
    public void testUm5SerotypeLSkipped() {
        assertNotHas(check("serotype 9L"), "um5", "fail");
        assertNotHas(check("type 2l"), "um5", "fail");
    }

    @Test
    public void testUm5MgStillFails() {
        assertHas(check("40mg"), "um5", "fail");
    }

    @Test
    public void testAr2AdverbialSkipped() {
        assertNotHas(check("au moins 10 figures"), "ar2", "fail");
        assertNotHas(check("au plus 5 entites"), "ar2", "fail");
    }

    // ---------------- null safety ----------------

    @Test
    public void testNullTermDoesNotThrow() {
        List<Finding> findings = check(stub(null));
        assertNotNull(findings);
    }

    @Test
    public void testEmptyTermDoesNotThrow() {
        List<Finding> findings = check(stub(""));
        assertNotNull(findings);
    }
}