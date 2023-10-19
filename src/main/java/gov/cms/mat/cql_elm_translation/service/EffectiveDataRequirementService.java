package gov.cms.mat.cql_elm_translation.service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.cqframework.cql.cql2elm.CqlCompilerOptions;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.model.CompiledLibrary;
import org.cqframework.cql.elm.requirements.fhir.DataRequirementsProcessor;
import org.hl7.fhir.convertors.advisors.impl.BaseAdvisor_40_50;
import org.hl7.fhir.convertors.conv40_50.VersionConvertor_40_50;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import gov.cms.mat.cql_elm_translation.cql_translator.TranslationResource;
import gov.cms.mat.cql_elm_translation.data.RequestData;
import gov.cms.mat.cql_elm_translation.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EffectiveDataRequirementService {
  private final FhirContext fhirContext;
  private final FhirContext fhirContextForR5;

  private final CqlConversionService cqlConversionService;

  public <T extends Resource> T createFhirResourceFromJson(String json, Class<T> clazz) {
    if (StringUtils.isEmpty(json)) {
      return null;
    }
    return getR4Parser().parseResource(clazz, json);
  }

  protected IParser getR4Parser() {
    return fhirContext.newJsonParser();
  }

  private RequestData createDefaultRequestData(String identifier, String cql) {
    return RequestData.builder()
        .cqlData(cql)
        .identifier(identifier)
        .showWarnings(false)
        .annotations(true)
        .locators(true)
        .disableListDemotion(true)
        .disableListPromotion(true)
        .disableMethodInvocation(false)
        .validateUnits(true)
        .resultTypes(true)
        .build();
  }

  /**
   * @param bundleResource Bundle resource
   * @return BundleEntry which is of type Measure
   */
  public Optional<Bundle.BundleEntryComponent> getMeasureEntry(Bundle bundleResource) {
    return bundleResource.getEntry().stream()
        .filter(
            entry ->
                StringUtils.equalsIgnoreCase(
                    "Measure", entry.getResource().getResourceType().toString()))
        .findFirst();
  }

  /**
   * @param r5Measure retrieved from measure bundle
   * @param library measure library
   * @param accessToken used by MadieLibrarySourceProvider to make calls to madie-fhir-services
   * @return effective data requirement of type R5 library
   */
  public org.hl7.fhir.r5.model.Library getEffectiveDataRequirements(
      org.hl7.fhir.r5.model.Measure r5Measure, Library library, String accessToken) {
    Attachment attachment =
        library.getContent().stream()
            .filter(content -> "text/cql".equals(content.getContentType()))
            .findFirst()
            .orElse(null);

    if (attachment == null) {
      log.error("Unable to find CQL text in library resource for library {} ", library.getId());
      throw new ResourceNotFoundException("Library", library.getId());
    }
    String cql = new String(attachment.getData());

    // setting up the librarySourceProvider to fetch included libraries
    cqlConversionService.setUpLibrarySourceProvider(cql, accessToken);

    var translationResource = TranslationResource.getInstance(true);
    RequestData requestData = createDefaultRequestData(library.getId(), cql);
    CqlTranslator cqlTranslator = translationResource.buildTranslator(requestData);
    CompiledLibrary translatedLibrary = cqlTranslator.getTranslatedLibrary();
    LibraryManager libraryManager = translationResource.getLibraryManager();

    // providing compiled measureLibrary, as it cannot be fetched using
    // LibrarySourceProvider ( we
    // are not storing measure libraries in HAPI)

    // MAT-6402:  Following line was commented out because cacheLibrary was unceremoniously
    //  removed from the LibraryManager class.. it appears that it can be replaced by resolveLibrary
    // ¯\_(ツ)_/¯
    //  libraryManager.cacheLibrary(translatedLibrary);
    // libraryManager.resolveLibrary(requestData.getSourceInfo());

    Set<String> expressionList = getExpressions(r5Measure);
    var dqReqTrans = new DataRequirementsProcessor();
    CqlCompilerOptions options = CqlCompilerOptions.defaultOptions();
    options.setCollapseDataRequirements(true); // removing duplicate data requirements

    org.hl7.fhir.r5.model.Library effectiveDataRequirements =
        dqReqTrans.gatherDataRequirements(
            libraryManager, translatedLibrary, options, expressionList, true);

    effectiveDataRequirements.setId("effective-data-requirements");
    return effectiveDataRequirements;
  }

  private Set<String> getExpressions(org.hl7.fhir.r5.model.Measure r5Measure) {
    Set<String> expressionSet = new HashSet<>();
    r5Measure
        .getSupplementalData()
        .forEach(supData -> expressionSet.add(supData.getCriteria().getExpression()));
    r5Measure
        .getGroup()
        .forEach(
            groupMember -> {
              groupMember
                  .getPopulation()
                  .forEach(
                      population -> expressionSet.add(population.getCriteria().getExpression()));
              groupMember
                  .getStratifier()
                  .forEach(
                      stratifier -> expressionSet.add(stratifier.getCriteria().getExpression()));
            });
    return expressionSet;
  }

  public Optional<Bundle.BundleEntryComponent> getMeasureLibraryEntry(
      Bundle bundleResource, String libraryName) {

    return bundleResource.getEntry().stream()
        .filter(
            entry ->
                StringUtils.equalsIgnoreCase(
                        "Library", entry.getResource().getResourceType().toString())
                    && StringUtils.equalsIgnoreCase(libraryName, entry.getResource().getIdPart()))
        .findFirst();
  }

  public org.hl7.fhir.r5.model.Measure getR5MeasureFromR4MeasureResource(Resource measureResource) {
    var versionConvertor_40_50 = new VersionConvertor_40_50(new BaseAdvisor_40_50());
    return (org.hl7.fhir.r5.model.Measure) versionConvertor_40_50.convertResource(measureResource);
  }

  public String getEffectiveDataRequirementsStr(org.hl7.fhir.r5.model.Library r5Library) {
    return getR5Parser().setPrettyPrint(true).encodeResourceToString(r5Library);
  }

  protected IParser getR5Parser() {
    return fhirContextForR5.newJsonParser();
  }
}
