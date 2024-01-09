package gov.cms.mat.cql_elm_translation.service;

import gov.cms.mat.cql.CqlTextParser;
import gov.cms.mat.cql_elm_translation.cql_translator.MadieLibrarySourceProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cqframework.cql.cql2elm.CqlIncludeException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;

@Service
@Slf4j
@RequiredArgsConstructor
public class CqlLibraryService {

  private final RestTemplate restTemplate;

  @Value("${madie.library.service.baseUrl}")
  private String madieLibraryService;

  @Value("${madie.library.service.cql.uri}")
  private String librariesCqlUri;

  public void setUpLibrarySourceProvider(String cql, String accessToken) {
    MadieLibrarySourceProvider.setUsing(new CqlTextParser(cql).getUsing());
    MadieLibrarySourceProvider.setCqlLibraryService(this);
    MadieLibrarySourceProvider.setAccessToken(accessToken);
  }

  public String getLibraryCql(String name, String version, String accessToken) {
    URI uri = buildMadieLibraryServiceUri(name, version);
    log.debug("Getting Madie library: {} ", uri);

    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", accessToken);

    ResponseEntity<String> responseEntity =
        restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);

    if (responseEntity.getStatusCode().is2xxSuccessful()) {
      if (responseEntity.hasBody()) {
        log.debug("Retrieved a valid cqlPayload");
        if (new CqlTextParser(responseEntity.getBody())
            .getUsing()
            .getLine()
            .equals(MadieLibrarySourceProvider.getUsingProperties().getLine())) {
          return responseEntity.getBody();
        }
        log.error("Library model and version does not match the Measure model and version");
        throw new CqlIncludeException(
            String.format(
                "Library model and version does not match the Measure model and version for name: %s, version: %s",
                name, version),
            null,
            name,
            version);

      } else {
        log.error("Cannot find Cql payload in the response");
        return null;
      }
    } else if (responseEntity.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
      log.error("Cannot find a Cql Library with name: {}, version: {}", name, version);
    } else if (responseEntity.getStatusCode().equals(HttpStatus.CONFLICT)) {
      log.error(
          "Multiple libraries found with name: {}, version: {}, but only one was expected",
          name,
          version);
    }
    return null;
  }

  private URI buildMadieLibraryServiceUri(String name, String version) {
    return UriComponentsBuilder.fromHttpUrl(madieLibraryService + librariesCqlUri)
        .queryParam("name", name)
        .queryParam("version", version)
        .build()
        .encode()
        .toUri();
  }
}
