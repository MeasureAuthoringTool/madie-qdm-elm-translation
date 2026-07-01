package gov.cms.mat.cql_elm_translation.config.security;

import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class UserServiceClientConfig {

  @Bean
  public RestTemplate userServiceRestTemplate(RestTemplateBuilder builder) {
    return builder.build();
  }
}
