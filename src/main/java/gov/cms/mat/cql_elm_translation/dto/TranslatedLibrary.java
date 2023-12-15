package gov.cms.mat.cql_elm_translation.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TranslatedLibrary {
  private String librayName;
  private String version;
  private String cql;
  private String elmXml;
  private String elmJson;
}
