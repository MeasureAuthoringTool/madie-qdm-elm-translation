package gov.cms.mat.cql_elm_translation;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"madie.ig-resource-pattern=classpath:igs/*.json"})
class CqlElmTranslationApplicationTests {

  @Test
  void contextLoads() {}
}
