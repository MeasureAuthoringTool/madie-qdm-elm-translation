package gov.cms.mat.cql_elm_translation.utils.cql.parsing.model;

import lombok.Data;

@Data
public class CQLError implements Comparable<CQLError> {

  public static final String ERROR_SEVERITY = "Error";
  public static final String SEVERE_SEVERITY = "Severe";

  private int errorInLine;
  private int errorAtOffset;

  private int startErrorInLine;
  private int startErrorAtOffset;

  private int endErrorInLine;
  private int endErrorAtOffset;

  private String errorMessage;

  private String severity;

  @Override
  public String toString() {
    return this.errorInLine + ";" + this.errorAtOffset + ":" + this.errorMessage;
  }

  @Override
  public int compareTo(CQLError o) {

    if (this.errorInLine > o.errorInLine) {
      return 1;
    } else if (this.errorInLine < o.errorInLine) {
      return -1;
    }

    return 0;
  }
}
