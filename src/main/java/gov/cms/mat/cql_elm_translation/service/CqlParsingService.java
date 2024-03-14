package gov.cms.mat.cql_elm_translation.service;

import gov.cms.mat.cql_elm_translation.dto.CqlLookups;
import gov.cms.mat.cql_elm_translation.dto.ElementLookup;
import gov.cms.mat.cql_elm_translation.utils.cql.CQLTools;
import gov.cms.mat.cql_elm_translation.utils.cql.parsing.model.CQLCode;
import gov.cms.mat.cql_elm_translation.utils.cql.parsing.model.CQLCodeSystem;
import gov.cms.mat.cql_elm_translation.utils.cql.parsing.model.CQLDefinition;
import gov.cms.mat.cql_elm_translation.utils.cql.parsing.model.CQLIncludeLibrary;
import gov.cms.mat.cql_elm_translation.utils.cql.parsing.model.CQLParameter;
import gov.cms.mat.cql_elm_translation.utils.cql.parsing.model.CQLValueSet;
import gov.cms.mat.cql_elm_translation.utils.cql.parsing.model.DefinitionContent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static java.util.stream.Collectors.toSet;

@Slf4j
@Service
@RequiredArgsConstructor
public class CqlParsingService extends CqlTooling {
  private final CqlLibraryService cqlLibraryService;

  /**
   * Parses the CQL and generates objects for all CQL Definitions and Functions found in the Main
   * and Included Libraries.
   *
   * @param cql Main Library CQL
   * @param accessToken Requesting User's Okta Bearer token
   * @return Set of all CQL Definitions and Functions in the main and included Libraries.
   */
  public Set<CQLDefinition> getAllDefinitions(String cql, String accessToken) {
    CQLTools cqlTools = parseCql(cql, accessToken, cqlLibraryService, null);
    return buildCqlDefinitions(cqlTools);
  }

  /**
   * Parses the CQL and generates cql artifacts(including for the CQL of the included Libraries).
   * refer CQL artifacts- gov.cms.mat.cql_elm_translation.dto.CQLLookups
   *
   * @param cql- measure cql
   * @param measureExpressions- set of cql definitions used in measure groups, SDEs & RAVs
   * @param accessToken Requesting User's Okta Bearer token
   * @return CQLLookups
   */
  public CqlLookups getCqlLookups(String cql, Set<String> measureExpressions, String accessToken) {
    if (StringUtils.isBlank(cql) || CollectionUtils.isEmpty(measureExpressions)) {
      return null;
    }

    CQLTools cqlTools = parseCql(cql, accessToken, cqlLibraryService, measureExpressions);
    String name = cqlTools.getLibrary().getIdentifier().getId();
    String version = cqlTools.getLibrary().getIdentifier().getVersion();
    String model = cqlTools.getUsingProperties().getLibraryType();
    String modelVersion = cqlTools.getUsingProperties().getVersion();
    Set<CQLParameter> parameters =
        cqlTools.getUsedParameters().stream()
            .filter(parameter -> parameter.getParameterName().split("\\|").length == 1)
            .collect(toSet());
    Set<String> usedDefinitions = new HashSet<>(measureExpressions);
    // add used definitions
    for (var entry : cqlTools.getUsedDefinitions().entrySet()) {
      usedDefinitions.add(entry.getKey());
      usedDefinitions.addAll(entry.getValue());
    }
    // add used functions
    for (var entry : cqlTools.getUsedFunctions().entrySet()) {
      usedDefinitions.add(entry.getKey());
      usedDefinitions.addAll(entry.getValue());
    }

    // all CQLDefinitions
    Set<CQLDefinition> allCqlDefinitions = buildCqlDefinitions(cqlTools);
    // only used CQLDefinitions(measure definitions + any used by measure definitions)
    Set<CQLDefinition> usedCqlDefinition =
        buildUsedCqlDefinitions(allCqlDefinitions, usedDefinitions);
    Set<CQLCodeSystem> cqlCodeSystems = buildCqlCodeSystems(cqlTools.getUsedCodes());
    Set<ElementLookup> elementLookups = buildElementLookups(cqlTools);
    Set<CQLIncludeLibrary> includeLibraries =
        updateIncludedLibraries(cqlTools.getUsedLibraries(), modelVersion);
    return CqlLookups.builder()
        .context("Patient")
        .library(name)
        .version(version)
        .usingModel(model)
        .usingModelVersion(modelVersion)
        .parameters(parameters)
        .valueSets(cqlTools.getUsedCQLValuesets())
        .codes(cqlTools.getUsedCodes())
        .definitions(usedCqlDefinition)
        .codeSystems(cqlCodeSystems)
        .includeLibraries(includeLibraries)
        .elementLookups(elementLookups)
        .build();
  }

