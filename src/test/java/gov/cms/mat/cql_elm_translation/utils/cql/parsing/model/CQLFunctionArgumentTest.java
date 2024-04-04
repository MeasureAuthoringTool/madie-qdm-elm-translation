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

  @Test
  void testQdmClone() {
    CQLFunctionArgument arg = new CQLFunctionArgument();
    arg.setArgumentName("  This is a test   ");
    arg.setQdmDataType("QdmDataType");
    CQLFunctionArgument arg2 = arg.clone();
    assertEquals(arg2.getReturnType(), arg.getReturnType());
  }

  @Test
  void testOtherClone() {
    CQLFunctionArgument arg = new CQLFunctionArgument();
    arg.setArgumentName("  This is a test   ");
    arg.setOtherType("OtherType");
    CQLFunctionArgument arg2 = arg.clone();
    assertEquals("OtherType", arg2.getReturnType());
  }

  @Test
  void testArgumentClone() {
    CQLFunctionArgument arg = new CQLFunctionArgument();
    arg.setArgumentName("  This is a test   ");
    arg.setArgumentType("Arg");
    CQLFunctionArgument arg2 = arg.clone();
    assertEquals("Arg", arg2.getReturnType());
  }
}
