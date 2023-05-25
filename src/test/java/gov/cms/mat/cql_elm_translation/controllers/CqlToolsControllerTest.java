package gov.cms.mat.cql_elm_translation.controllers;

import gov.cms.mat.cql_elm_translation.ResourceFileUtil;
import gov.cms.mat.cql_elm_translation.dto.SourceDataCriteria;
import gov.cms.mat.cql_elm_translation.exceptions.CqlFormatException;
import gov.cms.mat.cql_elm_translation.service.DataCriteriaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;
import java.util.List;
import java.util.Objects;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CqlToolsControllerTest implements ResourceFileUtil {

  @InjectMocks private CqlToolsController cqlToolsController;
  @Mock private DataCriteriaService dataCriteriaService;

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
    var sdc =
        SourceDataCriteria.builder()
            .codeListId("1.2.3")
            .description("EP: Test")
            .qdmTitle("EP")
            .build();
    when(dataCriteriaService.getSourceDataCriteria(anyString(), anyString()))
        .thenReturn(List.of(sdc));
    var result = cqlToolsController.getSourceDataCriteria(cql, token);
    SourceDataCriteria sourceDataCriteria = result.getBody().get(0);
    assertThat(sourceDataCriteria.getCodeListId(), is(equalTo(sdc.getCodeListId())));
    assertThat(sourceDataCriteria.getDescription(), is(equalTo(sdc.getDescription())));
    assertThat(sourceDataCriteria.getQdmTitle(), is(equalTo(sdc.getQdmTitle())));
  }

  private boolean inputMatchesOutput(String input, String output) {
    return input
        .replaceAll("[\\s\\u0000\\u00a0]", "")
        .equals(output.replaceAll("[\\s\\u0000\\u00a0]", ""));
  }
}
