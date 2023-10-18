package gov.cms.mat.cql_elm_translation.utils.cql.parsing.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class CQLLibraryDataSetObject {
  private String id;
  private String cqlName;
  private String version = "0.0";
  private boolean isDraft;
  private Timestamp finalizedDate;
  private String measureSetId;
  private String ownerEmailAddress;
  private boolean isFamily;
  private String releaseVersion;
  private String qdmVersion;
  private String fhirVersion;
  private String ownerFirstName;
  private String ownerLastName;
  private String cqlText;
  private String measureId;
  private boolean isSelected;
  private CQLModel cqlModel;
  private boolean isSharable;
  private String revisionNumber = "000";
  private String ownerId;
  private String cqlSetId;
  private boolean isEditable;
  private boolean isTransferable;
  private boolean isDraftable;
  private boolean isVersionable;
  private boolean isDeletable;
  private String libraryModelType;
  private boolean fhirConvertible;
  private transient int clickCount;

  private List<CQLError> cqlErrors = new ArrayList<>();

  public void scrubForMarkUp() {
    String markupRegExp = "<[^>]+>";
    if (this.getCqlName() != null) {
      String noMarkupText = this.getCqlName().trim().replaceAll(markupRegExp, "");
      if (this.getCqlName().trim().length() > noMarkupText.length()) {
        this.setCqlName(noMarkupText);
      }
    }
  }
}
