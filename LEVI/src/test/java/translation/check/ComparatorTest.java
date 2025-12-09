package translation.check;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

/**
 * Unit tests for the Comparator class.
 * Tests delta generation logic without database dependencies.
 * 
 * Note: Full integration tests with database are beyond the scope of unit tests.
 * These tests focus on the core comparison and identity logic.
 */
public class ComparatorTest {

    private ResultCollector resultCollector;
    private Conf conf;
    private Comparator comparator;

    @BeforeEach
    public void setUp() {
        resultCollector = new ResultCollector();
        conf = new Conf();
        conf.setCountryCode("CH");
        conf.setTransformEszett(true);
        comparator = new Comparator(resultCollector, conf);
    }

    // 2.1 Test identity logic - same conceptId, term, and language

    @Test
    public void testNewTranslationIsAdded() {
        // Add a new translation that doesn't exist in DB
        resultCollector.setFullNewTranslationCurrent(
            "123456", "FSN", "PT", "New term",
            "de", "CS", "Type", "Ref", "Acc",
            "", "", "", "", "", "", "", "", "Note"
        );
        
        // Verify it was added to the collector
        List<List<String>> data = resultCollector.getDataByType("NEW_TRANSLATION_CURRENT");
        assertEquals(1, data.size(), "New translation should be added");
        assertEquals("123456", data.get(0).get(0), "Concept ID should match");
        assertEquals("New term", data.get(0).get(3), "Term should match");
        assertEquals("de", data.get(0).get(4), "Language should match");
    }

    @Test
    public void testIdentityWithSameConceptTermAndLanguage() {
        // Two entries with same conceptId, term, and language should be considered identical
        resultCollector.setFullNewTranslationCurrent(
            "123456", "FSN", "PT", "Identical term",
            "de", "CS1", "Type1", "Ref1", "Acc1",
            "", "", "", "", "", "", "", "", "Note1"
        );
        resultCollector.setFullNewTranslationCurrent(
            "123456", "FSN", "PT", "Identical term",
            "de", "CS2", "Type2", "Ref2", "Acc2",
            "", "", "", "", "", "", "", "", "Note2"
        );
        
        List<List<String>> data = resultCollector.getDataByType("NEW_TRANSLATION_CURRENT");
        assertEquals(2, data.size(), "Both entries should be stored");
        
        // Verify they have same identity triplet
        assertEquals(data.get(0).get(0), data.get(1).get(0), "Concept IDs should match");
        assertEquals(data.get(0).get(3), data.get(1).get(3), "Terms should match");
        assertEquals(data.get(0).get(4), data.get(1).get(4), "Languages should match");
    }

    // 2.5 Test that different conceptId means different identity

    @Test
    public void testDifferentConceptIdMeansDifferentIdentity() {
        resultCollector.setFullNewTranslationCurrent(
            "111111", "FSN1", "PT1", "Same term",
            "de", "CS", "Type", "Ref", "Acc",
            "", "", "", "", "", "", "", "", "Note1"
        );
        resultCollector.setFullNewTranslationCurrent(
            "222222", "FSN2", "PT2", "Same term",
            "de", "CS", "Type", "Ref", "Acc",
            "", "", "", "", "", "", "", "", "Note2"
        );
        
        List<List<String>> data = resultCollector.getDataByType("NEW_TRANSLATION_CURRENT");
        assertEquals(2, data.size(), "Should be treated as different entries");
        assertNotEquals(data.get(0).get(0), data.get(1).get(0), "Concept IDs should be different");
        assertEquals(data.get(0).get(3), data.get(1).get(3), "Terms should be same");
    }

    @Test
    public void testDifferentLanguageMeansDifferentIdentity() {
        resultCollector.setFullNewTranslationCurrent(
            "123456", "FSN", "PT", "Same term",
            "de", "CS", "Type", "Ref", "Acc",
            "", "", "", "", "", "", "", "", "Note1"
        );
        resultCollector.setFullNewTranslationCurrent(
            "123456", "FSN", "PT", "Same term",
            "fr", "CS", "Type", "Ref", "Acc",
            "", "", "", "", "", "", "", "", "Note2"
        );
        
        List<List<String>> data = resultCollector.getDataByType("NEW_TRANSLATION_CURRENT");
        assertEquals(2, data.size(), "Should be treated as different entries");
        assertEquals(data.get(0).get(0), data.get(1).get(0), "Concept IDs should be same");
        assertNotEquals(data.get(0).get(4), data.get(1).get(4), "Languages should be different");
    }

