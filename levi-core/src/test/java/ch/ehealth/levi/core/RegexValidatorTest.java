package ch.ehealth.levi.core;

import org.junit.jupiter.api.Test;

import ch.ehealth.levi.core.compare.RegexValidator;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the RegexValidator class.
 * Tests language-specific validation rules for German, French, and Italian.
 */
public class RegexValidatorTest {

    // 4.1 & 4.2 Test positive and negative cases

    // Test German validation rules

    @Test
    public void testGermanValidTermUppercase() {
        List<String> results = RegexValidator.validateTerm("de", "Diabetes mellitus");
        assertEquals("OK", results.get(0), "Valid German term should pass quotes check");
        assertEquals("OK", results.get(1), "Valid German term should pass soft hyphen check");
        assertEquals("OK", results.get(2), "Valid German term should pass space around slash check");
        assertEquals("OK", results.get(3), "Valid German term should pass apostrophe check");
        assertEquals("OK", results.get(4), "German term starting with uppercase should pass");
    }

    @Test
    public void testGermanInvalidTermLowercase() {
        List<String> results = RegexValidator.validateTerm("de", "diabetes mellitus");
        assertEquals("Please check", results.get(4), "German term starting with lowercase should fail");
    }

    @Test
    public void testGermanTermWithValidUppercase() {
        List<String> results = RegexValidator.validateTerm("de", "Akute Bronchitis");
        assertEquals("OK", results.get(4), "German term starting with uppercase should be valid");
    }

    // Test French validation rules

    @Test
    public void testFrenchValidTermLowercase() {
        List<String> results = RegexValidator.validateTerm("fr", "diabète sucré");
        assertEquals("OK", results.get(0), "Valid French term should pass quotes check");
        assertEquals("OK", results.get(1), "Valid French term should pass soft hyphen check");
        assertEquals("OK", results.get(2), "Valid French term should pass space around slash check");
        assertEquals("OK", results.get(3), "Valid French term should pass apostrophe check");
        assertEquals("OK", results.get(4), "French term starting with lowercase should pass");
    }

    @Test
    public void testFrenchInvalidTermUppercase() {
        List<String> results = RegexValidator.validateTerm("fr", "Diabète sucré");
        assertEquals("Please check", results.get(4), "French term starting with uppercase should fail");
    }

    // Test Italian validation rules

    @Test
    public void testItalianValidTermLowercase() {
        List<String> results = RegexValidator.validateTerm("it", "diabete mellito");
        assertEquals("OK", results.get(0), "Valid Italian term should pass quotes check");
        assertEquals("OK", results.get(1), "Valid Italian term should pass soft hyphen check");
        assertEquals("OK", results.get(2), "Valid Italian term should pass space around slash check");
        assertEquals("OK", results.get(3), "Valid Italian term should pass apostrophe check");
        assertEquals("OK", results.get(4), "Italian term starting with lowercase should pass");
    }

    @Test
    public void testItalianInvalidTermUppercase() {
        List<String> results = RegexValidator.validateTerm("it", "Diabete mellito");
        assertEquals("Please check", results.get(4), "Italian term starting with uppercase should fail");
    }

    // Test forbidden special characters

    @Test
    public void testTermWithQuotes() {
        List<String> results = RegexValidator.validateTerm("de", "Test \"quoted\" term");
        assertEquals("Please check", results.get(0), "Term with quotes should fail");
    }

    @Test
    public void testTermWithCurlyQuotes() {
        // Using Unicode escapes for curly quotes U+201C and U+201D
        List<String> results = RegexValidator.validateTerm("de", "Test \u201Ccurly\u201D term");
        assertEquals("Please check", results.get(0), "Term with curly quotes should fail");
    }

    @Test
    public void testTermWithGermanQuotes() {
        // Using Unicode escapes for German quotes U+201E and U+201C
        List<String> results = RegexValidator.validateTerm("de", "Test \u201EGerman\u201C term");
        assertEquals("Please check", results.get(0), "Term with German quotes should fail");
    }

    @Test
    public void testTermWithoutQuotes() {
        List<String> results = RegexValidator.validateTerm("de", "Normal term without quotes");
        assertEquals("OK", results.get(0), "Term without quotes should pass");
    }

    @Test
    public void testTermWithSoftHyphen() {
        // Soft hyphen is Unicode U+00AD
        List<String> results = RegexValidator.validateTerm("de", "Test\u00ADterm");
        assertEquals("Please check", results.get(1), "Term with soft hyphen should fail");
    }

    @Test
    public void testTermWithoutSoftHyphen() {
        List<String> results = RegexValidator.validateTerm("de", "Test-term");
        assertEquals("OK", results.get(1), "Term with regular hyphen should pass");
    }

    @Test
    public void testTermWithSpaceBeforeSlash() {
        List<String> results = RegexValidator.validateTerm("de", "Test /term");
        assertEquals("Please check", results.get(2), "Term with space before slash should fail");
    }

