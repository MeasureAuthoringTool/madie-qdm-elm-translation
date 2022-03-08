package gov.cms.mat.cql_elm_translation.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;

@Service
@Slf4j
public class MatFhirServices {

    @Qualifier("internalRestTemplate")
    private final RestTemplate restTemplate;

    @Value("${fhir.conversion.baseurl}")
    private String baseURL;

    public MatFhirServices(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String getHapiFhirCql(String name, String version) {
        URI uri = buildFindMatUri(name, version);
        log.debug("Getting Mat library: {} ", uri);

        ResponseEntity<String> responseEntity = restTemplate.getForEntity(uri, String.class);

        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            if (responseEntity.hasBody()) {
                if (responseEntity.getBody() == null) {
                    log.error("cqlPayload is INVALID (null)");
                    return null;
                } else {
                    log.debug("cqlPayload is valid");
                    return responseEntity.getBody();
                }
            } else {
                log.error("cqlPayload has no Body");
                return null;
            }
        } else {
            log.error("cqlPayload has invalid status code: {}", responseEntity.getStatusCode() );
            return null;
        }
    }

    private URI buildFindMatUri(String name, String version) {
        return UriComponentsBuilder
                .fromHttpUrl(baseURL + "/library/find/hapi")
                .queryParam("name", name)
                .queryParam("version", version)
                .build()
                .encode()
                .toUri();
    }
}