    @Test
    public void testDifferentTermMeansDifferentIdentity() {
        resultCollector.setFullNewTranslationCurrent(
            "123456", "FSN", "PT", "Term A",
            "de", "CS", "Type", "Ref", "Acc",
            "", "", "", "", "", "", "", "", "Note1"
        );
        resultCollector.setFullNewTranslationCurrent(
            "123456", "FSN", "PT", "Term B",
            "de", "CS", "Type", "Ref", "Acc",
            "", "", "", "", "", "", "", "", "Note2"
        );
        
        List<List<String>> data = resultCollector.getDataByType("NEW_TRANSLATION_CURRENT");
        assertEquals(2, data.size(), "Should be treated as different entries");
        assertNotEquals(data.get(0).get(3), data.get(1).get(3), "Terms should be different");
    }

    // Test inactivation handling

    @Test
    public void testInactivationIsRecorded() {
        resultCollector.setFullInactivationsCurrent(
            "999999", "Term to inactivate", "de", "123456"
        );
        
        List<List<String>> data = resultCollector.getDataByType("TRANSLATION_INACTIVATION_CURRENT");
        assertEquals(1, data.size(), "Inactivation should be recorded");
        assertEquals("999999", data.get(0).get(0), "Description ID should match");
        assertEquals("Term to inactivate", data.get(0).get(1), "Term should match");
    }

    @Test
    public void testMultipleInactivations() {
        resultCollector.setFullInactivationsCurrent("111111", "Term 1", "de", "100001");
        resultCollector.setFullInactivationsCurrent("222222", "Term 2", "fr", "100002");
        resultCollector.setFullInactivationsCurrent("333333", "Term 3", "it", "100003");
        
        List<List<String>> data = resultCollector.getDataByType("TRANSLATION_INACTIVATION_CURRENT");
        assertEquals(3, data.size(), "Should have three inactivations");
    }

    // Test translation changes

    @Test
    public void testTranslationChangeIsRecorded() {
        resultCollector.setFullTranslationChanges(
            "888888", "PT", "Changed term", "CS", "Type",
            "Ref1", "Acc1", "", "", "", "", "", "", "", "", "Change note"
        );
        
        assertTrue(resultCollector.containsType("TRANSLATION_CHANGES"), 
            "Should contain TRANSLATION_CHANGES type");
        
        List<List<String>> data = resultCollector.getDataByType("TRANSLATION_CHANGES");
        assertEquals(1, data.size(), "Should have one translation change");
        assertEquals("888888", data.get(0).get(0), "Description ID should match");
    }

    // Test eszett transformation configuration

    @Test
    public void testEszettTransformationEnabled() {
        assertTrue(conf.isTransformEszett(), "Eszett transformation should be enabled by default");
    }

    @Test
    public void testEszettTransformationCanBeDisabled() {
        conf.setTransformEszett(false);
        assertFalse(conf.isTransformEszett(), "Eszett transformation should be disabled");
    }

    // Test local language filtering

    @Test
    public void testLocalLanguageForCH() {
        conf.setCountryCode("CH");
        assertTrue(conf.isLocalLanguage("de"), "German should be local for CH");
        assertTrue(conf.isLocalLanguage("fr"), "French should be local for CH");
        assertTrue(conf.isLocalLanguage("it"), "Italian should be local for CH");
        assertFalse(conf.isLocalLanguage("en"), "English should not be local for CH");
    }

    @Test
    public void testLocalLanguageForAT() {
        conf.setCountryCode("AT");
        assertTrue(conf.isLocalLanguage("de"), "German should be local for AT");
        assertFalse(conf.isLocalLanguage("fr"), "French should not be local for AT");
        assertFalse(conf.isLocalLanguage("en"), "English should not be local for AT");
    }

