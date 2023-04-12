package gov.cms.mat.cql_elm_translation.config;

import java.util.Properties;

import org.springframework.boot.info.GitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GitConfig {

  @Bean
  public GitProperties gitProperties() {
    return new GitProperties(new Properties());
  }
}
