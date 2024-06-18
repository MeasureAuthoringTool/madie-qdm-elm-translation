package gov.cms.mat.cql_elm_translation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@Data
public class TranslatorVersionConfig {

  @Value("${madie.translatorVersion.currentVersion}")
  private String currentTranslatorVersion;

  @Value("${madie.translatorVersion.mostRecentVersion}")
  private String mostRecentTranslatorVersion;
}
