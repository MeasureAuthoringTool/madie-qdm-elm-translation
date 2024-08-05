package gov.cms.mat.cql_elm_translation.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class CqlBuilderLookup {
  private Set<Lookup> parameters;
  private Set<Lookup> definitions;
  private Set<Lookup> functions;
  private Set<Lookup> fluentFunctions;

  @Builder
  @Data
  public static class Lookup {
    private String name;
    private String libraryName;
    private String libraryAlias;
    private String logic;
  }
}
