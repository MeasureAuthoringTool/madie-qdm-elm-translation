package gov.cms.mat.cql_elm_translation.utils.cql.parsing.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CQLParameter implements CQLExpression {
  private String parameterName;
  private String cqlType;
  private String defaultValue;
  private String parameterLogic;
  private String id;
  private boolean readOnly;
  private String commentString = "";

  public String getParameterName() {
    return parameterName.trim();
  }

  public void setParameterName(String name) {
    this.parameterName = name.trim();
  }

  public String getParameterLogic() {
    return parameterLogic.trim();
  }

  public void setParameterLogic(String logic) {
    this.parameterLogic = logic.trim();
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  @Override
  public String getName() {
    return getParameterName();
  }

  @Override
  public void setName(String name) {
    setParameterName(name);
  }

  @Override
  public String getLogic() {
    return getParameterLogic();
  }

  @Override
  public void setLogic(String logic) {
    setParameterLogic(logic);
  }
}
