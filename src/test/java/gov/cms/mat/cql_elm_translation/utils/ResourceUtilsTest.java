package gov.cms.mat.cql_elm_translation.utils;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import gov.cms.mat.cql_elm_translation.exceptions.InternalServerException;

@ExtendWith(MockitoExtension.class)
public class ResourceUtilsTest {

  @Test
  public void testReadDataThrowsInternalServerException() {
    assertThrows(InternalServerException.class, () -> ResourceUtils.getData(null));
  }
}
