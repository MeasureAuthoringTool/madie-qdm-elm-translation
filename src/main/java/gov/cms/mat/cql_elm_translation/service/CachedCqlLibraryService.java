package gov.cms.mat.cql_elm_translation.service;

import gov.cms.madie.cql_elm_translator.service.CqlLibraryService;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

// TODO: Remove this class and its associated tests once madie-qdm-elm-translation is upgraded
// to a version of madie-translator-commons (will upgrade in MAT-9441) that includes built-in
// caching in CqlLibraryService.
// This class exists because qdm-elm cannot upgrade to the newer madie-translator-commons due to
// an incompatibility between info.cqframework:3.29.0 (used by qdm-elm) and
// org.cqframework:4.7.0 (pulled in by the newer commons).
@Service
@Primary
public class CachedCqlLibraryService extends CqlLibraryService {

  private final CacheManager cacheManager;

  public CachedCqlLibraryService(RestTemplate restTemplate, CacheManager cacheManager) {
    super(restTemplate);
    this.cacheManager = cacheManager;
  }

  @Override
  public String getLibraryCql(String name, String version, String accessToken) {
    try {
      return cacheManager
          .getCache("cqlLibraries")
          .get(name + "_" + version, () -> super.getLibraryCql(name, version, accessToken));
    } catch (Cache.ValueRetrievalException e) {
      throw (RuntimeException) e.getCause();
    }
  }
}
