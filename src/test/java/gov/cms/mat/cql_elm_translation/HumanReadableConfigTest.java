package gov.cms.mat.cql_elm_translation;

import freemarker.template.Template;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class HumanReadableConfigTest {
  @Autowired Template baseHumanReadableTemplate;

  @Test
  public void humanReadableServiceInits() {
    assertNotNull(baseHumanReadableTemplate);
    assertEquals("humanreadable/human_readable.ftl", baseHumanReadableTemplate.getName());
  }
}
