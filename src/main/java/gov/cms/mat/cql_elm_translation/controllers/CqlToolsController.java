package gov.cms.mat.cql_elm_translation.controllers;

import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.PopulationType;
import gov.cms.mat.cql_elm_translation.data.DataCriteria;
import gov.cms.mat.cql_elm_translation.dto.SourceDataCriteria;
import gov.cms.mat.cql_elm_translation.exceptions.CqlFormatException;
import gov.cms.mat.cql_elm_translation.service.CqlConversionService;
import gov.cms.mat.cql_elm_translation.service.CqlParsingService;
import gov.cms.mat.cql_elm_translation.service.DataCriteriaService;
import gov.cms.mat.cql_elm_translation.service.HumanReadableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cqframework.cql.tools.formatter.CqlFormatterVisitor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CqlToolsController {

  private final DataCriteriaService dataCriteriaService;
  private final CqlConversionService cqlConversionService;
  private final HumanReadableService humanReadableService;
  private final CqlParsingService cqlParsingService;

  @PutMapping("/cql/format")
  public ResponseEntity<String> formatCql(@RequestBody String cqlData, Principal principal) {
    try (var cqlDataStream = new ByteArrayInputStream(cqlData.getBytes())) {
      CqlFormatterVisitor.FormatResult formatResult =
          CqlFormatterVisitor.getFormattedOutput(cqlDataStream);
      if (formatResult.getErrors() != null && !formatResult.getErrors().isEmpty()) {
        log.info("User [{}] requested to format the CQL, but errors found", principal.getName());
        throw new CqlFormatException(
            "Unable to format CQL, because one or more Syntax errors are identified");
      }
      log.info("User [{}] successfully formatted the CQL", principal.getName());
      return ResponseEntity.ok(formatResult.getOutput());
    } catch (IOException e) {
      log.info("User [{}] is unable to format the CQL", principal.getName());
      throw new CqlFormatException(e.getMessage());
    }
  }

  @PutMapping("/cql/source-data-criteria")
  public ResponseEntity<List<SourceDataCriteria>> getSourceDataCriteria(
      @RequestBody String cql, @RequestHeader("Authorization") String accessToken) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(dataCriteriaService.getSourceDataCriteria(cql, accessToken));
  }

  @PutMapping("/cql/definitionsGraph")
  public ResponseEntity< List<String>> getDefinitionsGraph(
          @RequestBody String cql,  @RequestHeader("Authorization") String accessToken) {
    List<String> populations= Arrays.asList("test1", "test2", "Initial Population", "test3");
    return ResponseEntity.status(HttpStatus.OK)
            .body(cqlParsingService.parseUsedDefinitionsFromCql(cql, populations, accessToken));
  }

  @PutMapping("/human-readable")
  public ResponseEntity<String> generateHumanReadable(
      @RequestBody Measure madieMeasure, Principal principal) {
    log.info(
        "User [{}] requested QDM Human Readable for Measure ID [{}]",
        principal.getName(),
        madieMeasure.getId());
    return ResponseEntity.status(HttpStatus.OK).body(humanReadableService.generate(madieMeasure));
  }

  @PutMapping("/cql/elm")
  public ResponseEntity<List<String>> getLibraryElms(
      @RequestBody String cql, @RequestHeader("Authorization") String accessToken) {
    try {
      return ResponseEntity.status(HttpStatus.OK)
          .body(cqlConversionService.getElmForCql(cql, accessToken));
    } catch (IOException e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @PutMapping("/qdm/relevant-elements")
  public ResponseEntity<Set<SourceDataCriteria>> getRelevantElements(
      @RequestBody Measure measure, @RequestHeader("Authorization") String accessToken) {
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(dataCriteriaService.getRelevantElements(measure, accessToken));
  }
}
