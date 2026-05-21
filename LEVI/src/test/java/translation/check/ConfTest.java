package translation.check;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Conf configuration class.
 * Tests configuration and parameter processing requirements.
 */
public class ConfTest {

    private Conf conf;

    @BeforeEach
    public void setUp() {
        conf = new Conf();
    }

    // 1.1 Test setting and reading fields

    @Test
    public void testSetAndGetDbUrl() {
        String dbUrl = "jdbc:mysql://testhost:3306/testdb";
        conf.setDbUrl(dbUrl);
        assertEquals(dbUrl, conf.getSERVER_URL(), "DB URL should be correctly set and retrieved");
    }

    @Test
    public void testSetAndGetDbUsername() {
        String username = "testuser";
        conf.setDbUsername(username);
        assertEquals(username, conf.getUSERNAME(), "DB username should be correctly set and retrieved");
    }

    @Test
    public void testSetAndGetDbPassword() {
        String password = "testpass123";
        conf.setDbPassword(password);
        assertEquals(password, conf.getPASSWORD(), "DB password should be correctly set and retrieved");
    }

    @Test
    public void testSetAndGetCountryCodeCH() {
        conf.setCountryCode("CH");
        assertEquals("CH", conf.getCountryCode(), "Country code CH should be correctly set and retrieved");
    }

    @Test
    public void testSetAndGetCountryCodeAT() {
        conf.setCountryCode("AT");
        assertEquals("AT", conf.getCountryCode(), "Country code AT should be correctly set and retrieved");
    }

    @Test
    public void testSetAndGetCountryCodeDE() {
        conf.setCountryCode("DE");
        assertEquals("DE", conf.getCountryCode(), "Country code DE should be correctly set and retrieved");
    }

    @Test
    public void testSetAndGetFilePathCurrent() {
        String path = "/path/to/current/file.csv";
        conf.setFilePathCurrent(path);
        assertEquals(path, conf.getFilePathCurrent(), "Current file path should be correctly set and retrieved");
    }

    @Test
    public void testSetAndGetFilePathPrevious() {
        String path = "/path/to/previous/file.csv";
        conf.setFilePathPrevious(path);
        assertEquals(path, conf.getFilePathPrevious(), "Previous file path should be correctly set and retrieved");
    }

    @Test
    public void testSetAndGetDestination() {
        String destination = "/path/to/destination";
        conf.setDestination(destination);
        assertEquals(destination, conf.getDestination(), "Destination path should be correctly set and retrieved");
    }

    // 1.2 Test default values

    @Test
    public void testDefaultCountryCode() {
        assertEquals("CH", conf.getCountryCode(), "Default country code should be CH");
    }

    @Test
    public void testDefaultTransformEszett() {
        assertTrue(conf.isTransformEszett(), "Default transformEszett should be true");
    }

    @Test
    public void testDefaultRegexCheck() {
        assertTrue(conf.checkRegex(), "Default regexCheck should be true");
    }

    @Test
    public void testSetTransformEszettFalse() {
        conf.setTransformEszett(false);
        assertFalse(conf.isTransformEszett(), "transformEszett should be false when set to false");
    }

    @Test
    public void testSetRegexCheckFalse() {
        conf.setRegexCheck(false);
        assertFalse(conf.checkRegex(), "regexCheck should be false when set to false");
    }

    // 1.3 Test language reference set resolution

    @Test
    public void testLanguageRefSetForCH_DE() {
        conf.setCountryCode("CH");
        String refSetId = conf.getLanguageRefSetId("de");
        assertEquals("2041000195100", refSetId, "CH German language refset ID should be correct");
    }

    @Test
    public void testLanguageRefSetForCH_FR() {
        conf.setCountryCode("CH");
        String refSetId = conf.getLanguageRefSetId("fr");
        assertEquals("2021000195106", refSetId, "CH French language refset ID should be correct");
    }

    @Test
    public void testLanguageRefSetForCH_IT() {
        conf.setCountryCode("CH");
        String refSetId = conf.getLanguageRefSetId("it");
        assertEquals("2031000195108", refSetId, "CH Italian language refset ID should be correct");
    }

    @Test
    public void testLanguageRefSetForAT_DE() {
        conf.setCountryCode("AT");
        String refSetId = conf.getLanguageRefSetId("de");
        assertEquals("21000234103", refSetId, "AT German language refset ID should be correct");
    }

    @Test
    public void testLanguageRefSetForFR_FR() {
        conf.setCountryCode("FR");
        String refSetId = conf.getLanguageRefSetId("fr");
        assertEquals("10031000315102", refSetId, "FR French language refset ID should be correct");
    }

    @Test
    public void testLanguageRefSetForInvalidLanguage() {
        conf.setCountryCode("CH");
        String refSetId = conf.getLanguageRefSetId("es");
        assertNull(refSetId, "Unknown language should return null");
    }

    @Test
    public void testLanguageRefSetForInvalidCountry() {
        conf.setCountryCode("XY");
        String refSetId = conf.getLanguageRefSetId("de");
        assertNull(refSetId, "Unknown country should return null for any language");
    }

    // Test local language checking

    @Test
    public void testIsLocalLanguageForCH_DE() {
        conf.setCountryCode("CH");
        assertTrue(conf.isLocalLanguage("de"), "German should be a local language for CH");
    }

    @Test
    public void testIsLocalLanguageForCH_FR() {
        conf.setCountryCode("CH");
        assertTrue(conf.isLocalLanguage("fr"), "French should be a local language for CH");
    }

    @Test
    public void testIsLocalLanguageForCH_IT() {
        conf.setCountryCode("CH");
        assertTrue(conf.isLocalLanguage("it"), "Italian should be a local language for CH");
    }

    @Test
    public void testIsLocalLanguageForCH_EN_False() {
        conf.setCountryCode("CH");
        assertFalse(conf.isLocalLanguage("en"), "English should not be a local language for CH");
    }

    @Test
    public void testIsLocalLanguageForAT_DE() {
        conf.setCountryCode("AT");
        assertTrue(conf.isLocalLanguage("de"), "German should be a local language for AT");
    }

    @Test
    public void testIsLocalLanguageForAT_FR_False() {
        conf.setCountryCode("AT");
        assertFalse(conf.isLocalLanguage("fr"), "French should not be a local language for AT");
    }

    @Test
    public void testIsLocalLanguageWithNull() {
        conf.setCountryCode("CH");
        assertFalse(conf.isLocalLanguage(null), "Null language code should return false");
    }

    @Test
    public void testGetLocalLanguagesForCH() {
        conf.setCountryCode("CH");
        var languages = conf.getLocalLanguages();
        assertEquals(3, languages.size(), "CH should have 3 local languages");
        assertTrue(languages.contains("de"), "CH local languages should include 'de'");
        assertTrue(languages.contains("fr"), "CH local languages should include 'fr'");
        assertTrue(languages.contains("it"), "CH local languages should include 'it'");
    }

    @Test
    public void testGetLocalLanguagesForAT() {
        conf.setCountryCode("AT");
        var languages = conf.getLocalLanguages();
        assertEquals(1, languages.size(), "AT should have 1 local language");
        assertTrue(languages.contains("de"), "AT local languages should include 'de'");
    }

    @Test
    public void testGetLocalLanguagesForUnknownCountry() {
        conf.setCountryCode("XY");
        var languages = conf.getLocalLanguages();
        assertEquals(0, languages.size(), "Unknown country should return empty language set");
    }
}
