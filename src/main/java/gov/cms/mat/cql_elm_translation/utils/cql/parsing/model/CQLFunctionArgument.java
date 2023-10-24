package gov.cms.mat.cql_elm_translation.utils.cql.parsing.model;

import lombok.Data;

@Data
public class CQLFunctionArgument implements Cloneable {
  private String id;
  private String argumentName;
  private String argumentType;
  private String otherType;
  private String qdmDataType;
  private String attributeName;
  private boolean isValid;

  public void setArgumentName(String argumentName) {
    this.argumentName = argumentName.trim();
  }

  public String getReturnType() {
    if (this.qdmDataType != null) {
      return this.qdmDataType;
    } else if (this.otherType != null) {
      return this.otherType;
    } else {
      return this.argumentType;
    }
  }

  public CQLFunctionArgument clone() {
    CQLFunctionArgument argumentClone = new CQLFunctionArgument();
    argumentClone.setArgumentName(this.getArgumentName());
    argumentClone.setId(this.getId());
    argumentClone.setArgumentType(this.getArgumentType());
    argumentClone.setAttributeName(this.getAttributeName());
    argumentClone.setQdmDataType(this.getQdmDataType());
    argumentClone.setOtherType(this.getOtherType());
    return argumentClone;
  }
}
