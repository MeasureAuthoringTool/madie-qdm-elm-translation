package gov.cms.mat.cql_elm_translation.utils.cql.parsing.model;

import lombok.Data;

@Data
public class CQLIdentifierObject {
  private String aliasName;
  private String id;
  private String identifier;
  private String returnType;

  public CQLIdentifierObject(String aliasName, String identifier, String id) {
    this.aliasName = aliasName;
    this.identifier = identifier;
    this.id = id;
  }

  public CQLIdentifierObject(String aliasName, String identifier) {
    this.aliasName = aliasName;
    this.identifier = identifier;
  }

  public CQLIdentifierObject() {}

  public String getDisplay() {
    if (aliasName != null && !aliasName.isEmpty()) {
      return aliasName + "." + identifier;
    } else {
      return identifier;
    }
  }

  @Override
  public String toString() {
    if (aliasName != null && !aliasName.isEmpty()) {
      return aliasName + "." + "\"" + identifier + "\"";
    } else {
      return "\"" + identifier + "\"";
    }
  }
}
