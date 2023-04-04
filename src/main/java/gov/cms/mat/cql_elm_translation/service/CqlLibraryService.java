package gov.cms.mat.cql_elm_translation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        return responseEntity.getBody();
      } else {
        log.error("Cannot find Cql payload in the response");
        return null;
      }
    } else if (responseEntity.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
      log.error("Cannot find a Cql Library with name: {}, version: {}", name, version);
    } else if (responseEntity.getStatusCode().equals(HttpStatus.CONFLICT)) {
      log.error("Multiple libraries found with name: {}, version: {}, but only one was expected", name, version);
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
