package gov.cms.mat.cql_elm_translation.controllers;

import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.cql_elm_translator.dto.CqlBuilderLookup;
import gov.cms.mat.cql_elm_translation.dto.RelevantElement;
import gov.cms.mat.cql_elm_translation.service.CqlParsingService;
import gov.cms.mat.cql_elm_translation.service.DataCriteriaService;
import gov.cms.madie.cql_elm_translator.utils.cql.parsing.model.CQLDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cqframework.cql.cql2elm.CqlCompilerException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CqlToolsController {

  private final DataCriteriaService dataCriteriaService;
  private final CqlParsingService cqlParsingService;

  // QDM specific now but we would need one for QICore as well in future while building QICore
  // testcase builder
  @PutMapping("/cql/relevant-elements")
  public ResponseEntity<Set<RelevantElement>> getRelevantElements(
      @RequestBody Measure measure, @RequestHeader("Authorization") String accessToken) {
    long startNanos = System.nanoTime();
    Set<RelevantElement> relevantElements =
        dataCriteriaService.getRelevantElements(measure, accessToken);
    long durationNanos = System.nanoTime() - startNanos;
    log.info("getRelevantElements execution time: {} seconds", durationNanos / 1_000_000_000.0);
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(relevantElements);
  }

  @PutMapping("/cql/callstacks")
  public ResponseEntity<Map<String, Set<CQLDefinition>>> getDefinitionCallstack(
      @RequestBody String cql,
      @RequestHeader("Authorization") String accessToken,
      @RequestParam(defaultValue = "Info") CqlCompilerException.ErrorSeverity errorSeverity) {
    return ResponseEntity.ok(
        cqlParsingService.getDefinitionCallstacks(cql, accessToken, errorSeverity));
  }

  @PutMapping(
      value = "/cql-builder-lookups",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.TEXT_PLAIN_VALUE)
  public ResponseEntity<CqlBuilderLookup> getCqlBuilderLookups(
      @RequestBody String cql,
      @RequestHeader("Authorization") String accessToken,
      @RequestParam(defaultValue = "Info") CqlCompilerException.ErrorSeverity errorSeverity) {
    return ResponseEntity.ok(
        cqlParsingService.getCqlBuilderLookups(cql, accessToken, errorSeverity));
  }
}
