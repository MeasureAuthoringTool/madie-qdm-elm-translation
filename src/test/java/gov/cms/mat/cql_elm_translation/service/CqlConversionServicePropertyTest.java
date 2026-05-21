package gov.cms.mat.cql_elm_translation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import gov.cms.mat.cql.dto.CqlConversionPayload;
import gov.cms.mat.cql_elm_translation.ResourceFileUtil;
import gov.cms.madie.cql_elm_translator.utils.cql.data.RequestData;
import org.cqframework.cql.cql2elm.CqlCompilerException;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.LibraryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CqlConversionServicePropertyTest implements ResourceFileUtil {
  CqlConversionService cqlConversionService = new CqlConversionService(null);

  String cqlData;
  LibraryBuilder.SignatureLevel signatureLevel;
  Boolean annotations;
  Boolean locators;
  Boolean disableListDemotion;
  Boolean disableListPromotion;
  Boolean disableMethodInvocation;
  Boolean validateUnits;
  Boolean resultTypes;

  @BeforeEach
  public void setUp() {
    cqlData = getData("/fhir.cql");
    annotations = Boolean.TRUE;
    locators = Boolean.TRUE;
    disableListDemotion = Boolean.TRUE;
    disableListPromotion = Boolean.TRUE;
    disableMethodInvocation = Boolean.TRUE;
    validateUnits = Boolean.TRUE;
    resultTypes = Boolean.TRUE;

    cqlConversionService = new CqlConversionService(null);
  }

  @Test
  void process_Good() {
    CqlTranslator cqlTranslator = buildCqlTranslator();
    assertTrue(cqlTranslator.getErrors().isEmpty());
    assertFalse(cqlTranslator.toJson().contains("CqlToElmError"));
  }

  @Disabled
  void process_SignatureLevelNone() {
    String jsonDefault = getJson();

    signatureLevel = LibraryBuilder.SignatureLevel.None;

    String jsonSignatureLevelNone = getJson();

    // NO change expected null signatureLevel and LibraryBuilder.SignatureLevel.None behave the same
    assertEquals(jsonDefault, jsonSignatureLevelNone);
  }

  @Disabled
  void process_SignatureLevelAll() {
    String jsonDefault = getJson();

    signatureLevel = LibraryBuilder.SignatureLevel.All;

    String jsonSignatureLevelNone = getJson();

    assertEquals(jsonDefault, jsonSignatureLevelNone); // NO change TODO not expected
  }

  @Test
  void process_annotations() {
    String jsonDefault = getJson();

    annotations = Boolean.FALSE;

    String jsonAnnotations = getJson();

    assertNotEquals(jsonDefault, jsonAnnotations); // data changed
  }

  @Test
  void process_locators() throws JsonProcessingException {
    String jsonDefault = getJson();
    assertTrue(containsField(new ObjectMapper().readTree(jsonDefault), "locator"));

    locators = Boolean.FALSE;

    String jsonSignatureLevelNone = getJson();
    assertFalse(containsField(new ObjectMapper().readTree(jsonSignatureLevelNone), "locator"));

    assertNotEquals(jsonDefault, jsonSignatureLevelNone); // data changed
  }

  @Test
  void process_validateUnits() {
    String jsonDefault = getJson();

    validateUnits = Boolean.FALSE;

    String jsonAnnotations = getJson();

    assertEquals(jsonDefault, jsonAnnotations); // NO change TODO not expected
  }

  @Test
  void testProcessCqlDataWithErrors() throws JsonProcessingException {
    cqlData = getData("/cv_populations.cql");
    RequestData requestData = buildRequestData();
    CqlConversionPayload cqlConversionPayload = cqlConversionService.translateCqlToElm(requestData);
    String elmJson = cqlConversionPayload.getJson();
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode rootNode = objectMapper.readTree(elmJson);
    ArrayNode defines = (ArrayNode) rootNode.get("library").get("statements").get("def");

    // initial population
    JsonNode ipNode = findCqlDefinitionNode("Initial Population", defines);
    assertEquals(ipNode.get("resultTypeSpecifier").get("type").asText(), "ListTypeSpecifier");
    assertEquals(
        ipNode.get("resultTypeSpecifier").get("elementType").get("name").asText(),
        "{http://hl7.org/fhir}Encounter");

    // Measure Population Exclusions
    JsonNode mpeNode = findCqlDefinitionNode("Measure Population Exclusions", defines);
    assertEquals(mpeNode.get("resultTypeSpecifier").get("type").asText(), "ListTypeSpecifier");
    assertEquals(
        mpeNode.get("resultTypeSpecifier").get("elementType").get("name").asText(),
        "{http://hl7.org/fhir}Encounter");

    // Boolean define
    JsonNode booleanNode = findCqlDefinitionNode("Unused Boolean Definition", defines);
    assertEquals(booleanNode.get("resultTypeName").asText(), "{urn:hl7-org:elm-types:r1}Boolean");

    // Integer type for function
    JsonNode moNode = findCqlDefinitionNode("Measure Observation", defines);
    assertEquals(moNode.get("resultTypeName").asText(), "{urn:hl7-org:elm-types:r1}Integer");
  }

  private JsonNode findCqlDefinitionNode(String cqlDefinition, ArrayNode defines) {
    Iterator<JsonNode> definitionIterator = defines.iterator();
    while (definitionIterator.hasNext()) {
      JsonNode node = definitionIterator.next();
      if (node.get("name").asText().contains(cqlDefinition)) {
        return node;
      }
    }
    return null;
  }

  private boolean containsField(JsonNode node, String fieldName) {
    if (node == null) {
      return false;
    }

    if (node.isObject()) {
      if (node.has(fieldName)) {
        return true;
      }

      Iterator<JsonNode> fields = node.elements();
      while (fields.hasNext()) {
        if (containsField(fields.next(), fieldName)) {
          return true;
        }
      }
    } else if (node.isArray()) {
      for (JsonNode child : node) {
        if (containsField(child, fieldName)) {
          return true;
        }
      }
    }

    return false;
  }

  private String getJson() {
    CqlTranslator cqlTranslator = buildCqlTranslator();

    assertTrue(cqlTranslator.getErrors().isEmpty());

    return cqlTranslator.toJson();
  }

  private CqlTranslator buildCqlTranslator() {
    RequestData requestData = buildRequestData();
    return cqlConversionService.processCqlData(requestData);
  }

  private RequestData buildRequestData() {
    return RequestData.builder()
        .cqlData(cqlData)
        .errorSeverity(CqlCompilerException.ErrorSeverity.Info)
        .signatures(signatureLevel)
        .annotations(annotations)
        .locators(locators)
        .disableListDemotion(disableListDemotion)
        .disableListPromotion(disableListPromotion)
        .disableMethodInvocation(disableMethodInvocation)
        .validateUnits(validateUnits)
        .resultTypes(resultTypes)
        .build();
  }
}
