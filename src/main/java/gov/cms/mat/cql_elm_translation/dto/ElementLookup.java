package gov.cms.mat.cql_elm_translation.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ElementLookup {
  private String value;
  private boolean code;
  private String datatype;
  private String id;
  private String name;
  private String oid;
  private String release;
  private String taxonomy;
  private String type;
  private String uuid;
  private String version;
  private String codeSystemName;
  private String codeIdentifier;
  private String codeName;
  private String codeSystemOID;
  private String codeSystemVersion;
  private String displayName;
}
