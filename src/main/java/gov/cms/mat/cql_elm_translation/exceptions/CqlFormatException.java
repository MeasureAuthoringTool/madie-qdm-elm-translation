package gov.cms.mat.cql_elm_translation.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class CqlFormatException extends RuntimeException {
  public CqlFormatException(String message) {
    super(message);
  }
}
