package gov.cms.mat.cql_elm_translation.utils.cql.parsing.model;

import java.util.List;

import lombok.Data;

@Data
public class CQLFunction implements CQLExpression {

  private String aliasName;

  /** The id. */
  private String id;

  /** The function name. */
  private String functionName;

  /** The function logic. */
  private String functionLogic;

  /** The argument. */
  private List<CQLFunctionArgument> argument;

  /** The context. */
  private String context = "Patient";

  private String commentString = "";

  private String returnType;

  /**
   * Gets the argument list.
   *
   * @return the argument list
   */
  public List<CQLFunctionArgument> getArgumentList() {
    return argument;
  }

  /**
   * Sets the argument list.
   *
   * @param argumentList the new argument list
   */
  public void setArgumentList(List<CQLFunctionArgument> argumentList) {
    this.argument = argumentList;
  }

  /**
   * Gets the id.
   *
   * @return the id
   */
  @Override
  public String getId() {
    return id;
  }

  /**
   * Sets the id.
   *
   * @param id the new id
   */
  @Override
  public void setId(String id) {
    this.id = id;
  }

  @Override
  public String getName() {
    return getFunctionName();
  }

  @Override
  public void setName(String name) {
    setFunctionName(name);
  }

  @Override
  public String getLogic() {
    return getFunctionLogic();
  }

  @Override
  public void setLogic(String logic) {
    setFunctionLogic(logic);
  }
}
