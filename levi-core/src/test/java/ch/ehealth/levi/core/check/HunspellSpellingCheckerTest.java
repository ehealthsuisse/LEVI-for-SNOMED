package ch.ehealth.levi.core.check;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class HunspellSpellingCheckerTest {

    @Test
    public void testSeedResourcesLoaded() {
        HunspellSpellingChecker checker = new HunspellSpellingChecker();
        // INN (case-insensitive)
        assertTrue(checker.isInLexicon("adalimumab"));
        assertTrue(checker.isInLexicon("Adalimumab"));
        // Latin (case-insensitive)
        assertTrue(checker.isInLexicon("albicans"));
        assertTrue(checker.isInLexicon("Albicans"));
        // Eponym / taxon (case-sensitive: only exact casing accepted)
        assertTrue(checker.isInLexicon("Parkinson"));
        assertFalse(checker.isInLexicon("parkinson"));
        assertTrue(checker.isInLexicon("Escherichia"));
        assertFalse(checker.isInLexicon("escherichia"));
    }

    @Test
    public void testExternalLexiconDirOverridesAndAugments(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("fr_inn.txt"), "zincloremine\n");
        Files.writeString(dir.resolve("fr_eponyms.txt"), "Zinkbeim\n");
        Files.writeString(dir.resolve("fr_typos.txt"), "zinclorémine=zincloremine\n");

        HunspellSpellingChecker checker = new HunspellSpellingChecker(null, null, dir);
        assertTrue(checker.isInLexicon("zincloremine"));
        assertTrue(checker.isInLexicon("Zinkbeim"));
        assertFalse(checker.isInLexicon("zinkbeim")); // case-sensitive
    }

    @Test
    public void testLexiconRootWithDictionarySubfolder(@TempDir Path dir) throws IOException {
        // A lexicon root containing dictionary_fr/ is the new canonical layout
        Path dictDir = dir.resolve("dictionary_fr");
        Files.createDirectories(dictDir);
        Files.writeString(dictDir.resolve("fr_inn.txt"), "rootdrug\n");
        Files.writeString(dictDir.resolve("fr_eponyms.txt"), "Rootbein\n");

        HunspellSpellingChecker checker = new HunspellSpellingChecker(null, null, dir);
        // INN is case-insensitive
        assertTrue(checker.isInLexicon("rootdrug"));
        assertTrue(checker.isInLexicon("Rootdrug"));
        // Eponym is case-sensitive
        assertTrue(checker.isInLexicon("Rootbein"));
        assertFalse(checker.isInLexicon("rootbein"));
    }

    @Test
    public void testLexiconDirFallbackStillWorks(@TempDir Path dir) throws IOException {
        // Flat layout (files directly in the dir) remains supported
        Files.writeString(dir.resolve("fr_inn.txt"), "flatdrug\n");
        HunspellSpellingChecker checker = new HunspellSpellingChecker(null, null, dir);
        assertTrue(checker.isInLexicon("flatdrug"));
    }

    @Test
    public void testLexiconDirDoesNotRequireAllFiles(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("fr_inn.txt"), "seedsubstance\n");
        HunspellSpellingChecker checker = new HunspellSpellingChecker(null, null, dir);
        assertTrue(checker.isInLexicon("seedsubstance"));
        // seed resources still loaded
        assertTrue(checker.isInLexicon("Parkinson"));
    }

    @Test
    public void testSystemPropertyLexiconDir(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("fr_inn.txt"), "propdrug\n");
        String old = System.getProperty("levi.spelling.lexiconDir");
        try {
            System.setProperty("levi.spelling.lexiconDir", dir.toString());
            HunspellSpellingChecker checker = new HunspellSpellingChecker();
            assertTrue(checker.isInLexicon("propdrug"));
            assertTrue(checker.isInLexicon("Parkinson")); // seed resources still loaded
        } finally {
            if (old == null) {
                System.clearProperty("levi.spelling.lexiconDir");
            } else {
                System.setProperty("levi.spelling.lexiconDir", old);
            }
        }
    }

    @Test
    public void testCheckHonorsDefiniteTypoAndKnownTokens() {
        HunspellSpellingChecker checker = new HunspellSpellingChecker();
        List<SpellingIssue> issues = checker.check("chirugie de l'épaule");
        assertTrue(issues.stream().anyMatch(i -> i.definiteTypo() && "chirurgie".equals(i.correction())));
        // no spurious issues from elision token "épaule" or known words
        assertFalse(issues.stream().anyMatch(i -> "épaule".equals(i.token())));
    }
}
