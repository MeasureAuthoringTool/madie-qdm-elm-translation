package gov.cms.mat.cql_elm_translation.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Comparator;

@Data
@Builder
public class SourceDataCriteria implements Comparable<SourceDataCriteria> {
  private String oid;
  private String title;
  private String description;
  private String type;
  private boolean drc;
  // MAT-6210: codeId used for drc
  private String codeId;
  private String name;

  @Override
  public int compareTo(SourceDataCriteria o) {
    return Comparator.comparing(SourceDataCriteria::getOid)
        .thenComparing(SourceDataCriteria::getType, Comparator.nullsFirst(String::compareTo))
        .compare(this, o);
  }
}
