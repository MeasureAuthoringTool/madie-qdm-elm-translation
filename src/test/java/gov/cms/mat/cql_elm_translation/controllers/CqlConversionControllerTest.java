package gov.cms.mat.cql_elm_translation.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import java.io.UncheckedIOException;

import gov.cms.mat.cql_elm_translation.service.CqlLibraryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.cms.mat.cql.dto.CqlConversionPayload;
import gov.cms.mat.cql_elm_translation.ResourceFileUtil;
import gov.cms.mat.cql_elm_translation.data.RequestData;
import gov.cms.mat.cql_elm_translation.service.CqlConversionService;

@ExtendWith(MockitoExtension.class)
class CqlConversionControllerTest implements ResourceFileUtil {
  private static final String translatorOptionsTag = "\"translatorOptions\"";

  @Mock private CqlConversionService cqlConversionService;
  @Mock private CqlLibraryService cqlLibraryService;
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

  @Test
  void translatorOptionsRemoverNoAnnotations() {

    String json = getData("/fhir4_std_lib_no_annotations.json");

    assertFalse(json.contains(translatorOptionsTag));

    CqlConversionController.TranslatorOptionsRemover translatorOptionsRemover =
        new CqlConversionController.TranslatorOptionsRemover(json);

    String cleaned = translatorOptionsRemover.clean();

    assertFalse(json.contains(translatorOptionsTag));
    assertEquals(json, cleaned);
  }

  @Test
  void translatorOptionsRemoverEmptyAnnotations()
      throws JsonMappingException, JsonProcessingException {

    String json = getData("/fhir4_std_lib_empty_array_annotations.json");

    assertFalse(json.contains(translatorOptionsTag));

    CqlConversionController.TranslatorOptionsRemover translatorOptionsRemover =
        new CqlConversionController.TranslatorOptionsRemover(json);

    String cleaned = translatorOptionsRemover.clean();

    assertFalse(cleaned.contains(translatorOptionsTag));
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode rootNode = objectMapper.readTree(cleaned);
    JsonNode libraryNode = rootNode.get("library");
    JsonNode annotationNode = libraryNode.get("annotation");
    assertNull(annotationNode);
  }

  @Test
  void translatorOptionsRemoverBadJson() {

    String json = "{this isn't json/>";
    CqlConversionController.TranslatorOptionsRemover translatorOptionsRemover =
        new CqlConversionController.TranslatorOptionsRemover(json);

    assertThrows(UncheckedIOException.class, () -> translatorOptionsRemover.clean());
  }
}
