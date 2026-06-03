package translation.check;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the FileReaderUtil class.
 * Tests file type detection and path processing.
 */
public class FileReaderUtilTest {

    // 5.1 & 5.5 Test file type detection and delimiter resolution

    @Test
    public void testCheckFilePathExtensionForPropCsv() {
        Object[] result = FileReaderUtil.checkFilePathExtension("/path/to/file.propcsv.csv");
        assertEquals(".propcsv.csv", result[0], "Should identify .propcsv.csv file type");
        assertEquals(';', result[1], "Should use semicolon delimiter for propcsv");
    }

    @Test
    public void testCheckFilePathExtensionForTermspace() {
        Object[] result = FileReaderUtil.checkFilePathExtension("/path/to/file.termspace.csv");
        assertEquals(".termspace.csv", result[0], "Should identify .termspace.csv file type");
        assertEquals('\t', result[1], "Should use tab delimiter for termspace");
    }

    @Test
    public void testCheckFilePathExtensionForAdditionsTsv() {
        Object[] result = FileReaderUtil.checkFilePathExtension("/path/to/fileAdditions.tsv");
        assertEquals("Additions.tsv", result[0], "Should identify Additions.tsv file type");
        assertEquals('\t', result[1], "Should use tab delimiter for Additions.tsv");
    }

    @Test
    public void testCheckFilePathExtensionForInactivationsTsv() {
        Object[] result = FileReaderUtil.checkFilePathExtension("/path/to/fileInactivations.tsv");
        assertEquals("Inactivations.tsv", result[0], "Should identify Inactivations.tsv file type");
        assertEquals('\t', result[1], "Should use tab delimiter for Inactivations.tsv");
    }

    @Test
    public void testCheckFilePathExtensionForSimpleOverview() {
        Object[] result = FileReaderUtil.checkFilePathExtension("/path/to/file.simpleOverview.tsv");
        assertEquals(".simpleOverview.tsv", result[0], "Should identify .simpleOverview.tsv file type");
        assertEquals('\t', result[1], "Should use tab delimiter for simpleOverview");
    }

    @Test
    public void testCheckFilePathExtensionForExcelXlsx() {
        Object[] result = FileReaderUtil.checkFilePathExtension("/path/to/file.xlsx");
        assertEquals("Excel", result[0], "Should identify .xlsx as Excel");
        assertNull(result[1], "Excel should have null delimiter");
    }

    @Test
    public void testCheckFilePathExtensionForExcelXls() {
        Object[] result = FileReaderUtil.checkFilePathExtension("/path/to/file.xls");
        assertEquals("Excel", result[0], "Should identify .xls as Excel");
        assertNull(result[1], "Excel should have null delimiter");
    }

    @Test
    public void testCheckFilePathExtensionForJson() {
        Object[] result = FileReaderUtil.checkFilePathExtension("/path/to/file.json");
        assertEquals("JSON", result[0], "Should identify .json file type");
        assertNull(result[1], "JSON should have null delimiter");
    }

    @Test
    public void testCheckFilePathExtensionForTxt() {
        Object[] result = FileReaderUtil.checkFilePathExtension("/path/to/file.txt");
        assertEquals(".txt", result[0], "Should identify .txt file type");
        assertEquals('\t', result[1], "Should use tab delimiter for txt");
    }

    @Test
    public void testCheckFilePathExtensionForUnknown() {
        Object[] result = FileReaderUtil.checkFilePathExtension("/path/to/file.unknown");
        assertEquals("Unknown file type", result[0], "Should return 'Unknown file type' for unrecognized extension");
        assertEquals('\t', result[1], "Should default to tab delimiter for unknown type");
    }

    // Test release type detection

    @Test
    public void testReleaseTypeDetectionForPrevious() {
        Object[] result = FileReaderUtil.checkFilePathExtension("/path/to/previous_file.propcsv.csv");
        assertEquals("previous", result[2], "Should detect 'previous' in file path");
    }

