package ch.ehealth.levi.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.ehealth.levi.core.export.ResultCollector;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the ResultCollector class.
 * Tests result and error collection functionality.
 */
public class ResultCollectorTest {

    private ResultCollector collector;

    @BeforeEach
    public void setUp() {
        collector = new ResultCollector();
    }

    // 6.1 Test adding entries of different types

    @Test
    public void testAddNewTranslationCurrent() {
        collector.setFullNewTranslationCurrent(
            "123456", "FSN term", "PT term", "Translated term",
            "de", "900000000000020002", "900000000000013009",
            "2041000195100", "900000000000548007",
            "", "", "", "", "", "", "", "", "Test note"
        );
        
        List<List<String>> data = collector.getDataByType("NEW_TRANSLATION_CURRENT");
        assertEquals(1, data.size(), "Should have one NEW_TRANSLATION_CURRENT entry");
        assertEquals("123456", data.get(0).get(0), "Concept ID should match");
        assertEquals("Translated term", data.get(0).get(3), "Term should match");
    }

    @Test
    public void testAddNewTranslationPrevious() {
        collector.setFullNewTranslationPrevious(
            "789012", "FSN old", "PT old", "Old term",
            "fr", "900000000000020002", "900000000000013009",
            "2021000195106", "900000000000548007",
            "", "", "", "", "", "", "", "", "Previous note"
        );
        
        List<List<String>> data = collector.getDataByType("NEW_TRANSLATION_PREVIOUS");
        assertEquals(1, data.size(), "Should have one NEW_TRANSLATION_PREVIOUS entry");
        assertEquals("789012", data.get(0).get(0), "Concept ID should match");
    }

    @Test
    public void testAddExtensionTranslation() {
        collector.setFullExtensionTranslation(
            "111222", "1", "FSN", "PT", "Extension term",
            "de", "900000000000020002", "900000000000013009",
            "2041000195100", "900000000000548007",
            "9999999", "1"
        );
        
        List<List<String>> data = collector.getDataByType("EXTENSION_TRANSLATION");
        assertEquals(1, data.size(), "Should have one EXTENSION_TRANSLATION entry");
        assertEquals("111222", data.get(0).get(0), "Concept ID should match");
        assertEquals("Extension term", data.get(0).get(4), "Term should match");
    }

    @Test
    public void testAddExtensionInactivation() {
        collector.setFullExtensionInactivations(
            "8888888", "de", "333444", "Preferred", "Inactivated term",
            "Reason", "Target1", "Target2", "Target3", "Target4", "Note"
        );
        
        List<List<String>> data = collector.getDataByType("EXTENSION_INACTIVATION");
        assertEquals(1, data.size(), "Should have one EXTENSION_INACTIVATION entry");
        assertEquals("8888888", data.get(0).get(0), "Description ID should match");
    }

    @Test
    public void testAddTranslationChanges() {
        collector.setFullTranslationChanges(
            "7777777", "PT", "Changed term", "900000000000020002",
            "900000000000013009", "RefSet1", "Accept1",
            "", "", "", "", "", "", "", "", "Change note"
        );
        
        List<List<String>> data = collector.getDataByType("TRANSLATION_CHANGES");
        assertEquals(1, data.size(), "Should have one TRANSLATION_CHANGES entry");
        assertEquals("7777777", data.get(0).get(0), "Description ID should match");
    }

    @Test
    public void testAddInactivationsCurrent() {
        collector.setFullInactivationsCurrent("666666", "Term text", "de", "555555");
        
        List<List<String>> data = collector.getDataByType("TRANSLATION_INACTIVATION_CURRENT");
        assertEquals(1, data.size(), "Should have one TRANSLATION_INACTIVATION_CURRENT entry");
        assertEquals("666666", data.get(0).get(0), "Description ID should match");
    }

    // 6.2 Test retrieving and filtering entries

    @Test
    public void testGetDataByTypeWithMultipleEntries() {
        collector.setFullNewTranslationCurrent(
            "111", "FSN1", "PT1", "Term1", "de", "CS1", "Type1",
            "Ref1", "Acc1", "", "", "", "", "", "", "", "", "Note1"
        );
        collector.setFullNewTranslationCurrent(
            "222", "FSN2", "PT2", "Term2", "fr", "CS2", "Type2",
            "Ref2", "Acc2", "", "", "", "", "", "", "", "", "Note2"
        );
        collector.setFullNewTranslationPrevious(
            "333", "FSN3", "PT3", "Term3", "it", "CS3", "Type3",
            "Ref3", "Acc3", "", "", "", "", "", "", "", "", "Note3"
        );
        
        List<List<String>> currentData = collector.getDataByType("NEW_TRANSLATION_CURRENT");
        assertEquals(2, currentData.size(), "Should have two NEW_TRANSLATION_CURRENT entries");
        
        List<List<String>> previousData = collector.getDataByType("NEW_TRANSLATION_PREVIOUS");
        assertEquals(1, previousData.size(), "Should have one NEW_TRANSLATION_PREVIOUS entry");
    }

