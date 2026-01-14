package gov.cms.mat.cql_elm_translation.exceptions;

public class UnsupportedModelException extends RuntimeException {
  public UnsupportedModelException() {
    super(
        "Only FHIR-based models are supported at this time. If you believe this is in error, please contact support.");
  }
}
