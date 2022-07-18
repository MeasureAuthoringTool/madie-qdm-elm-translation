package gov.cms.mat.cql_elm_translation;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

public class ServletInitializer extends SpringBootServletInitializer {

  // Todo Do we need this ServletInitializer ?
  // https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.traditional-deployment
  @Override
  protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
    return application.sources(CqlElmTranslationApplication.class);
  }
}
