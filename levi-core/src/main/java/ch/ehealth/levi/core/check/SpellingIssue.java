package ch.ehealth.levi.core.check;

import java.util.List;

public record SpellingIssue(String token, String correction, List<String> suggestions, boolean definiteTypo) {
}