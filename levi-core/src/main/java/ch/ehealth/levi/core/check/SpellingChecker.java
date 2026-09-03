package ch.ehealth.levi.core.check;

import java.util.List;

public interface SpellingChecker {

    boolean isAvailable();

    boolean isCorrect(String token);

    List<String> suggest(String token);

    List<SpellingIssue> check(String term);
}