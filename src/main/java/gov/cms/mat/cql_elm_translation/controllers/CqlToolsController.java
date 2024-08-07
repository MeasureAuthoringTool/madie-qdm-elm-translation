package gov.cms.mat.cql_elm_translation.controllers;

import gov.cms.madie.models.dto.TranslatedLibrary;
import gov.cms.madie.models.measure.Measure;
import gov.cms.mat.cql_elm_translation.dto.CqlLookupRequest;
import gov.cms.madie.cql_elm_translator.dto.CqlBuilderLookup;
import gov.cms.mat.cql_elm_translation.dto.CqlLookups;
import gov.cms.mat.cql_elm_translation.service.CqlParsingService;
import gov.cms.madie.cql_elm_translator.dto.SourceDataCriteria;
import gov.cms.mat.cql_elm_translation.service.CqlConversionService;
import gov.cms.mat.cql_elm_translation.service.DataCriteriaService;
import gov.cms.madie.cql_elm_translator.utils.cql.parsing.model.CQLDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CqlToolsController {

  private final DataCriteriaService dataCriteriaService;
  private final CqlConversionService cqlConversionService;
  private final CqlParsingService cqlParsingService;

  @PutMapping("/cql/elm")
  public ResponseEntity<List<TranslatedLibrary>> getLibraryElms(
      @RequestBody String cql, @RequestHeader("Authorization") String accessToken) {
    try {
      return ResponseEntity.status(HttpStatus.OK)
          .body(cqlConversionService.getTranslatedLibrariesForCql(cql, accessToken));
    } catch (IOException e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @PutMapping("/cql/relevant-elements")
  public ResponseEntity<Set<SourceDataCriteria>> getRelevantElements(
      @RequestBody Measure measure, @RequestHeader("Authorization") String accessToken) {
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(dataCriteriaService.getRelevantElements(measure, accessToken));
  }

  @PutMapping("/cql/callstacks")
  public ResponseEntity<Map<String, Set<CQLDefinition>>> getDefinitionCallstack(
      @RequestBody String cql, @RequestHeader("Authorization") String accessToken) {
    return ResponseEntity.ok(cqlParsingService.getDefinitionCallstacks(cql, accessToken));
  }

  @PutMapping(
      value = "/cql/lookups",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<CqlLookups> getCqlLookups(
      @RequestBody CqlLookupRequest lookupRequest,
      @RequestHeader("Authorization") String accessToken) {
    log.info("Translator is preparing CQL Lookups for simple xml");
    return ResponseEntity.ok(
        cqlParsingService.getCqlLookups(
            lookupRequest.getCql(), lookupRequest.getMeasureExpressions(), accessToken));
  }

  @PutMapping(
      value = "/cql-builder-lookups",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<CqlBuilderLookup> getCqlBuilderLookups(
      @RequestBody String cql, @RequestHeader("Authorization") String accessToken) {
    log.info("Preparing CqlBuilder Lookups");
    return ResponseEntity.ok(cqlParsingService.getCqlBuilderLookups(cql, accessToken));
  }
}
