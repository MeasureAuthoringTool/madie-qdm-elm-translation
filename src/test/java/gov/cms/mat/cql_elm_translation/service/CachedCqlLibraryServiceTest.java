package gov.cms.mat.cql_elm_translation.service;

import com.github.benmanes.caffeine.cache.Caffeine;
import gov.cms.madie.cql_elm_translator.exceptions.LibraryResourceLoaderException;
import org.springframework.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CachedCqlLibraryServiceTest {

  @Mock private RestTemplate restTemplate;

  private CachedCqlLibraryService cachedCqlLibraryService;

  private static final String NAME = "FHIRHelpers";
  private static final String VERSION = "4.1.000";
  private static final String ACCESS_TOKEN = "test-token";
  private static final String CQL =
      "library FHIRHelpers version '4.1.000'\nusing FHIR version '4.0.1'";

  @BeforeEach
  void setUp() {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager("cqlLibraries");
    cacheManager.setCaffeine(Caffeine.newBuilder());
    cachedCqlLibraryService = new CachedCqlLibraryService(restTemplate, cacheManager);
    ReflectionTestUtils.setField(
        cachedCqlLibraryService, "madieLibraryService", "https://localhost:9090/api");
    ReflectionTestUtils.setField(cachedCqlLibraryService, "librariesCqlUri", "/cql-libraries/cql");
    cachedCqlLibraryService.setUpLibrarySourceProvider(
        "library TestMeasure version '0.0.1'\nusing FHIR version '4.0.1'", ACCESS_TOKEN);
  }

  @Test
  void getLibraryCqlCachesOnFirstCallAndHitsOnSecond() {
    when(restTemplate.exchange(
            any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
        .thenReturn(new ResponseEntity<>(CQL, HttpStatus.OK));

    cachedCqlLibraryService.getLibraryCql(NAME, VERSION, ACCESS_TOKEN);
    cachedCqlLibraryService.getLibraryCql(NAME, VERSION, ACCESS_TOKEN);

    verify(restTemplate, times(1)).exchange(any(URI.class), any(), any(), eq(String.class));
  }

  @Test
  void getLibraryCqlCallsServiceAgainForDifferentVersion() {
    when(restTemplate.exchange(
            any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
        .thenReturn(new ResponseEntity<>(CQL, HttpStatus.OK));

    cachedCqlLibraryService.getLibraryCql(NAME, VERSION, ACCESS_TOKEN);
    cachedCqlLibraryService.getLibraryCql(NAME, "4.2.000", ACCESS_TOKEN);

    verify(restTemplate, times(2)).exchange(any(URI.class), any(), any(), eq(String.class));
  }

  @Test
  void getLibraryCqlDoesNotCacheOnNotFound() {
    HttpClientErrorException notFound = mock(HttpClientErrorException.NotFound.class);
    doThrow(notFound)
        .when(restTemplate)
        .exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));

    Cache.ValueRetrievalException ex1 =
        assertThrows(
            Cache.ValueRetrievalException.class,
            () -> cachedCqlLibraryService.getLibraryCql(NAME, VERSION, ACCESS_TOKEN));
    assertInstanceOf(LibraryResourceLoaderException.class, ex1.getCause());

    Cache.ValueRetrievalException ex2 =
        assertThrows(
            Cache.ValueRetrievalException.class,
            () -> cachedCqlLibraryService.getLibraryCql(NAME, VERSION, ACCESS_TOKEN));
    assertInstanceOf(LibraryResourceLoaderException.class, ex2.getCause());

    verify(restTemplate, times(2)).exchange(any(URI.class), any(), any(), eq(String.class));
  }
}
