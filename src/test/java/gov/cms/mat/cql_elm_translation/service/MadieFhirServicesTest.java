package gov.cms.mat.cql_elm_translation.service;

import gov.cms.madie.models.measure.Measure;
import gov.cms.mat.cql_elm_translation.exceptions.FhirBundleGenerationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MadieFhirServicesTest {

  @Mock
  private RestTemplate restTemplate;

  @InjectMocks MadieFhirServices madieFhirServices;

  private final HttpHeaders httpHeaders = new HttpHeaders();

  private URI libraryUri;

  private URI measureUri;

  private final String cqlLibraryName = "cqlLibraryName";

  private final String cqlLibraryVersion = "1.0.000";

  private final String accessToken = "okta-access-token";

  private Measure measure;

  @BeforeEach
  void setUp() throws URISyntaxException {
    ReflectionTestUtils.setField(madieFhirServices, "madieFhirService", "https://localhost:9090/api");
    ReflectionTestUtils.setField(madieFhirServices, "librariesUri", "/fhir/libraries");
    ReflectionTestUtils.setField(madieFhirServices, "measuresUri", "/fhir/measures");

    httpHeaders.add("Authorization", "okta-access-token");
    libraryUri = new URI("https://localhost:9090/api/fhir/libraries/cql?name=" + cqlLibraryName + "&version=" + cqlLibraryVersion);
    measureUri = new URI("https://localhost:9090/api/fhir/measures/bundles");
    measure = Measure.builder().id("test_measureId").build();
  }

  @Test
  void getHapiFhirCql() {
    when(restTemplate.exchange(libraryUri, HttpMethod.GET, new HttpEntity<>(httpHeaders), String.class))
        .thenReturn(new ResponseEntity<>("Response Cql String", HttpStatus.OK));
    String responseBody = madieFhirServices.getHapiFhirCql(cqlLibraryName, cqlLibraryVersion, accessToken);
    assertEquals(responseBody, "Response Cql String");
  }

  @Test
  void getHapiFhirCqlReturnsNull() {
    when(restTemplate.exchange(libraryUri, HttpMethod.GET, new HttpEntity<>(httpHeaders), String.class))
        .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));
    String responseBody = madieFhirServices.getHapiFhirCql(cqlLibraryName, cqlLibraryVersion, accessToken);
    assertNull(responseBody);
  }

  @Test
  void getHapiFhirCqlReturnsNullWhenNotFound() {
    when(restTemplate.exchange(libraryUri, HttpMethod.GET, new HttpEntity<>(httpHeaders), String.class))
        .thenReturn(new ResponseEntity<>(null, HttpStatus.NOT_FOUND));
    String responseBody = madieFhirServices.getHapiFhirCql(cqlLibraryName, cqlLibraryVersion, accessToken);
    assertNull(responseBody);
  }

  @Test
  void getFhirMeasureBundle() {
    when(restTemplate.exchange(measureUri, HttpMethod.PUT, new HttpEntity<>(measure, httpHeaders), String.class))
        .thenReturn(new ResponseEntity<>("Measure Bundle", HttpStatus.OK));
    String response = madieFhirServices.getFhirMeasureBundle(measure, accessToken);
    assertEquals("Measure Bundle", response);
  }

  @Test
  void getFhirMeasureBundleThrowsException() {
    when(restTemplate.exchange(measureUri, HttpMethod.PUT, new HttpEntity<>(measure, httpHeaders), String.class))
        .thenReturn(new ResponseEntity<>(null, HttpStatus.NOT_FOUND));
    assertThrows(
        FhirBundleGenerationException.class,
        () -> madieFhirServices.getFhirMeasureBundle(measure, accessToken));
  }
}