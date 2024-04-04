package gov.cms.mat.cql_elm_translation.utils;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import gov.cms.mat.cql_elm_translation.exceptions.InternalServerException;

@ExtendWith(MockitoExtension.class)
public class ResourceUtilsTest {

  @Test
  public void testReadData() {
    String result = ResourceUtils.getData("/cv_populations.cql");
    assertNotNull(result);
  }

  @Test
  public void testReadNoFileTrhowsInternalServerException() {
    assertThrows(InternalServerException.class, () -> ResourceUtils.getData("dummy.cql"));
  }

  @Test
  public void testReadDataNullThrowsInternalServerException() {
    assertThrows(InternalServerException.class, () -> ResourceUtils.getData(null));
  }

  @Test
  public void testReadDataThrowsIOException() {

    try (MockedStatic<StreamUtils> utilities = Mockito.mockStatic(StreamUtils.class)) {
      utilities.when(() -> ResourceUtils.getStream(anyString())).thenThrow(new IOException());
      assertThrows(UncheckedIOException.class, () -> ResourceUtils.getData(""));
    }
  }
}
