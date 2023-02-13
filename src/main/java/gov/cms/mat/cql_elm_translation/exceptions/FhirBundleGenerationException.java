package gov.cms.mat.cql_elm_translation.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.UNPROCESSABLE_ENTITY)
public class FhirBundleGenerationException extends RuntimeException {
  private static final String MESSAGE = "Unable to generate Fhir bundle for Measure for: %s";

  public FhirBundleGenerationException(String id) {
    super(String.format(MESSAGE, id));
  }
}

