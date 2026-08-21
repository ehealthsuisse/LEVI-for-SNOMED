package ch.ehealth.levi.core.db;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SctReleaseFileScanner}.
 */
public class SctReleaseFileScannerTest {

    @TempDir
    Path tempDir;

    private Path createFile(Path dir, String name) throws IOException {
        Files.createDirectories(dir);
        Path f = dir.resolve(name);
        Files.write(f, "id\teffectiveTime\tactive\n".getBytes());
        return f;
    }

    private void buildInternational(Path root) throws IOException {
        Path term = root.resolve("Full/Terminology");
        Path ref = root.resolve("Full/Refset/Language");
        createFile(term, "sct2_Concept_Full_INT_20260601.txt");
        createFile(term, "sct2_Description_Full-en_INT_20260601.txt");
        createFile(term, "sct2_Relationship_Full_INT_20260601.txt");
        createFile(ref, "der2_cRefset_LanguageFull-en_INT_20260601.txt");
    }

    private void buildSwissExtension(Path root) throws IOException {
        Path term = root.resolve("Full/Terminology");
        Path ref = root.resolve("Full/Refset/Language");
        createFile(term, "sct2_Concept_Full_CH1000195_20260607.txt");
        createFile(term, "sct2_Description_Full-de-ch_CH1000195_20260607.txt");
        createFile(term, "sct2_Description_Full-fr-ch_CH1000195_20260607.txt");
        createFile(term, "sct2_Description_Full-it-ch_CH1000195_20260607.txt");
        createFile(term, "sct2_Description_Full-en_CH1000195_20260607.txt");
        createFile(term, "sct2_Relationship_Full_CH1000195_20260607.txt");
        createFile(ref, "der2_cRefset_LanguageFull-de-ch_CH1000195_20260607.txt");
        createFile(ref, "der2_cRefset_LanguageFull-fr-ch_CH1000195_20260607.txt");
        createFile(ref, "der2_cRefset_LanguageFull-it-ch_CH1000195_20260607.txt");
        createFile(ref, "der2_cRefset_LanguageFull-en_CH1000195_20260607.txt");
    }

    private void buildAustrianSnapshot(Path root) throws IOException {
        Path term = root.resolve("Snapshot/Terminology");
        Path ref = root.resolve("Snapshot/Refset/Language");
        createFile(term, "sct2_Concept_Snapshot_AT1000234_20260315.txt");
        createFile(term, "sct2_Description_Snapshot-de_AT1000234_20260315.txt");
        createFile(term, "sct2_Description_Snapshot-en_AT1000234_20260315.txt");
        createFile(term, "sct2_Relationship_Snapshot_AT1000234_20260315.txt");
        createFile(ref, "der2_cRefset_LanguageSnapshot-de_AT1000234_20260315.txt");
        createFile(ref, "der2_cRefset_LanguageSnapshot-en_AT1000234_20260315.txt");
    }

    @Test
    public void testScanInternationalDetectsCoreFiles() throws IOException {
        Path root = tempDir.resolve("intl");
        buildInternational(root);

        SctReleaseFileScanner.SctRelease release =
                SctReleaseFileScanner.scan(root.toString());

        assertEquals(DbCreateConfig.ReleaseType.FULL, release.getReleaseType());
        assertEquals("INT", release.getModuleId());
        assertEquals("20260601", release.getReleaseDate());
        assertEquals("sct2_Concept_Full_INT_20260601.txt", release.getConceptFile().getName());
        assertEquals("sct2_Relationship_Full_INT_20260601.txt", release.getRelationshipFile().getName());
        assertTrue(release.getDescriptionFiles().containsKey("en"));
        assertTrue(release.getLanguageRefsetFiles().containsKey("en"));
    }

    @Test
    public void testScanSwissExtensionDetectsAllLanguages() throws IOException {
        Path root = tempDir.resolve("ch");
        buildSwissExtension(root);

        SctReleaseFileScanner.SctRelease release =
                SctReleaseFileScanner.scan(root.toString());

        assertEquals("CH1000195", release.getModuleId());
        assertEquals("20260607", release.getReleaseDate());
        assertEquals(4, release.getDescriptionFiles().size());
        assertEquals(4, release.getLanguageRefsetFiles().size());
        assertTrue(release.getLanguageCodes().contains("de-ch"));
        assertTrue(release.getLanguageCodes().contains("fr-ch"));
        assertTrue(release.getLanguageCodes().contains("it-ch"));
        assertTrue(release.getLanguageCodes().contains("en"));
    }

