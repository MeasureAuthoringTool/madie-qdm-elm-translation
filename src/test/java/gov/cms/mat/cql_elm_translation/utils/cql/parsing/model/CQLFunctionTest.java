package gov.cms.mat.cql_elm_translation.utils.cql.parsing.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class CQLFunctionTest {

  @Test
  void testGetSetArgument() {
    CQLFunction function = new CQLFunction();

    CQLFunctionArgument arg = new CQLFunctionArgument();
    arg.setArgumentName("  This is a test   ");
    function.setArgumentList(List.of(arg));
    List<CQLFunctionArgument> args = function.getArgumentList();
    assertEquals(1, args.size());
  }

  @Test
  void testGettersSetters() {
    CQLFunction function = new CQLFunction();

    function.setName("This is a name");
    assertEquals("This is a name", function.getName());

    function.setLogic("This is a logic");
    assertEquals("This is a logic", function.getLogic());
  }
}
