package gov.cms.mat.cql_elm_translation.utils.cql.parsing.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CQLIdentifierObjectTest {

  @Test
  void test() {
    CQLIdentifierObject identifier = new CQLIdentifierObject("One", "Two", "three");
    assertEquals("One.Two", identifier.getDisplay());
  }

  @Test
  void testNoAlias() {
    CQLIdentifierObject identifier = new CQLIdentifierObject("One", "Two", "three");
    identifier.setAliasName(null);
    assertEquals("Two", identifier.getDisplay());
  }

  @Test
  void testToString() {
    CQLIdentifierObject identifier = new CQLIdentifierObject("One", "Two", "three");
    assertEquals("One.\"Two\"", identifier.toString());
  }

  @Test
  void testToStringNoAlias() {
    CQLIdentifierObject identifier = new CQLIdentifierObject("One", "Two", "three");
    identifier.setAliasName(null);
    assertEquals("\"Two\"", identifier.toString());
  }
}
