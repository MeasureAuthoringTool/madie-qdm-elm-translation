package gov.cms.mat.cql_elm_translation.utils.cql.parsing.model;

import java.util.Objects;

import lombok.Data;

/** The Class CQLIncludeLibrary. */
@Data
public class CQLIncludeLibrary {

  private String id;
  private String aliasName;
  private String cqlLibraryId;
  private String version;
  private String cqlLibraryName;
  private String qdmVersion;
  private String setId;
  private String isComponent;
  private String measureId;
  private String libraryModelType = "QDM";

  public CQLIncludeLibrary(CQLLibraryDataSetObject dto) {
    this.cqlLibraryId = dto.getId();
    this.version = dto.getVersion().replace("v", "") + "." + dto.getRevisionNumber();
    this.cqlLibraryName = dto.getCqlName();
    this.qdmVersion = dto.getQdmVersion();
    this.setId = dto.getCqlSetId();
    this.libraryModelType = dto.getLibraryModelType();
  }

  public CQLIncludeLibrary(CQLIncludeLibrary includeLibrary) {
    this.aliasName = includeLibrary.getAliasName();
    this.id = includeLibrary.getId();
    this.cqlLibraryId = includeLibrary.getCqlLibraryId();
    this.version = includeLibrary.getVersion();
    this.cqlLibraryName = includeLibrary.getCqlLibraryName();
    this.qdmVersion = includeLibrary.getQdmVersion();
    this.setId = includeLibrary.getSetId();
    this.libraryModelType = includeLibrary.getLibraryModelType();
  }

  public CQLIncludeLibrary() {}

  @Override
  public boolean equals(Object arg0) {
    CQLIncludeLibrary cqlIncludeLibrary = (CQLIncludeLibrary) arg0;

    if (cqlIncludeLibrary == null) {
      return false;
    }

    // (cqlIncludeLibrary.libraryModelType == libraryModelType || libraryModelType != null &&
    // libraryModelType.equals(cqlIncludeLibrary.libraryModelType))
    if (Objects.equals(cqlIncludeLibrary.cqlLibraryId, cqlLibraryId)
        && Objects.equals(cqlIncludeLibrary.aliasName, aliasName)
        && Objects.equals(cqlIncludeLibrary.cqlLibraryName, cqlLibraryName)
        && Objects.equals(cqlIncludeLibrary.version, version)
        && Objects.equals(cqlIncludeLibrary.libraryModelType, libraryModelType)) {
      return true;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getAliasName(),
        getCqlLibraryId(),
        getVersion(),
        getCqlLibraryName(),
        getLibraryModelType());
  }

  public static class Comparator implements java.util.Comparator<CQLIncludeLibrary> {

    @Override
    public int compare(CQLIncludeLibrary o1, CQLIncludeLibrary o2) {
      return o1.getAliasName().compareTo(o2.getAliasName());
    }
  }

  public String toString() {
    return this.id
        + "|"
        + this.cqlLibraryId
        + "|"
        + this.cqlLibraryName
        + "|"
        + this.aliasName
        + "|"
        + this.version
        + "|"
        + this.libraryModelType;
  }
}
