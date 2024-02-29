package gov.cms.mat.cql_elm_translation.utils.cql.parsing.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class CQLDefinition implements CQLExpression {
  private String id;
  private String definitionName;
  private String definitionLogic;
  private String context = "Patient";
  private boolean supplDataElement;
  private boolean popDefinition;
  private String commentString = "";
  private String returnType;
  private String parentLibrary;
  private String libraryDisplayName;
  private String libraryVersion;
  private boolean isFunction;

  public static class Comparator implements java.util.Comparator<CQLDefinition> {

    /* (non-Javadoc)
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    @Override
    public int compare(CQLDefinition o1, CQLDefinition o2) {
      return o1.getName().compareTo(o2.getName());
    }
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
    return getDefinitionName();
  }

  @Override
  public void setName(String name) {
    setDefinitionName(name);
  }

  @Override
  public String getLogic() {
    return getDefinitionLogic();
  }

  @Override
  public void setLogic(String logic) {
    setDefinitionLogic(logic);
  }

  @Override
  public String toString() {
    return this.definitionName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CQLDefinition that = (CQLDefinition) o;
    return Objects.equals(definitionName, that.definitionName);
  }
}
