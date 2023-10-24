package gov.cms.mat.cql_elm_translation.service;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import gov.cms.mat.cql.elements.UsingProperties;
import gov.cms.mat.cql_elm_translation.ResourceFileUtil;
import gov.cms.mat.cql_elm_translation.cql_translator.MadieLibrarySourceProvider;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.mat.cql.dto.CqlConversionPayload;
import gov.cms.mat.cql_elm_translation.data.RequestData;

@SpringBootTest
class CqlConversionServiceTest implements ResourceFileUtil {

  @Autowired private CqlConversionService service;

  @Mock RequestData requestData;

  private final String LIBRARY_CQL =
      "library QICoreCommon version '1.3.000'\n" + "using QICore version '4.1.1'";

  @AfterEach
  void tearDown() {
    service.setUpLibrarySourceProvider("", "");
  }

  @Test
  void testSetUpLibrarySourceProvider() {
    service.setUpLibrarySourceProvider(LIBRARY_CQL, "ACCESS_TOKEN");
    assertThat(MadieLibrarySourceProvider.getAccessToken(), is(equalTo("ACCESS_TOKEN")));
    UsingProperties usingProperties = MadieLibrarySourceProvider.getUsingProperties();
    assertThat(usingProperties, is(notNullValue()));
    assertThat(usingProperties.getLibraryType(), is(equalTo("QICore")));
    assertThat(usingProperties.getVersion(), is(equalTo("4.1.1")));
  }

  @Test
  void testProcessCqlDataWithErrors() {
    String cqlData = StringUtils.EMPTY;
    File inputXmlFile = new File(this.getClass().getResource("/fhir.cql").getFile());

    try {
      cqlData = new String(Files.readAllBytes(inputXmlFile.toPath()));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    when(requestData.getCqlData()).thenReturn(cqlData);

    when(requestData.getCqlDataInputStream())
        .thenReturn(new ByteArrayInputStream(cqlData.getBytes()));
    MultivaluedMap<String, String> map = new MultivaluedHashMap<String, String>();
    List<String> trueList = new ArrayList<String>(Arrays.asList("true"));
    map.put("disable-method-invocation", trueList);
    map.put("validate-units", trueList);

    when(requestData.createMap()).thenReturn(map);
    CqlConversionPayload payload = service.processCqlDataWithErrors(requestData);
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
    String cqlData = StringUtils.EMPTY;
    File inputXmlFile = new File(this.getClass().getResource("/non_supported_model.cql").getFile());

    try {
      cqlData = new String(Files.readAllBytes(inputXmlFile.toPath()));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    when(requestData.getCqlData()).thenReturn(cqlData);

    when(requestData.getCqlDataInputStream())
        .thenReturn(new ByteArrayInputStream(cqlData.getBytes()));
    MultivaluedMap<String, String> map = new MultivaluedHashMap<String, String>();
    List<String> trueList = new ArrayList<String>(Arrays.asList("true"));
    map.put("disable-method-invocation", trueList);
    map.put("validate-units", trueList);

    when(requestData.createMap()).thenReturn(map);
    CqlConversionPayload payload = service.processCqlDataWithErrors(requestData);
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
    String cqlData = StringUtils.EMPTY;
    File inputXmlFile = new File(this.getClass().getResource("/qicore.cql").getFile());

    try {
      cqlData = new String(Files.readAllBytes(inputXmlFile.toPath()));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    when(requestData.getCqlData()).thenReturn(cqlData);

    when(requestData.getCqlDataInputStream())
        .thenReturn(new ByteArrayInputStream(cqlData.getBytes()));
    MultivaluedMap<String, String> map = new MultivaluedHashMap<String, String>();
    List<String> trueList = new ArrayList<String>(Arrays.asList("true"));
    map.put("disable-method-invocation", trueList);
    map.put("validate-units", trueList);

    when(requestData.createMap()).thenReturn(map);
    CqlConversionPayload payload = service.processCqlDataWithErrors(requestData);
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
    String cqlData = StringUtils.EMPTY;
    File inputXmlFile = new File(this.getClass().getResource("/missing-model.cql").getFile());
    try {
      cqlData = new String(Files.readAllBytes(inputXmlFile.toPath()));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    when(requestData.getCqlData()).thenReturn(cqlData);

    when(requestData.getCqlDataInputStream())
        .thenReturn(new ByteArrayInputStream(cqlData.getBytes()));
    MultivaluedMap<String, String> map = new MultivaluedHashMap<String, String>();
    List<String> trueList = new ArrayList<String>(Arrays.asList("true"));
    map.put("disable-method-invocation", trueList);
    map.put("validate-units", trueList);

    when(requestData.createMap()).thenReturn(map);
    CqlConversionPayload payload = service.processCqlDataWithErrors(requestData);
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
          node -> {
            foundMessage.set(
                Boolean.valueOf(
                    foundMessage.get()
                        || node.get("message")
                            .asText()
                            .contains("Model Type and version are required")));
          });
      assertTrue(foundMessage.get());
    } catch (JsonProcessingException e) {
      fail(e.getMessage());
    }
  }

  @Test
  void testGetElmForCql() throws IOException {
    String cql = getData("/qdm_data_criteria_retrieval_test.cql");
    List<String> elms = service.getElmForCql(cql, "token");
    AtomicBoolean foundAMatch = new AtomicBoolean();
    elms.forEach(
        elm -> {
          foundAMatch.set(elm.contains("DataCriteriaRetrivalTest"));
        });
    assertThat(foundAMatch.get(), is(true));
  }

  @Test
  void testGetElmForBlankCql() throws IOException {
    List<String> elms = service.getElmForCql(null, "token");
    assertThat(elms.size(), is(equalTo(0)));
  }
}
