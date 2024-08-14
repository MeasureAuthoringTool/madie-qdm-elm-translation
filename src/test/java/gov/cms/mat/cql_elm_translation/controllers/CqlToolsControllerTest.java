package gov.cms.mat.cql_elm_translation.controllers;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import gov.cms.madie.models.dto.TranslatedLibrary;

import gov.cms.mat.cql_elm_translation.dto.CqlLookupRequest;
import gov.cms.madie.cql_elm_translator.dto.CqlBuilderLookup;
import gov.cms.mat.cql_elm_translation.dto.CqlLookups;
import gov.cms.mat.cql_elm_translation.service.CqlParsingService;

import org.cqframework.cql.tools.formatter.CqlFormatterVisitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import gov.cms.madie.models.measure.Measure;
import gov.cms.mat.cql_elm_translation.ResourceFileUtil;
import gov.cms.madie.cql_elm_translator.dto.SourceDataCriteria;
import gov.cms.mat.cql_elm_translation.service.CqlConversionService;
import gov.cms.mat.cql_elm_translation.service.DataCriteriaService;
import gov.cms.madie.cql_elm_translator.utils.cql.parsing.model.CQLDefinition;

@ExtendWith(MockitoExtension.class)
class CqlToolsControllerTest implements ResourceFileUtil {

  @InjectMocks private CqlToolsController cqlToolsController;
  @Mock private DataCriteriaService dataCriteriaService;
  @Mock private CqlConversionService cqlConversionService;

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

  @Test
  void testGetCqlLookups() {
    when(cqlParsingService.getCqlLookups(any(), any(), any()))
        .thenReturn(CqlLookups.builder().library("Test").version("0.0.001").build());

    ResponseEntity<CqlLookups> result =
        cqlToolsController.getCqlLookups(new CqlLookupRequest(), "accessToken");
    CqlLookups cqlLookups = result.getBody();
    assertNotNull(cqlLookups);
    assertThat(cqlLookups.getLibrary(), is(equalTo("Test")));
    assertThat(cqlLookups.getVersion(), is(equalTo("0.0.001")));
  }

  @Test
  void testGetCqlBuilderLookups() {
    var p = CqlBuilderLookup.Lookup.builder().name("Parameter").logic("abc").build();
    var d = CqlBuilderLookup.Lookup.builder().name("Definition").logic("abcd").build();
    var f = CqlBuilderLookup.Lookup.builder().name("Function").logic("abcdef").build();
    when(cqlParsingService.getCqlBuilderLookups(anyString(), anyString()))
        .thenReturn(
            CqlBuilderLookup.builder()
                .parameters(Set.of(p))
                .definitions(Set.of(d))
                .functions(Set.of(f))
                .build());

    ResponseEntity<CqlBuilderLookup> result =
        cqlToolsController.getCqlBuilderLookups("CQL", "accessToken");
    CqlBuilderLookup cqlBuilderLookups = result.getBody();
    assertNotNull(cqlBuilderLookups);
    assertThat(cqlBuilderLookups.getParameters().size(), is(1));
    assertThat(cqlBuilderLookups.getDefinitions().size(), is(1));
    assertThat(cqlBuilderLookups.getFunctions().size(), is(1));
    assertThat(cqlBuilderLookups.getFluentFunctions(), is(nullValue()));
  }
}
