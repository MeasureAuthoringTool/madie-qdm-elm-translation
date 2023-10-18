package gov.cms.mat.cql_elm_translation.utils.cql.parsing.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CQLParameterTest {

  @Test
  void test() {
    CQLParameter param = new CQLParameter();
    param.setParameterLogic(" This is a test ");
    param.setParameterName(" This is a test ");
    assertEquals("This is a test", param.getParameterLogic());
    assertEquals("This is a test", param.getParameterName());
  }
}
