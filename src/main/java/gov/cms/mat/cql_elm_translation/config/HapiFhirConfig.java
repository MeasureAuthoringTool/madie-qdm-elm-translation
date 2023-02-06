package gov.cms.mat.cql_elm_translation.config;

import ca.uhn.fhir.context.FhirContext;
import lombok.extern.slf4j.Slf4j;
// import org.opencds.cqf.cql.engine.fhir.searchparam.SearchParameterResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class HapiFhirConfig {

  @Bean
  public FhirContext fhirContext() {
    return FhirContext.forR4();
  }

  //  @Bean
  //  public SearchParameterResolver searchParameterResolver(@Autowired FhirContext fhirContext) {
  //    return new SearchParameterResolver(fhirContext);
  //  }
}
