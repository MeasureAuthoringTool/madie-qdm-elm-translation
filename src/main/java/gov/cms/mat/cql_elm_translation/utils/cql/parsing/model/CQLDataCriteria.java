package gov.cms.mat.cql_elm_translation.utils.cql.parsing.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CQLDataCriteria {
  private String name;
  private String dataType;
  private String oid;
  private String version;
}
