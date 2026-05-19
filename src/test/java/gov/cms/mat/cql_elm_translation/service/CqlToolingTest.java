package gov.cms.mat.cql_elm_translation.service;

import gov.cms.madie.cql_elm_translator.service.CqlLibraryService;
import gov.cms.madie.cql_elm_translator.utils.cql.cql_translator.MadieLibrarySourceProvider;
import gov.cms.madie.cql_elm_translator.utils.cql.cql_translator.TranslationResource;
import gov.cms.madie.cql_elm_translator.utils.cql.data.RequestData;
import org.cqframework.cql.cql2elm.CqlCompilerException;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.LibraryBuilder;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.model.CompiledLibrary;
import org.cqframework.cql.cql2elm.utils.SourceKt;
import org.hl7.elm.r1.Library;
import org.hl7.elm.r1.VersionedIdentifier;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CqlToolingTest {

  private final TestableCqlTooling tooling = new TestableCqlTooling();

  @Test
  void testBuildRequestDataConfiguresLibrarySourceProvider() {
    CqlLibraryService cqlLibraryService = mock(CqlLibraryService.class);

    RequestData requestData =
        tooling.buildRequestData(
            "library Test version '1.0.0'\nusing QDM version '5.6'\ncontext Patient",
            "token",
            cqlLibraryService,
            CqlCompilerException.ErrorSeverity.Error);

    verify(cqlLibraryService)
        .setUpLibrarySourceProvider(
            "library Test version '1.0.0'\nusing QDM version '5.6'\ncontext Patient", "token");
    assertThat(
        requestData.getErrorSeverity(), is(equalTo(CqlCompilerException.ErrorSeverity.Error)));
    assertThat(
        requestData.getCqlData(),
        is(equalTo("library Test version '1.0.0'\nusing QDM version '5.6'\ncontext Patient")));
    assertNotNull(requestData.createMap());
  }

  @Test
  void testGetTranslatedLibrariesHandlesNullTranslationResource() {
    assertTrue(tooling.getTranslatedLibraries(null).isEmpty());

    TranslationResource translationResource = mock(TranslationResource.class);
    doReturn(null).when(translationResource).getLibraryManager();

    assertTrue(tooling.getTranslatedLibraries(translationResource).isEmpty());
  }

  @Test
  void testGetTranslatedLibrariesUsesFallbackIdentifierKey() {
    TranslationResource translationResource = mock(TranslationResource.class);
    LibraryManager libraryManager = mock(LibraryManager.class);

    CompiledLibrary withCompiledIdentifier = new CompiledLibrary();
    VersionedIdentifier compiledIdentifier = new VersionedIdentifier();
    compiledIdentifier.setId("CompiledId");
    withCompiledIdentifier.setIdentifier(compiledIdentifier);

    CompiledLibrary withoutCompiledIdentifier = new CompiledLibrary();
    VersionedIdentifier fallbackIdentifier = new VersionedIdentifier();
    fallbackIdentifier.setId("FallbackId");

    Map<VersionedIdentifier, CompiledLibrary> compiledLibraries = new HashMap<>();
    compiledLibraries.put(compiledIdentifier, withCompiledIdentifier);
    compiledLibraries.put(fallbackIdentifier, withoutCompiledIdentifier);

    doReturn(libraryManager).when(translationResource).getLibraryManager();
    doReturn(compiledLibraries).when(libraryManager).getCompiledLibraries();

    Map<String, CompiledLibrary> translatedLibraries =
        tooling.getTranslatedLibraries(translationResource);

    assertThat(translatedLibraries.size(), is(equalTo(2)));
    assertTrue(translatedLibraries.containsKey("CompiledId"));
    assertTrue(translatedLibraries.containsKey("FallbackId"));
  }

  @Test
  void testGetIncludedLibrariesCqlSkipsInvalidAndRuntimeFailures() {
    MadieLibrarySourceProvider librarySourceProvider = mock(MadieLibrarySourceProvider.class);

    CompiledLibrary validLibrary = new CompiledLibrary();
    VersionedIdentifier validCompiledIdentifier = new VersionedIdentifier();
    validCompiledIdentifier.setId("ValidLib");
    validCompiledIdentifier.setVersion("1.0.0");
    validLibrary.setIdentifier(validCompiledIdentifier);
    Library validElmLibrary = new Library();
    VersionedIdentifier validElmIdentifier = new VersionedIdentifier();
    validElmIdentifier.setId("ValidLib");
    validElmIdentifier.setVersion("1.0.0");
    validElmLibrary.setIdentifier(validElmIdentifier);
    validLibrary.setLibrary(validElmLibrary);

    CompiledLibrary missingIdentifierLibrary = new CompiledLibrary();
    Library missingIdElmLibrary = new Library();
    missingIdElmLibrary.setIdentifier(new VersionedIdentifier());
    missingIdentifierLibrary.setLibrary(missingIdElmLibrary);

    CompiledLibrary runtimeFailureLibrary = new CompiledLibrary();
    VersionedIdentifier runtimeCompiledIdentifier = new VersionedIdentifier();
    runtimeCompiledIdentifier.setId("BoomLib");
    runtimeCompiledIdentifier.setVersion("2.0.0");
    runtimeFailureLibrary.setIdentifier(runtimeCompiledIdentifier);
    Library runtimeElmLibrary = new Library();
    VersionedIdentifier runtimeElmIdentifier = new VersionedIdentifier();
    runtimeElmIdentifier.setId("BoomLib");
    runtimeElmIdentifier.setVersion("2.0.0");
    runtimeElmLibrary.setIdentifier(runtimeElmIdentifier);
    runtimeFailureLibrary.setLibrary(runtimeElmLibrary);

    doReturn(SourceKt.asSource("library ValidLib version '1.0.0'"))
        .when(librarySourceProvider)
        .getLibrarySource(validElmIdentifier);
    doThrow(new RuntimeException("fetch failed"))
        .when(librarySourceProvider)
        .getLibrarySource(runtimeElmIdentifier);

    Map<String, CompiledLibrary> translatedLibraries =
        Map.of(
            "ValidLib", validLibrary,
            "Missing", missingIdentifierLibrary,
            "BoomLib", runtimeFailureLibrary);

    Map<String, String> includedLibraries =
        tooling.getIncludedLibrariesCql(librarySourceProvider, translatedLibraries);

    assertThat(includedLibraries.size(), is(equalTo(1)));
    assertTrue(includedLibraries.containsKey("ValidLib-1.0.0"));
    assertTrue(includedLibraries.get("ValidLib-1.0.0").contains("library ValidLib"));
  }

  @Test
  void testBuildTranslationArtifactsBuildsTranslatorAndLibraryMap() {
    RequestData requestData =
        RequestData.builder()
            .cqlData(
                "library Main version '1.0.0'\n"
                    + "using QDM version '5.6'\n"
                    + "context Patient\n"
                    + "define \"Initial Population\": true")
            .errorSeverity(CqlCompilerException.ErrorSeverity.Error)
            .signatures(LibraryBuilder.SignatureLevel.All)
            .annotations(true)
            .locators(true)
            .disableListDemotion(true)
            .disableListPromotion(true)
            .disableMethodInvocation(false)
            .validateUnits(true)
            .resultTypes(true)
            .build();

    CqlTooling.TranslationArtifacts artifacts = tooling.buildTranslationArtifacts(requestData);
    CqlTranslator translator = artifacts.getTranslator();

    assertNotNull(translator);
    assertNotNull(artifacts.getTranslatedLibraries());
  }

  @Test
  void testGetIncludedLibrariesCqlSkipsNullEntry() {
    MadieLibrarySourceProvider librarySourceProvider = mock(MadieLibrarySourceProvider.class);

    Map<String, CompiledLibrary> translatedLibraries = new HashMap<>();
    translatedLibraries.put("NullLib", null);

    Map<String, String> includedLibraries =
        tooling.getIncludedLibrariesCql(librarySourceProvider, translatedLibraries);

    assertThat(includedLibraries.size(), is(equalTo(0)));
  }

  private static class TestableCqlTooling extends CqlTooling {}
}
