package gov.cms.mat.cql_elm_translation.utils.cql.parsing.model;

import lombok.Data;

/** The Class QualityDataSetDTO. */
@Data
public class CQLQualityDataSetDTO implements CQLExpression {

  /** QDM Modified At VSAC. */
  private boolean hasModifiedAtVSAC;

  private boolean isUsed;

  /** QDM is not available in VSAC. */
  private boolean notFoundInVSAC;

  private String codeListName;
  private String suffix;
  private String originalCodeListName;
  private String codeSystemName;
  private String dataType;
  private String id;
  private String displayName;
  private String oid;
  private String codeSystemOID;
  private String codeIdentifier;
  private boolean isReadOnly;
  private boolean suppDataElement;
  private String taxonomy;
  private String type;
  private String uuid;
  private String version;
  private String release;
  private String program;
  private boolean dataTypeHasRemoved;
  private String valueSetType;
  private String isValidatedWithVsac = "VALID";

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object o) {
    CQLQualityDataSetDTO temp = (CQLQualityDataSetDTO) o;
    if (temp.getId().equals(getId())) {
      return true;
    }
    return false;
  }

  public String getQDMElement() {
    return codeListName + ": " + dataType;
  }

  public int compare(CQLQualityDataSetDTO o1, CQLQualityDataSetDTO o2) {
    return o1.getUuid().compareTo(o2.getUuid());
  }

  @Override
  public String getName() {
    return getCodeListName();
  }

  @Override
  public void setName(String name) {
    setCodeListName(name);
  }

  @Override
  public String getLogic() {
    return null;
  }

  @Override
  public void setLogic(String logic) {}

  @Override
  public String toString() {
    return "CQLQualityDataSetDTO{"
        + "isValidatedWithVsac="
        + isValidatedWithVsac
        + ", hasModifiedAtVSAC="
        + hasModifiedAtVSAC
        + ", isUsed="
        + isUsed
        + ", notFoundInVSAC="
        + notFoundInVSAC
        + ", codeListName='"
        + codeListName
        + '\''
        + ", suffix='"
        + suffix
        + '\''
        + ", originalCodeListName='"
        + originalCodeListName
        + '\''
        + ", codeSystemName='"
        + codeSystemName
        + '\''
        + ", dataType='"
        + dataType
        + '\''
        + ", id='"
        + id
        + '\''
        + ", displayName='"
        + displayName
        + '\''
        + ", oid='"
        + oid
        + '\''
        + ", codeSystemOID='"
        + codeSystemOID
        + '\''
        + ", codeIdentifier='"
        + codeIdentifier
        + '\''
        + ", isReadOnly="
        + isReadOnly
        + ", suppDataElement="
        + suppDataElement
        + ", taxonomy='"
        + taxonomy
        + '\''
        + ", type='"
        + type
        + '\''
        + ", uuid='"
        + uuid
        + '\''
        + ", version='"
        + version
        + '\''
        + ", release='"
        + release
        + '\''
        + ", program='"
        + program
        + '\''
        + ", dataTypeHasRemoved="
        + dataTypeHasRemoved
        + ", valueSetType='"
        + valueSetType
        + '\''
        + '}';
  }

  public static class Comparator implements java.util.Comparator<CQLQualityDataSetDTO> {

    @Override
    public int compare(CQLQualityDataSetDTO o1, CQLQualityDataSetDTO o2) {
      return o1.getQDMElement().compareTo(o2.getQDMElement());
    }
  }
}
