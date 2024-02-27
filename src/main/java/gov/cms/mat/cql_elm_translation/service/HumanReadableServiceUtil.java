package gov.cms.mat.cql_elm_translation.service;

import java.util.List;
import java.util.Set;

import gov.cms.madie.qdm.humanreadable.model.HumanReadableExpressionModel;
import gov.cms.mat.cql_elm_translation.utils.cql.parsing.model.CQLDefinition;

public class HumanReadableServiceUtil {

  public static String getLogic(String definition, List<HumanReadableExpressionModel> definitions) {
    for (HumanReadableExpressionModel humanReadableDefinition : definitions) {
      if (definition.equalsIgnoreCase(humanReadableDefinition.getName())) {
        return humanReadableDefinition.getLogic();
      }
    }
    return "";
  }

  public static String getCQLDefinitionLogic(String id, Set<CQLDefinition> allDefinitions) {
    CQLDefinition cqlDefinition =
        allDefinitions.stream()
            .filter(definition -> id != null && id.equalsIgnoreCase(definition.getId()))
            .findFirst()
            .orElse(null);
    return cqlDefinition != null
        ? cqlDefinition.getLogic().substring(cqlDefinition.getLogic().indexOf('\n') + 1)
        : "";
  }
}
