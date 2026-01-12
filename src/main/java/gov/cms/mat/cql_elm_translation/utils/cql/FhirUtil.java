package gov.cms.mat.cql_elm_translation.utils.cql;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.mat.cql.elements.UsingProperties;
import gov.cms.mat.cql_elm_translation.service.ModelManagerFactory;
import org.hl7.fhir.convertors.advisors.impl.BaseAdvisor_40_50;
import org.hl7.fhir.convertors.conv40_50.VersionConvertor_40_50;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r5.model.ImplementationGuide;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Component
public class FhirUtil {
  // Future-proof for additional model support
  private static final ModelNode FHIR = new ModelNode("FHIR", null);
  private static final ModelNode USCORE = new ModelNode("USCORE", FHIR);
  private static final ModelNode QICORE = new ModelNode("QICORE", USCORE);
  private static final Map<String, ModelNode> MODEL_MAP = new HashMap<>();

  static {
    MODEL_MAP.put(FHIR.getName(), FHIR);
    MODEL_MAP.put(USCORE.getName(), USCORE);
    MODEL_MAP.put(QICORE.getName(), QICORE);
  }

  /**
   * Checks if the given model string is a supported FHIR model or its descendant.
   *
   * @param model the model string to check
   * @return true if the model is a supported FHIR model or descendant, false otherwise
   */
  public boolean isFhirModel(String model) {
    if (model == null) return false;
    String normalized = model.trim().toUpperCase();
    ModelNode node = MODEL_MAP.get(normalized);
    if (node == null) return false;
    return node.isOrIsDescendantOf("FHIR");
  }

  /**
   * Given a list of UsingProperties, returns the most specific one based on model tree depth. If
   * none match, returns null.
   *
   * @param usingPropertiesList List of UsingProperties
   * @return The most specific UsingProperties or null if none found
   */
  public UsingProperties getMostSpecificFhirModel(List<UsingProperties> usingPropertiesList) {
    if (usingPropertiesList == null || usingPropertiesList.isEmpty()) return null;
    UsingProperties mostSpecific = null;
    int maxDepth = -1;
    for (UsingProperties using : usingPropertiesList) {
      if (using == null) continue;
      String type = using.getLibraryType();
      if (type != null) {
        ModelNode node = MODEL_MAP.get(type.trim().toUpperCase());
        if (node != null && node.isOrIsDescendantOf("FHIR")) {
          int depth = getDepth(node);
          if (depth > maxDepth) {
            maxDepth = depth;
            mostSpecific = using;
          }
        }
      }
    }
    return mostSpecific;
  }

  private int getDepth(ModelNode node) {
    int depth = 0;
    ModelNode checkNode = node;
    while (checkNode.getParent() != null) {
      depth++;
      checkNode = checkNode.getParent();
    }
    return depth;
  }

  public ImplementationGuide loadImplementationGuide(String resourcePath) {
    Resource igResource =
        (Resource)
            FhirContext.forR4Cached()
                .newJsonParser()
                .parseResource(
                    ModelManagerFactory.class.getClassLoader().getResourceAsStream(resourcePath));

    VersionConvertor_40_50 convertor = new VersionConvertor_40_50(new BaseAdvisor_40_50());
    return (ImplementationGuide) convertor.convertResource(igResource);
  }
}
