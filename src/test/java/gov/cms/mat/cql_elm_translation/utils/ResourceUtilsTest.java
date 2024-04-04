package gov.cms.mat.cql_elm_translation.utils;

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
  public void testReadDataThrowsInternalServerException() {
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