    @Test
    public void testReleaseTypeDetectionForCurrent() {
        Object[] result = FileReaderUtil.checkFilePathExtension("/path/to/current_file.propcsv.csv");
        assertEquals("current", result[2], "Should detect 'current' as default");
    }

    @Test
    public void testReleaseTypeDetectionCaseInsensitive() {
        Object[] result = FileReaderUtil.checkFilePathExtension("/path/to/PREVIOUS_file.propcsv.csv");
        assertEquals("previous", result[2], "Should detect 'previous' case-insensitively");
    }

    @Test
    public void testReleaseTypeDetectionNoPreviousKeyword() {
        Object[] result = FileReaderUtil.checkFilePathExtension("/path/to/normalfile.propcsv.csv");
        assertEquals("current", result[2], "Should default to 'current' when 'previous' not found");
    }

    // Test various file path formats

    @Test
    public void testFilePathWithWindowsDelimiters() {
        Object[] result = FileReaderUtil.checkFilePathExtension("C:\\Users\\test\\file.propcsv.csv");
        assertEquals(".propcsv.csv", result[0], "Should work with Windows-style paths");
    }

    @Test
    public void testFilePathWithComplexName() {
        Object[] result = FileReaderUtil.checkFilePathExtension("/complex/path-with_special.chars/fileAdditions.tsv");
        assertEquals("Additions.tsv", result[0], "Should handle complex path with special characters");
    }

    @Test
    public void testFilePathEndingCheck() {
        // Test that it checks the END of the path, not just contains
        Object[] result1 = FileReaderUtil.checkFilePathExtension("/Additions.tsv/otherfile.csv");
        assertNotEquals("Additions.tsv", result1[0], "Should not match 'Additions.tsv' in middle of path");
        
        Object[] result2 = FileReaderUtil.checkFilePathExtension("/path/to/Additions.tsv");
        assertEquals("Additions.tsv", result2[0], "Should match 'Additions.tsv' at end of path");
    }

    @Test
    public void testCheckInactivationFile() {
        Object[] result = FileReaderUtil.checkFilePathExtension("/path/Check_inactivation.tsv");
        assertEquals("Check_inactivation.tsv", result[0], "Should identify Check_inactivation.tsv file type");
        assertEquals('\t', result[1], "Should use tab delimiter");
    }

    // Test that results array has correct structure

    @Test
    public void testResultArrayStructure() {
        Object[] result = FileReaderUtil.checkFilePathExtension("/path/to/file.propcsv.csv");
        assertEquals(3, result.length, "Result array should have 3 elements");
        assertTrue(result[0] instanceof String, "First element should be file type (String)");
        assertTrue(result[1] == null || result[1] instanceof Character, "Second element should be delimiter (Character or null)");
        assertTrue(result[2] instanceof String, "Third element should be release type (String)");
    }

    @Test
    public void testResultArrayForExcel() {
        Object[] result = FileReaderUtil.checkFilePathExtension("/path/to/file.xlsx");
        assertEquals(3, result.length, "Result array should have 3 elements");
        assertEquals("Excel", result[0], "File type should be 'Excel'");
        assertNull(result[1], "Delimiter should be null for Excel");
        assertNotNull(result[2], "Release type should not be null");
    }

    @Test
    public void testMultipleExtensionsInPath() {
        // Test file with multiple dots
        Object[] result = FileReaderUtil.checkFilePathExtension("/path/with.dots/file.name.propcsv.csv");
        assertEquals(".propcsv.csv", result[0], "Should correctly identify type with multiple dots in path");
    }

    @Test
    public void testCaseSensitivityForExtensions() {
        // The implementation checks extensions case-sensitively
        Object[] resultUpper = FileReaderUtil.checkFilePathExtension("/path/to/file.XLSX");
        Object[] resultLower = FileReaderUtil.checkFilePathExtension("/path/to/file.xlsx");
        
        // Only lowercase extensions are recognized
        assertEquals("Unknown file type", resultUpper[0], "Uppercase .XLSX is not recognized");
        assertEquals("Excel", resultLower[0], "Lowercase .xlsx should be recognized");
    }
}
