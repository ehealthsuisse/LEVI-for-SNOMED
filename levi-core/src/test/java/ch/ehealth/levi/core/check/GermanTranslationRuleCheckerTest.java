package ch.ehealth.levi.core.check;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GermanTranslationRuleCheckerTest {

    private final GermanTranslationRuleChecker checker = new GermanTranslationRuleChecker();

    private TranslationCheckContext ctx(String term) {
        return new TranslationCheckContext("", "", "", term, "de", "ci", "900000000000013009", "");
    }

    private TranslationCheckContext ctx(String term, String fsn, String pt) {
        return new TranslationCheckContext("", fsn, pt, term, "de", "ci", "900000000000013009", "");
    }

    private boolean hasFinding(List<Finding> findings, String ruleId, String status) {
        return findings.stream().anyMatch(f -> ruleId.equals(f.ruleId()) && status.equals(f.status()));
    }

    @Test
    void failsOnLowercaseStart() {
        var findings = checker.check(ctx("akutes abdomen"));
        assertTrue(hasFinding(findings, "ss1", "fail"));
    }

    @Test
    void passesOnUppercaseStart() {
        var findings = checker.check(ctx("Akutes Abdomen"));
        assertFalse(hasFinding(findings, "ss1", "fail"));
    }

    @Test
    void articleAtStartFails() {
        var findings = checker.check(ctx("Der Tumor"));
        assertTrue(hasFinding(findings, "ar2", "fail"));
    }

    @Test
    void decimalPointFails() {
        var findings = checker.check(ctx("1.5 mg"));
        assertTrue(hasFinding(findings, "sc3", "fail"));
    }

    @Test
    void decimalCommaPasses() {
        var findings = checker.check(ctx("1,5 mg"));
        assertFalse(hasFinding(findings, "sc3", "fail"));
    }

    @Test
    void percentSpacingEnforced() {
        assertTrue(hasFinding(checker.check(ctx("75%")), "um6", "fail"));
        assertTrue(hasFinding(checker.check(ctx("75 %")), "um6", "pass"));
    }

    @Test
    void microSymbolReplacement() {
        assertTrue(hasFinding(checker.check(ctx("1 µg")), "um8", "fail"));
        assertTrue(hasFinding(checker.check(ctx("1 ug")), "um8", "pass"));
    }

    @Test
    void slashSpacingWarning() {
        var findings = checker.check(ctx("A / B"));
        assertTrue(hasFinding(findings, "se3", "uncertain"));
    }

    @Test
    void inPatientRuleUsesContext() {
        var findings = checker.check(ctx("Therapie", "Inpatient therapy", "Inpatient therapy"));
        assertTrue(hasFinding(findings, "en4", "fail"));
    }

    @Test
    void phraseRuleMatches() {
        var findings = checker.check(new TranslationCheckContext("", "Abdominal cavity injury", "Abdominal cavity injury",
                "Brustverletzung", "de", "ci", "900000000000013009", ""));
        assertTrue(hasFinding(findings, "cp1", "uncertain"));
    }
}
