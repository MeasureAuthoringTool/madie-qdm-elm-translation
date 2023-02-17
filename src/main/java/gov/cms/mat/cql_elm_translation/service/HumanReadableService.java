package gov.cms.mat.cql_elm_translation.service;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.madie.models.measure.Measure;
import gov.cms.mat.cql_elm_translation.cql_translator.TranslationResource;

import gov.cms.mat.cql_elm_translation.data.RequestData;
import gov.cms.mat.cql_elm_translation.exceptions.HumanReadableGenerationException;
import gov.cms.mat.cql_elm_translation.exceptions.ResourceNotFoundException;
import gov.cms.mat.cql_elm_translation.utils.ResourceUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.model.CompiledLibrary;
import org.cqframework.cql.elm.requirements.fhir.DataRequirementsProcessor;
import org.hl7.fhir.convertors.advisors.impl.BaseAdvisor_40_50;
import org.hl7.fhir.convertors.conv40_50.VersionConvertor_40_50;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r5.model.Extension;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r5.test.utils.TestingUtilities;
import org.hl7.fhir.r5.utils.LiquidEngine;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HumanReadableService extends ResourceUtils {
  private final FhirContext fhirContext;
  private final MadieFhirServices madieFhirServices;

  private final CqlConversionService cqlConversionService;

  private static final String EFFECTIVE_DATA_REQUIREMENT_URL =
      "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-effectiveDataRequirements";

  public String generateHumanReadable(Measure madieMeasure, String accessToken) {
    String fhirMeasureBundle = madieFhirServices.getFhirMeasureBundle(madieMeasure, accessToken);
    Bundle bundleResource = createFhirResourceFromJson(fhirMeasureBundle, Bundle.class);
    if (bundleResource != null) {
      if (CollectionUtils.isEmpty(bundleResource.getEntry())) {
        log.error("Unable to find bundle entry for measure {}", madieMeasure.getId());
        throw new ResourceNotFoundException("bundle entry", madieMeasure.getId());
      }
      try {
        Optional<Bundle.BundleEntryComponent> measureEntry = getMeasureEntry(bundleResource);
        if (measureEntry.isEmpty()) {
          log.error("Unable to find measure entry for measure {}", madieMeasure.getId());
          throw new ResourceNotFoundException("measure entry", madieMeasure.getId());
        }
        Resource measureResource = measureEntry.get().getResource();

        Optional<Bundle.BundleEntryComponent> measureLibraryEntry =
            getMeasureLibraryEntry(bundleResource, madieMeasure);
        if (measureLibraryEntry.isEmpty()) {
          log.error("Unable to find library entry for measure {}", madieMeasure.getId());
          throw new ResourceNotFoundException("library entry", madieMeasure.getId());
        }
        Library library = (Library) measureLibraryEntry.get().getResource();

        // converting measure resource from R4 to R5
        var versionConvertor_40_50 = new VersionConvertor_40_50(new BaseAdvisor_40_50());
        org.hl7.fhir.r5.model.Measure r5Measure =
            (org.hl7.fhir.r5.model.Measure) versionConvertor_40_50.convertResource(measureResource);

        org.hl7.fhir.r5.model.Library effectiveDataRequirements =
            getEffectiveDataRequirements(r5Measure, library, accessToken);
        r5Measure.addContained(effectiveDataRequirements);
        r5Measure.getExtension().add(createExtension());

        String measureTemplate = getData("/templates/Measure.liquid");
        // TODO: Need to write our own implementation of TestingUtilities.getWorkerContext
        LiquidEngine engine = new LiquidEngine(TestingUtilities.context(), null);
        LiquidEngine.LiquidDocument doc = engine.parse(measureTemplate, "hr-script");
        String measureHr = engine.evaluate(doc, r5Measure, null);
        String humanReadable = getData("/templates/HumanReadable.liquid");
        return humanReadable.replace("human_readable_content_holder", measureHr);
      } catch (FHIRException fhirException) {
        log.error(
            "Unable to generate Human readable for measure {} Reason => {}",
            madieMeasure.getId(),
            fhirException);
        throw new HumanReadableGenerationException("measure", madieMeasure.getId());
      }
    } else {
      log.error("Unable to find a bundleResource for measure {}", madieMeasure.getId());
      throw new ResourceNotFoundException("bundle", madieMeasure.getId());
    }
  }

  private <T extends Resource> T createFhirResourceFromJson(String json, Class<T> clazz) {
    if (StringUtils.isEmpty(json)) {
      return null;
    }
    return fhirContext.newJsonParser().parseResource(clazz, json);
  }

  private RequestData createDefaultRequestData() {
    return RequestData.builder()
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
  private Optional<Bundle.BundleEntryComponent> getMeasureEntry(Bundle bundleResource) {
    return bundleResource.getEntry().stream()
        .filter(
            entry ->
                StringUtils.equalsIgnoreCase(
                    "Measure", entry.getResource().getResourceType().toString()))
        .findFirst();
  }

  /**
   * @param bundleResource Bundle resource
   * @param madieMeasure madie measure
   * @return BundleEntry which is a Measure library
   */
  private Optional<Bundle.BundleEntryComponent> getMeasureLibraryEntry(
      Bundle bundleResource, Measure madieMeasure) {
    return bundleResource.getEntry().stream()
        .filter(
            entry ->
                StringUtils.equalsIgnoreCase(
                        "Library", entry.getResource().getResourceType().toString())
                    && StringUtils.equalsIgnoreCase(
                        "Library/" + madieMeasure.getCqlLibraryName(), entry.getResource().getId()))
        .findFirst();
  }

  /**
   * @param r5Measure retrieved from measure bundle
   * @param library measure library
   * @param accessToken used by MadieLibrarySourceProvider to make calls to madie-fhir-services
   * @return effective data requirement of type R5 library
   */
  private org.hl7.fhir.r5.model.Library getEffectiveDataRequirements(
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
    CqlTranslator cqlTranslator =
        translationResource.buildTranslator(
            new ByteArrayInputStream(cql.getBytes()), createDefaultRequestData().createMap());
    CompiledLibrary translatedLibrary = cqlTranslator.getTranslatedLibrary();
    LibraryManager libraryManager = translationResource.getLibraryManager();

    // providing compiled measureLibrary, as it cannot be fetched using LibrarySourceProvider ( we
    // are not storing measure libraries in HAPI)
    libraryManager.cacheLibrary(translatedLibrary);

    Set<String> expressionList = getExpressions(r5Measure);
    var dqReqTrans = new DataRequirementsProcessor();
    CqlTranslatorOptions options = CqlTranslatorOptions.defaultOptions();

    org.hl7.fhir.r5.model.Library effectiveDataRequirements =
        dqReqTrans.gatherDataRequirements(
            libraryManager, translatedLibrary, options, expressionList, true);
    effectiveDataRequirements.setContent(createAttachment(library.getContent()));
    effectiveDataRequirements.setId("effective-data-requirements");
    return effectiveDataRequirements;
  }

  private Extension createExtension() {
    var extension = new Extension();
    extension.setUrl(EFFECTIVE_DATA_REQUIREMENT_URL);
    extension.getValueReference().setReference("#effective-data-requirements");
    return extension;
  }

  private List<org.hl7.fhir.r5.model.Attachment> createAttachment(List<Attachment> r4attachments) {
    return r4attachments.stream()
        .map(
            attachment ->
                new org.hl7.fhir.r5.model.Attachment()
                    .setContentType(attachment.getContentType())
                    .setData(attachment.getData()))
        .collect(Collectors.toList());
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
}
