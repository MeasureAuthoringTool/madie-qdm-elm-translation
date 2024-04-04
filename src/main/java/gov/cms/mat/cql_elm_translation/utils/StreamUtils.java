package gov.cms.mat.cql_elm_translation.utils;

import java.io.IOException;
import java.io.InputStream;

public class StreamUtils {

  public static InputStream getStream(String resource) throws IOException {
    return StreamUtils.class.getResourceAsStream(resource);
  }
}
