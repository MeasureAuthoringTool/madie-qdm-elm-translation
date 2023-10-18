package gov.cms.mat.cql_elm_translation.utils.cql.parsing.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CQLIncludeLibraryTest {

  @Test
  void testCQLIncludeLibrary() {
    CQLLibraryDataSetObject dto1 = new CQLLibraryDataSetObject();
    dto1.setId("ONE");
    dto1.setCqlName("<test>This is a test</test>.");
    dto1.setVersion("1.1");
    dto1.setRevisionNumber("2");
    dto1.setQdmVersion("123");
    dto1.setCqlSetId("111");
    CQLIncludeLibrary library1 = new CQLIncludeLibrary(dto1);
    assertEquals(library1.getVersion(), "1.1.2");

    CQLIncludeLibrary library2 = new CQLIncludeLibrary(dto1);

    assertEquals(library1, library2);
    assertEquals(library1.toString(), library2.toString());

    CQLIncludeLibrary library3 = new CQLIncludeLibrary(library1);

    assertEquals(library1, library3);
  }
}
