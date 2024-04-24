package gov.cms.mat.cql_elm_translation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CqlLookupRequest {
  private String cql;
  private Set<String> measureExpressions;
}
