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
public class MadieFhirServices {

    private final RestTemplate restTemplate;

    @Value("${madie.fhir.service.baseUrl}")
    private String madieFhirService;

    @Value("${madie.fhir.service.libraries.uri}")
    private String librariesUri;

    public String getHapiFhirCql(String name, String version, String accessToken) {
        URI uri = buildMadieFhirServiceUri(name, version);
        log.debug("Getting Madie library: {} ", uri);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", accessToken);

        ResponseEntity<String> responseEntity = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);

        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            if (responseEntity.hasBody()) {
                log.debug("Retrieved a valid cqlPayload");
                return responseEntity.getBody();
            } else {
                log.error("Cannot find Cql payload in the response");
                return null;
            }
        } else if (responseEntity.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
            log.error("Cannot find a Cql Library with name: {}, version: {}", name, version );
        }
        return null;
    }

    private URI buildMadieFhirServiceUri(String name, String version) {
        return UriComponentsBuilder
            .fromHttpUrl(madieFhirService + librariesUri + "/cql")
            .queryParam("name", name)
            .queryParam("version", version)
            .build()
            .encode()
            .toUri();
    }
}
