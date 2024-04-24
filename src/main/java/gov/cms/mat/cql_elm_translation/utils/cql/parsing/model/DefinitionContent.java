package gov.cms.mat.cql_elm_translation.utils.cql.parsing.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DefinitionContent {
  private String name;
  private String content;
  private List<CQLFunctionArgument> functionArguments;
}