  /**
   * Maps the references between CQL Definitions. In other words, which CQL Definitions and
   * Functions are called by which other CQL Definition.
   *
   * <p>Note that the resulting Map will most likely not contain all CQL Definitions and Function in
   * the provided CQL, and should be used when needing to work with all CQL Definitions and/or
   * Functions.
   *
   * @param cql CQL to parse.
   * @param accessToken Application user's Okta Bearer token.
   * @return Map representation of the callstack for CQL Definitions that call at least 1 other CQL
   *     Definition and/or Function.
   *     <p>Keys: Strings of CQL Definitions that reference/call 1 or more other CQL Definitions and
   *     Functions. CQL Definitions that do not reference any other CQL Definition and/or Function
   *     will not appear as a Key.
   *     <p>Values: Set of CQL Definition Objects that are referenced in the Key CQL Definition.
   */
  public Map<String, Set<CQLDefinition>> getDefinitionCallstacks(String cql, String accessToken) {
    CQLTools cqlTools = parseCql(cql, accessToken, cqlLibraryService, null);
    Map<String, Set<String>> nodeGraph = cqlTools.getCallstack();
    Set<String> keys = nodeGraph.keySet();
    Set<CQLDefinition> cqlDefinitions =
        cqlTools.getDefinitionContents().stream()
            .filter(definitionContent -> keys.contains(definitionContent.getName()))
            .map(this::buildCqlDefinition)
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

  private Set<ElementLookup> buildElementLookups(CQLTools cqlTools) {
    Set<ElementLookup> cqlElementLookups = new HashSet<>();
    // collect element lookups for codes and code retrieves.
    // if retrieve with cql code available, use retrieve else use code to build lookup
    if (CollectionUtils.isNotEmpty(cqlTools.getUsedCodes())) {
      Map<CQLCode, Set<String>> cqlCodeSetMap =
          cqlTools.getDataCriteria().getDataCriteriaWithCodes();
      for (CQLCode usedCode : cqlTools.getUsedCodes()) {
        Set<String> datatypes = getCodeDatatypes(cqlCodeSetMap, usedCode.getId());
        if (CollectionUtils.isNotEmpty(datatypes)) {
          Set<ElementLookup> valueSetLookups =
              datatypes.stream()
                  .map(value -> buildElementLookupForCqlCode(usedCode, value))
                  .collect(toSet());
          cqlElementLookups.addAll(valueSetLookups);
        } else {
          cqlElementLookups.add(buildElementLookupForCqlCode(usedCode, null));
        }
      }
    }
    // collect element lookups for value sets and value set retrieves
    // if retrieve with value set available, use retrieve else use value set to build lookup
    if (CollectionUtils.isNotEmpty(cqlTools.getUsedCQLValuesets())) {
      Map<CQLValueSet, Set<String>> cqlValueSetSetMap =
          cqlTools.getDataCriteria().getDataCriteriaWithValueSets();
      for (CQLValueSet cqlValueSet : cqlTools.getUsedCQLValuesets()) {
        Set<String> datatypes = getValueSetDatatypes(cqlValueSetSetMap, cqlValueSet.getOid());
        if (CollectionUtils.isNotEmpty(datatypes)) {
          Set<ElementLookup> valueSetLookups =
              datatypes.stream()
                  .map(value -> buildElementLookupForValueSet(cqlValueSet, value))
                  .collect(toSet());
          cqlElementLookups.addAll(valueSetLookups);
        } else {
          cqlElementLookups.add(buildElementLookupForValueSet(cqlValueSet, null));
        }
      }
    }
    return cqlElementLookups;
  }

  // returns the data types associated with value set
  private Set<String> getValueSetDatatypes(
      Map<CQLValueSet, Set<String>> cqlValueSetSetMap, String oid) {
    Optional<Set<String>> values =
        cqlValueSetSetMap.entrySet().stream()
            .filter(
                entry -> {
                  CQLValueSet cqlValueSet = entry.getKey();
                  return StringUtils.equals(cqlValueSet.getOid(), oid);
                })
            .map(Map.Entry::getValue)
            .findFirst();
    return values.orElse(null);
  }

  // returns the data types associated with code
  private Set<String> getCodeDatatypes(Map<CQLCode, Set<String>> cqlCodeSetMap, String code) {
    Optional<Set<String>> values =
        cqlCodeSetMap.entrySet().stream()
            .filter(
                entry -> {
                  CQLCode cqlCode = entry.getKey();
                  return StringUtils.equals(cqlCode.getId(), code);
                })
            .map(Map.Entry::getValue)
            .findFirst();
    return values.orElse(null);
  }

  private ElementLookup buildElementLookupForCqlCode(CQLCode cqlCode, String dataType) {
    return ElementLookup.builder()
        .code(true)
        .id(UUID.randomUUID().toString())
        .codeName(cqlCode.getCodeName())
        .codeSystemOID(cqlCode.getCodeSystemOID())
        .codeSystemVersion(cqlCode.getCodeSystemVersion())
        .displayName(cqlCode.getDisplayName())
        .name(cqlCode.getName())
        .oid(cqlCode.getId())
        .datatype(dataType)
        .taxonomy(cqlCode.getCodeSystemName())
        .build();
  }

  private ElementLookup buildElementLookupForValueSet(CQLValueSet cqlValueSet, String dataType) {
    return ElementLookup.builder()
        .code(false)
        .id(UUID.randomUUID().toString())
        .name(cqlValueSet.getName())
        .oid(cqlValueSet.getOid())
        .datatype(dataType)
        .taxonomy("Grouping") // hardcoded for now
        .build();
  }

  private Set<CQLDefinition> buildCqlDefinitions(CQLTools cqlTools) {
    return cqlTools.getDefinitionContents().stream().map(this::buildCqlDefinition).collect(toSet());
  }

  private Set<CQLDefinition> buildUsedCqlDefinitions(
      Set<CQLDefinition> cqlDefinitions, Set<String> usedDefinitions) {
    return cqlDefinitions.stream()
        .filter(cqlDefinition -> usedDefinitions.contains(cqlDefinition.getName()))
        .map(
            cqlDefinition -> {
              String logic = cqlDefinition.getLogic();
              return cqlDefinition.toBuilder()
                  .uuid(UUID.randomUUID().toString())
                  .definitionName(cqlDefinition.getName())
                  // TODO: Ideally should come from listener/visitor
                  .definitionLogic(logic.split(":", 2)[1])
                  .build();
            })
        .collect(toSet());
  }

  private Set<CQLCodeSystem> buildCqlCodeSystems(Set<CQLCode> cqlCodes) {
    if (CollectionUtils.isEmpty(cqlCodes)) {
      return Set.of();
    }
    return cqlCodes.stream()
        .map(
            cqlCode ->
                CQLCodeSystem.builder()
                    .codeSystemName(cqlCode.getCodeSystemName())
                    .codeSystemVersion(cqlCode.getCodeSystemVersion())
                    .codeSystem(cqlCode.getCodeSystemOID().split("urn:oid:")[1])
                    .build())
        .collect(toSet());
  }

  private Set<CQLIncludeLibrary> updateIncludedLibraries(
      Set<CQLIncludeLibrary> includeLibraries, String modelVersion) {
    if (CollectionUtils.isEmpty(includeLibraries)) {
      return Set.of();
    }
    return includeLibraries.stream()
        .map(
            library -> {
              var libraryCopy = library.toBuilder().build();
              libraryCopy.setQdmVersion(modelVersion);
              return libraryCopy;
            })
        .collect(toSet());
  }

  private CQLDefinition buildCqlDefinition(DefinitionContent definitionContent) {
    // Graph includes retrieves, functions, and included library references.
    // Filter out any node that does not have CQL Definition text content.
    if (StringUtils.isBlank(definitionContent.getContent())) {
      return null;
    }

    CQLDefinition definition = new CQLDefinition();
    definition.setId(definitionContent.getName());
    definition.setDefinitionLogic(definitionContent.getContent());
    definition.setFunctionArguments(definitionContent.getFunctionArguments());

    // Included Lib Define: AHAOverall-2.5.000|AHA|Has Left Ventricular Assist Device
    // Main Lib Define: Numerator
    String[] parts = definitionContent.getName().split("\\|");
    if (parts.length == 1) { // Define from Main Library
      definition.setDefinitionName(definitionContent.getName());
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

  public Map<String, Set<String>> getUsedFunctions(String cql, String accessToken) {
    CQLTools cqlTools = parseCql(cql, accessToken, cqlLibraryService, null);
    return cqlTools.getUsedFunctions();
  }
}