    @Test
    public void testGetIdsByType() {
        collector.setFullNewTranslationCurrent(
            "100", "FSN", "PT", "Term", "de", "CS", "Type",
            "Ref", "Acc", "", "", "", "", "", "", "", "", "Note"
        );
        collector.setFullNewTranslationCurrent(
            "200", "FSN", "PT", "Term", "fr", "CS", "Type",
            "Ref", "Acc", "", "", "", "", "", "", "", "", "Note"
        );
        
        List<String> ids = collector.getIdsByType("NEW_TRANSLATION_CURRENT");
        assertEquals(2, ids.size(), "Should have two concept IDs");
        assertTrue(ids.contains("100"), "Should contain concept ID 100");
        assertTrue(ids.contains("200"), "Should contain concept ID 200");
    }

    @Test
    public void testContainsType() {
        collector.setFullNewTranslationCurrent(
            "123", "FSN", "PT", "Term", "de", "CS", "Type",
            "Ref", "Acc", "", "", "", "", "", "", "", "", "Note"
        );
        
        assertTrue(collector.containsType("NEW_TRANSLATION_CURRENT"), 
            "Should contain NEW_TRANSLATION_CURRENT type");
        assertFalse(collector.containsType("TRANSLATION_CHANGES"), 
            "Should not contain TRANSLATION_CHANGES type");
    }

    // 6.3 Test empty collector behavior

    @Test
    public void testEmptyCollector() {
        assertTrue(collector.isEmpty(), "New collector should be empty");
    }

    @Test
    public void testIsEmptyAfterAddingEntry() {
        collector.setFullNewTranslationCurrent(
            "123", "FSN", "PT", "Term", "de", "CS", "Type",
            "Ref", "Acc", "", "", "", "", "", "", "", "", "Note"
        );
        
        assertFalse(collector.isEmpty(), "Collector should not be empty after adding entry");
    }

    @Test
    public void testGetDataByTypeOnEmpty() {
        List<List<String>> data = collector.getDataByType("NEW_TRANSLATION_CURRENT");
        assertNotNull(data, "Should return non-null list for empty collector");
        assertEquals(0, data.size(), "Should return empty list for non-existent type");
    }

    @Test
    public void testGetIdsByTypeOnEmpty() {
        List<String> ids = collector.getIdsByType("NEW_TRANSLATION_CURRENT");
        assertNotNull(ids, "Should return non-null list for empty collector");
        assertEquals(0, ids.size(), "Should return empty list for non-existent type");
    }

    @Test
    public void testContainsTypeOnEmpty() {
        assertFalse(collector.containsType("ANY_TYPE"), 
            "Empty collector should not contain any type");
    }

    @Test
    public void testClear() {
        collector.setFullNewTranslationCurrent(
            "123", "FSN", "PT", "Term", "de", "CS", "Type",
            "Ref", "Acc", "", "", "", "", "", "", "", "", "Note"
        );
        
        assertFalse(collector.isEmpty(), "Collector should not be empty");
        
        collector.clear();
        
        assertTrue(collector.isEmpty(), "Collector should be empty after clear");
    }

    @Test
    public void testGetAllEntries() {
        collector.setFullNewTranslationCurrent(
            "123", "FSN", "PT", "Term1", "de", "CS", "Type",
            "Ref", "Acc", "", "", "", "", "", "", "", "", "Note"
        );
        collector.setFullNewTranslationPrevious(
            "456", "FSN", "PT", "Term2", "fr", "CS", "Type",
            "Ref", "Acc", "", "", "", "", "", "", "", "", "Note"
        );
        
        var allEntries = collector.getAllEntries();
        assertEquals(2, allEntries.size(), "Should have two entries total");
    }

    @Test
    public void testMultipleSameTypeEntries() {
        // Add 5 entries of the same type
        for (int i = 1; i <= 5; i++) {
            collector.setFullNewTranslationCurrent(
                String.valueOf(100 + i), "FSN" + i, "PT" + i, "Term" + i,
                "de", "CS", "Type", "Ref", "Acc",
                "", "", "", "", "", "", "", "", "Note" + i
            );
        }
        
        List<List<String>> data = collector.getDataByType("NEW_TRANSLATION_CURRENT");
        assertEquals(5, data.size(), "Should have five entries of the same type");
        
        List<String> ids = collector.getIdsByType("NEW_TRANSLATION_CURRENT");
        assertEquals(5, ids.size(), "Should have five concept IDs");
    }
}
