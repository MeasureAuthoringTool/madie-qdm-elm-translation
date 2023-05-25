package gov.cms.mat.cql_elm_translation.service;

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
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.LibraryBuilder;
import org.cqframework.cql.cql2elm.model.CompiledLibrary;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataCriteriaService {

  private final CqlConversionService cqlConversionService;

  public DataCriteria parseDataCriteriaFromCql(String cql, String accessToken) {
    // Run Translator to compile libraries
    MadieLibrarySourceProvider librarySourceProvider = new MadieLibrarySourceProvider();
    cqlConversionService.setUpLibrarySourceProvider(cql, accessToken);
    CqlTranslator cqlTranslator = runTranslator(cql);

    CQLTools cqlTools =
        new CQLTools(
            cql,
            getIncludedLibrariesCql(librarySourceProvider, cqlTranslator),
            getParentExpressions(cql),
            cqlTranslator);

    try {
      cqlTools.generate();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return cqlTools.getDataCriteria();
  }

  public List<SourceDataCriteria> getSourceDataCriteria(String cql, String accessToken) {
    DataCriteria dataCriteria = parseDataCriteriaFromCql(cql, accessToken);
    Map<CQLValueSet, Set<String>> criteriaWithValueSet =
        dataCriteria.getDataCriteriaWithValueSets();
    if (MapUtils.isEmpty(criteriaWithValueSet)) {
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
    Map<CQLCode, Set<String>> criteriaWithCodes = dataCriteria.getDataCriteriaWithCodes();
    List<SourceDataCriteria> codeCriteria =
        criteriaWithCodes.entrySet().stream()
            .map(criteria -> buildSourceDataCriteriaForCode(criteria.getKey(), criteria.getValue()))
            .toList();

    valueSetCriteria.addAll(codeCriteria);
    return valueSetCriteria;
  }

  private SourceDataCriteria buildSourceDataCriteriaForCode(CQLCode code, Set<String> dataTypes) {
    String dataType = dataTypes.stream().findFirst().orElse(null);
    // e.g "Encounter, Performed" becomes "EncounterPerformed"
    String type = dataType.replace(",", "").replace(" ", "");
    return SourceDataCriteria.builder()
        // generate fake code list id for drc, as it doesn't have one
        .codeListId("drc-" + DigestUtils.md5Hex(type))
        .qdmTitle(code.getName())
        .description(dataType + ": " + code.getName())
        .type(type)
        .drc(true)
        .build();
  }

  private SourceDataCriteria buildSourceDataCriteriaForValueSet(
      CQLValueSet valueSet, Set<String> dataTypes) {
    String dataType = dataTypes.stream().findFirst().orElse(null);
    return SourceDataCriteria.builder()
        .codeListId(valueSet.getOid())
        .qdmTitle(valueSet.getName())
        .description(dataType + ": " + valueSet.getName())
        .type(dataType.replace(",", "").replace(" ", ""))
        .build();
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
