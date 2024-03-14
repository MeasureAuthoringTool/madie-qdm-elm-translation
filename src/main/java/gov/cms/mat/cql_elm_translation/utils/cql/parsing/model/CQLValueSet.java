package gov.cms.mat.cql_elm_translation.utils.cql.parsing.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CQLValueSet {
  private String name;
  private String oid;
  private String version;
  private String identifier;
}
