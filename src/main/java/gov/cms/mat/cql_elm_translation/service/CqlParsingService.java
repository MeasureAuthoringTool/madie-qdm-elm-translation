package gov.cms.mat.cql_elm_translation.service;

import gov.cms.mat.cql_elm_translation.utils.cql.CQLTools;
import gov.cms.mat.cql_elm_translation.utils.cql.parsing.model.CQLDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
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

  /**
   * Parses the CQL and generates objects for all CQL Definitions
   * and Functions found in the Main and Included Libraries.
   * @param cql Main Library CQL
   * @param accessToken Requesting User's Okta Bearer token
   * @return Set of all CQL Definitions and Functions in the main and included Libraries.
   */
  public Set<CQLDefinition> getAllDefinitions(String cql, String accessToken) {
    CQLTools cqlTools = parseCql(cql, accessToken, cqlConversionService);
    return  cqlTools.getDefinitionContent().keySet().stream()
        .map(def ->
            parseDefinitionNode(def, cqlTools.getDefinitionContent()))
        .collect(toSet());
  }

  /***
   * <p>
   * Maps the references between CQL Definitions. In other words, which CQL Definitions and Functions
   * are called by which other CQL Definition.
   * <p>
   * Note that the resulting Map will most likely not contain all CQL Definitions and Function in the provided CQL,
   * and should be used when needing to work with all CQL Definitions and/or Functions.
   * </p>
   *
   * @param cql CQL to parse.
   * @param accessToken Application user's Okta Bearer token.
   * @return Map representation of the callstack for CQL Definitions that call at least 1 other
   *  CQL Definition and/or Function.
   *  <p>
   *  Keys: Strings of CQL Definitions that reference/call 1 or more other CQL Definitions and Functions.
   *  CQL Definitions that do not reference any other CQL Definition and/or Function will not appear as a Key.
   *  </p><p>
   *  Values: Set of CQL Definition Objects that are referenced in the Key CQL Definition.
   *  </p>
   */
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

    //Included Lib Define: AHAOverall-2.5.000|AHA|Has Left Ventricular Assist Device
    //Main Lib Define: Numerator
    String[] parts = node.split("\\|");
    if (parts.length == 1) {  // Define from Main Library
      definition.setDefinitionName(node);
    } else if (parts.length >= 3) { // Define from some included Library
      definition.setLibraryDisplayName(parts[1]);
      definition.setDefinitionName(parts[2]);
      String[] libraryParts = parts[0].split("-");
      if (libraryParts.length == 2) {
        definition.setParentLibrary(libraryParts[0]);
        definition.setLibraryVersion(libraryParts[1]);
      }
    }
    // TODO could use a stronger comparator for determining if node is Definition or Function
    definition.setFunction(definition.getDefinitionLogic().startsWith("define function"));
    return definition;
  }
}
