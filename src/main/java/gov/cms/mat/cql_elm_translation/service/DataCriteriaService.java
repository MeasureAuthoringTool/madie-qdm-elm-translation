package gov.cms.mat.cql_elm_translation.service;

import gov.cms.madie.models.measure.Measure;
import gov.cms.mat.cql_elm_translation.data.DataCriteria;
import gov.cms.mat.cql_elm_translation.dto.SourceDataCriteria;
import gov.cms.mat.cql_elm_translation.utils.cql.CQLTools;
import gov.cms.mat.cql_elm_translation.utils.cql.parsing.model.CQLCode;
import gov.cms.mat.cql_elm_translation.utils.cql.parsing.model.CQLValueSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataCriteriaService extends CqlTooling {

  private final CqlLibraryService cqlLibraryService;

  public DataCriteria parseDataCriteriaFromCql(String cql, String accessToken) {
    return parseCql(cql, accessToken, cqlLibraryService, null).getDataCriteria();
  }

  public Set<SourceDataCriteria> getRelevantElements(Measure measure, String accessToken) {
    if (StringUtils.isBlank(measure.getCql())) {
      log.info("Data criteria not found as cql is blank");
      return Collections.emptySet();
    }
    List<SourceDataCriteria> sourceDataCriteria =
        getSourceDataCriteria(measure.getCql(), accessToken);
    CQLTools tools = parseCql(measure.getCql(), accessToken, cqlLibraryService, null);
    Set<String> usedDefinitions = getUsedDefinitionsFromMeasure(measure);
    // Combines explicitly called definitions with any in the tree
    Set<String> allUsedDefinitions = new HashSet<>();
    usedDefinitions.forEach(
        entry -> {
          allUsedDefinitions.add(entry);
          tools
              .getUsedDefinitions()
              .forEach(
                  (definition, parentExpressions) -> {
                    if (parentExpressions.contains(entry)) {
                      allUsedDefinitions.add(definition);
                    }
                  });
          tools
              .getUsedFunctions()
              .forEach(
                  (function, parentExpressions) -> {
                    if (parentExpressions.contains(entry)) {
                      allUsedDefinitions.add(function);
                    }
                  });
        });

    Set<String> values = new HashSet<>();
    allUsedDefinitions.forEach(
        def -> {
          if (!MapUtils.isEmpty(tools.getExpressionNameToValuesetDataTypeMap())
              && !MapUtils.isEmpty(tools.getExpressionNameToValuesetDataTypeMap().get(def))) {
            tools
                .getExpressionNameToValuesetDataTypeMap()
                .get(def)
                .forEach((expression, valueSet) -> values.add(expression));
          }
          if (!MapUtils.isEmpty(tools.getExpressionNameToCodeDataTypeMap())
              && !MapUtils.isEmpty(tools.getExpressionNameToCodeDataTypeMap().get(def))) {
            tools
                .getExpressionNameToCodeDataTypeMap()
                .get(def)
                .forEach((expression, valueSet) -> values.add(expression));
          }
        });

    Set<SourceDataCriteria> relevantSet = new TreeSet<>();
    sourceDataCriteria.stream()
        .filter(sourceDataCriteria1 -> values.contains(sourceDataCriteria1.getName()))
        .forEach(
            src -> {
              relevantSet.add(src);
            });
    return relevantSet;
  }

  private Set<String> getUsedDefinitionsFromMeasure(Measure measure) {
    Set<String> usedDefinitions = new HashSet<>();
    measure
        .getGroups()
        .forEach(
            group -> {
              group
                  .getPopulations()
                  .forEach(
                      population -> {
                        if (!population.getDefinition().isEmpty()) {
                          usedDefinitions.add(population.getDefinition());
                        }
                      });
              if (!CollectionUtils.isEmpty(group.getMeasureObservations())) {
                group
                    .getMeasureObservations()
                    .forEach(
                        measureObservation -> {
                          if (!measureObservation.getDefinition().isEmpty()) {
                            usedDefinitions.add(measureObservation.getDefinition());
                          }
                        });
              }
              if (!CollectionUtils.isEmpty(group.getStratifications())) {
                group
                    .getStratifications()
                    .forEach(
                        stratification -> {
                          if (!stratification.getCqlDefinition().isEmpty()) {
                            usedDefinitions.add(stratification.getCqlDefinition());
                          }
                        });
              }
            });
    measure
        .getSupplementalData()
        .forEach(defDescPair -> usedDefinitions.add(defDescPair.getDefinition()));
    measure
        .getRiskAdjustments()
        .forEach(defDescPair -> usedDefinitions.add(defDescPair.getDefinition()));
    return usedDefinitions;
  }

  public List<SourceDataCriteria> getSourceDataCriteria(String cql, String accessToken) {
    if (StringUtils.isBlank(cql)) {
      log.info("Data criteria not found as cql is blank");
      return Collections.emptyList();
    }

    DataCriteria dataCriteria = parseDataCriteriaFromCql(cql, accessToken);
    Map<CQLValueSet, Set<String>> criteriaWithValueSet =
        dataCriteria.getDataCriteriaWithValueSets();

    Map<CQLCode, Set<String>> criteriaWithCodes = dataCriteria.getDataCriteriaWithCodes();
    if (MapUtils.isEmpty(criteriaWithValueSet) && MapUtils.isEmpty(criteriaWithCodes)) {
      log.info("Data criteria not found for given cql");
      return Collections.emptyList();
    }
    // data criteria from value sets
    List<SourceDataCriteria> valueSetCriteria =
        criteriaWithValueSet.entrySet().stream()
            .map(
                criteria ->
                    buildSourceDataCriteriasForValueSet(criteria.getKey(), criteria.getValue()))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

    // data criteria from direct reference codes
    List<SourceDataCriteria> codeCriteria =
        criteriaWithCodes.entrySet().stream()
            .map(
                criteria -> buildSourceDataCriteriasForCode(criteria.getKey(), criteria.getValue()))
            .flatMap(Collection::stream)
            .toList();

    valueSetCriteria.addAll(codeCriteria);
    return valueSetCriteria;
  }

  private Set<SourceDataCriteria> buildSourceDataCriteriasForCode(
      CQLCode code, Set<String> dataTypes) {
    // return nothing if datatype is missing..otherwise we'll get an NPE in buildCriteriaType
    if (CollectionUtils.isEmpty(dataTypes)) {
      return Set.of();
    }
    return dataTypes.stream()
        .map(
            dataType -> {
              String type = buildCriteriaType(dataType);
              String name = splitByPipeAndGetLast(code.getName());
              return SourceDataCriteria.builder()
                  // generate fake oid for drc, as it doesn't have one: e.g.id='71802-3',
                  // codeSystemName='LOINC', codeSystemVersion='null'
                  .oid(
                      "drc-"
                          + DigestUtils.md5Hex(
                              code.getCodeSystemName()
                                  + code.getId()
                                  + code.getCodeSystemVersion()))
                  .title(name)
                  .description(dataType + ": " + name)
                  .type(type)
                  .drc(true)
                  .codeId(code.getId())
                  .name(code.getName())
                  .build();
            })
        .collect(Collectors.toSet());
  }

  private Set<SourceDataCriteria> buildSourceDataCriteriasForValueSet(
      CQLValueSet valueSet, Set<String> dataTypes) {
    // return nothing if datatype is missing..otherwise we'll get an NPE in buildCriteriaType
    if (CollectionUtils.isEmpty(dataTypes)) {
      return Set.of();
    }
    return dataTypes.stream()
        .map(
            dataType -> {
              String name = splitByPipeAndGetLast(valueSet.getName());
              String oid = valueSet.getOid();
              return SourceDataCriteria.builder()
                  .oid(oid)
                  .title(name)
                  .description(dataType + ": " + name)
                  .type(buildCriteriaType(dataType))
                  .name(valueSet.getName())
                  .build();
            })
        .collect(Collectors.toSet());
  }

  private String splitByPipeAndGetLast(String criteria) {
    String[] parts = criteria.split("\\|");
    // get last part
    return parts[parts.length - 1];
  }

  private String buildCriteriaType(String dataType) {
    // e.g "Encounter, Performed" becomes "EncounterPerformed",
    // e.g for negation: "Assessment, Not Performed" becomes "AssessmentPerformed"
    return dataType.replace(",", "").replace(" ", "").replace("Not", "");
  }
}
