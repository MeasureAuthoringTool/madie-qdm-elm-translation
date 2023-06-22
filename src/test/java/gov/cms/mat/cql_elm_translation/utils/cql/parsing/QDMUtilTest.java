package gov.cms.mat.cql_elm_translation.utils.cql.parsing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class QDMUtilTest {

  //   How are we populating this dataTypes to Attributes Map
  @Test
  void getQDMContainer() {
    QDMContainer qdmContainer = QDMUtil.getQDMContainer();
    assertEquals(84, qdmContainer.getDatatypes().size());
    assertEquals(71, qdmContainer.getAttributes().size());
  }
}
