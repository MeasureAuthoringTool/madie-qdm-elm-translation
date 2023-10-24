package gov.cms.mat.cql_elm_translation.utils.cql.parsing.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CQLLibraryDataSetObjectTest {

  @Test
  void testCreateCQLLibraryDataSetObject() {
    CQLLibraryDataSetObject dto1 = new CQLLibraryDataSetObject();
    dto1.setId("ONE");
    dto1.setCqlName("<test>This is a test</test>.");
    CQLLibraryDataSetObject dto2 = new CQLLibraryDataSetObject();
    dto2.setId("ONE");
    dto2.setCqlName("<test>This is a test</test>.");
    assertEquals(dto1, dto2);
    assertEquals(dto1.toString(), dto2.toString());

    dto1.scrubForMarkUp();

    assertNotEquals(dto1, dto2);
    assertEquals(dto1.getCqlName(), "This is a test.");
  }
}
