package gov.cms.mat.cql_elm_translation.service.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.mat.fhir.rest.dto.MatCqlConversionException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.cqframework.cql.cql2elm.CqlCompilerException;
import org.cqframework.cql.elm.tracking.TrackBack;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class CqlExceptionErrorProcessor {

  private static final ObjectMapper mapper = new ObjectMapper();

  private final List<CqlCompilerException> cqlErrors;
  private final String json;

  /**
   * Transforms CqlTranslatorException to MatCqlConversionException and prepend with
   * "errorExceptions" object to the translator.json
   */
  public CqlExceptionErrorProcessor(List<CqlCompilerException> cqlErrors, String json) {
    this.cqlErrors = cqlErrors;
    this.json = json;
  }

  public String process() {
    try {
      if (CollectionUtils.isEmpty(cqlErrors)) {
        return json;
      } else {
        return addErrorsToJson();
      }
    } catch (Exception e) {
      log.error("Cannot parse json.", e);
      log.trace(json);
      return json;
    }
  }

  private String addErrorsToJson() throws JsonProcessingException {
    mapper.readTree(json);

    List<MatCqlConversionException> matErrors = buildMatErrors();
    String jsonToInsert = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(matErrors);

    return json.replaceFirst("\n", "\n  \"errorExceptions\":" + jsonToInsert + ",\n");
  }

  private List<MatCqlConversionException> buildMatErrors() {
    return cqlErrors.stream().map(this::createDto).collect(Collectors.toList());
  }

  private MatCqlConversionException createDto(CqlCompilerException cqlException) {
    MatCqlConversionException matCqlConversionException = buildMatError(cqlException);

    if (cqlException.getLocator() == null) {
      log.warn("Locator is null");
    } else {
      addLocatorData(cqlException.getLocator(), matCqlConversionException);
    }

    return matCqlConversionException;
  }

  private MatCqlConversionException buildMatError(CqlCompilerException cqlTranslatorException) {
    MatCqlConversionException matCqlConversionException = new MatCqlConversionException();
    matCqlConversionException.setErrorSeverity(cqlTranslatorException.getSeverity().name());
    log.debug("buildMatError" + cqlTranslatorException.getMessage());
    try {
      String payload = escape(cqlTranslatorException.getMessage());

      if (StringUtils.contains(payload, "UsingProperties.getVersion")) {
        log.info("cqlTranslatorException: " + payload);
        String rawPayload = clean(payload);
        if (rawPayload.equals(
            "CannotinvokegovcmsmatcqlelementsUsingProperties"
                + "getVersionbecausethereturnvalueofjavalangThreadLocalgetisnull")) {
          payload = "Model Type and version are required";
        }
      }

      matCqlConversionException.setMessage(payload);
    } catch (Exception e) {
      log.info("Error building MADiEError message", e.getMessage());
      log.debug("Error building MADiEError", e);
      matCqlConversionException.setMessage("Exception");
    }

    return matCqlConversionException;
  }

  private void addLocatorData(
      TrackBack locator, MatCqlConversionException matCqlConversionException) {
    matCqlConversionException.setStartLine(locator.getStartLine());
    matCqlConversionException.setStartChar(locator.getStartChar());
    matCqlConversionException.setEndLine(locator.getEndLine());
    matCqlConversionException.setEndChar(locator.getEndChar());
    matCqlConversionException.setTargetIncludeLibraryVersionId(locator.getLibrary().getVersion());
    matCqlConversionException.setTargetIncludeLibraryId(locator.getLibrary().getId());
  }

  private String escape(String raw) {
    String escaped = raw;
    escaped = escaped.replace("\\", "\\\\");
    escaped = escaped.replace("\"", "\\\"");
    escaped = escaped.replace("\b", "\\b");
    escaped = escaped.replace("\f", "\\f");
    escaped = escaped.replace("\n", "\\n");
    escaped = escaped.replace("\r", "\\r");
    escaped = escaped.replace("\t", "\\t");
    // TODO: escape other non-printing characters using uXXXX notation
    return escaped;
  }

  private String clean(String raw) {
    return raw.replaceAll("[^a-zA-Z0-9]", "");
  }
}
