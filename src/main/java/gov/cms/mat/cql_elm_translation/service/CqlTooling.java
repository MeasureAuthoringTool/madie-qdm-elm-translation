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
import org.hl7.elm.r1.VersionedIdentifier;
import kotlinx.io.SourcesKt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Base class for QDM CQL tooling. Compiles a measure's CQL (and its included libraries) via the
 * cqframework translator and produces a {@link CQLTools} instance that exposes the parsed/analyzed
 * artifacts (dependency graph, value sets, codes, definitions, etc.).
 */
@RequiredArgsConstructor
public abstract class CqlTooling {

  /**
   * Compiles the given CQL, gathers the source of every included library, and builds a fully
   * generated {@link CQLTools} for downstream analysis.
   *
   * @param cql the main measure CQL to compile
   * @param accessToken bearer token used to resolve included library sources
   * @param cqlLibraryService service that wires up the library source provider
   * @param parentExpressions expressions to treat as top-level; when empty, all expressions from
   *     the main CQL are used
   * @return a generated {@link CQLTools} instance
   */
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
            getIncludedLibrariesCql(
                new MadieLibrarySourceProvider(), translationArtifacts.getCompiledLibraries()),
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

  /**
   * Runs the translator and bundles the results: the {@link CqlTranslator}, the id-keyed compiled
   * library map, and the full version-keyed compiled library map.
   */
  protected TranslationArtifacts buildTranslationArtifacts(RequestData requestData) {
    TranslationResource translationResource = getTranslationResource(requestData);
    CqlTranslator cqlTranslator = translationResource.buildTranslator(requestData);
    return new TranslationArtifacts(
        cqlTranslator,
        getTranslatedLibraries(translationResource),
        translationResource.getLibraryManager().getCompiledLibraries());
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

  /**
   * Resolves the raw CQL text for every compiled library, keyed by {@code id-version}. Iterating
   * the full version-keyed map ensures multiple versions of the same library id are all retained.
   * Libraries with missing identifiers or unresolvable sources are skipped.
   */
  protected Map<String, String> getIncludedLibrariesCql(
      MadieLibrarySourceProvider librarySourceProvider,
      Map<VersionedIdentifier, CompiledLibrary> compiledLibraries) {
    Map<String, String> includedLibrariesCql = new HashMap<>();
    for (CompiledLibrary l : compiledLibraries.values()) {
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

  /**
   * Null-safe wrapper around the source provider that returns {@code null} instead of throwing when
   * a library has no identifier or its source cannot be resolved.
   */
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

  /**
   * Flattens the library manager's compiled libraries into a map keyed by library id (no version).
   * Note: when multiple versions of the same id are present, the last one wins.
   */
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
    private final Map<VersionedIdentifier, CompiledLibrary> compiledLibraries;

    protected TranslationArtifacts(
        CqlTranslator translator,
        Map<String, CompiledLibrary> translatedLibraries,
        Map<VersionedIdentifier, CompiledLibrary> compiledLibraries) {
      this.translator = translator;
      this.translatedLibraries = translatedLibraries;
      this.compiledLibraries = compiledLibraries;
    }

    protected CqlTranslator getTranslator() {
      return translator;
    }

    protected Map<String, CompiledLibrary> getTranslatedLibraries() {
      return translatedLibraries;
    }

    protected Map<VersionedIdentifier, CompiledLibrary> getCompiledLibraries() {
      return compiledLibraries;
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
