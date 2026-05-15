package gov.cms.mat.cql_elm_translation.service;

import gov.cms.madie.cql_elm_translator.service.CqlLibraryService;
import gov.cms.madie.cql_elm_translator.utils.cql.CQLTools;
import gov.cms.madie.cql_elm_translator.utils.cql.cql_translator.MadieLibrarySourceProvider;
import gov.cms.madie.cql_elm_translator.utils.cql.cql_translator.TranslationResource;
import gov.cms.madie.cql_elm_translator.utils.cql.data.RequestData;
import gov.cms.madie.cql_elm_translator.utils.cql.parsing.model.CQLModel;
import gov.cms.mat.cql.CqlTextParser;
import gov.cms.mat.cql.elements.UsingProperties;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.cqframework.cql.cql2elm.CqlCompilerException;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.LibraryBuilder;
import org.cqframework.cql.cql2elm.model.CompiledLibrary;
import kotlinx.io.SourcesKt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
public abstract class CqlTooling {
  protected CQLTools parseCql(
      String cql,
      String accessToken,
      CqlLibraryService cqlLibraryService,
      Set<String> parentExpressions) {
    // Run Translator to compile libraries
    TranslationArtifacts translationArtifacts =
        buildTranslationArtifacts(
            cql, accessToken, cqlLibraryService, CqlCompilerException.ErrorSeverity.Error);
    CqlTranslator cqlTranslator = translationArtifacts.getTranslator();
    Map<String, CompiledLibrary> translatedLibraries =
        translationArtifacts.getTranslatedLibraries();
    // if no parentExpressions provided, consider all expressions from main CQL
    Set<String> topLevelExpressions;
    if (CollectionUtils.isEmpty(parentExpressions)) {
      topLevelExpressions = getParentExpressions();
    } else {
      topLevelExpressions = parentExpressions;
    }

    CQLTools cqlTools =
        new CQLTools(
            cql,
            getIncludedLibrariesCql(new MadieLibrarySourceProvider(), translatedLibraries),
            topLevelExpressions,
            cqlTranslator,
            translatedLibraries);

    try {
      cqlTools.generate();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return cqlTools;
  }

    protected TranslationArtifacts buildTranslationArtifacts(
            String cql,
            String accessToken,
            CqlLibraryService cqlLibraryService,
            CqlCompilerException.ErrorSeverity errorSeverity) {
        return buildTranslationArtifacts(
                buildRequestData(cql, accessToken, cqlLibraryService, errorSeverity));
    }

    protected TranslationArtifacts buildTranslationArtifacts(RequestData requestData) {
        TranslationResource translationResource = getTranslationResource(requestData);
        CqlTranslator cqlTranslator = translationResource.buildTranslator(requestData);
        return new TranslationArtifacts(cqlTranslator, getTranslatedLibraries(translationResource));
    }

    protected RequestData buildRequestData(
            String cql,
            String accessToken,
            CqlLibraryService cqlLibraryService,
            CqlCompilerException.ErrorSeverity errorSeverity) {
        cqlLibraryService.setUpLibrarySourceProvider(cql, accessToken);
        return RequestData.builder()
                .cqlData(cql)
                .errorSeverity(errorSeverity)
                .signatures(LibraryBuilder.SignatureLevel.All)
                .annotations(true)
                .locators(true)
                .disableListDemotion(true)
                .disableListPromotion(true)
                .disableMethodInvocation(false)
                .validateUnits(true)
                .resultTypes(true)
                .build();
    }

  protected Map<String, String> getIncludedLibrariesCql(
      MadieLibrarySourceProvider librarySourceProvider,
      Map<String, CompiledLibrary> translatedLibraries) {
    Map<String, String> includedLibrariesCql = new HashMap<>();
    for (CompiledLibrary l : translatedLibraries.values()) {
      if (l == null || l.getIdentifier() == null || l.getIdentifier().getId() == null) {
        continue;
      }
      var librarySource = getLibrarySourceSafely(librarySourceProvider, l);
      if (librarySource == null) {
        continue;
      }
      String libraryText =
          new String(SourcesKt.readByteArray(librarySource), StandardCharsets.UTF_8);
      includedLibrariesCql.putIfAbsent(
          l.getIdentifier().getId() + "-" + l.getIdentifier().getVersion(), libraryText);
    }
    return includedLibrariesCql;
  }

  private kotlinx.io.Source getLibrarySourceSafely(
      MadieLibrarySourceProvider librarySourceProvider, CompiledLibrary library) {
    if (library == null
        || library.getLibrary() == null
        || library.getLibrary().getIdentifier() == null) {
      return null;
    }
    try {
      return librarySourceProvider.getLibrarySource(library.getLibrary().getIdentifier());
    } catch (RuntimeException ex) {
      return null;
    }
  }

  protected Map<String, CompiledLibrary> getTranslatedLibraries(
      TranslationResource translationResource) {
    Map<String, CompiledLibrary> translatedLibraries = new HashMap<>();
    if (translationResource == null || translationResource.getLibraryManager() == null) {
      return translatedLibraries;
    }
    translationResource
        .getLibraryManager()
        .getCompiledLibraries()
        .forEach(
            (identifier, compiledLibrary) -> {
              if (compiledLibrary != null
                  && compiledLibrary.getIdentifier() != null
                  && compiledLibrary.getIdentifier().getId() != null) {
                translatedLibraries.put(compiledLibrary.getIdentifier().getId(), compiledLibrary);
              } else if (compiledLibrary != null
                  && identifier != null
                  && identifier.getId() != null) {
                translatedLibraries.put(identifier.getId(), compiledLibrary);
              }
            });
    return translatedLibraries;
  }

  protected CqlTranslator processCqlData(RequestData requestData) {
    return getTranslationResource(requestData).buildTranslator(requestData);
  }

  protected TranslationResource getTranslationResource(RequestData requestData) {
    CqlTextParser cqlTextParser = new CqlTextParser(requestData.getCqlData());
    UsingProperties usingProperties = cqlTextParser.getUsing();
    return new TranslationResource(
        usingProperties != null && "FHIR".equals(usingProperties.getLibraryType()));
  }

  protected static class TranslationArtifacts {
    private final CqlTranslator translator;
    private final Map<String, CompiledLibrary> translatedLibraries;

    protected TranslationArtifacts(
        CqlTranslator translator, Map<String, CompiledLibrary> translatedLibraries) {
      this.translator = translator;
      this.translatedLibraries = translatedLibraries;
    }

    protected CqlTranslator getTranslator() {
      return translator;
    }

    protected Map<String, CompiledLibrary> getTranslatedLibraries() {
      return translatedLibraries;
    }
  }

  private Set<String> getParentExpressions() {

    // CqlParserListener listener = new CqlParserListener(cql);
    CQLModel cqlModel = new CQLModel();
    // GAK MAT-6865 setting to default value because that is how it was when
    // this code was copied from CqlParserListener
    cqlModel.setContext("Patient");

    return cqlModel.getExpressionListFromCqlModel();
  }
}
