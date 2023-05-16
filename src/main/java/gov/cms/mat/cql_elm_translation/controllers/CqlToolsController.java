package gov.cms.mat.cql_elm_translation.controllers;

import gov.cms.mat.cql_elm_translation.cql_translator.MadieLibrarySourceProvider;
import gov.cms.mat.cql_elm_translation.data.RequestData;
import gov.cms.mat.cql_elm_translation.exceptions.CqlFormatException;
import gov.cms.mat.cql_elm_translation.service.CqlConversionService;
import gov.cms.mat.cql_elm_translation.service.DataCriteriaService;
import gov.cms.mat.cql_elm_translation.utils.cql.CQLFilter;
import gov.cms.mat.cql_elm_translation.utils.cql.parsing.CqlParserListener;
import gov.cms.mat.cql_elm_translation.utils.cql.parsing.model.CQLModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.LibraryBuilder;
import org.cqframework.cql.cql2elm.model.CompiledLibrary;
import org.cqframework.cql.tools.formatter.CqlFormatterVisitor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.w3._1999.xhtml.Q;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CqlToolsController {

  private final CqlConversionService cqlConversionService;
  private final DataCriteriaService dataCriteriaService;

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

  @GetMapping("/cql/dataCriteria")
  public ResponseEntity<Map<String,Set<String>>> getDataCriteria(
      @RequestBody String cql,
      @RequestHeader("Authorization") String accessToken) {

    //Run Translator to compile libraries
    RequestData requestData =
        RequestData.builder()
            .cqlData(cql)
            .showWarnings(false)
            .signatures(LibraryBuilder.SignatureLevel.All)
            .annotations(true)
            .locators(true)
            .disableListDemotion(true)
            .disableListPromotion(true)
            .disableMethodInvocation(false)
            .validateUnits(true)
            .resultTypes(true)
            .build();

    MadieLibrarySourceProvider librarySourceProvider = new MadieLibrarySourceProvider();
    cqlConversionService.setUpLibrarySourceProvider(cql, accessToken);
    CqlTranslator cqlTranslator = cqlConversionService.processCqlData(requestData);

    Map<String, String> includedLibrariesCql = new HashMap<>();
    for (CompiledLibrary l : cqlTranslator.getTranslatedLibraries().values()) {
      try {
        includedLibrariesCql.putIfAbsent(
            l.getIdentifier().getId() +"-"+l.getIdentifier().getVersion(),
            new String(
                librarySourceProvider
                    .getLibrarySource(l.getLibrary().getIdentifier())
                    .readAllBytes(),
                StandardCharsets.UTF_8));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    return ResponseEntity.ok(
        dataCriteriaService.parseDataCriteriaFromCql(cql, includedLibrariesCql, cqlTranslator));
  }
}
