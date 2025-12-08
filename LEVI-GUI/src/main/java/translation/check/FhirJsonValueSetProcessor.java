package translation.check;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.ValueSet;


public class FhirJsonValueSetProcessor {

    private final FhirContext fhirContext = FhirContext.forR4();
    private final IParser jsonParser = fhirContext.newJsonParser();
    private final ResultCollector resultCollector;
    private static Conf conf = new Conf();

    public FhirJsonValueSetProcessor(ResultCollector collector) {
        this.resultCollector = collector;
    }

    public void processValueSet(String jsonContent) {
        ValueSet valueSet = jsonParser.parseResource(ValueSet.class, jsonContent);

        if (valueSet.hasCompose() && valueSet.getCompose().hasInclude()) {
            valueSet.getCompose().getInclude().forEach(include -> {
                if (include.hasConcept()) {
                    include.getConcept().forEach(concept -> {
                        String code = concept.getCode();
                        String display = concept.getDisplay();
                        if(concept.hasDesignation()) {
							 concept.getDesignation().forEach(designation-> {
								 String language = designation.getLanguage();
								 String value = designation.getValue();
								 String baseLanguageCode = language.split("-")[0];
							     String languageReferenceSet = conf.getLanguageRefSetId(baseLanguageCode);
								 resultCollector.setFullNewTranslationCurrent(
			                                code,
			                                display, // Using display as FSN (Fully Specified Name)
			                                "", // PT (Preferred Term)
			                                value, // Term
			                                baseLanguageCode, // Language code
			                                "", // Case significance
			                                "", // Type
			                                languageReferenceSet, // language_reference_set
			                                "", // acceptabilityId
			                                "", // language_reference_set2
			                                "", // acceptabilityId2
			                                "", // language_reference_set3
			                                "", // acceptabilityId3
			                                "", // language_reference_set4
			                                "", // acceptabilityId4
			                                "", // language_reference_set5
			                                "", // acceptabilityId5
			                                ""  // Notes
			                        );
							 });
						}
                    });
                }
            });
        }
    }
}
