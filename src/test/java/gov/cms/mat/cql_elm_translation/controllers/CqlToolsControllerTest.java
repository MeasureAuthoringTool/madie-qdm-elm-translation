package gov.cms.mat.cql_elm_translation.controllers;

import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.Population;
import gov.cms.mat.cql_elm_translation.ResourceFileUtil;
import gov.cms.mat.cql_elm_translation.dto.SourceDataCriteria;
import gov.cms.mat.cql_elm_translation.exceptions.CqlFormatException;
import gov.cms.mat.cql_elm_translation.service.CqlConversionService;
import gov.cms.mat.cql_elm_translation.service.DataCriteriaService;
import gov.cms.mat.cql_elm_translation.service.HumanReadableService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CqlToolsControllerTest implements ResourceFileUtil {

  @InjectMocks private CqlToolsController cqlToolsController;
  @Mock private DataCriteriaService dataCriteriaService;
  @Mock private CqlConversionService cqlConversionService;

  @Mock private HumanReadableService humanReadableService;

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
    when(cqlConversionService.getElmForCql(anyString(), anyString()))
        .thenReturn(List.of("Elm 1", "Elm 2"));
    var result = cqlToolsController.getLibraryElms("test cql", "john");
    List<String> elms = result.getBody();
    assertThat(elms.size(), is(equalTo(2)));
    assertThat(elms.get(0), is(equalTo("Elm 1")));
    assertThat(elms.get(1), is(equalTo("Elm 2")));
  }

  @Test
  void testGenerateHumanReadable() {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("test.user");
    when(humanReadableService.generate(any())).thenReturn("test human Readable");
    var result = cqlToolsController.generateHumanReadable(new Measure(), principal);
    assertEquals(result.getBody(), "test human Readable");
    assertEquals(result.getStatusCode(), HttpStatus.OK);
  }

  @Test
  void testGetRelevantElements() {
    String cql = getData("/qdm_data_criteria_retrieval_test.cql");
    Measure measure = Measure.builder().cql(cql).build();
    String token = "john";
    var sdc = SourceDataCriteria.builder().oid("1.2.3").description("EP: Test").title("EP").build();
    when(dataCriteriaService.getRelevantElements(any(Measure.class), anyString()))
        .thenReturn(List.of(sdc));
    var result = cqlToolsController.getRelevantElements(measure, token);
    SourceDataCriteria sourceDataCriteria = result.getBody().get(0);
    assertThat(sourceDataCriteria.getOid(), is(equalTo(sdc.getOid())));
    assertThat(sourceDataCriteria.getDescription(), is(equalTo(sdc.getDescription())));
    assertThat(sourceDataCriteria.getTitle(), is(equalTo(sdc.getTitle())));
  }

  private boolean inputMatchesOutput(String input, String output) {
    return input
        .replaceAll("[\\s\\u0000\\u00a0]", "")
        .equals(output.replaceAll("[\\s\\u0000\\u00a0]", ""));
  }
}
