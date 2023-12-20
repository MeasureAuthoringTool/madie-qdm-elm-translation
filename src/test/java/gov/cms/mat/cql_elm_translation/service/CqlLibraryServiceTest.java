package gov.cms.mat.cql_elm_translation.service;

import gov.cms.mat.cql.elements.UsingProperties;
import gov.cms.mat.cql_elm_translation.cql_translator.MadieLibrarySourceProvider;
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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

@ExtendWith(MockitoExtension.class)
class CqlLibraryServiceTest {

  @Mock private RestTemplate restTemplate;

  @InjectMocks CqlLibraryService cqlLibraryService;

  private final HttpHeaders httpHeaders = new HttpHeaders();

  private URI libraryUri;

  private final String cqlLibraryName = "cqlLibraryName";

  private final String cqlLibraryVersion = "1.0.000";

  private final String accessToken = "okta-access-token";

  @BeforeEach
  void setUp() throws URISyntaxException {
    ReflectionTestUtils.setField(
        cqlLibraryService, "madieLibraryService", "https://localhost:9090/api");
    ReflectionTestUtils.setField(cqlLibraryService, "librariesCqlUri", "/cql-libraries/cql");

    httpHeaders.add("Authorization", "okta-access-token");
    libraryUri =
        new URI(
            "https://localhost:9090/api/cql-libraries/cql?name="
                + cqlLibraryName
                + "&version="
                + cqlLibraryVersion);
  }

  @Test
  void getLibraryCql() {
    when(restTemplate.exchange(
            libraryUri, HttpMethod.GET, new HttpEntity<>(httpHeaders), String.class))
        .thenReturn(new ResponseEntity<>("Response Cql String", HttpStatus.OK));
    String responseBody =
        cqlLibraryService.getLibraryCql(cqlLibraryName, cqlLibraryVersion, accessToken);
    assertEquals(responseBody, "Response Cql String");
  }

  @Test
  void getLibraryCqlReturnsNull() {
    when(restTemplate.exchange(
            libraryUri, HttpMethod.GET, new HttpEntity<>(httpHeaders), String.class))
        .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));
    String responseBody =
        cqlLibraryService.getLibraryCql(cqlLibraryName, cqlLibraryVersion, accessToken);
    assertNull(responseBody);
  }

  @Test
  void getLibraryCqlReturnsNullWhenNotFound() {
    when(restTemplate.exchange(
            libraryUri, HttpMethod.GET, new HttpEntity<>(httpHeaders), String.class))
        .thenReturn(new ResponseEntity<>(null, HttpStatus.NOT_FOUND));
    String responseBody =
        cqlLibraryService.getLibraryCql(cqlLibraryName, cqlLibraryVersion, accessToken);
    assertNull(responseBody);
  }

  @Test
  void getLibraryCqlReturnsNullWhenConflict() {
    when(restTemplate.exchange(
            libraryUri, HttpMethod.GET, new HttpEntity<>(httpHeaders), String.class))
        .thenReturn(new ResponseEntity<>(null, HttpStatus.CONFLICT));
    String responseBody =
        cqlLibraryService.getLibraryCql(cqlLibraryName, cqlLibraryVersion, accessToken);
    assertNull(responseBody);
  }

  @Test
  void testSetUpLibrarySourceProvider() {
    String cql = "library QICoreCommon version '1.3.000'\n" + "using QICore version '4.1.1'";
    cqlLibraryService.setUpLibrarySourceProvider(cql, "ACCESS_TOKEN");
    assertThat(MadieLibrarySourceProvider.getAccessToken(), is(equalTo("ACCESS_TOKEN")));
    UsingProperties usingProperties = MadieLibrarySourceProvider.getUsingProperties();
    assertThat(usingProperties, is(notNullValue()));
    assertThat(usingProperties.getLibraryType(), is(equalTo("QICore")));
    assertThat(usingProperties.getVersion(), is(equalTo("4.1.1")));
  }
}
