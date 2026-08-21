package ch.ehealth.levi.core.check;

import java.util.List;
import java.util.Map;

public interface TranslationRuleChecker {

    String getLanguageCode();

    List<Finding> check(TranslationCheckContext context);

    Map<String, RuleMetadata> getRules();
}