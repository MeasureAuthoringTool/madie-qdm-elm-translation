package gov.cms.mat.cql_elm_translation.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SourceDataCriteria {
  private String oid;
  private String title;
  private String description;
  private String type;
  private boolean drc;
}
