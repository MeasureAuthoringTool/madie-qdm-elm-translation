package gov.cms.mat.cql_elm_translation.service.filters;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

public interface JsonHelpers {
  default Optional<String> getTextFromNodeId(JsonNode node, String id) {
    return getTextFromNode(node.path(id));
  }

  default Optional<String> getTextFromNode(JsonNode jsonNode) {
    if (jsonNode.isMissingNode()) {
      return Optional.empty();
    }

    String textValue = jsonNode.toString();
    if (StringUtils.isEmpty(textValue)) {
      return Optional.empty();
    }
    return Optional.of(StringUtils.replace(textValue, "\"", ""));
  }
}