    @Test
    public void testScanSnapshotAutoDetectsSnapshotType() throws IOException {
        Path root = tempDir.resolve("at");
        buildAustrianSnapshot(root);

        SctReleaseFileScanner.SctRelease release =
                SctReleaseFileScanner.scan(root.toString());

        assertEquals(DbCreateConfig.ReleaseType.SNAPSHOT, release.getReleaseType());
        assertEquals("AT1000234", release.getModuleId());
        assertEquals("20260315", release.getReleaseDate());
        assertEquals(2, release.getDescriptionFiles().size());
        assertTrue(release.getLanguageCodes().contains("de"));
        assertTrue(release.getLanguageCodes().contains("en"));
    }

    @Test
    public void testScanExplicitTypeMatches() throws IOException {
        Path root = tempDir.resolve("intl");
        buildInternational(root);

        SctReleaseFileScanner.SctRelease release = SctReleaseFileScanner.scan(
                root.toString(), DbCreateConfig.ReleaseType.FULL);

        assertEquals(DbCreateConfig.ReleaseType.FULL, release.getReleaseType());
        assertEquals("INT", release.getModuleId());
    }

    @Test
    public void testScanMissingFolderThrows() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> SctReleaseFileScanner.scan(tempDir.resolve("does-not-exist").toString()));
        assertTrue(ex.getMessage().contains("does not exist"));
    }

    @Test
    public void testScanEmptyReleaseThrows() throws IOException {
        Path root = tempDir.resolve("empty");
        Files.createDirectories(root);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> SctReleaseFileScanner.scan(root.toString()));
        assertTrue(ex.getMessage().contains("No 'Full' or 'Snapshot' folder"));
    }

    @Test
    public void testScanTypeFolderMissingThrows() throws IOException {
        Path root = tempDir.resolve("onlyfull");
        buildInternational(root);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> SctReleaseFileScanner.scan(root.toString(), DbCreateConfig.ReleaseType.SNAPSHOT));
        assertTrue(ex.getMessage().contains("Snapshot"));
    }

    @Test
    public void testSuggestDbName() {
        assertEquals("SCT:CH_Jun26", SctReleaseFileScanner.suggestDbName("CH", "20260601"));
        assertEquals("SCT:AT_Mar25", SctReleaseFileScanner.suggestDbName("AT", "20250315"));
        assertEquals("SCT:CH_", SctReleaseFileScanner.suggestDbName("CH", null));
        assertEquals("SCT:CH_Jun26", SctReleaseFileScanner.suggestDbName(null, "20260601"));
    }

    @Test
    public void testSuggestDbNameWithVariant() {
        assertEquals("SCT:CH_Beta_Aug26",
                SctReleaseFileScanner.suggestDbName("CH", "20260801", DbVariant.BETA));
        assertEquals("SCT:CH_PreProdJun26",
                SctReleaseFileScanner.suggestDbName("CH", "20260601", DbVariant.PRE_PRODUCTION));
        assertEquals("SCT:AT_Mar25",
                SctReleaseFileScanner.suggestDbName("AT", "20250315", DbVariant.AT));
        assertEquals("SCT:CH_Jun26",
                SctReleaseFileScanner.suggestDbName("CH", "20260601", null));
    }

    @Test
    public void testDetectVariant() {
        assertEquals(DbVariant.PRODUCTION, SctReleaseFileScanner.detectVariant(
                "SnomedCT_ManagedServiceCH_PRODUCTION_CH1000195_20260607T120000Z"));
        assertEquals(DbVariant.BETA, SctReleaseFileScanner.detectVariant(
                "SnomedCT_ManagedServiceCH_DAILYBUILD_BETA_CH1000195_20261207T120000Z"));
        assertEquals(DbVariant.BETA, SctReleaseFileScanner.detectVariant(
                "SnomedCT_ManagedServiceCH_BETA_CH1000195_20261207T120000Z"));
        assertEquals(DbVariant.PRE_PRODUCTION, SctReleaseFileScanner.detectVariant(
                "SnomedCT_ManagedServiceCH_PREPRODUCTION_CH1000195_20260607T120000Z"));
        assertEquals(DbVariant.AT, SctReleaseFileScanner.detectVariant("AT1000234"));
        assertEquals(DbVariant.PRODUCTION, SctReleaseFileScanner.detectVariant(
                "/home/user/Downloads/SnomedCT_ManagedServiceAT_PRODUCTION_AT1000234_20260315T120000Z"));
        assertNull(SctReleaseFileScanner.detectVariant("CH1000195"));
        assertNull(SctReleaseFileScanner.detectVariant(null));
    }

    @Test
    public void testDetectCountry() {
        assertEquals("CH", SctReleaseFileScanner.detectCountry("CH1000195"));
        assertEquals("AT", SctReleaseFileScanner.detectCountry("AT1000234"));
        assertEquals("CH", SctReleaseFileScanner.detectCountry(null));
        assertEquals("CH", SctReleaseFileScanner.detectCountry(""));
        assertEquals("CH", SctReleaseFileScanner.detectCountry("ZZ12345"));
    }
}
