package gov.cms.mat.cql_elm_translation.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;

import org.cqframework.cql.cql2elm.CqlTranslator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import gov.cms.mat.cql.dto.CqlConversionPayload;
import gov.cms.mat.cql_elm_translation.ResourceFileUtil;
import gov.cms.mat.cql_elm_translation.data.RequestData;
import gov.cms.mat.cql_elm_translation.service.CqlConversionService;

@ExtendWith(MockitoExtension.class)
public class MatMeasureControllerTest implements ResourceFileUtil {

  @Mock private CqlConversionService cqlConversionService;
  @Mock private CqlTranslator cqlTranslator;
  @Mock private HttpServletRequest request;
  @InjectMocks private MatMeasureController matMeasureController;

  @Test
  public void testCqlToElmJsonForMatTransferredMeasureSuccess() {
    String cqlData = getData("/cv_populations.cql");
    String result = getData("/cv_populations.json");
    CqlConversionPayload payload = CqlConversionPayload.builder().json(result).build();
    when(cqlConversionService.processCqlDataWithErrors(any(RequestData.class))).thenReturn(payload);

    when(request.getHeader("api-key")).thenReturn("key4api");
    when(request.getHeader("harp-id")).thenReturn("test");
    CqlConversionPayload cqlConversionPayload =
        matMeasureController.cqlToElmJsonForMatTransferredMeasure(
            cqlData, null, true, true, true, true, true, true, true, true, request, "key4api");

    assertEquals(result, cqlConversionPayload.getJson());
    verify(cqlConversionService).processCqlDataWithErrors(any());
  }

  @Test
  public void testCqlToElmJsonForMatTransferredMeasureFail() {
    String cqlData = getData("/cv_populations.cql");
    String result = getData("/cv_populations.json");

    when(request.getHeader("api-key")).thenReturn("key4api");
    when(request.getHeader("harp-id")).thenReturn("test2");
    CqlConversionPayload cqlConversionPayload =
        matMeasureController.cqlToElmJsonForMatTransferredMeasure(
            cqlData, null, true, true, true, true, true, true, true, true, request, "test3");

    assertNotEquals(result, cqlConversionPayload.getJson());
    assertTrue(cqlConversionPayload.getJson().contains("UNAUTHORIZED"));
  }
}
