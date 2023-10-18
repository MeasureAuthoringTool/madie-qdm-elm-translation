package gov.cms.mat.cql_elm_translation.utils.cql.parsing.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CQLQualityDataSetDTOTest {

  @Test
  void testCreateCQLQualityDataSetDTO() {
    CQLQualityDataSetDTO dto1 = new CQLQualityDataSetDTO();
    dto1.setId("ONE");
    CQLQualityDataSetDTO dto2 = new CQLQualityDataSetDTO();
    dto2.setId("ONE");
    assertEquals(dto1, dto2);
    assertEquals(dto1.toString(), dto2.toString());
  }
}
