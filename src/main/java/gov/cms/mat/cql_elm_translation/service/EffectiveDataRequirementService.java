package gov.cms.mat.cql_elm_translation.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import gov.cms.madie.cql_elm_translator.service.CqlLibraryService;
import gov.cms.madie.cql_elm_translator.utils.cql.data.RequestData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EffectiveDataRequirementService {
  private final FhirContext fhirContextForR5;
  private final CqlLibraryService cqlLibraryService;

  private RequestData createDefaultRequestData(String cql) {
    return RequestData.builder()
        .cqlData(cql)
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

  public String getEffectiveDataRequirementsStr(org.hl7.fhir.r5.model.Library r5Library) {
    return getR5Parser().setPrettyPrint(true).encodeResourceToString(r5Library);
  }

  protected IParser getR5Parser() {
    return fhirContextForR5.newJsonParser();
  }
}
