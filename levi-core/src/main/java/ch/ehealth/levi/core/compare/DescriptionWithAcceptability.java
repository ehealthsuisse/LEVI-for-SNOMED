package ch.ehealth.levi.core.compare;

public record DescriptionWithAcceptability(
    String conceptId,
    String descriptionId,
    String term,
    String typeId,
    String languageCode,
    String caseSignificance,
    String acceptability,
    String refsetId,
    String active) {}