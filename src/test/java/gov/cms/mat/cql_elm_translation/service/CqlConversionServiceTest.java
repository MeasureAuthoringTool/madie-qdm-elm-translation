package gov.cms.mat.cql_elm_translation.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import gov.cms.madie.cql_elm_translator.utils.cql.cql_translator.TranslationResource;
import gov.cms.madie.models.dto.TranslatedLibrary;
import gov.cms.mat.cql.CqlTextParser;
import gov.cms.mat.cql.dto.CqlConversionPayload;
import gov.cms.mat.cql_elm_translation.ResourceFileUtil;
import gov.cms.madie.cql_elm_translator.utils.cql.cql_translator.MadieLibrarySourceProvider;
import gov.cms.madie.cql_elm_translator.utils.cql.data.RequestData;
import gov.cms.madie.cql_elm_translator.exceptions.InternalServerException;
import gov.cms.madie.cql_elm_translator.service.CqlLibraryService;

import org.cqframework.cql.cql2elm.CqlCompilerException;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.LibraryContentType;
import org.cqframework.cql.cql2elm.model.CompiledLibrary;
import org.cqframework.cql.cql2elm.tracking.TrackBack;
import org.hl7.elm.r1.CodeFilterElement;
import org.hl7.elm.r1.Library;
import org.hl7.elm.r1.Retrieve;
import org.hl7.elm.r1.VersionedIdentifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@ExtendWith(MockitoExtension.class)
class CqlConversionServiceTest implements ResourceFileUtil {

  @Mock private CqlLibraryService cqlLibraryService;
  @InjectMocks private CqlConversionService service;

  private static RequestData requestData;

  @BeforeAll
  static void setUp() {
    requestData =
        RequestData.builder()
            .errorSeverity(CqlCompilerException.ErrorSeverity.Info)
            .annotations(true)
            .locators(true)
            .disableListDemotion(true)
            .disableListPromotion(true)
            .disableMethodInvocation(false)
            .validateUnits(true)
            .resultTypes(true)
            .build();
  }