    @Test
    public void testTermWithSpaceAfterSlash() {
        List<String> results = RegexValidator.validateTerm("de", "Test/ term");
        assertEquals("Please check", results.get(2), "Term with space after slash should fail");
    }

    @Test
    public void testTermWithSpaceAroundSlash() {
        List<String> results = RegexValidator.validateTerm("de", "Test / term");
        assertEquals("Please check", results.get(2), "Term with spaces around slash should fail");
    }

    @Test
    public void testTermWithValidSlash() {
        List<String> results = RegexValidator.validateTerm("de", "Test/term");
        assertEquals("OK", results.get(2), "Term with slash without spaces should pass");
    }

    // 4.3 Test language-specific behavior

    @Test
    public void testSameTermDifferentLanguages() {
        String term = "Diabetes";
        
        List<String> germanResults = RegexValidator.validateTerm("de", term);
        assertEquals("OK", germanResults.get(4), "German should accept uppercase start");
        
        List<String> frenchResults = RegexValidator.validateTerm("fr", term);
        assertEquals("Please check", frenchResults.get(4), "French should reject uppercase start");
        
        List<String> italianResults = RegexValidator.validateTerm("it", term);
        assertEquals("Please check", italianResults.get(4), "Italian should reject uppercase start");
    }

    @Test
    public void testLowercaseTermDifferentLanguages() {
        String term = "diabetes";
        
        List<String> germanResults = RegexValidator.validateTerm("de", term);
        assertEquals("Please check", germanResults.get(4), "German should reject lowercase start");
        
        List<String> frenchResults = RegexValidator.validateTerm("fr", term);
        assertEquals("OK", frenchResults.get(4), "French should accept lowercase start");
        
        List<String> italianResults = RegexValidator.validateTerm("it", term);
        assertEquals("OK", italianResults.get(4), "Italian should accept lowercase start");
    }

    // 4.4 Test multiple rule violations

    @Test
    public void testMultipleViolationsGerman() {
        // lowercase start + quotes + soft hyphen + space before slash
        String term = "test \"quoted\" \u00ADterm /with";
        List<String> results = RegexValidator.validateTerm("de", term);
        
        assertEquals("Please check", results.get(0), "Should detect quotes violation");
        assertEquals("Please check", results.get(1), "Should detect soft hyphen violation");
        assertEquals("Please check", results.get(2), "Should detect space slash violation");
        assertEquals("Please check", results.get(4), "Should detect lowercase violation for German");
    }

    @Test
    public void testMultipleViolationsFrench() {
        // uppercase start + quotes + space after slash
        String term = "Test \"quoted\"/ term";
        List<String> results = RegexValidator.validateTerm("fr", term);
        
        assertEquals("Please check", results.get(0), "Should detect quotes violation");
        assertEquals("Please check", results.get(2), "Should detect space slash violation");
        assertEquals("Please check", results.get(4), "Should detect uppercase violation for French");
    }

    @Test
    public void testAllRulesPass() {
        List<String> germanResults = RegexValidator.validateTerm("de", "Perfekte Bezeichnung");
        assertEquals("OK", germanResults.get(0), "Should pass quotes check");
        assertEquals("OK", germanResults.get(1), "Should pass soft hyphen check");
        assertEquals("OK", germanResults.get(2), "Should pass space slash check");
        assertEquals("OK", germanResults.get(3), "Should pass apostrophe check");
        assertEquals("OK", germanResults.get(4), "Should pass case check");
    }

    // Test null and empty strings

    @Test
    public void testNullTerm() {
        List<String> results = RegexValidator.validateTerm("de", null);
        assertEquals(5, results.size(), "Should return 5 validation results even for null");
        assertEquals("OK", results.get(0), "Null term should pass quotes check");
        assertEquals("OK", results.get(1), "Null term should pass soft hyphen check");
        assertEquals("OK", results.get(2), "Null term should pass space slash check");
        assertEquals("OK", results.get(3), "Null term should pass apostrophe check");
        assertEquals("Please check", results.get(4), "Null term should fail uppercase check for German");
    }

    @Test
    public void testEmptyTerm() {
        List<String> results = RegexValidator.validateTerm("de", "");
        assertEquals(5, results.size(), "Should return 5 validation results for empty string");
    }

    // Test unknown language code

    @Test
    public void testUnknownLanguageCode() {
        List<String> results = RegexValidator.validateTerm("es", "Término español");
        assertEquals(5, results.size(), "Should return 5 validation results");
        assertEquals("OK", results.get(0), "Should still check general rules");
        assertEquals("language code not recognized and therefore not checked", results.get(4), 
            "Should indicate unknown language for case check");
    }

    @Test
    public void testValidationResultsSize() {
        List<String> results = RegexValidator.validateTerm("de", "Test");
        assertEquals(5, results.size(), "Should always return exactly 5 validation results");
    }
}
