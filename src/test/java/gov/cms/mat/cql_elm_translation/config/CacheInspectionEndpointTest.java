package gov.cms.mat.cql_elm_translation.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import gov.cms.mat.cql_elm_translation.CqlElmTranslationApplication;
import gov.cms.mat.cql_elm_translation.clients.UserRoleConverter;
import gov.cms.mat.cql_elm_translation.config.security.SecurityConfig;
import gov.cms.mat.cql_elm_translation.config.security.SecurityFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.actuate.web.WebMvcEndpointManagementContextConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CacheInspectionEndpointTest {

  @Test
  void configuredCachesShouldExposeAccurateStatsAndPreserveCapacity() {
    CacheManager manager = new CqlElmTranslationApplication().cacheManager();
    assertThat(manager.getCacheNames()).containsExactly("cqlLibraries");
    for (String name : manager.getCacheNames()) {
      CaffeineCache cache = (CaffeineCache) manager.getCache(name);
      assertThat(cache).isNotNull();
      var nativeCache = cache.getNativeCache();
      assertThat(nativeCache.policy().isRecordingStats()).isTrue();
      assertThat(nativeCache.policy().eviction().orElseThrow().getMaximum()).isEqualTo(500);
      nativeCache.put("present", "value");
      nativeCache.getIfPresent("present");
      nativeCache.getIfPresent("missing");
      Map<?, ?> stats = (Map<?, ?>) new CacheInspectionEndpoint(manager).inspect().get(name);
      assertThat(stats.get("hitCount")).isEqualTo(1L);
      assertThat(stats.get("missCount")).isEqualTo(1L);
      assertThat(stats.get("hitRate")).isEqualTo("50.00%");
      assertThat(stats.get("entryCount")).isEqualTo(1L);
      assertThat(stats.containsKey("keys")).isFalse();
      assertThat(stats.containsKey("estimatedMemoryBytes")).isFalse();
      for (int i = 0; i < 500; i++) {
        nativeCache.put(i, "value");
      }
      nativeCache.cleanUp();
      stats = (Map<?, ?>) new CacheInspectionEndpoint(manager).inspect().get(name);
      assertThat(stats.get("entryCount")).isEqualTo(500L);
      assertThat(stats.get("evictionCount")).isEqualTo(1L);
    }
  }

  @Test
  void inspectShouldHandleEmptyCachesAndDisabledStats() {
    CaffeineCacheManager manager = new CaffeineCacheManager("empty");
    manager.setCaffeine(Caffeine.newBuilder());
    Map<String, Object> result = new CacheInspectionEndpoint(manager).inspect();
    Map<?, ?> stats = (Map<?, ?>) result.get("empty");
    assertThat(stats.get("hitRate")).isEqualTo("N/A");
    assertThat(stats.get("statsEnabled")).isEqualTo(false);
    assertThat(stats.get("hitCount")).isEqualTo(0L);
    assertThat(stats.get("missCount")).isEqualTo(0L);
    Map<?, ?> memory = (Map<?, ?>) result.get("jvmMemory");
    assertThat((Long) memory.get("heapUsedBytes")).isGreaterThanOrEqualTo(0);
    assertThat((Long) memory.get("heapCommittedBytes"))
        .isGreaterThanOrEqualTo((Long) memory.get("heapUsedBytes"));
    assertThat((Long) memory.get("heapMaxBytes")).isNotZero();
  }

  @Test
  void inspectShouldReportMissingAndNonCaffeineCachesWithoutExposingEntries() {
    CacheManager manager = mock(CacheManager.class);
    ConcurrentMapCache cache = new ConcurrentMapCache("other");
    cache.put("sensitive-key", "sensitive-value");
    when(manager.getCacheNames()).thenReturn(List.of("missing", "other"));
    when(manager.getCache("other")).thenReturn(cache);

    Map<String, Object> result = new CacheInspectionEndpoint(manager).inspect();

    assertThat(result.get("missing"))
        .isEqualTo(Map.of("type", "N/A", "message", "Cache not found"));
    assertThat(result.get("other"))
        .isEqualTo(
            Map.of(
                "type", "ConcurrentMapCache",
                "message", "Cache statistics are only available for Caffeine caches"));
    assertThat(cache.get("sensitive-key", String.class)).isEqualTo("sensitive-value");
  }

  @Test
  void inspectShouldReportSuccessfulAndFailedCacheLoads() {
    LoadingCache<Object, Object> nativeCache =
        Caffeine.newBuilder()
            .recordStats()
            .build(
                key -> {
                  if (key.equals("failed")) {
                    throw new IllegalStateException("load failed");
                  }
                  return "loaded";
                });
    CaffeineCache cache = new CaffeineCache("loading", nativeCache);
    CacheManager manager = mock(CacheManager.class);
    when(manager.getCacheNames()).thenReturn(List.of("loading"));
    when(manager.getCache("loading")).thenReturn(cache);

    assertThat(nativeCache.get("success")).isEqualTo("loaded");
    assertThat(nativeCache.get("success")).isEqualTo("loaded");
    assertThatThrownBy(() -> nativeCache.get("failed")).isInstanceOf(IllegalStateException.class);

    Map<?, ?> stats = (Map<?, ?>) new CacheInspectionEndpoint(manager).inspect().get("loading");

    assertThat(stats.get("statsEnabled")).isEqualTo(true);
    assertThat(stats.get("loadCount")).isEqualTo(2L);
    assertThat(stats.get("entryCount")).isEqualTo(1L);
    assertThat(stats.get("hitCount")).isEqualTo(1L);
    assertThat(stats.get("missCount")).isEqualTo(2L);
    assertThat(stats.get("hitRate")).isEqualTo("33.33%");
    assertThat(stats.get("averageLoadPenaltyMs")).asString().matches("\\d+\\.\\d{2}");
  }

  @ParameterizedTest
  @ValueSource(strings = {"default", "local", "aws", "it"})
  void actuatorShouldExposeInspectorUsingApplicationConfiguration(String profile) {
    new WebApplicationContextRunner()
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .withPropertyValues("spring.profiles.active=" + profile, "mat-api-key=Enabled")
        .withConfiguration(
            AutoConfigurations.of(
                EndpointAutoConfiguration.class,
                WebEndpointAutoConfiguration.class,
                DispatcherServletAutoConfiguration.class,
                WebMvcEndpointManagementContextConfiguration.class,
                WebMvcAutoConfiguration.class,
                JacksonAutoConfiguration.class,
                HttpMessageConvertersAutoConfiguration.class))
        .withUserConfiguration(SecurityConfig.class)
        .withBean(UserRoleConverter.class, () -> mock(UserRoleConverter.class))
        .withBean(JwtDecoder.class, () -> mock(JwtDecoder.class))
        .withBean(SecurityFilter.class)
        .withBean(CacheManager.class, () -> new CqlElmTranslationApplication().cacheManager())
        .withBean(CacheInspectionEndpoint.class)
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context.getBean(WebEndpointsSupplier.class).getEndpoints())
                  .anyMatch(
                      endpoint -> endpoint.getEndpointId().toString().equals("cache-inspection"));
              var mvc =
                  MockMvcBuilders.webAppContextSetup(context)
                      .apply(springSecurity())
                      .addFilters(context.getBean(SecurityFilter.class))
                      .build();
              mvc.perform(get("/api/qdm/actuator/cache-inspection").contextPath("/api/qdm"))
                  .andExpect(status().isOk())
                  .andExpect(jsonPath("$.cqlLibraries.statsEnabled").value(true))
                  .andExpect(jsonPath("$.jvmMemory.heapUsedBytes").isNumber())
                  .andExpect(jsonPath("$.jvmMemory.heapMaxBytes").isNumber());
            });
  }
}
