package gov.cms.mat.cql_elm_translation.service;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.mat.cql_elm_translation.utils.ResourceUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.convertors.advisors.impl.BaseAdvisor_40_50;
import org.hl7.fhir.convertors.conv40_50.VersionConvertor_40_50;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r5.test.utils.TestingUtilities;
import org.hl7.fhir.r5.utils.LiquidEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class HumanReadableService {
  @Autowired private FhirContext fhirContext;

  public String generateHumanReadable() {
    // 1. call madie-fhir-service to get the measure bundle
    // 2. remove hardcoded liquid_measure.json and use bundle from madie-service
    String fhirMeasureJson = ResourceUtils.getData("/templates/liquid_measure.json");
    org.hl7.fhir.r4.model.Measure measure =
        createFhirResourceFromJson(fhirMeasureJson, org.hl7.fhir.r4.model.Measure.class);
    VersionConvertor_40_50 versionConvertor_40_50 =
        new VersionConvertor_40_50(new BaseAdvisor_40_50());
    org.hl7.fhir.r5.model.Measure r5Measure =
        (org.hl7.fhir.r5.model.Measure) versionConvertor_40_50.convertResource(measure);
    String template = ResourceUtils.getData("/templates/Measure.liquid");
    // TODO: Need to write our own implementation of TestingUtilities.getWorkerContext
    LiquidEngine engine = new LiquidEngine(TestingUtilities.context(), null);
    LiquidEngine.LiquidDocument doc = engine.parse(template, "hr-script");
    return engine.evaluate(doc, r5Measure, null);
  }

  public <T extends Resource> T createFhirResourceFromJson(String json, Class<T> clazz) {
    if (StringUtils.isEmpty(json)) {
      return null;
    }
    return fhirContext.newJsonParser().parseResource(clazz, json);
  }
}
