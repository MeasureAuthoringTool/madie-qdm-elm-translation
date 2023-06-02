package gov.cms.mat.cql_elm_translation.data;

import gov.cms.mat.cql_elm_translation.utils.cql.parsing.model.CQLCode;
import gov.cms.mat.cql_elm_translation.utils.cql.parsing.model.CQLValueSet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
// TODO Move to madie java models
public class DataCriteria {
  @Builder.Default private Map<CQLCode, Set<String>> dataCriteriaWithCodes = new HashMap<>();

  @Builder.Default
  private Map<CQLValueSet, Set<String>> dataCriteriaWithValueSets = new HashMap<>();
}
