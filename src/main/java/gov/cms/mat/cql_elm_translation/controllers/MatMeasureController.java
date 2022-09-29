package gov.cms.mat.cql_elm_translation.controllers;

import javax.servlet.http.HttpServletRequest;

import org.cqframework.cql.cql2elm.LibraryBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gov.cms.mat.cql.dto.CqlConversionPayload;
import gov.cms.mat.cql_elm_translation.controllers.CqlConversionController.TranslatorOptionsRemover;
import gov.cms.mat.cql_elm_translation.data.RequestData;
import gov.cms.mat.cql_elm_translation.service.CqlConversionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping(path = "/mat/translator")
@Tag(name = "Transfer-Translator", description = "API for translating MAT transferred measure CQL to ELM.")
@Slf4j
@RequiredArgsConstructor
public class MatMeasureController {

  private final CqlConversionService cqlConversionService;

  @PutMapping(path = "/cqlToElm", consumes = "text/plain", produces = "application/elm+json")
  @PreAuthorize("#request.getHeader('api-key') == #apiKey")
  public CqlConversionPayload cqlToElmJsonForMatTransferredMeasure(
      @RequestBody String cqlData,
      @RequestParam(required = false) LibraryBuilder.SignatureLevel signatures,
      @RequestParam(defaultValue = "false") Boolean showWarnings,
      @RequestParam(defaultValue = "true") Boolean annotations,
      @RequestParam(defaultValue = "true") Boolean locators,
      @RequestParam(value = "disable-list-demotion", defaultValue = "true")
          Boolean disableListDemotion,
      @RequestParam(value = "disable-list-promotion", defaultValue = "true")
          Boolean disableListPromotion,
      @RequestParam(value = "disable-method-invocation", defaultValue = "false")
          Boolean disableMethodInvocation,
      @RequestParam(value = "validate-units", defaultValue = "true") Boolean validateUnits,
      @RequestParam(value = "result-types", defaultValue = "true") Boolean resultTypes,
      HttpServletRequest request,
      @Value("${lambda-api-key}") String apiKey) {

    log.debug("Entering cqlToElmJsonForMatTransferredMeasure()");
    String apikey = request.getHeader("api-key");
    String harpid = request.getHeader("harp-id");
    if (apikey == null || harpid == null || !apiKey.equals(request.getHeader("api-key"))) {
      return CqlConversionPayload.builder()
          .json("{\"errorExceptions\": [{\"Error\":\"UNAUTHORIZED\"}]}")
          .build();
    }

    RequestData requestData =
        RequestData.builder()
            .cqlData(cqlData)
            .showWarnings(showWarnings)
            .signatures(signatures)
            .annotations(annotations)
            .locators(locators)
            .disableListDemotion(disableListDemotion)
            .disableListPromotion(disableListPromotion)
            .disableMethodInvocation(disableMethodInvocation)
            .validateUnits(validateUnits)
            .resultTypes(resultTypes)
            .build();

    cqlConversionService.setUpLibrarySourceProvider(cqlData, apiKey.concat("-" + harpid));

    CqlConversionPayload cqlConversionPayload =
        cqlConversionService.processCqlDataWithErrors(requestData);
    // Todo Do we need to remove empty annotations from library object, Also why are we removing
    // translatorOptions from annotations, Could be MAT specific.
    TranslatorOptionsRemover remover = new TranslatorOptionsRemover(cqlConversionPayload.getJson());
    String cleanedJson = remover.clean();
    cqlConversionPayload.setJson(cleanedJson);
    return cqlConversionPayload;
  }
}
