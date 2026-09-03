package ch.ehealth.levi.core.check;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FrLexiconBuilderTest {

    @Test
    public void testAnalyzeCountsFrequencies() {
        FrLexiconBuilder.FrLexiconResult result = FrLexiconBuilder.analyze(List.of(
                "fracture de la hanche",
                "fracture du col du fémur",
                "fracture de fatigue"));
        assertEquals(3L, result.frequencies().get("fracture"));
        assertEquals(2L, result.frequencies().get("de"));
        assertEquals(1L, result.frequencies().get("hanche"));
        assertTrue(result.highFrequencyTokens().contains("fracture"));
        assertFalse(result.highFrequencyTokens().contains("hanche"));
    }

    @Test
    public void testAnalyzeTokenizesElisionAndHyphen() {
        FrLexiconBuilder.FrLexiconResult result = FrLexiconBuilder.analyze(List.of(
                "l'hypertension artérielle",
                "cou-de-pied"));
        assertTrue(result.frequencies().containsKey("hypertension"));
        assertTrue(result.frequencies().containsKey("cou"));
        assertTrue(result.frequencies().containsKey("pied"));
        assertFalse(result.frequencies().containsKey("l'"));
    }

    @Test
    public void testCaseInsensitiveCountingPreservesExamples() {
        FrLexiconBuilder.FrLexiconResult result = FrLexiconBuilder.analyze(List.of(
                "maladie de Parkinson",
                "syndrome de Parkinson"));
        assertEquals(2L, result.frequencies().get("parkinson"));
        assertTrue(result.capitalizedCounts().getOrDefault("parkinson", 0L) >= 2L);
        assertNotNull(result.examples().get("parkinson"));
    }

    @Test
    public void testClassifyInnSuffix() {
        assertEquals(FrLexiconBuilder.CAT_INN, FrLexiconBuilder.classify("amoxicilline", false, false));
        assertEquals(FrLexiconBuilder.CAT_INN, FrLexiconBuilder.classify("adalimumab", false, false));
        assertEquals(FrLexiconBuilder.CAT_INN, FrLexiconBuilder.classify("atorvastatine", false, false));
    }

    @Test
    public void testClassifyAcronym() {
        assertEquals(FrLexiconBuilder.CAT_ACRONYM, FrLexiconBuilder.classify("IRM", false, false));
        assertEquals(FrLexiconBuilder.CAT_ACRONYM, FrLexiconBuilder.classify("TDAH", false, false));
    }

    @Test
    public void testClassifyEponym() {
        assertEquals(FrLexiconBuilder.CAT_EPONYM, FrLexiconBuilder.classify("Parkinson", true, false));
        assertEquals(FrLexiconBuilder.CAT_EPONYM, FrLexiconBuilder.classify("Alzheimer", true, false));
    }

    @Test
    public void testClassifyTaxon() {
        assertEquals(FrLexiconBuilder.CAT_TAXON, FrLexiconBuilder.classify("Escherichia", true, true));
        assertEquals(FrLexiconBuilder.CAT_TAXON, FrLexiconBuilder.classify("Salmonella", true, true));
    }

    @Test
    public void testAnalyzeDetectsTaxonFromBinomial() {
        FrLexiconBuilder.FrLexiconResult result = FrLexiconBuilder.analyze(List.of(
                "infection à Escherichia coli",
                "infection à Salmonella typhi"));
        assertEquals(FrLexiconBuilder.CAT_TAXON, result.categories().get("escherichia"));
        assertEquals(FrLexiconBuilder.CAT_TAXON, result.categories().get("salmonella"));
        assertEquals(FrLexiconBuilder.CAT_UNKNOWN, result.categories().get("infection"));
    }

    @Test
    public void testNullAndBlankTermsIgnored() {
        FrLexiconBuilder.FrLexiconResult result = FrLexiconBuilder.analyze(java.util.Arrays.asList(null, "", "  "));
        assertTrue(result.descriptions().isEmpty());
        assertTrue(result.frequencies().isEmpty());
    }

    @Test
    public void testWriteOutputsCreatesFiles(@TempDir Path dir) throws IOException {
        FrLexiconBuilder.FrLexiconResult result = FrLexiconBuilder.analyze(List.of(
                "fracture de la hanche",
                "fracture du col du fémur",
                "fracture de fatigue",
                "zygomatiquee fracture")); // deliberately suspicious token
        Path allow = FrLexiconBuilder.writeOutputs(result, dir, null);

        Path dictDir = dir.resolve("dictionary_fr");
        assertTrue(Files.isDirectory(dictDir));
        assertTrue(Files.exists(allow));
        List<String> allowLines = Files.readAllLines(allow);
        assertTrue(allowLines.contains("fracture"));
        assertFalse(allowLines.contains("hanche")); // frequency 1 -> suspect, not allow-list

        Path suspects = dictDir.resolve("fr_suspects.tsv");
        assertTrue(Files.exists(suspects));
        String suspectContent = Files.readString(suspects);
        assertTrue(suspectContent.contains("zygomatiquee"));
        assertTrue(suspectContent.contains("hanche"));

        Path stats = dictDir.resolve("fr_stats.txt");
        assertTrue(Files.exists(stats));
        assertTrue(Files.readString(stats).contains("uniqueTokens"));
    }

    @Test
    public void testCategoryFilesWritten(@TempDir Path dir) throws IOException {
        FrLexiconBuilder.FrLexiconResult result = FrLexiconBuilder.analyze(List.of(
                "amoxicilline pour otite",
                "amoxicilline en comprimé",
                "amoxicilline pédiatrique",
                "IRM du genou",
                "IRM de l'épaule",
                "IRM cérébrale",
                "infection à Escherichia coli",
                "septicémie à Escherichia coli",
                "méningite à Escherichia coli"));
        FrLexiconBuilder.writeOutputs(result, dir, null);

        Path dictDir = dir.resolve("dictionary_fr");
        List<String> innLines = Files.readAllLines(dictDir.resolve("fr_inn.txt"));
        assertTrue(innLines.contains("amoxicilline"));

        List<String> taxonLines = Files.readAllLines(dictDir.resolve("fr_taxons.txt"));
        assertTrue(taxonLines.contains("Escherichia"));
    }

    @Test
    public void testWriteOutputsWithSpellcheckFilter(@TempDir Path dir) throws IOException {
        // A tiny spell-checker that knows only "fracture" -> high-freq known word filtered out
        SpellingChecker known = new SpellingChecker() {
            @Override
            public boolean isAvailable() {
                return true;
            }

            @Override
            public boolean isCorrect(String token) {
                return "fracture".equalsIgnoreCase(token);
            }

            @Override
            public java.util.List<String> suggest(String token) {
                return java.util.List.of();
            }

            @Override
            public java.util.List<SpellingIssue> check(String term) {
                return java.util.List.of();
            }
        };
        FrLexiconBuilder.FrLexiconResult result = FrLexiconBuilder.analyze(List.of(
                "fracture de la hanche",
                "fracture du col du fémur",
                "fracture de fatigue",
                "zygomatiquee fracture",
                "fracture zygomatiquee",
                "douleur zygomatiquee"));
        Path allow = FrLexiconBuilder.writeOutputs(result, dir, known);
        List<String> allowLines = Files.readAllLines(allow);
        assertFalse(allowLines.contains("fracture"));
        assertTrue(allowLines.contains("zygomatiquee"));
    }

    @Test
    public void testBuildPreservesContextInExamples() {
        FrLexiconBuilder.FrLexiconResult result = FrLexiconBuilder.analyzeDescriptions(List.of(
                new FrLexiconBuilder.FrDescription("fracture de la hanche", "12345", "900000000000548007")));
        FrLexiconBuilder.FrDescription ex = result.examples().get("hanche");
        assertNotNull(ex);
        assertEquals("12345", ex.conceptId());
        assertEquals("900000000000548007", ex.acceptability());
    }
}
