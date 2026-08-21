package ch.ehealth.levi.core.check;

public record RuleMetadata(String ruleId, String severity, int pdfPage, String section, String quote) {
}