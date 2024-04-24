package gov.cms.mat.cql_elm_translation.utils.cql.parsing.model;

import org.apache.commons.lang3.StringUtils;

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
    if (StringUtils.isNotEmpty(aliasName)) {
      return aliasName + "." + identifier;
    } else {
      return identifier;
    }
  }

  @Override
  public String toString() {
    if (StringUtils.isNotEmpty(aliasName)) {
      return aliasName + "." + "\"" + identifier + "\"";
    } else {
      return "\"" + identifier + "\"";
    }
  }
}
