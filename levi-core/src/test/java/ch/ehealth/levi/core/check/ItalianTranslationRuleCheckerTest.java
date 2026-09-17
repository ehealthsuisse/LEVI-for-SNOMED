package ch.ehealth.levi.core.check;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ItalianTranslationRuleCheckerTest {

    private final ItalianTranslationRuleChecker checker = new ItalianTranslationRuleChecker();

    private TranslationCheckContext ctx(String term) {
        return ctx(term, "", "", "");
    }

    private TranslationCheckContext ctx(String term, String cid, String fsn, String pt) {
        return new TranslationCheckContext(cid, fsn, pt, term, "it", "ci",
                "900000000000013009", "");
    }

    private TranslationCheckContext ctxPreferred(String term) {
        return new TranslationCheckContext("", "", "", term, "it", "ci",
                "900000000000013009", "Preferred");
    }

    private boolean hasFail(List<Finding> findings, String ruleId) {
        return findings.stream().anyMatch(f -> ruleId.equals(f.ruleId()) && "fail".equals(f.status()));
    }

    private boolean hasPass(List<Finding> findings, String ruleId) {
        return findings.stream().anyMatch(f -> ruleId.equals(f.ruleId()) && "pass".equals(f.status()));
    }

    private boolean hasUncertain(List<Finding> findings, String ruleId) {
        return findings.stream().anyMatch(f -> ruleId.equals(f.ruleId()) && "uncertain".equals(f.status()));
    }

    // ---- ar2: article at start ----

    @Test
    void articleAtStartFails() {
        List<Finding> findings = checker.check(ctx("la sindrome epatorenale"));
        assertTrue(hasFail(findings, "ar2"));
    }

    @Test
    void articleLoAtStartFails() {
        List<Finding> findings = checker.check(ctx("lo pneumotorace"));
        assertTrue(hasFail(findings, "ar2"));
    }

    @Test
    void articleLAtStartFails() {
        List<Finding> findings = checker.check(ctx("l'infarto miocardico"));
        assertTrue(hasFail(findings, "ar2"));
    }

    @Test
    void articleIlAtStartFails() {
        List<Finding> findings = checker.check(ctx("il diabete mellito"));
        assertTrue(hasFail(findings, "ar2"));
    }

    @Test
    void noArticleAtStartPasses() {
        assertFalse(hasFail(checker.check(ctx("sindrome epatorenale")), "ar2"));
        assertFalse(hasFail(checker.check(ctx("diabete mellito")), "ar2"));
    }

    // ---- se10: apostrophe ----

    @Test
    void typographicApostropheFails() {
        List<Finding> findings = checker.check(ctx("malattia dell\u2019orecchio"));
        assertTrue(hasFail(findings, "se10"));
    }

    @Test
    void straightApostrophePasses() {
        List<Finding> findings = checker.check(ctx("malattia dell'orecchio"));
        assertTrue(hasPass(findings, "se10"));
    }

    @Test
    void noApostrophe() {
        assertFalse(checker.check(ctx("diabete mellito")).stream()
                .anyMatch(f -> "se10".equals(f.ruleId())));
    }

    // ---- se11: period at end ----

    @Test
    void periodAtEndFails() {
        List<Finding> findings = checker.check(ctx("diabete mellito."));
        assertTrue(hasFail(findings, "se11"));
    }

    @Test
    void noPeriodAtEndPasses() {
        assertFalse(hasFail(checker.check(ctx("diabete mellito")), "se11"));
    }

    @Test
    void sppExceptionAllowed() {
        assertFalse(hasFail(checker.check(ctx("Candida spp.")), "se11"));
    }

    // ---- sc3: decimal comma ----

    @Test
    void decimalPointFails() {
        List<Finding> findings = checker.check(ctx("1.5 mg"));
        assertTrue(hasFail(findings, "sc3"));
    }

    @Test
    void decimalCommaPasses() {
        assertFalse(hasFail(checker.check(ctx("1,5 mg")), "sc3"));
    }

    // ---- sc6: comparison symbols ----

    @Test
    void lessThanSymbolFails() {
        List<Finding> findings = checker.check(ctx("dimensione < 1 cm"));
        assertTrue(hasFail(findings, "sc6"));
    }

    @Test
    void greaterThanSymbolFails() {
        List<Finding> findings = checker.check(ctx("> 5 cm"));
        assertTrue(hasFail(findings, "sc6"));
    }

    // ---- um4: litre ----

    @Test
    void lowercaseLitreFails() {
        List<Finding> findings = checker.check(ctx("10 ml"));
        assertTrue(hasFail(findings, "um4"));
    }

    @Test
    void uppercaseLitrePasses() {
        List<Finding> findings = checker.check(ctx("10 mL"));
        assertTrue(hasPass(findings, "um4"));
    }

    // ---- um5: space number-unit ----

    @Test
    void noSpaceBeforeUnitFails() {
        List<Finding> findings = checker.check(ctx("10mg"));
        assertTrue(hasFail(findings, "um5"));
    }

    @Test
    void spaceBeforeUnitPasses() {
        List<Finding> findings = checker.check(ctx("10 mg"));
        assertTrue(hasPass(findings, "um5"));
    }

    // ---- um6: percent ----

    @Test
    void noSpaceBeforePercentFails() {
        List<Finding> findings = checker.check(ctx("75%"));
        assertTrue(hasFail(findings, "um6"));
    }

    @Test
    void spaceBeforePercentPasses() {
        List<Finding> findings = checker.check(ctx("75 %"));
        assertTrue(hasPass(findings, "um6"));
    }

    // ---- um8: micro ----

    @Test
    void microSignFails() {
        List<Finding> findings = checker.check(ctx("\u00B5mol/L"));
        assertTrue(hasFail(findings, "um8"));
    }

    @Test
    void microUWithNumberPasses() {
        // "10 ug" contains a digit before ug -> triggers pass
        List<Finding> findings = checker.check(ctx("10 ug"));
        assertTrue(hasPass(findings, "um8"));
    }

    // ---- um2: temperature ----

    @Test
    void celsiusSymbolFails() {
        List<Finding> findings = checker.check(ctx("38 \u00B0C"));
        assertTrue(hasFail(findings, "um2"));
    }

    // ---- se9: parentheses spacing ----

    @Test
    void spaceInsideParenthesesFails() {
        List<Finding> findings = checker.check(ctx("test ( spazio )"));
        assertTrue(hasFail(findings, "se9"));
    }

    @Test
    void correctParenthesesPasses() {
        List<Finding> findings = checker.check(ctx("test(spazio)"));
        assertTrue(hasPass(findings, "se9"));
    }

    // ---- sc4: thousand separator ----

    @Test
    void dotThousandSeparatorFails() {
        List<Finding> findings = checker.check(ctx("100.000.000"));
        assertTrue(hasFail(findings, "sc4"));
    }

    // ---- sc8: ordinals ----

    @Test
    void ordinalAbbreviationInPTPrefersFullForm() {
        List<Finding> findings = checker.check(ctxPreferred("5\u00AA malattia"));
        assertTrue(hasFail(findings, "sc8"));
    }

    @Test
    void ordinalFullFormInPTPassesForSynonym() {
        assertFalse(hasFail(checker.check(ctx("5\u00AA malattia")), "sc8"));
    }

    // ---- ab3: lexicalized acronyms ----

    @Test
    void laserUppercaseFails() {
        List<Finding> findings = checker.check(ctx("LASER"));
        assertTrue(hasFail(findings, "ab3"));
    }

    // ---- ec1: specimen ----

    @Test
    void specimenConceptWrongTerm() {
        List<Finding> findings = checker.check(ctx("specimen", "123038009", "", ""));
        assertTrue(hasFail(findings, "ec1"));
    }

    @Test
    void specimenConceptCorrectTerm() {
        List<Finding> findings = checker.check(ctx("campione", "123038009", "", ""));
        assertTrue(hasPass(findings, "ec1"));
    }

    // ---- drug templates (me1) ----

    @Test
    void drugProductWithArticleUncertainForNonPreferred() {
        List<Finding> findings = checker.check(ctx("prodotto contenente il paracetamolo e la codeina"));
        assertTrue(hasUncertain(findings, "me1"));
    }

    @Test
    void drugProductWithArticleFailsForPreferred() {
        List<Finding> findings = checker.check(ctxPreferred("prodotto contenente il paracetamolo e la codeina"));
        assertTrue(hasFail(findings, "me1"));
    }

    // ---- ss5: latin plurals ----

    @Test
    void latinPluralItalianForm() {
        assertFalse(hasFail(checker.check(ctx("minimi, massimi, stimoli")), "ss5"));
    }

    // ---- se1: comma spacing ----

    @Test
    void commaSpaceBeforeFails() {
        List<Finding> findings = checker.check(ctx("colite , enterite"));
        assertTrue(hasFail(findings, "se1"));
    }

    @Test
    void commaCorrect() {
        assertFalse(hasFail(checker.check(ctx("colite, enterite")), "se1"));
    }

    // ---- um7: superscript ----

    @Test
    void superscriptCharacterFails() {
        List<Finding> findings = checker.check(ctx("mm\u00B3"));
        assertTrue(hasFail(findings, "um7"));
    }

    // ---- ss4: multiple spaces ----

    @Test
    void multipleSpacesTriggersWarning() {
        List<Finding> findings = checker.check(ctx("test  test"));
        // Double space should trigger an ss4 fail
        assertTrue(hasFail(findings, "ss4"));
    }

    // ---- Proper nouns with hyphen ----

    @Test
    void properNameHyphenPasses() {
        List<Finding> findings = checker.check(ctx("virus Epstein-Barr"));
        assertTrue(hasPass(findings, "ss1"));
    }

    // ---- um1: English unit words ----

    @Test
    void englishUnitWordFails() {
        List<Finding> findings = checker.check(ctx("10 milligram"));
        assertTrue(hasFail(findings, "um1"));
    }

    // ---- hs1: history ----

    @Test
    void historyTermPasses() {
        List<Finding> findings = checker.check(ctx("anamnesi familiare di asma"));
        assertTrue(hasPass(findings, "hs1"));
    }

    // ---- en4/en5: inpatient/outpatient ----

    @Test
    void inpatientTermPasses() {
        List<Finding> findings = checker.check(ctx("terapia per paziente ricoverato"));
        assertTrue(hasPass(findings, "en4"));
    }

    @Test
    void outpatientTermPasses() {
        List<Finding> findings = checker.check(ctx("visita ambulatoriale"));
        assertTrue(hasPass(findings, "en5"));
    }
}