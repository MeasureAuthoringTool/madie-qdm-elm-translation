package gov.cms.mat.cql_elm_translation.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RelevantElement {
  private String type;
  private String profile;
}
