package translation.check;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ResultCollector {
	private String type;
	private List<String> data;
	private List<ResultCollector> entries = new ArrayList<>();

	public ResultCollector(String type, List<String> data) {
		this.type = type;
		this.data = data;
	}

	public ResultCollector() {
		// Empty constructor for initializing without parameters
	}

	private void addEntry(String type, List<String> data) {
		entries.add(new ResultCollector(type, data));
	}

	public void setFullNewTranslationPrevious(String conceptId, String fsn, String pt, String term,
			String language_Code, String case_Significance, String type, String language_reference_set,
			String acceptabilityId, String language_reference_set2, String acceptabilityId2,
			String language_reference_set3, String acceptabilityId3, String language_reference_set4,
			String acceptabilityId4, String language_reference_set5, String acceptabilityId5, String notes) {
		List<String> L = new ArrayList<>(Arrays.asList(conceptId, fsn, pt, term, language_Code, case_Significance, type,
				language_reference_set, acceptabilityId, language_reference_set2, acceptabilityId2,
				language_reference_set3, acceptabilityId3, language_reference_set4, acceptabilityId4,
				language_reference_set5, acceptabilityId5, notes));
		addEntry("NEW_TRANSLATION_PREVIOUS", L);
	}

	public void setFullNewTranslationCurrent(String conceptId, String fsn, String pt, String term, String language_Code,
			String case_Significance, String type, String language_reference_set, String acceptabilityId,
			String language_reference_set2, String acceptabilityId2, String language_reference_set3,
			String acceptabilityId3, String language_reference_set4, String acceptabilityId4,
			String language_reference_set5, String acceptabilityId5, String notes) {
		List<String> L = new ArrayList<>(Arrays.asList(conceptId, fsn, pt, term, language_Code, case_Significance, type,
				language_reference_set, acceptabilityId, language_reference_set2, acceptabilityId2,
				language_reference_set3, acceptabilityId3, language_reference_set4, acceptabilityId4,
				language_reference_set5, acceptabilityId5, notes));
		addEntry("NEW_TRANSLATION_CURRENT", L);
	}

	public void setFullExtensionTranslation(String conceptId, String status, String fsn, String pt, String term,
			String languageCode, String caseSignificance, String type, String languageReferenceSet1,
			String acceptabilityId1, String descriptionId, String descriptionStatus) {
		List<String> L = new ArrayList<>(Arrays.asList(conceptId, status, fsn, pt, term, languageCode, caseSignificance,
				type, languageReferenceSet1, acceptabilityId1, descriptionId, descriptionStatus));
		addEntry("EXTENSION_TRANSLATION", L);
	}
	
	public void setFullExtensionInactivations(String descriptionId, String languageCode, String conceptId, String preferredTerm, String term, String inactivationReason, String associationtargetId1,
			String associationtargetId2, String associationtargetId3, String associationtargetId4, String notes) {
		List<String> L = new ArrayList<>(Arrays.asList(descriptionId, languageCode, conceptId, preferredTerm, term, inactivationReason,
				associationtargetId1, associationtargetId2, associationtargetId3, associationtargetId4, notes));
		addEntry("EXTENSION_INACTIVATION", L);
	}

	public void setFullTranslationOverview(String conceptId, String term, String typeId, String languageCode,
			String status) {
		List<String> L = new ArrayList<>(Arrays.asList(conceptId, term, typeId, languageCode, status));
		addEntry("TRANSLATION_OVERVIEW", L);
	}

	public void setFullInactivationsPrevious(String descriptionId, String term, String langageCode, String conceptId) {
		List<String> L = new ArrayList<>(Arrays.asList(descriptionId, term, langageCode, conceptId));
		addEntry("TRANSLATION_INACTIVATION_PREVIOUS", L);
	}

	public void setFullInactivationsCurrent(String descriptionId, String term, String langageCode, String conceptId) {
		List<String> L = new ArrayList<>(Arrays.asList(descriptionId, term, langageCode, conceptId));
		addEntry("TRANSLATION_INACTIVATION_CURRENT", L);
	}

	public void setFullTranslationChanges(String description_ID, String preferredTerm, String term, String caseSignificance,
			String type, String languageRefSet1, String acceptability1, String languageRefSet2, String acceptability2, String languageRefSet3,
			String acceptability3, String languageRefSet4, String acceptability4, String languageRefSet5, String acceptability5, String notes) {
		List<String> L = new ArrayList<>(Arrays.asList(description_ID, preferredTerm, term, caseSignificance, type,
				languageRefSet1, acceptability1, languageRefSet2, acceptability2, languageRefSet3, acceptability3,
				languageRefSet4, acceptability4, languageRefSet5, acceptability5, notes));
		addEntry("TRANSLATION_CHANGES", L);
	}

	
	
	public List<ResultCollector> getEntriesByType(String type) {
		return entries.stream().filter(e -> e.getType().equals(type)).collect(Collectors.toList());
	}

	public List<ResultCollector> getAllEntries() {
		return new ArrayList<>(entries);
	}

	/**
	 * Returns a list of IDs either concept or description IDs depending on the type
	 * of entries.
	 *
	 * @return List of IDs
	 */
	public List<String> getIds() {
		return entries.stream().map(e -> e.getData().get(0)).collect(Collectors.toList());
	}

	public List<String> getOnlyData() {
		return entries.stream().flatMap(e -> e.getData().stream()) // TODO: no flatMap, should return List<List<String>>
				.collect(Collectors.toList());
	}

	public List<List<String>> getDataByType(String type) {
		return entries.stream().filter(e -> e.getType().equals(type)).map(ResultCollector::getData)
				.collect(Collectors.toList());
	}
	
	public List<String> getIdsByType(String type) {
	    return entries.stream()
	        .filter(e -> e.getType().equals(type))
	        .map(e -> e.getData().get(0)) // Concept ID
	        .collect(Collectors.toList());
	}
	
	public boolean containsType(String type) {
	    return entries.stream().anyMatch(e -> e.getType().equals(type));
	}

	public boolean isEmpty() {
		return entries.isEmpty();
	}

	public String getType() {
		return type;
	}

	public List<String> getData() {
		return data;
	}

	public void clear() {
		entries.clear();
	}

}