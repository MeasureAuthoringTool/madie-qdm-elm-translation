package gov.cms.mat.cql_elm_translation.controllers;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.*;

import gov.cms.madie.cql_elm_translator.dto.CqlBuilderLookup;
import gov.cms.mat.cql_elm_translation.dto.RelevantElement;
import org.cqframework.cql.cql2elm.CqlCompilerException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import gov.cms.madie.models.measure.Measure;
import gov.cms.mat.cql_elm_translation.ResourceFileUtil;
import gov.cms.mat.cql_elm_translation.service.CqlParsingService;
import gov.cms.mat.cql_elm_translation.service.DataCriteriaService;
import gov.cms.madie.cql_elm_translator.utils.cql.parsing.model.CQLDefinition;

@ExtendWith(MockitoExtension.class)
class CqlToolsControllerTest implements ResourceFileUtil {

  @InjectMocks private CqlToolsController cqlToolsController;
  @Mock private DataCriteriaService dataCriteriaService;
  @Mock private CqlParsingService cqlParsingService;

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
  void testGetRelevantElements() {
    String cql = getData("/qdm_data_criteria_retrieval_test.cql");
    Measure measure = Measure.builder().cql(cql).build();
    String token = "john";
    var relevantElement1 =
        RelevantElement.builder()
            .profile("http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-patient")
            .type("Patient")
            .build();

    var relevantElement2 =
        RelevantElement.builder()
            .profile(
                "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-condition-encounter-diagnosis")
            .type("Condition")
            .build();
    when(dataCriteriaService.getRelevantElements(any(Measure.class), anyString()))
        .thenReturn(Set.of(relevantElement1, relevantElement2));
    var result = cqlToolsController.getRelevantElements(measure, token);
    assertThat(result.getBody(), is(notNullValue()));
    Set<RelevantElement> relevantElements = result.getBody();
    assertThat(relevantElements.size(), is(equalTo(2)));

    // Convert to list for reliable ordering or check that both elements exist
    assertThat(relevantElements, hasItem(relevantElement1));
    assertThat(relevantElements, hasItem(relevantElement2));
  }

  @Test
  void testGetDefinitionCallstack() {
    Map<String, Set<CQLDefinition>> definitionCallstacks = new HashMap<>();
    definitionCallstacks.put("test", allDefinitions);
    when(cqlParsingService.getDefinitionCallstacks(
            anyString(), anyString(), any(CqlCompilerException.ErrorSeverity.class)))
        .thenReturn(definitionCallstacks);

    ResponseEntity<Map<String, Set<CQLDefinition>>> result =
        cqlToolsController.getDefinitionCallstack(
            "test cql", "accessToken", CqlCompilerException.ErrorSeverity.Error);
    Set<CQLDefinition> defintions = result.getBody().get("test");
    assertThat(defintions.size(), is(equalTo(1)));
  }

  @Test
  void testGetCqlBuilderLookups() {
    var p = CqlBuilderLookup.Lookup.builder().name("Parameter").logic("abc").build();
    var d = CqlBuilderLookup.Lookup.builder().name("Definition").logic("abcd").build();
    var f = CqlBuilderLookup.Lookup.builder().name("Function").logic("abcdef").build();
    when(cqlParsingService.getCqlBuilderLookups(
            anyString(), anyString(), any(CqlCompilerException.ErrorSeverity.class)))
        .thenReturn(
            CqlBuilderLookup.builder()
                .parameters(Set.of(p))
                .definitions(Set.of(d))
                .functions(Set.of(f))
                .build());

    ResponseEntity<CqlBuilderLookup> result =
        cqlToolsController.getCqlBuilderLookups(
            "CQL", "accessToken", CqlCompilerException.ErrorSeverity.Error);
    CqlBuilderLookup cqlBuilderLookups = result.getBody();
    assertNotNull(cqlBuilderLookups);
    assertThat(cqlBuilderLookups.getParameters().size(), is(1));
    assertThat(cqlBuilderLookups.getDefinitions().size(), is(1));
    assertThat(cqlBuilderLookups.getFunctions().size(), is(1));
    assertThat(cqlBuilderLookups.getFluentFunctions(), is(nullValue()));
  }
}
