package gov.cms.mat.cql_elm_translation.controllers;

import gov.cms.mat.cql.dto.CqlConversionPayload;
import gov.cms.mat.cql_elm_translation.ResourceFileUtil;
import gov.cms.mat.cql_elm_translation.data.RequestData;
import gov.cms.mat.cql_elm_translation.service.CqlConversionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class CqlConversionControllerTest implements ResourceFileUtil {
  private static final String translatorOptionsTag = "\"translatorOptions\"";

  @Mock private CqlConversionService cqlConversionService;

  @InjectMocks private CqlConversionController cqlConversionController;

  @Test
  void cqlToElmJson() {
    String cqlData = getData("/cv_populations.cql");
    String result = getData("/cv_populations.json");
    CqlConversionPayload payload = CqlConversionPayload.builder().json(result).build();
    Mockito.when(cqlConversionService.processCqlDataWithErrors(any(RequestData.class)))
        .thenReturn(payload);

    CqlConversionPayload cqlConversionPayload =
        cqlConversionController.cqlToElmJson(
            cqlData, null, true, true, true, true, true, true, true, true, "test");

    assertEquals(result, cqlConversionPayload.getJson());
    Mockito.verify(cqlConversionService).processCqlDataWithErrors(any());
  }

  @Test
  void translatorOptionsRemoverNoErrors() {
    String json = getData("/fhir4_std_lib_no_errors.json");

    assertTrue(json.contains(translatorOptionsTag));

    CqlConversionController.TranslatorOptionsRemover translatorOptionsRemover =
        new CqlConversionController.TranslatorOptionsRemover(json);

    String cleaned = translatorOptionsRemover.clean();

    assertFalse(cleaned.contains(translatorOptionsTag));
  }

  @Test
  void translatorOptionsRemoverErrors() {

    String json = getData("/fhir4_std_lib_errors.json");

    assertTrue(json.contains(translatorOptionsTag));

    CqlConversionController.TranslatorOptionsRemover translatorOptionsRemover =
        new CqlConversionController.TranslatorOptionsRemover(json);

    String cleaned = translatorOptionsRemover.clean();

    assertFalse(cleaned.contains(translatorOptionsTag));
  }
}
