package gov.cms.mat.cql_elm_translation.controllers;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import gov.cms.madie.models.dto.TranslatedLibrary;

import org.cqframework.cql.tools.formatter.CqlFormatterVisitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import gov.cms.madie.models.measure.Measure;
import gov.cms.mat.cql_elm_translation.ResourceFileUtil;
import gov.cms.mat.cql_elm_translation.dto.SourceDataCriteria;
import gov.cms.mat.cql_elm_translation.exceptions.CqlFormatException;
import gov.cms.mat.cql_elm_translation.service.CqlConversionService;
import gov.cms.mat.cql_elm_translation.service.CqlParsingService;
import gov.cms.mat.cql_elm_translation.service.DataCriteriaService;
import gov.cms.mat.cql_elm_translation.service.HumanReadableService;
import gov.cms.mat.cql_elm_translation.utils.cql.parsing.model.CQLDefinition;

@ExtendWith(MockitoExtension.class)
class CqlToolsControllerTest implements ResourceFileUtil {

  @InjectMocks private CqlToolsController cqlToolsController;
  @Mock private DataCriteriaService dataCriteriaService;
  @Mock private CqlConversionService cqlConversionService;

  @Mock private HumanReadableService humanReadableService;
  @Mock private CqlParsingService cqlParsingService;
  @Mock private CqlFormatterVisitor cqlFormatterVisitor;

  private Set<CQLDefinition> allDefinitions;

  @BeforeEach
  void setUp() {
    CQLDefinition definition1 =
        CQLDefinition.builder()
            .id("Initial Population")
            .definitionName("Initial Population")
            .parentLibrary(null)
            .definitionLogic(
                "define \"Initial Population\":\n  \"Encounter with Opioid Administration Outside of Operating Room\"")
            .build();
    allDefinitions = new HashSet<>(Arrays.asList(definition1));
  }

  @Test
  void formatCql() {
    String cqlData = getData("/cv_populations.cql");

    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user");

    var result = cqlToolsController.formatCql(cqlData, principal);
    assertTrue(inputMatchesOutput(cqlData, Objects.requireNonNull(result.getBody())));
  }

  @Test
  void formatCqlWithMissingModel() {
    String cqlData = getData("/missing-model.cql");

    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user");

    var result = cqlToolsController.formatCql(cqlData, principal);
    assertTrue(inputMatchesOutput(cqlData, Objects.requireNonNull(result.getBody())));
  }

  @Test
  void formatCqlWithInvalidSyntax() {
    String cqlData = getData("/invalid_syntax.cql");

    Principal principal = mock(Principal.class);
    assertThrows(CqlFormatException.class, () -> cqlToolsController.formatCql(cqlData, principal));
  }

  @Test
  void formatCqlWithNoData() {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user");

    var result = cqlToolsController.formatCql("", principal);
    assertTrue(inputMatchesOutput("", Objects.requireNonNull(result.getBody())));
  }

  @Test
  void testGetSourceDataCriteria() {
    String cql = getData("/qdm_data_criteria_retrieval_test.cql");
    String token = "john";
    var sdc = SourceDataCriteria.builder().oid("1.2.3").description("EP: Test").title("EP").build();
    when(dataCriteriaService.getSourceDataCriteria(anyString(), anyString()))
        .thenReturn(List.of(sdc));
    var result = cqlToolsController.getSourceDataCriteria(cql, token);
    SourceDataCriteria sourceDataCriteria = result.getBody().get(0);
    assertThat(sourceDataCriteria.getOid(), is(equalTo(sdc.getOid())));
    assertThat(sourceDataCriteria.getDescription(), is(equalTo(sdc.getDescription())));
    assertThat(sourceDataCriteria.getTitle(), is(equalTo(sdc.getTitle())));
  }

