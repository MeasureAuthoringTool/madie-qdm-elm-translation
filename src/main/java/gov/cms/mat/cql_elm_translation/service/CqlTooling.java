package gov.cms.mat.cql_elm_translation.service;

import gov.cms.mat.cql_elm_translation.cql_translator.MadieLibrarySourceProvider;
import gov.cms.mat.cql_elm_translation.data.RequestData;
import gov.cms.mat.cql_elm_translation.utils.cql.CQLTools;
import gov.cms.mat.cql_elm_translation.utils.cql.parsing.CqlParserListener;
import gov.cms.mat.cql_elm_translation.utils.cql.parsing.model.CQLModel;
import lombok.RequiredArgsConstructor;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.LibraryBuilder;
import org.cqframework.cql.cql2elm.model.CompiledLibrary;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public abstract class CqlTooling {
  public CQLTools parseCql(
      String cql, String accessToken, CqlConversionService cqlConversionService) {
    // Run Translator to compile libraries
    cqlConversionService.setUpLibrarySourceProvider(cql, accessToken);
    CqlTranslator cqlTranslator = runTranslator(cql, cqlConversionService);

    Map<String, CompiledLibrary> translatedLibraries = new HashMap<>();
    cqlTranslator
        .getTranslatedLibraries()
        .forEach((key, value) -> translatedLibraries.put(key.getId(), value));

    CQLTools cqlTools =
        new CQLTools(
            cql,
            getIncludedLibrariesCql(new MadieLibrarySourceProvider(), cqlTranslator),
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

  private CqlTranslator runTranslator(String cql, CqlConversionService cqlConversionService) {
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
