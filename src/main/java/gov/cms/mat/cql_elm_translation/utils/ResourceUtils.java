package gov.cms.mat.cql_elm_translation.utils;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

public class ResourceUtils {
  public static String getData(String resource) {
    File file = new File(ResourceUtils.class.getResource(resource).getFile());
    try {
      return new String(Files.readAllBytes(file.toPath()));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
