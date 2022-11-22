package gov.cms.mat.cql_elm_translation.controllers;

import gov.cms.mat.cql_elm_translation.exceptions.CqlFormatException;
import org.junit.jupiter.api.Test;
import gov.cms.mat.cql_elm_translation.ResourceFileUtil;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CqlToolsControllerTest implements ResourceFileUtil {

  @InjectMocks private CqlToolsController cqlToolsController;

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

  private boolean inputMatchesOutput(String input, String output) {
    return input
        .replaceAll("[\\s\\u0000\\u00a0]", "")
        .equals(output.replaceAll("[\\s\\u0000\\u00a0]", ""));
  }
}
