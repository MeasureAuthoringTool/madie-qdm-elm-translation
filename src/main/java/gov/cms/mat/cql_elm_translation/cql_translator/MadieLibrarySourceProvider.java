package gov.cms.mat.cql_elm_translation.cql_translator;

import gov.cms.mat.cql.elements.UsingProperties;
import gov.cms.mat.cql_elm_translation.service.CqlLibraryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.hl7.elm.r1.VersionedIdentifier;
import org.springframework.cache.annotation.Cacheable;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
public class MadieLibrarySourceProvider implements LibrarySourceProvider {

  private static final String[] STRING_ARR = new String[0];
  private static final ThreadLocal<UsingProperties> threadLocalValue = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalValueAccessToken = new ThreadLocal<>();
  private static CqlLibraryService cqlLibraryService;
  private static final Map<String, String[]> supportedLibrariesMap =
      Map.of(
          "FHIR", List.of("FHIR").toArray(STRING_ARR),
          "QICORE", List.of("FHIR", "QICore").toArray(STRING_ARR),
          "QDM", List.of("QDM").toArray(STRING_ARR));

  public static String getAccessToken() {
    return threadLocalValueAccessToken.get();
  }

  public static UsingProperties getUsingProperties() {
    return UsingProperties.builder()
        .libraryType(threadLocalValue.get().getLibraryType())
        .version(threadLocalValue.get().getVersion())
        .line(threadLocalValue.get().getLine())
        .comment(threadLocalValue.get().getComment())
        .build();
  }

  public static void setCqlLibraryService(CqlLibraryService cqlLibraryService) {
    MadieLibrarySourceProvider.cqlLibraryService = cqlLibraryService;
  }

  public static void setUsing(UsingProperties usingProperties) {
    threadLocalValue.set(usingProperties);
  }

  public static void setAccessToken(String accessToken) {
    threadLocalValueAccessToken.set(accessToken);
  }

  private static String createKey(String name, String qdmVersion, String version) {
    return name + "-" + qdmVersion + "-" + version;
  }

  @Override
  @Cacheable("cqlLibraries")
  public InputStream getLibrarySource(VersionedIdentifier libraryIdentifier) {
    String usingVersion = threadLocalValue.get().getVersion(); // using FHIR version '4.0.0
    String key = createKey(libraryIdentifier.getId(), usingVersion, libraryIdentifier.getVersion());
    return processLibrary(libraryIdentifier, key);
  }

  private InputStream processLibrary(VersionedIdentifier libraryIdentifier, String key) {
    String[] supportedLibraries =
        supportedLibrariesMap.get(threadLocalValue.get().getLibraryType().toUpperCase());
    if (Arrays.stream(supportedLibraries)
        .anyMatch(threadLocalValue.get().getLibraryType()::contains)) {
      return getInputStream(libraryIdentifier, key);
    } else {
      throw new IllegalArgumentException(
          String.format("%s is not supported.", threadLocalValue.get().getLibraryType()));
    }
  }

  private InputStream getInputStream(VersionedIdentifier libraryIdentifier, String key) {
    String cql =
        cqlLibraryService.getLibraryCql(
            libraryIdentifier.getId(),
            libraryIdentifier.getVersion(),
            threadLocalValueAccessToken.get());
    return processCqlFromService(key, cql);
  }

  private InputStream processCqlFromService(String key, String cql) {
    if (StringUtils.isEmpty(cql)) {
      log.debug("Did not find any cql for key : {}", key);
      return null;
    } else {
      return getInputStream(cql);
    }
  }

  private InputStream getInputStream(String cql) {
    return IOUtils.toInputStream(cql, StandardCharsets.UTF_8);
  }
}
