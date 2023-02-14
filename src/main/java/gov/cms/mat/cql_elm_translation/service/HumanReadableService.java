package gov.cms.mat.cql_elm_translation.service;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.madie.models.measure.Measure;
import gov.cms.mat.cql_elm_translation.exceptions.HumanReadableGenerationException;
import gov.cms.mat.cql_elm_translation.exceptions.ResourceNotFoundException;
import gov.cms.mat.cql_elm_translation.utils.ResourceUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.convertors.advisors.impl.BaseAdvisor_40_50;
import org.hl7.fhir.convertors.conv40_50.VersionConvertor_40_50;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r5.test.utils.TestingUtilities;
import org.hl7.fhir.r5.utils.LiquidEngine;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HumanReadableService {
  private final FhirContext fhirContext;
  private final MadieFhirServices madieFhirServices;

  public String generateHumanReadable(Measure madieMeasure, String accessToken) {
    String fhirMeasureBundle = madieFhirServices.getFhirMeasureBundle(madieMeasure, accessToken);
    Bundle bundleResource = createFhirResourceFromJson(fhirMeasureBundle, Bundle.class);
    if (bundleResource != null) {
      if (CollectionUtils.isEmpty(bundleResource.getEntry())) {
        log.error("Unable to find bundle entry for measure {}", madieMeasure.getId());
        throw new ResourceNotFoundException("Measure", madieMeasure.getId());
      }
      try {
        Optional<Bundle.BundleEntryComponent> bundleEntryComponent =
            bundleResource.getEntry().stream()
                .filter(
                    entry ->
                        StringUtils.equalsIgnoreCase(
                            "Measure", entry.getResource().getResourceType().toString()))
                .findFirst();
        if (bundleEntryComponent.isEmpty()) {
          log.error("Unable to find BundleEntryComponent for measure {}", madieMeasure.getId());
          throw new ResourceNotFoundException("Measure", madieMeasure.getId());
        }
        Resource measureResource = bundleEntryComponent.get().getResource();

        // converting measure resource from R4 to R5
        VersionConvertor_40_50 versionConvertor_40_50 =
            new VersionConvertor_40_50(new BaseAdvisor_40_50());
        org.hl7.fhir.r5.model.Measure r5Measure =
            (org.hl7.fhir.r5.model.Measure) versionConvertor_40_50.convertResource(measureResource);
        String template = ResourceUtils.getData("/templates/Measure.liquid");
        // TODO: Need to write our own implementation of TestingUtilities.getWorkerContext
        LiquidEngine engine = new LiquidEngine(TestingUtilities.context(), null);
        LiquidEngine.LiquidDocument doc = engine.parse(template, "hr-script");
        return engine.evaluate(doc, r5Measure, null);
      } catch (FHIRException fhirException) {
        log.error(
            "Unable to generate Human readable for measure {} Reason => {}",
            madieMeasure.getId(),
            fhirException);
        throw new HumanReadableGenerationException("Measure", madieMeasure.getId());
      }
    } else {
      log.error("Unable to find a bundleResource for measure {}", madieMeasure.getId());
      throw new ResourceNotFoundException("Bundle", madieMeasure.getId());
    }
  }

  private <T extends Resource> T createFhirResourceFromJson(String json, Class<T> clazz) {
    if (StringUtils.isEmpty(json)) {
      return null;
    }
    return fhirContext.newJsonParser().parseResource(clazz, json);
  }
}