  @Test
  void testGetLibraryElms() throws IOException {
    TranslatedLibrary translatedLibrary1 =
        TranslatedLibrary.builder().cql("cql 1").elmJson("elm json 1").elmXml("elm xml 1").build();
    TranslatedLibrary translatedLibrary2 =
        TranslatedLibrary.builder().cql("cql 2").elmJson("elm json 2").elmXml("elm xml 2").build();

    when(cqlConversionService.getTranslatedLibrariesForCql(anyString(), anyString()))
        .thenReturn(List.of(translatedLibrary1, translatedLibrary2));
    var result = cqlToolsController.getLibraryElms("test cql", "john");
    List<TranslatedLibrary> libraries = result.getBody();
    assertThat(libraries.size(), is(equalTo(2)));
    assertThat(libraries.get(0).getCql(), is(equalTo(translatedLibrary1.getCql())));
    assertThat(libraries.get(0).getElmJson(), is(equalTo(translatedLibrary1.getElmJson())));
    assertThat(libraries.get(0).getElmXml(), is(equalTo(translatedLibrary1.getElmXml())));
    assertThat(libraries.get(1).getCql(), is(equalTo(translatedLibrary2.getCql())));
    assertThat(libraries.get(1).getElmJson(), is(equalTo(translatedLibrary2.getElmJson())));
    assertThat(libraries.get(1).getElmXml(), is(equalTo(translatedLibrary2.getElmXml())));
  }

  @Test
  void testGetLibraryElmsThrowsException() throws IOException {
    when(cqlConversionService.getTranslatedLibrariesForCql(anyString(), anyString()))
        .thenThrow(IOException.class);
    var result = cqlToolsController.getLibraryElms("test cql", "john");
    List<TranslatedLibrary> libraries = result.getBody();
    assertNull(libraries);
  }

  @Test
  void testGenerateHumanReadable() {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user");
    when(humanReadableService.generate(any(), anyString())).thenReturn("test human Readable");
    var result = cqlToolsController.generateHumanReadable(new Measure(), principal, "accessToken");
    assertEquals(result.getBody(), "test human Readable");
    assertEquals(result.getStatusCode(), HttpStatus.OK);
  }

  @Test
  void testGetRelevantElements() {
    String cql = getData("/qdm_data_criteria_retrieval_test.cql");
    Measure measure = Measure.builder().cql(cql).build();
    String token = "john";
    var sdc = SourceDataCriteria.builder().oid("1.2.3").description("EP: Test").title("EP").build();
    TreeSet<SourceDataCriteria> sdcSet = new TreeSet<SourceDataCriteria>();
    sdcSet.add(sdc);
    when(dataCriteriaService.getRelevantElements(any(Measure.class), anyString()))
        .thenReturn(sdcSet);
    var result = cqlToolsController.getRelevantElements(measure, token);
    SourceDataCriteria sourceDataCriteria =
        ((TreeSet<SourceDataCriteria>) result.getBody()).first();
    assertThat(sourceDataCriteria.getOid(), is(equalTo(sdc.getOid())));
    assertThat(sourceDataCriteria.getDescription(), is(equalTo(sdc.getDescription())));
    assertThat(sourceDataCriteria.getTitle(), is(equalTo(sdc.getTitle())));
  }

  private boolean inputMatchesOutput(String input, String output) {
    return input
        .replaceAll("[\\s\\u0000\\u00a0]", "")
        .equals(output.replaceAll("[\\s\\u0000\\u00a0]", ""));
  }

  @Test
  void testGetAllDefinitions() {

    when(cqlParsingService.getAllDefinitions(any(), anyString())).thenReturn(allDefinitions);
    ResponseEntity<Set<CQLDefinition>> result =
        cqlToolsController.getAllDefinitions("test cql", "accessToken");
    Set<CQLDefinition> defintions = result.getBody();
    assertThat(defintions.size(), is(equalTo(1)));
  }

  @Test
  void testGetDefinitionCallstack() {
    Map<String, Set<CQLDefinition>> definitionCallstacks = new HashMap<>();
    definitionCallstacks.put("test", allDefinitions);
    when(cqlParsingService.getDefinitionCallstacks(anyString(), anyString()))
        .thenReturn(definitionCallstacks);

    ResponseEntity<Map<String, Set<CQLDefinition>>> result =
        cqlToolsController.getDefinitionCallstack("test cql", "accessToken");
    Set<CQLDefinition> defintions = result.getBody().get("test");
    assertThat(defintions.size(), is(equalTo(1)));
  }
}
