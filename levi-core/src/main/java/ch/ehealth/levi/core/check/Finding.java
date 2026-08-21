package ch.ehealth.levi.core.check;

public record Finding(String ruleId, String severity, String status, String message, int pdfPage, String section) {

    public boolean needsNote() {
        return "fail".equals(status) || "uncertain".equals(status);
    }
}