package gov.cms.mat.cql_elm_translation.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.cms.mat.cql.dto.CqlConversionPayload;
import gov.cms.mat.cql_elm_translation.service.CqlConversionService;
import gov.cms.madie.cql_elm_translator.utils.cql.data.RequestData;
import gov.cms.madie.cql_elm_translator.service.CqlLibraryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cqframework.cql.cql2elm.LibraryBuilder;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.Objects;

@RestController
@RequestMapping(path = "/cql/translator")
@Tag(name = "Conversion-Controller", description = "API for converting CQL to ELM.")
@Slf4j
@RequiredArgsConstructor
public class CqlConversionController {

  private final CqlConversionService cqlConversionService;
  private final CqlLibraryService cqlLibraryService;

  @PutMapping(path = "/cql", consumes = "text/plain", produces = "application/elm+json")
  public CqlConversionPayload cqlToElmJson(
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
      @RequestHeader("Authorization") String accessToken) {

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
    cqlLibraryService.setUpLibrarySourceProvider(cqlData, accessToken);

    CqlConversionPayload cqlConversionPayload =
        cqlConversionService.processCqlDataWithErrors(requestData);
    // Todo Do we need to remove empty annotations from library object, Also why are we removing
    // translatorOptions from annotations, Could be MAT specific.
    TranslatorOptionsRemover remover = new TranslatorOptionsRemover(cqlConversionPayload.getJson());
    String cleanedJson = remover.clean();
    cqlConversionPayload.setJson(cleanedJson);
    return cqlConversionPayload;
  }

  /**
   * Removes this node, when present, which blows up array processing for annotation array. {
   * "translatorOptions":
   * "DisableMethodInvocation,EnableLocators,DisableListPromotion,EnableDetailedErrors,
   * EnableAnnotations,DisableListDemotion", "type": "CqlToElmInfo" },
   */
  static class TranslatorOptionsRemover {
    final String json;

    TranslatorOptionsRemover(String json) {
      this.json = json;
    }

    String clean() {

      try {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(json);
        JsonNode libraryNode = rootNode.get("library");
        JsonNode annotationNode = libraryNode.get("annotation");

        if (annotationNode == null || annotationNode.isMissingNode()) {
          return json;
        }

        if (annotationNode.isEmpty() && libraryNode instanceof ObjectNode) {
          ObjectNode objectNode = (ObjectNode) libraryNode;
          objectNode.remove("annotation");
          return rootNode.toPrettyString();
        }

        for (int i = 0; i < annotationNode.size(); i++) {
          // remove translator options that are not the version
          if (annotationNode.get(i).has("translatorOptions")) {
            Iterator<String> fieldNames = annotationNode.get(i).fieldNames();
            while (fieldNames.hasNext()) {
              if (!Objects.equals(fieldNames.next(), "translatorVersion")) {
                fieldNames.remove();
              }
            }
          }
        }

        return rootNode.toPrettyString();

      } catch (JsonProcessingException e) {
        throw new UncheckedIOException(e);
      }
    }
  }
}
