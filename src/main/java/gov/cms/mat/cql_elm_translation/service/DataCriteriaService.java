package gov.cms.mat.cql_elm_translation.service;

import gov.cms.madie.models.measure.Measure;
import gov.cms.mat.cql_elm_translation.cql_translator.MadieLibrarySourceProvider;
import gov.cms.mat.cql_elm_translation.data.DataCriteria;
import gov.cms.mat.cql_elm_translation.data.RequestData;
import gov.cms.mat.cql_elm_translation.dto.SourceDataCriteria;
import gov.cms.mat.cql_elm_translation.utils.cql.CQLTools;
import gov.cms.mat.cql_elm_translation.utils.cql.parsing.CqlParserListener;
import gov.cms.mat.cql_elm_translation.utils.cql.parsing.model.CQLCode;
import gov.cms.mat.cql_elm_translation.utils.cql.parsing.model.CQLModel;
import gov.cms.mat.cql_elm_translation.utils.cql.parsing.model.CQLValueSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.MapUtils;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.LibraryBuilder;
import org.cqframework.cql.cql2elm.model.CompiledLibrary;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataCriteriaService {

  private final CqlConversionService cqlConversionService;

  public DataCriteria parseDataCriteriaFromCql(String cql, String accessToken) {
    return parseCql(cql, accessToken).getDataCriteria();
  }

  private CQLTools parseCql(String cql, String accessToken) {
    // Run Translator to compile libraries

    MadieLibrarySourceProvider librarySourceProvider = new MadieLibrarySourceProvider();
    cqlConversionService.setUpLibrarySourceProvider(cql, accessToken);
    CqlTranslator cqlTranslator = runTranslator(cql);

    Map<String, CompiledLibrary> translatedLibraries = new HashMap<>();
    cqlTranslator
        .getTranslatedLibraries()
        .forEach((key, value) -> translatedLibraries.put(key.getId(), value));

    CQLTools cqlTools =
        new CQLTools(
            cql,
            getIncludedLibrariesCql(librarySourceProvider, cqlTranslator),
            getParentExpressions(cql),
            cqlTranslator,
            translatedLibraries);

    try {
      cqlTools.generate();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return cqlTools;
  }

  public Set<SourceDataCriteria> getRelevantElements(Measure measure, String accessToken) {
    if (StringUtils.isBlank(measure.getCql())) {
      log.info("Data criteria not found as cql is blank");
      return Collections.emptySet();
    }

    List<SourceDataCriteria> sourceDataCriteria =
        getSourceDataCriteria(measure.getCql(), accessToken);

    CQLTools tools = parseCql(measure.getCql(), accessToken);

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
            });
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

  private String buildCriteriaType(String dataType) {
    // e.g "Encounter, Performed" becomes "EncounterPerformed"
    return dataType.replace(",", "").replace(" ", "");
  }

  private Map<String, String> getIncludedLibrariesCql(
      MadieLibrarySourceProvider librarySourceProvider, CqlTranslator cqlTranslator) {
    Map<String, String> includedLibrariesCql = new HashMap<>();
    for (CompiledLibrary l : cqlTranslator.getTranslatedLibraries().values()) {
      try {
        includedLibrariesCql.putIfAbsent(
            l.getIdentifier().getId() + "-" + l.getIdentifier().getVersion(),
            new String(
                librarySourceProvider
                    .getLibrarySource(l.getLibrary().getIdentifier())
                    .readAllBytes(),
                StandardCharsets.UTF_8));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return includedLibrariesCql;
  }

  private CqlTranslator runTranslator(String cql) {
    RequestData requestData =
        RequestData.builder()
            .cqlData(cql)
            .showWarnings(false)
            .signatures(LibraryBuilder.SignatureLevel.All)
            .annotations(true)
            .locators(true)
            .disableListDemotion(true)
            .disableListPromotion(true)
            .disableMethodInvocation(false)
            .validateUnits(true)
            .resultTypes(true)
            .build();

    return cqlConversionService.processCqlData(requestData);
  }

  private List<String> getParentExpressions(String cql) {
    CQLModel cqlModel;
    try {
      CqlParserListener listener = new CqlParserListener(cql);
      cqlModel = listener.getCQLModel();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return cqlModel.getExpressionListFromCqlModel();
  }
}
