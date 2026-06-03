package ch.ehealth.levi.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ch.ehealth.levi.core.compare.Comparator;
import ch.ehealth.levi.core.export.ResultCollector;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.sql.SQLException;
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
    private DbConnection mockDbConnection;
    private InputStream originalSystemIn;

    @BeforeEach
    public void setUp() throws Exception {
        resultCollector = new ResultCollector();
        conf = new Conf();
        conf.setCountryCode("CH");
        conf.setTransformEszett(true);
        comparator = new Comparator(resultCollector, conf);

        // Inject mock DbConnection to avoid real DB connections in unit tests
        mockDbConnection = Mockito.mock(DbConnection.class);
        Field dbConnectionField = Comparator.class.getDeclaredField("dbConnection");
        dbConnectionField.setAccessible(true);
        dbConnectionField.set(comparator, mockDbConnection);

        originalSystemIn = System.in;
    }

    @AfterEach
    public void tearDown() {
        System.setIn(originalSystemIn);
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

    // =========================================================
    // Tests for Comparator.generateDescriptionChangesDelta()
    // =========================================================

    @Test
    public void testGenerateDescriptionChangesDeltaHeaderOnly() throws IOException, SQLException, ClassNotFoundException {
        List<List<String>> result = comparator.generateDescriptionChangesDelta("TRANSLATION_CHANGES");
        assertEquals(1, result.size(), "Should contain only the header row when no entries exist");
        assertEquals("Description ID", result.get(0).get(0), "First column header should be Description ID");
    }

    @Test
    public void testGenerateDescriptionChangesDeltaWithOneEntry() throws IOException, SQLException, ClassNotFoundException {
        resultCollector.setFullTranslationChanges(
            "7777777", "PT", "Changed term", "CS", "Type",
            "Ref1", "Acc1", "", "", "", "", "", "", "", "", "Change note"
        );
        List<List<String>> result = comparator.generateDescriptionChangesDelta("TRANSLATION_CHANGES");
        assertEquals(2, result.size(), "Should have header + 1 entry");
        assertEquals("7777777", result.get(1).get(0), "Description ID should match");
        assertEquals("Changed term", result.get(1).get(2), "Term should match");
    }

    @Test
    public void testGenerateDescriptionChangesDeltaWithReactivation() throws IOException, SQLException, ClassNotFoundException {
        resultCollector.setFullTranslationReactivation(
            "8888888", "PT", "Reactivated term", "CS", "Type",
            "Ref1", "Acc1", "", "", "", "", "", "", "", "", "Reactivation note"
        );
        List<List<String>> result = comparator.generateDescriptionChangesDelta("TRANSLATION_REACTIVATION");
        assertEquals(2, result.size(), "Should have header + 1 reactivation entry");
        assertEquals("8888888", result.get(1).get(0), "Description ID should match");
    }

    @Test
    public void testGenerateDescriptionChangesDeltaWithMultipleEntries() throws IOException, SQLException, ClassNotFoundException {
        resultCollector.setFullTranslationChanges(
            "1111111", "PT", "Term 1", "CS", "Type", "R", "A", "", "", "", "", "", "", "", "", "Note"
        );
        resultCollector.setFullTranslationChanges(
            "2222222", "PT", "Term 2", "CS", "Type", "R", "A", "", "", "", "", "", "", "", "", "Note"
        );
        List<List<String>> result = comparator.generateDescriptionChangesDelta("TRANSLATION_CHANGES");
        assertEquals(3, result.size(), "Should have header + 2 entries");
    }

    @Test
    public void testGenerateDescriptionChangesDeltaUnknownTypeReturnsHeaderOnly() throws IOException, SQLException, ClassNotFoundException {
        // Populate a different type - it should not appear in the result
        resultCollector.setFullTranslationChanges(
            "1111111", "PT", "Term", "CS", "Type", "R", "A", "", "", "", "", "", "", "", "", "Note"
        );
        List<List<String>> result = comparator.generateDescriptionChangesDelta("TRANSLATION_REACTIVATION");
        assertEquals(1, result.size(), "Requesting a different type should return only the header");
    }

    // =========================================================
    // Tests for Comparator.generateDeltaOfNotPublishedTranslations()
    // =========================================================

    @Test
    public void testDeltaOfNotPublished_EmptyInputReturnsHeaderOnly() throws IOException, SQLException, ClassNotFoundException {
        List<List<String>> result = comparator.generateDeltaOfNotPublishedTranslations();
        assertEquals(1, result.size(), "Empty input should return only the header row");
        assertEquals("Description ID", result.get(0).get(0), "First column header should be Description ID");
    }

    @Test
    public void testDeltaOfNotPublished_PreviousEntryNotInCurrentAppearsInDelta() throws IOException, SQLException, ClassNotFoundException {
        resultCollector.setFullNewTranslationPrevious(
            "C1", "FSN", "PT", "My term", "de", "CS", "Type",
            "Ref", "Acc", "", "", "", "", "", "", "", "", "Note"
        );

        List<List<String>> result = comparator.generateDeltaOfNotPublishedTranslations();

        assertEquals(2, result.size(), "Should have header + 1 delta entry");
        List<String> deltaRow = result.get(1);
        assertEquals("", deltaRow.get(0), "Description ID should be empty for not-published entries");
        assertEquals("de", deltaRow.get(1), "Language code should be at index 1");
        assertEquals("C1", deltaRow.get(2), "Concept ID should be at index 2");
        assertEquals("My term", deltaRow.get(4), "Term should be at index 4");
    }

    @Test
    public void testDeltaOfNotPublished_PreviousEntryInCurrentExcludedFromDelta() throws IOException, SQLException, ClassNotFoundException {
        resultCollector.setFullNewTranslationPrevious(
            "C1", "FSN", "PT", "My term", "de", "CS", "Type",
            "Ref", "Acc", "", "", "", "", "", "", "", "", "Note"
        );
        resultCollector.setFullNewTranslationCurrent(
            "C1", "FSN", "PT", "My term", "de", "CS", "Type",
            "Ref", "Acc", "", "", "", "", "", "", "", "", "Note"
        );

        List<List<String>> result = comparator.generateDeltaOfNotPublishedTranslations();
        assertEquals(1, result.size(), "Should have only header when entry exists in current");
    }

    @Test
    public void testDeltaOfNotPublished_PreviousEntryInactivatedExcludedFromDelta() throws IOException, SQLException, ClassNotFoundException {
        resultCollector.setFullNewTranslationPrevious(
            "C1", "FSN", "PT", "My term", "de", "CS", "Type",
            "Ref", "Acc", "", "", "", "", "", "", "", "", "Note"
        );
        // Inactivation entry: [descriptionId, term, languageCode, conceptId]
        resultCollector.setFullInactivationsCurrent("D1", "My term", "de", "C1");

        List<List<String>> result = comparator.generateDeltaOfNotPublishedTranslations();
        assertEquals(1, result.size(), "Should have only header when previous entry is inactivated");
    }

    @Test
    public void testDeltaOfNotPublished_DifferentLanguageTreatedSeparately() throws IOException, SQLException, ClassNotFoundException {
        resultCollector.setFullNewTranslationPrevious(
            "C1", "FSN", "PT", "My term", "de", "CS", "Type",
            "Ref", "Acc", "", "", "", "", "", "", "", "", "Note"
        );
        // Current has same concept + term but different language (fr)
        resultCollector.setFullNewTranslationCurrent(
            "C1", "FSN", "PT", "My term", "fr", "CS", "Type",
            "Ref", "Acc", "", "", "", "", "", "", "", "", "Note"
        );

        List<List<String>> result = comparator.generateDeltaOfNotPublishedTranslations();
        assertEquals(2, result.size(), "DE previous entry should appear in delta despite FR current entry");
    }

    @Test
    public void testDeltaOfNotPublished_DifferentTermTreatedSeparately() throws IOException, SQLException, ClassNotFoundException {
        resultCollector.setFullNewTranslationPrevious(
            "C1", "FSN", "PT", "Old term", "de", "CS", "Type",
            "Ref", "Acc", "", "", "", "", "", "", "", "", "Note"
        );
        resultCollector.setFullNewTranslationCurrent(
            "C1", "FSN", "PT", "New term", "de", "CS", "Type",
            "Ref", "Acc", "", "", "", "", "", "", "", "", "Note"
        );

        List<List<String>> result = comparator.generateDeltaOfNotPublishedTranslations();
        assertEquals(2, result.size(), "Previous entry with different term should appear in delta");
    }

    @Test
    public void testDeltaOfNotPublished_MultiplePreviousEntriesSomeExcluded() throws IOException, SQLException, ClassNotFoundException {
        // Entry 1: only in PREVIOUS → included
        resultCollector.setFullNewTranslationPrevious(
            "C1", "FSN", "PT", "Only previous", "de", "CS", "Type",
            "Ref", "Acc", "", "", "", "", "", "", "", "", "Note"
        );
        // Entry 2: also in CURRENT → excluded
        resultCollector.setFullNewTranslationPrevious(
            "C2", "FSN", "PT", "In current too", "de", "CS", "Type",
            "Ref", "Acc", "", "", "", "", "", "", "", "", "Note"
        );
        resultCollector.setFullNewTranslationCurrent(
            "C2", "FSN", "PT", "In current too", "de", "CS", "Type",
            "Ref", "Acc", "", "", "", "", "", "", "", "", "Note"
        );

        List<List<String>> result = comparator.generateDeltaOfNotPublishedTranslations();
        assertEquals(2, result.size(), "Should have header + 1 entry (only the one missing from current)");
        assertEquals("C1", result.get(1).get(2), "Only the entry not in current should appear");
    }

    // =========================================================
    // Tests for Comparator.generateDescriptionInactivationDelta()
    // =========================================================

    @Test
    public void testGenerateDescriptionInactivationDelta_ForeignRowHasForeignNote() throws IOException, SQLException, ClassNotFoundException {
        // "en" is a foreign language for CH
        resultCollector.setFullInactivationsCurrent("D1", "English term", "en", "C1");
        Mockito.doNothing().when(mockDbConnection).searchDescriptions(Mockito.anyList());

        List<List<String>> result = comparator.generateDescriptionInactivationDelta();

        assertEquals(2, result.size(), "Should have header + 1 foreign row");
        List<String> row = result.get(1);
        assertEquals("D1", row.get(0), "Description ID should be at index 0");
        assertEquals("en", row.get(1), "Language code should be at index 1");
        assertEquals("C1", row.get(2), "Concept ID should be at index 2");
        assertEquals("English term", row.get(4), "Term should be at index 4");
        assertTrue(row.get(10).contains("Foreign language term"), "Note should flag foreign language");
    }

    @Test
    public void testGenerateDescriptionInactivationDelta_EszettTransformedForGerman() throws IOException, SQLException, ClassNotFoundException {
        conf.setTransformEszett(true);
        resultCollector.setFullInactivationsCurrent("D1", "Stra\u00dfe", "de", "C1");
        Mockito.doNothing().when(mockDbConnection).searchDescriptions(Mockito.anyList());

        comparator.generateDescriptionInactivationDelta();

        List<List<String>> inactivations = resultCollector.getDataByType("TRANSLATION_INACTIVATION_CURRENT");
        assertEquals("Strasse", inactivations.get(0).get(1), "Eszett 'ß' should be replaced with 'ss'");
    }

    @Test
    public void testGenerateDescriptionInactivationDelta_EszettNotTransformedWhenDisabled() throws IOException, SQLException, ClassNotFoundException {
        conf.setTransformEszett(false);
        resultCollector.setFullInactivationsCurrent("D1", "Stra\u00dfe", "de", "C1");
        Mockito.doNothing().when(mockDbConnection).searchDescriptions(Mockito.anyList());

        comparator.generateDescriptionInactivationDelta();

        List<List<String>> inactivations = resultCollector.getDataByType("TRANSLATION_INACTIVATION_CURRENT");
        assertEquals("Stra\u00dfe", inactivations.get(0).get(1), "Eszett should not be transformed when disabled");
    }

    @Test
    public void testGenerateDescriptionInactivationDelta_EszettNotTransformedForNonGerman() throws IOException, SQLException, ClassNotFoundException {
        conf.setTransformEszett(true);
        // French entry with 'ß' – should not be transformed
        resultCollector.setFullInactivationsCurrent("D1", "term\u00df", "fr", "C1");
        Mockito.doNothing().when(mockDbConnection).searchDescriptions(Mockito.anyList());

        comparator.generateDescriptionInactivationDelta();

        List<List<String>> inactivations = resultCollector.getDataByType("TRANSLATION_INACTIVATION_CURRENT");
        assertEquals("term\u00df", inactivations.get(0).get(1), "Eszett should only be transformed for German");
    }

    @Test
    public void testGenerateDescriptionInactivationDelta_HeaderIsPresentAlways() throws IOException, SQLException, ClassNotFoundException {
        Mockito.doNothing().when(mockDbConnection).searchDescriptions(Mockito.anyList());

        List<List<String>> result = comparator.generateDescriptionInactivationDelta();

        assertFalse(result.isEmpty(), "Result should always contain at least the header");
        assertEquals("Description ID", result.get(0).get(0), "First column should be Description ID");
    }

    // =========================================================
    // Tests for Comparator.generateDescriptionAdditionAndChangesDelta()
    // =========================================================

    @Test
    public void testGenerateDescriptionAdditionDelta_NewEntryAppearsInDelta() throws IOException, SQLException, ClassNotFoundException {
        // Simulate pressing Enter to skip language selection
        System.setIn(new ByteArrayInputStream("\n".getBytes()));
        Mockito.doNothing().when(mockDbConnection).searchTranslations(Mockito.anySet());

        resultCollector.setFullNewTranslationCurrent(
            "C1", "FSN", "PT", "New term", "de", "CS", "Type", "Ref", "Acc",
            "", "", "", "", "", "", "", "", "Note"
        );

        List<List<String>> result = comparator.generateDescriptionAdditionAndChangesDelta();

        assertTrue(result.size() > 1, "Delta should contain at least the new entry beyond the header");
        boolean found = result.stream().skip(1).anyMatch(row -> row.contains("New term"));
        assertTrue(found, "New entry without DB match should appear in the delta");
    }

    @Test
    public void testGenerateDescriptionAdditionDelta_EszettTransformedForGerman() throws IOException, SQLException, ClassNotFoundException {
        System.setIn(new ByteArrayInputStream("\n".getBytes()));
        Mockito.doNothing().when(mockDbConnection).searchTranslations(Mockito.anySet());

        resultCollector.setFullNewTranslationCurrent(
            "C1", "FSN", "PT", "Stra\u00dfe", "de", "CS", "Type", "Ref", "Acc",
            "", "", "", "", "", "", "", "", "Note"
        );

        List<List<String>> result = comparator.generateDescriptionAdditionAndChangesDelta();

        boolean found = result.stream().skip(1).anyMatch(row -> row.contains("Strasse"));
        assertTrue(found, "German term with 'ß' should be transformed to 'ss' in the delta");
    }

    @Test
    public void testGenerateDescriptionAdditionDelta_HeaderContainsRegexColumnsWhenEnabled() throws IOException, SQLException, ClassNotFoundException {
        conf.setRegexCheck(true);
        System.setIn(new ByteArrayInputStream("\n".getBytes()));
        Mockito.doNothing().when(mockDbConnection).searchTranslations(Mockito.anySet());

        resultCollector.setFullNewTranslationCurrent(
            "C1", "FSN", "PT", "Some term", "de", "CS", "Type", "Ref", "Acc",
            "", "", "", "", "", "", "", "", "Note"
        );

        List<List<String>> result = comparator.generateDescriptionAdditionAndChangesDelta();

        List<String> header = result.get(0);
        assertTrue(header.contains("Quotes"), "Header should contain 'Quotes' when regex check is enabled");
        assertTrue(header.contains("SoftHyphen"), "Header should contain 'SoftHyphen' when regex check is enabled");
        assertTrue(header.contains("SpaceAroundSlash"), "Header should contain 'SpaceAroundSlash' when regex check is enabled");
    }

    @Test
    public void testGenerateDescriptionAdditionDelta_HeaderNoRegexColumnsWhenDisabled() throws IOException, SQLException, ClassNotFoundException {
        conf.setRegexCheck(false);
        System.setIn(new ByteArrayInputStream("\n".getBytes()));
        Mockito.doNothing().when(mockDbConnection).searchTranslations(Mockito.anySet());

        List<List<String>> result = comparator.generateDescriptionAdditionAndChangesDelta();

        List<String> header = result.get(0);
        assertFalse(header.contains("Quotes"), "Header should not contain 'Quotes' when regex check is disabled");
        assertFalse(header.contains("SoftHyphen"), "Header should not contain 'SoftHyphen' when regex check is disabled");
    }

    @Test
    public void testGenerateDescriptionAdditionDelta_EmptyCurrentReturnsHeaderOnly() throws IOException, SQLException, ClassNotFoundException {
        System.setIn(new ByteArrayInputStream("\n".getBytes()));
        Mockito.doNothing().when(mockDbConnection).searchTranslations(Mockito.anySet());

        List<List<String>> result = comparator.generateDescriptionAdditionAndChangesDelta();

        assertEquals(1, result.size(), "Delta should contain only header when no new translations exist");
    }

    // =========================================================
    // Tests for Comparator.checkEszettInExtension()
    // =========================================================

    @Test
    public void testCheckEszettInExtension_ResultHasTwoLists() throws Exception {
        Mockito.doNothing().when(mockDbConnection).searchEszett();

        List<List<List<String>>> result = comparator.checkEszettInExtension();

        assertNotNull(result);
        assertEquals(2, result.size(), "Result should have exactly 2 lists: inactivations and additions");
    }

    @Test
    public void testCheckEszettInExtension_BothListsHaveHeaders() throws Exception {
        Mockito.doNothing().when(mockDbConnection).searchEszett();

        List<List<List<String>>> result = comparator.checkEszettInExtension();

        assertFalse(result.get(0).isEmpty(), "Inactivations list should have at least the header");
        assertFalse(result.get(1).isEmpty(), "Additions list should have at least the header");
        assertEquals("Description ID", result.get(0).get(0).get(0), "Inactivations header first column");
        assertEquals("Concept ID", result.get(1).get(0).get(0), "Additions header first column");
    }

    @Test
    public void testCheckEszettInExtension_AdditionRowHasEszettReplaced() throws Exception {
        Mockito.doNothing().when(mockDbConnection).searchEszett();

        // Simulate DB having populated an extension term with 'ß'
        resultCollector.setFullExtensionTranslation(
            "C1", "1", "FSN", "PT", "Stra\u00dfe", "de",
            "CS", "Type", "Ref", "Acc", "D1", "1"
        );

        List<List<List<String>>> result = comparator.checkEszettInExtension();

        List<List<String>> additions = result.get(1);
        assertTrue(additions.size() > 1, "Additions should have at least one data row beyond the header");
        boolean found = additions.stream().skip(1).anyMatch(row -> row.contains("Strasse"));
        assertTrue(found, "Addition row should contain 'ß' replaced with 'ss'");
    }

    @Test
    public void testCheckEszettInExtension_InactivationRowPreservesOriginalTerm() throws Exception {
        Mockito.doNothing().when(mockDbConnection).searchEszett();

        resultCollector.setFullExtensionTranslation(
            "C1", "1", "FSN", "PT", "Stra\u00dfe", "de",
            "CS", "Type", "Ref", "Acc", "D1", "1"
        );

        List<List<List<String>>> result = comparator.checkEszettInExtension();

        List<List<String>> inactivations = result.get(0);
        assertTrue(inactivations.size() > 1, "Inactivations should have at least one data row beyond the header");
        boolean found = inactivations.stream().skip(1).anyMatch(row -> row.contains("Stra\u00dfe"));
        assertTrue(found, "Inactivation row should preserve the original term with 'ß'");
    }
}
