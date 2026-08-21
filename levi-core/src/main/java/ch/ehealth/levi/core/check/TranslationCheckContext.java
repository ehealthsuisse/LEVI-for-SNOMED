package ch.ehealth.levi.core.check;

public record TranslationCheckContext(
        String conceptId,
        String fsn,
        String pt,
        String term,
        String languageCode,
        String caseSignificance,
        String typeId,
        String acceptability) {
}