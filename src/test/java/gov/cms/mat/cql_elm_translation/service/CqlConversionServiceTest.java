package gov.cms.mat.cql_elm_translation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.mat.cql.dto.CqlConversionPayload;
import gov.cms.mat.cql_elm_translation.ResourceFileUtil;
import gov.cms.mat.cql_elm_translation.data.RequestData;
import gov.cms.mat.cql_elm_translation.dto.TranslatedLibrary;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
class CqlConversionServiceTest implements ResourceFileUtil {

  @Mock private CqlLibraryService cqlLibraryService;
  @InjectMocks private CqlConversionService service;

  private static RequestData requestData;

  @BeforeAll
  static void setUp() {
    requestData =
        RequestData.builder()
            .showWarnings(true)
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
    CqlConversionPayload payload = service.processCqlDataWithErrors(data);
    assertNotNull(payload);
    String resultJson = payload.getJson();
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      JsonNode jsonNode = objectMapper.readTree(resultJson);
      assertNotNull(jsonNode);
      JsonNode libraryNode = jsonNode.at("/errorExceptions");
      assertNotNull(libraryNode);
      assertTrue(libraryNode.isMissingNode());
    } catch (JsonProcessingException e) {
      fail(e.getMessage());
    }
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
    CqlConversionPayload payload = service.processCqlDataWithErrors(data);
    assertNotNull(payload);
    String resultJson = payload.getJson();
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      JsonNode jsonNode = objectMapper.readTree(resultJson);
      assertNotNull(jsonNode);
      JsonNode libraryNode = jsonNode.at("/errorExceptions");
      assertNotNull(libraryNode);
      assertFalse(libraryNode.isMissingNode());
    } catch (JsonProcessingException e) {
      fail(e.getMessage());
    }
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
    CqlConversionPayload payload = service.processCqlDataWithErrors(data);
    assertNotNull(payload);
    String resultJson = payload.getJson();
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      JsonNode jsonNode = objectMapper.readTree(resultJson);
      assertNotNull(jsonNode);
      JsonNode libraryNode = jsonNode.at("/errorExceptions");
      assertNotNull(libraryNode);
      assertTrue(libraryNode.isMissingNode());
    } catch (JsonProcessingException e) {
      fail(e.getMessage());
    }
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
    CqlConversionPayload payload = service.processCqlDataWithErrors(data);
    assertNotNull(payload);
    String resultJson = payload.getJson();
    ObjectMapper objectMapper = new ObjectMapper();
    try {
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
    } catch (JsonProcessingException e) {
      fail(e.getMessage());
    }
  }

  @Test
  void testGetElmForCql() throws IOException {
    String cql = getData("/qdm_data_criteria_retrieval_test.cql");
    List<TranslatedLibrary> libraries = service.getElmForCql(cql, "token");
    AtomicBoolean foundAMatch = new AtomicBoolean();
    libraries.forEach(
        library -> {
          foundAMatch.set(library.getElmJson().contains("DataCriteriaRetrivalTest"));
        });
    assertThat(foundAMatch.get(), is(true));
  }

  @Test
  void testGetElmForBlankCql() throws IOException {
    List<TranslatedLibrary> elms = service.getElmForCql(null, "token");
    assertThat(elms.size(), is(equalTo(0)));
  }
}
