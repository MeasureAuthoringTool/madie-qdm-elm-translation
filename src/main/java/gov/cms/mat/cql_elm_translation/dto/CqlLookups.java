package gov.cms.mat.cql_elm_translation.dto;

import gov.cms.mat.cql_elm_translation.utils.cql.parsing.model.CQLCode;
import gov.cms.mat.cql_elm_translation.utils.cql.parsing.model.CQLCodeSystem;
import gov.cms.mat.cql_elm_translation.utils.cql.parsing.model.CQLDefinition;
import gov.cms.mat.cql_elm_translation.utils.cql.parsing.model.CQLIncludeLibrary;
import gov.cms.mat.cql_elm_translation.utils.cql.parsing.model.CQLParameter;
import gov.cms.mat.cql_elm_translation.utils.cql.parsing.model.CQLValueSet;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class CqlLookups {
  private String context;
  private String library;
  private String version;
  private String usingModel;
  private String usingModelVersion;
  private String cqlContext;
  private Set<CQLParameter> parameters;
  private Set<CQLCode> codes;
  private Set<CQLCodeSystem> codeSystems;
  private Set<CQLValueSet> valueSets;
  private Set<CQLDefinition> definitions;
  private Set<CQLIncludeLibrary> includeLibraries;
  private Set<ElementLookup> elementLookups;
}
