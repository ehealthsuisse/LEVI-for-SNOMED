package ch.ehealth.levi.core.compare;

public record MatchResult(
    String conceptId_FR,
    String conceptId_CH,
    String descriptionId_FR,
    String descriptionId_CH,
    String term,
    String preferredTerm,
    String fsnTerm,
    String typeId,
    String languageCode,
    String caseSignificance,
    String refsetId,
    String acceptability_FR,
    String acceptability_CH,
    String active_FR,
    String active_CH,
    MatchStatus status,
    String note) {}