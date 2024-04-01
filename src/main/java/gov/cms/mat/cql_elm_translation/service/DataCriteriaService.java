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
import org.springframework.util.StopWatch;

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

    Set<String> usedDefinitions = getUsedDefinitionsFromMeasure(measure);

    StopWatch watch = new StopWatch();
    watch.start("parseCql");
    log.info("parsing CQL for measure: {}", measure.getMeasureName());
    CQLTools cqlTools = parseCql(measure.getCql(), accessToken, cqlLibraryService, usedDefinitions);
    watch.stop();
    log.info("done parsing CQL for measure: {};; took {}ms", measure.getMeasureName(), watch.getLastTaskTimeMillis());

    List<SourceDataCriteria> sourceDataCriteria = getSourceDataCriteria(cqlTools);

    log.info("sourceDataCriteria: {}", sourceDataCriteria);


    // Combines explicitly called definitions with any in the tree
    Set<String> allUsedDefinitions = new HashSet<>();
    usedDefinitions.forEach(
        entry -> {
          allUsedDefinitions.add(entry);
          cqlTools
              .getUsedDefinitions()
              .forEach(
                  (definition, parentExpressions) -> {
                    if (parentExpressions.contains(entry)) {
                      allUsedDefinitions.add(definition);
                    }
                  });
          cqlTools
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
          if (!MapUtils.isEmpty(cqlTools.getExpressionNameToValuesetDataTypeMap())
              && !MapUtils.isEmpty(cqlTools.getExpressionNameToValuesetDataTypeMap().get(def))) {
            cqlTools
                .getExpressionNameToValuesetDataTypeMap()
                .get(def)
                .forEach((expression, valueSet) -> values.add(expression));
          }
          if (!MapUtils.isEmpty(cqlTools.getExpressionNameToCodeDataTypeMap())
              && !MapUtils.isEmpty(cqlTools.getExpressionNameToCodeDataTypeMap().get(def))) {
            cqlTools
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
    if (measure == null || org.springframework.util.CollectionUtils.isEmpty(measure.getGroups())) {
      return Set.of();
    }
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

  public List<SourceDataCriteria> getSourceDataCriteria(CQLTools cqlTools) {
      DataCriteria dataCriteria = cqlTools.getDataCriteria();
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
                                      buildSourceDataCriteriaForValueSet(criteria.getKey(), criteria.getValue()))
                      .collect(Collectors.toList());

      // data criteria from direct reference codes
      List<SourceDataCriteria> codeCriteria =
              criteriaWithCodes.entrySet().stream()
                      .map(criteria -> buildSourceDataCriteriaForCode(criteria.getKey(), criteria.getValue()))
                      .toList();

      valueSetCriteria.addAll(codeCriteria);
      return valueSetCriteria;
  }


  public List<SourceDataCriteria> getSourceDataCriteria(String cql, String accessToken) {
    if (StringUtils.isBlank(cql)) {
      log.info("Data criteria not found as cql is blank");
      return Collections.emptyList();
    }

    CQLTools cqlTools = parseCql(cql, accessToken, cqlLibraryService, null);

//    DataCriteria dataCriteria = parseDataCriteriaFromCql(cql, accessToken);
    return getSourceDataCriteria(cqlTools);
  }

  private SourceDataCriteria buildSourceDataCriteriaForCode(CQLCode code, Set<String> dataTypes) {
    String dataType = dataTypes.stream().findFirst().orElse(null);
    String type = buildCriteriaType(dataType);
    String name = splitByPipeAndGetLast(code.getName());
    return SourceDataCriteria.builder()
        // generate fake oid for drc, as it doesn't have one: e.g.id='71802-3',
        // codeSystemName='LOINC', codeSystemVersion='null'
        .oid(
            "drc-"
                + DigestUtils.md5Hex(
                    code.getCodeSystemName() + code.getId() + code.getCodeSystemVersion()))
        .title(name)
        .description(dataType + ": " + name)
        .type(type)
        .drc(true)
        .codeId(code.getId())
        .name(code.getName())
        .build();
  }

  private SourceDataCriteria buildSourceDataCriteriaForValueSet(
      CQLValueSet valueSet, Set<String> dataTypes) {
    String dataType = dataTypes.stream().findFirst().orElse(null);
    String name = splitByPipeAndGetLast(valueSet.getName());
    String oid = valueSet.getOid();
    SourceDataCriteria result =
        SourceDataCriteria.builder()
            .oid(oid)
            .title(name)
            .description(dataType + ": " + name)
            .type(buildCriteriaType(dataType))
            .name(valueSet.getName())
            .build();
    return result;
  }

  private String splitByPipeAndGetLast(String criteria) {
    String[] parts = criteria.split("\\|");
    // get last part
    return parts[parts.length - 1];
  }

  String buildCriteriaType(String dataType) {
    // e.g "Encounter, Performed" becomes "EncounterPerformed",
    // e.g for negation: "Assessment, Not Performed" becomes "AssessmentPerformed"
    return dataType.replace(",", "").replace(" ", "").replace("Not", "");
  }
}
