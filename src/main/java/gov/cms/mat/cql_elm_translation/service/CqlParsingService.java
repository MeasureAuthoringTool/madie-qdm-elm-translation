package gov.cms.mat.cql_elm_translation.service;

import gov.cms.mat.cql_elm_translation.utils.cql.CQLTools;
import gov.cms.mat.cql_elm_translation.utils.cql.parsing.model.CQLDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

@Slf4j
@Service
@RequiredArgsConstructor
public class CqlParsingService extends CqlTooling {
  private final CqlConversionService cqlConversionService;

  public Set<CQLDefinition> getAllDefinitions(String cql, String accessToken) {
    CQLTools cqlTools = parseCql(cql, accessToken, cqlConversionService);
    return  cqlTools.getDefinitionContent().keySet().stream()
        .map(def ->
            parseDefinitionNode(def, cqlTools.getDefinitionContent()))
        .collect(toSet());
  }

  public Map<String, Set<CQLDefinition>> getDefinitionCallstacks(String cql, String accessToken) {
    CQLTools cqlTools = parseCql(cql, accessToken, cqlConversionService);
    Map<String, Set<String>> nodeGraph = cqlTools.getCallstack();

    Set<CQLDefinition> cqlDefinitions =
        nodeGraph.keySet().stream()
            .map(node -> parseDefinitionNode(node, cqlTools.getDefinitionContent()))
            .filter(Objects::nonNull) // mapping function will return null for non-Definition nodes
            .collect(toSet());
    // remove null key, only contains included library references
    nodeGraph.remove(null);
    // remove nodes that don't reference any other Definition
    nodeGraph.keySet().removeIf(def -> nodeGraph.get(def).isEmpty());
    // remove nodes for functions -- ensures function paths are not included
    //    nodeGraph.keySet().removeIf(def -> def.endsWith("|function"));

    Map<String, Set<CQLDefinition>> callstack = new HashMap<>();

    for (String parentDefinition : nodeGraph.keySet()) {
      Set<String> defNames = nodeGraph.get(parentDefinition);
      Set<CQLDefinition> calledDefinitions = new HashSet<>(defNames.size());

      for (String defName : defNames) {
        Optional<CQLDefinition> calledDefinition =
            cqlDefinitions.stream().filter(d -> d.getId().equals(defName)).findFirst();
        calledDefinition.ifPresent(calledDefinitions::add);
      }

      if (!calledDefinitions.isEmpty()) {
        callstack.putIfAbsent(parentDefinition, calledDefinitions);
      }
    }
    return callstack;
  }

  private CQLDefinition parseDefinitionNode(String node, Map<String, String> cqlDefinitionContent) {
    // Graph includes retrieves, functions, and included library references.
    // Filter out any node that does not have CQL Definition text content.
    if (!cqlDefinitionContent.containsKey(node)) {
      return null;
    }

    CQLDefinition definition = new CQLDefinition();
    definition.setId(node);
    definition.setDefinitionLogic(cqlDefinitionContent.get(node));

    String[] parts = node.split("\\|");
    if (parts.length == 1) {
      definition.setDefinitionName(node);
    } else if (parts.length >= 3) {
      definition.setLibraryDisplayName(parts[1]);
      definition.setDefinitionName(parts[2]);
      String[] libraryParts = parts[0].split("-");
      if (libraryParts.length == 2) {
        definition.setParentLibrary(libraryParts[0]);
        definition.setLibraryVersion(libraryParts[1]);
      }
    }

    if(definition.getDefinitionLogic().startsWith("define function")) {
      definition.setFunction(true);
    }
    return definition;
  }
}
