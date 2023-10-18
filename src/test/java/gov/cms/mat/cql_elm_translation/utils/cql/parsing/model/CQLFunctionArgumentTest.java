package gov.cms.mat.cql_elm_translation.utils.cql.parsing.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CQLFunctionArgumentTest {

  @Test
  void test() {
    CQLFunctionArgument arg = new CQLFunctionArgument();
    arg.setArgumentName("  This is a test   ");
    assertEquals("This is a test", arg.getArgumentName());
  }
}
