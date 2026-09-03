package ch.ehealth.levi.core.compare;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the RF2 column mapping in {@link SnomedLoader}.
 * Verifies the loader reads real sct2_Description / der2_cRefset_Language
 * layouts and that enrich() joins the two correctly.
 */
public class SnomedLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    public void testLoadDescriptionsReadsRf2Columns() throws Exception {
        Path file = tempDir.resolve("sct2_Description.txt");
        Files.write(file, List.of(
            "id\teffectiveTime\tactive\tmoduleId\tconceptId\tlanguageCode\ttypeId\tterm\tcaseSignificanceId",
            "D001\t20260621\t1\t1000315\t123456789\tfr\t900000000000013009\tTerme FR\t900000000000448009",
            "D002\t20260621\t0\t1000315\t123456789\tfr\t900000000000013009\tTerme FR inactif\t900000000000448009"
        ));

        SnomedLoader loader = new SnomedLoader();
        Map<String, SnomedLoader.Description> map = loader.loadDescriptions(file);

        assertEquals(2, map.size());
        SnomedLoader.Description d = map.get("D001");
        assertNotNull(d);
        assertEquals("1", d.active());
        assertEquals("123456789", d.conceptId());
        assertEquals("fr", d.languageCode());
        assertEquals("900000000000013009", d.typeId());
        assertEquals("Terme FR", d.term());
        assertEquals("900000000000448009", d.caseSignificanceId());
    }

    @Test
    public void testLoadLanguageRefsetKeysByReferencedComponentId() throws Exception {
        Path file = tempDir.resolve("der2_cRefset_Language.txt");
        Files.write(file, List.of(
            "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId\tacceptabilityId",
            "L001\t20260621\t1\t1000315\t2021000195106\tD001\t900000000000548007"
        ));

        SnomedLoader loader = new SnomedLoader();
        Map<String, SnomedLoader.LanguageRefset> map = loader.loadLanguageRefset(file);

        assertEquals(1, map.size());
        assertTrue(map.containsKey("D001"), "map must be keyed by referencedComponentId (description id)");
        SnomedLoader.LanguageRefset lr = map.get("D001");
        assertEquals("2021000195106", lr.refsetId());
        assertEquals("900000000000548007", lr.acceptabilityId());
        assertEquals("1", lr.active());
    }

    @Test
    public void testEnrichJoinsDescriptionWithLanguageRefset() throws Exception {
        SnomedLoader loader = new SnomedLoader();

        Path descFile = tempDir.resolve("sct2_Description.txt");
        Files.write(descFile, List.of(
            "id\teffectiveTime\tactive\tmoduleId\tconceptId\tlanguageCode\ttypeId\tterm\tcaseSignificanceId",
            "D001\t20260621\t1\t1000315\t123456789\tfr\t900000000000013009\tTerme FR\t900000000000448009"
        ));

        Path langFile = tempDir.resolve("der2_cRefset_Language.txt");
        Files.write(langFile, List.of(
            "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId\tacceptabilityId",
            "L001\t20260621\t1\t1000315\t2021000195106\tD001\t900000000000548007"
        ));

        List<DescriptionWithAcceptability> enriched = loader.enrich(
                loader.loadDescriptions(descFile),
                loader.loadLanguageRefset(langFile));

        assertEquals(1, enriched.size());
        DescriptionWithAcceptability d = enriched.get(0);
        assertEquals("123456789", d.conceptId());
        assertEquals("Terme FR", d.term());
        assertEquals("2021000195106", d.refsetId());
        assertEquals("Preferred", d.acceptability());
        assertEquals("1", d.active());
    }
}
