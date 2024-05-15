package gov.cms.mat.cql_elm_translation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode
public class ElementLookup {
  private String value;
  private boolean code;
  private String datatype;
  @EqualsAndHashCode.Exclude // ID is a randomly generated UUID and will never be equal to another
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
