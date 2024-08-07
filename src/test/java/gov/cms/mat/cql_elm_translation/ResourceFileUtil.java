package gov.cms.mat.cql_elm_translation;

import gov.cms.madie.cql_elm_translator.exceptions.InternalServerException;
import gov.cms.madie.cql_elm_translator.utils.ResourceUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

public interface ResourceFileUtil {
  default String getData(String resource) {
    try (InputStream inputStream = ResourceUtils.class.getResourceAsStream(resource)) {
      if (inputStream == null) {
        throw new InternalServerException("Unable to fetch resource " + resource);
      }
      return new String(inputStream.readAllBytes());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (NullPointerException nullPointerException) {
      throw new InternalServerException("Resource name cannot be null");
    }
  }
}
