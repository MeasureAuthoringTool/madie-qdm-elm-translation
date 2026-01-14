package gov.cms.mat.cql_elm_translation.utils.cql;

import java.util.*;

/**
 * Represents a model node in the FHIR model tree. Each node can have children and a parent, and
 * stores its name.
 */
public class ModelNode {
  private final String name;
  private final ModelNode parent;
  private final List<ModelNode> children = new ArrayList<>();

  public ModelNode(String name, ModelNode parent) {
    this.name = name.trim().toUpperCase();
    this.parent = parent;
    if (parent != null) {
      parent.addChild(this);
    }
  }

  public String getName() {
    return name;
  }

  public ModelNode getParent() {
    return parent;
  }

  public List<ModelNode> getChildren() {
    return Collections.unmodifiableList(children);
  }

  private void addChild(ModelNode child) {
    children.add(child);
  }

  /** Checks if this node or any ancestor matches the given model name. */
  public boolean isOrIsDescendantOf(String modelName) {
    String normalized = modelName.trim().toUpperCase();
    ModelNode current = this;
    while (current != null) {
      if (current.name.equals(normalized)) return true;
      current = current.parent;
    }
    return false;
  }
}
