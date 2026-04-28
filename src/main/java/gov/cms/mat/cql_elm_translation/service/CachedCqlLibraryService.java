package gov.cms.mat.cql_elm_translation.service;

import gov.cms.madie.cql_elm_translator.service.CqlLibraryService;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
    return cacheManager
        .getCache("cqlLibraries")
        .get(name + "_" + version, () -> super.getLibraryCql(name, version, accessToken));
  }
}