  @Test
  void testProcessCqlDataWithErrors() {
    String cqlData;
    File inputCqlFile = new File(this.getClass().getResource("/fhir.cql").getFile());

    try {
      cqlData = new String(Files.readAllBytes(inputCqlFile.toPath()));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    RequestData data = requestData.toBuilder().cqlData(cqlData).build();
    CqlConversionPayload payload = service.translateCqlToElm(data);
    assertNotNull(payload);
    String resultJson = payload.getJson();
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode jsonNode = objectMapper.readTree(resultJson);
    assertNotNull(jsonNode);
    JsonNode libraryNode = jsonNode.at("/errorExceptions");
    assertNotNull(libraryNode);
    assertTrue(libraryNode.isMissingNode());
  }

  @Test
  void testProcessCqlDataWithErrorsNonSupportedModel() {
    String cqlData;
    File inputCqlFile = new File(this.getClass().getResource("/non_supported_model.cql").getFile());

    try {
      cqlData = new String(Files.readAllBytes(inputCqlFile.toPath()));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    RequestData data = requestData.toBuilder().cqlData(cqlData).build();
    CqlConversionPayload payload = service.translateCqlToElm(data);
    assertNotNull(payload);
    String resultJson = payload.getJson();
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode jsonNode = objectMapper.readTree(resultJson);
    assertNotNull(jsonNode);
    JsonNode libraryNode = jsonNode.at("/errorExceptions");
    assertNotNull(libraryNode);
    assertFalse(libraryNode.isMissingNode());
  }

  @Test
  void testProcessCqlDataWithErrorsQICore() {
    String cqlData;
    File inputCqlFile = new File(this.getClass().getResource("/qicore.cql").getFile());

    try {
      cqlData = new String(Files.readAllBytes(inputCqlFile.toPath()));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    RequestData data = requestData.toBuilder().cqlData(cqlData).build();
    CqlConversionPayload payload = service.translateCqlToElm(data);
    assertNotNull(payload);
    String resultJson = payload.getJson();
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode jsonNode = objectMapper.readTree(resultJson);
    assertNotNull(jsonNode);
    JsonNode libraryNode = jsonNode.at("/errorExceptions");
    assertNotNull(libraryNode);
    assertTrue(libraryNode.isMissingNode());
  }

  @Test
  void testProcessCqlDataWithErrorsMissingModel() {
    String cqlData;
    File inputCqlFile = new File(this.getClass().getResource("/missing-model.cql").getFile());
    try {
      cqlData = new String(Files.readAllBytes(inputCqlFile.toPath()));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    RequestData data = requestData.toBuilder().cqlData(cqlData).build();
    MadieLibrarySourceProvider.setUsing(new CqlTextParser(cqlData).getUsing());
    MadieLibrarySourceProvider.setCqlLibraryService(cqlLibraryService);
    MadieLibrarySourceProvider.setAccessToken("access token");
    CqlConversionPayload payload = service.translateCqlToElm(data);
    assertNotNull(payload);
    String resultJson = payload.getJson();
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode jsonNode = objectMapper.readTree(resultJson);
    assertNotNull(jsonNode);
    JsonNode libraryNode = jsonNode.at("/errorExceptions");
    assertNotNull(libraryNode);

    assertFalse(libraryNode.isMissingNode());
    final AtomicBoolean foundMessage = new AtomicBoolean(Boolean.FALSE);
    libraryNode.forEach(
        node ->
            foundMessage.set(
                foundMessage.get()
                    || node.get("message")
                        .asText()
                        .contains("Model Type and version are required")));
    assertTrue(foundMessage.get());
  }

  @Test
  void testGetTranslatedLibrariesForCqlForCql() throws IOException {
    String cql = getData("/qdm_data_criteria_retrieval_test.cql");
    String matGlobal = getData("/mat_global_common_functions.cql");
    MadieLibrarySourceProvider.setUsing(new CqlTextParser(cql).getUsing());
    MadieLibrarySourceProvider.setCqlLibraryService(cqlLibraryService);
    doReturn(matGlobal)
        .when(cqlLibraryService)
        .getLibraryCql(eq("MATGlobalCommonFunctions"), eq("7.0.000"), nullable(String.class));

    List<TranslatedLibrary> libraries =
        service.getTranslatedLibrariesForCql(
            cql, "token", CqlCompilerException.ErrorSeverity.Error);
    AtomicBoolean foundAMatch = new AtomicBoolean();
    var matchingLib =
        libraries.stream()
            .filter(library -> library.getElmJson().contains("DataCriteriaRetrivalTest"))
            .findFirst();
    assertThat(matchingLib.get().getName(), is(equalTo("DataCriteriaRetrivalTest")));
  }

  @Test
  void testBuildTranslatedLibrary() {
    Library library = new Library();
    VersionedIdentifier identifier = new VersionedIdentifier();
    identifier.setId("test");
    identifier.setVersion("0.0.000");
    library.setIdentifier(identifier);
    CompiledLibrary compiledLibrary = new CompiledLibrary();
    compiledLibrary.setLibrary(library);
    TranslatedLibrary translatedLibrary =
        service.buildTranslatedLibrary(compiledLibrary, Map.of("test-0.0.000", "test cql"));
    assertThat(translatedLibrary.getName(), is(equalTo(identifier.getId())));
    assertThat(translatedLibrary.getVersion(), is(equalTo(identifier.getVersion())));
    assertThat(translatedLibrary.getCql(), is(equalTo("test cql")));
  }

  @Test
  void testBuildTranslatedLibraryWhenExceptionThrown() throws IOException {
    Library library = new Library();
    VersionedIdentifier identifier = new VersionedIdentifier();
    identifier.setId("test");
    identifier.setVersion("0.0.000");
    library.setIdentifier(identifier);
    CompiledLibrary compiledLibrary = new CompiledLibrary();
    compiledLibrary.setLibrary(library);
    CqlConversionService conversionService = spy(service);
    doThrow(new IOException("Failed to build the library"))
        .when(conversionService)
        .convertToJson(library, LibraryContentType.JSON);
    assertThrows(
        InternalServerException.class,
        () ->
            conversionService.buildTranslatedLibrary(
                compiledLibrary, Map.of("test-0.0.000", "test cql")),
        "An error occurred while building translated artifacts for library test");
  }

  @Test
  void testBuildTranslatedLibraryWhenCompiledLibraryIsNull() {
    TranslatedLibrary library = service.buildTranslatedLibrary(null, null);
    assertNull(library);
  }

  @Test
  void testGetElmForBlankCql() throws IOException {
    List<TranslatedLibrary> elms =
        service.getTranslatedLibrariesForCql(
            null, "token", CqlCompilerException.ErrorSeverity.Error);
    assertThat(elms.size(), is(equalTo(0)));
  }

  @Test
  void testGetTranslatedLibrariesForCqlIncludedLibraryNull() throws IOException {
    String cql = getData("/qdm_data_criteria_retrieval_test.cql");

    List<TranslatedLibrary> libraries =
        service.getTranslatedLibrariesForCql(
            cql, "token", CqlCompilerException.ErrorSeverity.Error);
    var matchingLib =
        libraries.stream()
            .filter(library -> library.getElmJson().contains("DataCriteriaRetrivalTest"))
            .findFirst();
    assertThat(matchingLib.get().getName(), is(equalTo("DataCriteriaRetrivalTest")));
  }

  @Test
  void testValidateRetrieveWithNoCodeOrValueSetFoundShouldRaiseErrors() {
    String cql = getData("/cql_retrieve.cql");
    RequestData requestData =
        RequestData.builder()
            .cqlData(cql)
            .errorSeverity(CqlCompilerException.ErrorSeverity.Info)
            .annotations(false)
            .locators(true)
            .disableListDemotion(true)
            .disableListPromotion(true)
            .disableMethodInvocation(true)
            .validateUnits(false)
            .resultTypes(true)
            .build();

    CqlTranslator cqlTranslator = new TranslationResource(true).buildTranslator(requestData);
    assertThat(cqlTranslator.getExceptions().size(), is(equalTo(0)));
    service.validateRetrieve(cqlTranslator);
    assertThat(cqlTranslator.getExceptions().size(), is(equalTo(3)));
    assertThat(
        cqlTranslator.getExceptions().get(0).getMessage(),
        is(equalTo("Retrieves must contain a code or value set filter")));
  }

  @Test
  void testValidateRetrieveIfNoRetrieveFoundShouldNotRaiseErrors() {
    RequestData requestData =
        RequestData.builder()
            .cqlData(
                "library NoRetrieve version '0.0.000'\n"
                    + "using QDM version '5.6'\n"
                    + "context Patient\n"
                    + "define \"Initial Population\": true")
            .errorSeverity(CqlCompilerException.ErrorSeverity.Info)
            .annotations(false)
            .locators(true)
            .disableListDemotion(true)
            .disableListPromotion(true)
            .disableMethodInvocation(true)
            .validateUnits(false)
            .resultTypes(true)
            .build();

    CqlTranslator cqlTranslator = new TranslationResource(true).buildTranslator(requestData);
    assertThat(cqlTranslator.getExceptions().size(), is(equalTo(0)));
    service.validateRetrieve(cqlTranslator);
    assertThat(cqlTranslator.getExceptions().size(), is(equalTo(0)));
  }

  @Test
  void testValidateRetrieveWithSimpleLocatorUsesStartAsEnd() {
    CqlTranslator cqlTranslator = mock(CqlTranslator.class);
    doReturn(buildLibraryWithSingleRetrieve("10:2", "Encounter")).when(cqlTranslator).toELM();
    doReturn(new ArrayList<CqlCompilerException>()).when(cqlTranslator).getExceptions();

    service.validateRetrieve(cqlTranslator);

    CqlCompilerException exception = cqlTranslator.getExceptions().get(0);
    TrackBack locator = exception.getLocator();
    assertThat(locator.getStartLine(), is(equalTo(10)));
    assertThat(locator.getStartChar(), is(equalTo(2)));
    assertThat(locator.getEndLine(), is(equalTo(10)));
    assertThat(locator.getEndChar(), is(equalTo(2)));
  }

  @Test
  void testValidateRetrieveWithInvalidLocatorFallsBackToZeros() {
    CqlTranslator cqlTranslator = mock(CqlTranslator.class);
    doReturn(buildLibraryWithSingleRetrieve("invalid-locator", "Encounter"))
        .when(cqlTranslator)
        .toELM();
    doReturn(new ArrayList<CqlCompilerException>()).when(cqlTranslator).getExceptions();

    service.validateRetrieve(cqlTranslator);

    CqlCompilerException exception = cqlTranslator.getExceptions().get(0);
    TrackBack locator = exception.getLocator();
    assertThat(locator.getStartLine(), is(equalTo(0)));
    assertThat(locator.getStartChar(), is(equalTo(0)));
    assertThat(locator.getEndLine(), is(equalTo(0)));
    assertThat(locator.getEndChar(), is(equalTo(0)));
  }

  @Test
  void testValidateRetrieveWithNullElmDoesNotAddErrors() {
    CqlTranslator translator = mock(CqlTranslator.class);
    doReturn(null).when(translator).toELM();
    doReturn(new ArrayList<CqlCompilerException>()).when(translator).getExceptions();

    service.validateRetrieve(translator);

    assertThat(translator.getExceptions().size(), is(equalTo(0)));
  }

  @Test
  void testGetTranslatedLibrariesForCqlFiltersMainLibraryFromIncludedMap() throws IOException {
    CqlConversionService conversionService = spy(service);
    String cql =
        "library Main version '1.0.0'\n"
            + "using QDM version '5.6'\n"
            + "context Patient\n"
            + "define \"Initial Population\": true";

    CqlTranslator translator =
        new TranslationResource(false)
            .buildTranslator(requestData.toBuilder().cqlData(cql).build());

    CompiledLibrary mainLibrary = translator.getTranslatedLibrary();
    Map<String, CompiledLibrary> includedLibraries = Map.of("Main", mainLibrary);
    CqlTooling.TranslationArtifacts artifacts =
        new CqlTooling.TranslationArtifacts(translator, includedLibraries, Map.of());

    doReturn(artifacts)
        .when(conversionService)
        .buildTranslationArtifacts(
            eq(cql),
            eq("token"),
            eq(cqlLibraryService),
            eq(CqlCompilerException.ErrorSeverity.Error));
    doReturn(Map.of()).when(conversionService).getIncludedLibrariesCql(any(), any());

    List<TranslatedLibrary> libraries =
        conversionService.getTranslatedLibrariesForCql(
            cql, "token", CqlCompilerException.ErrorSeverity.Error);

    assertThat(libraries.size(), is(equalTo(1)));
    assertThat(libraries.get(0).getName(), is(equalTo("Main")));
  }

  @Test
  void testValidateRetrieveSkipsPatientRetrieve() {
    CqlTranslator cqlTranslator = mock(CqlTranslator.class);
    doReturn(buildLibraryWithSingleRetrieve("10:2-10:15", "Patient")).when(cqlTranslator).toELM();
    doReturn(new ArrayList<CqlCompilerException>()).when(cqlTranslator).getExceptions();

    service.validateRetrieve(cqlTranslator);

    assertThat(cqlTranslator.getExceptions().size(), is(equalTo(0)));
  }

  @Test
  void testValidateRetrieveSkipsRetrieveWithBlankLocator() {
    CqlTranslator cqlTranslator = mock(CqlTranslator.class);
    doReturn(buildLibraryWithSingleRetrieve("", "Encounter")).when(cqlTranslator).toELM();
    doReturn(new ArrayList<CqlCompilerException>()).when(cqlTranslator).getExceptions();

    service.validateRetrieve(cqlTranslator);

    assertThat(cqlTranslator.getExceptions().size(), is(equalTo(0)));
  }

  @Test
  void testValidateRetrieveSkipsRetrieveWithCodeFilter() {
    CqlTranslator cqlTranslator = mock(CqlTranslator.class);
    Library library = buildLibraryWithSingleRetrieve("10:2-10:15", "Encounter");
    // Add a code filter to the retrieve so it should be skipped
    Retrieve retrieve = (Retrieve) library.getStatements().getDef().get(0).getExpression();
    retrieve.getCodeFilter().add(new CodeFilterElement());
    doReturn(library).when(cqlTranslator).toELM();
    doReturn(new ArrayList<CqlCompilerException>()).when(cqlTranslator).getExceptions();

    service.validateRetrieve(cqlTranslator);

    assertThat(cqlTranslator.getExceptions().size(), is(equalTo(0)));
  }

  @Test
  void testValidateRetrieveWithRangeLocatorParsesCorrectly() {
    CqlTranslator cqlTranslator = mock(CqlTranslator.class);
    doReturn(buildLibraryWithSingleRetrieve("5:3-7:10", "Encounter")).when(cqlTranslator).toELM();
    doReturn(new ArrayList<CqlCompilerException>()).when(cqlTranslator).getExceptions();

    service.validateRetrieve(cqlTranslator);

    CqlCompilerException exception = cqlTranslator.getExceptions().get(0);
    TrackBack locator = exception.getLocator();
    assertThat(locator.getStartLine(), is(equalTo(5)));
    assertThat(locator.getStartChar(), is(equalTo(3)));
    assertThat(locator.getEndLine(), is(equalTo(7)));
    assertThat(locator.getEndChar(), is(equalTo(10)));
  }

  @Test
  void testConvertToJsonWithXmlContentType() throws IOException {
    Library library = new Library();
    VersionedIdentifier identifier = new VersionedIdentifier();
    identifier.setId("TestLib");
    identifier.setVersion("1.0.0");
    library.setIdentifier(identifier);
    String result = service.convertToJson(library, LibraryContentType.XML);
    assertNotNull(result);
    assertTrue(result.contains("TestLib"));
  }

  @Test
  void testConvertToJsonWithJsonContentType() throws IOException {
    Library library = new Library();
    VersionedIdentifier identifier = new VersionedIdentifier();
    identifier.setId("TestLib");
    identifier.setVersion("1.0.0");
    library.setIdentifier(identifier);
    String result = service.convertToJson(library, LibraryContentType.JSON);
    assertNotNull(result);
    assertTrue(result.contains("TestLib"));
  }

  @Test
  void testIsMainLibraryReturnsFalseForNullIdentifierAndVersionMismatch() throws Exception {
    Method method =
        CqlConversionService.class.getDeclaredMethod(
            "isMainLibrary", CompiledLibrary.class, String.class, String.class);
    method.setAccessible(true);

    CompiledLibrary withoutIdentifier = new CompiledLibrary();
    boolean nullIdentifierResult =
        (boolean) method.invoke(service, withoutIdentifier, "Main", "1.0.0");
    assertFalse(nullIdentifierResult);

    CompiledLibrary withIdentifier = new CompiledLibrary();
    VersionedIdentifier identifier = new VersionedIdentifier();
    identifier.setId("Main");
    identifier.setVersion("1.0.0");
    withIdentifier.setIdentifier(identifier);

    boolean versionMismatchResult =
        (boolean) method.invoke(service, withIdentifier, "Main", "2.0.0");
    assertFalse(versionMismatchResult);
  }

  @Test
  void testIsPatientRetrieveReturnsFalseWhenRetrieveOrDatatypeMissing() throws Exception {
    Method method =
        CqlConversionService.class.getDeclaredMethod("isPatientRetrieve", Retrieve.class);
    method.setAccessible(true);

    boolean nullRetrieveResult = (boolean) method.invoke(service, new Object[] {null});
    assertFalse(nullRetrieveResult);

    Retrieve retrieveWithoutDatatype = new Retrieve();
    boolean nullDatatypeResult = (boolean) method.invoke(service, retrieveWithoutDatatype);
    assertFalse(nullDatatypeResult);
  }

  @Test
  void testParseLocatorReturnsZerosForBlankLocator() throws Exception {
    Method method = CqlConversionService.class.getDeclaredMethod("parseLocator", String.class);
    method.setAccessible(true);

    int[] parsed = (int[]) method.invoke(service, "  ");

    assertArrayEquals(new int[] {0, 0, 0, 0}, parsed);
  }

  private Library buildLibraryWithSingleRetrieve(String locator, String dataType) {
    Retrieve retrieve = new Retrieve();
    retrieve.setLocator(locator);
    retrieve.setDataType(new javax.xml.namespace.QName("urn:test", dataType));

    org.hl7.elm.r1.ExpressionDef expressionDef = new org.hl7.elm.r1.ExpressionDef();
    expressionDef.setName("RetrieveDef");
    expressionDef.setExpression(retrieve);

    Library.Statements statements = new Library.Statements();
    statements.getDef().add(expressionDef);

    VersionedIdentifier identifier = new VersionedIdentifier();
    identifier.setId("TestLibrary");
    identifier.setVersion("1.0.0");

    Library library = new Library();
    library.setIdentifier(identifier);
    library.setStatements(statements);
    return library;
  }
}