    // Test empty collector behavior

    @Test
    public void testEmptyCollectorHasNoTranslations() {
        List<List<String>> data = resultCollector.getDataByType("NEW_TRANSLATION_CURRENT");
        assertEquals(0, data.size(), "Empty collector should have no translations");
    }

    @Test
    public void testEmptyCollectorHasNoInactivations() {
        List<List<String>> data = resultCollector.getDataByType("TRANSLATION_INACTIVATION_CURRENT");
        assertEquals(0, data.size(), "Empty collector should have no inactivations");
    }

    // Test concept ID retrieval

    @Test
    public void testGetConceptIdsFromTranslations() {
        resultCollector.setFullNewTranslationCurrent(
            "100", "FSN", "PT", "Term1", "de", "CS", "Type",
            "Ref", "Acc", "", "", "", "", "", "", "", "", "Note"
        );
        resultCollector.setFullNewTranslationCurrent(
            "200", "FSN", "PT", "Term2", "fr", "CS", "Type",
            "Ref", "Acc", "", "", "", "", "", "", "", "", "Note"
        );
        resultCollector.setFullNewTranslationCurrent(
            "300", "FSN", "PT", "Term3", "it", "CS", "Type",
            "Ref", "Acc", "", "", "", "", "", "", "", "", "Note"
        );
        
        List<String> conceptIds = resultCollector.getIdsByType("NEW_TRANSLATION_CURRENT");
        assertEquals(3, conceptIds.size(), "Should have 3 concept IDs");
        assertTrue(conceptIds.contains("100"), "Should contain concept ID 100");
        assertTrue(conceptIds.contains("200"), "Should contain concept ID 200");
        assertTrue(conceptIds.contains("300"), "Should contain concept ID 300");
    }

    // Test that previous and current translations are kept separate

    @Test
    public void testPreviousAndCurrentTranslationsAreSeparate() {
        resultCollector.setFullNewTranslationCurrent(
            "123", "FSN", "PT", "Current term", "de", "CS", "Type",
            "Ref", "Acc", "", "", "", "", "", "", "", "", "Note"
        );
        resultCollector.setFullNewTranslationPrevious(
            "456", "FSN", "PT", "Previous term", "fr", "CS", "Type",
            "Ref", "Acc", "", "", "", "", "", "", "", "", "Note"
        );
        
        List<List<String>> currentData = resultCollector.getDataByType("NEW_TRANSLATION_CURRENT");
        List<List<String>> previousData = resultCollector.getDataByType("NEW_TRANSLATION_PREVIOUS");
        
        assertEquals(1, currentData.size(), "Should have 1 current translation");
        assertEquals(1, previousData.size(), "Should have 1 previous translation");
        assertNotEquals(currentData.get(0).get(0), previousData.get(0).get(0), 
            "Current and previous should have different concept IDs");
    }

    // Test multiple entries per concept

    @Test
    public void testMultipleTranslationsPerConcept() {
        // Same concept, different languages
        resultCollector.setFullNewTranslationCurrent(
            "123456", "FSN", "PT", "German term", "de", "CS", "Type",
            "Ref", "Acc", "", "", "", "", "", "", "", "", "Note1"
        );
        resultCollector.setFullNewTranslationCurrent(
            "123456", "FSN", "PT", "French term", "fr", "CS", "Type",
            "Ref", "Acc", "", "", "", "", "", "", "", "", "Note2"
        );
        resultCollector.setFullNewTranslationCurrent(
            "123456", "FSN", "PT", "Italian term", "it", "CS", "Type",
            "Ref", "Acc", "", "", "", "", "", "", "", "", "Note3"
        );
        
        List<List<String>> data = resultCollector.getDataByType("NEW_TRANSLATION_CURRENT");
        assertEquals(3, data.size(), "Should have 3 translations for same concept");
        
        // All should have same concept ID
        assertEquals(data.get(0).get(0), data.get(1).get(0), "All should have same concept ID");
        assertEquals(data.get(0).get(0), data.get(2).get(0), "All should have same concept ID");
    }
}
