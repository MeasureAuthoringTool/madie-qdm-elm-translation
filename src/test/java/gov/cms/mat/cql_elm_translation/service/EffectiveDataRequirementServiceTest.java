package gov.cms.mat.cql_elm_translation.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.JsonParser;
import gov.cms.madie.cql_elm_translator.dto.CqlLibraryDetails;
import gov.cms.mat.cql.CqlTextParser;
import gov.cms.madie.cql_elm_translator.service.CqlLibraryService;
import gov.cms.madie.cql_elm_translator.utils.cql.cql_translator.MadieLibrarySourceProvider;
import gov.cms.madie.cql_elm_translator.utils.ResourceUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EffectiveDataRequirementServiceTest {

  @Mock FhirContext fhirContextForR5;
  @Mock CqlLibraryService cqlLibraryService;
  @Mock JsonParser r5Parser;
  @Mock JsonParser jsonParserPrettier;

  @InjectMocks EffectiveDataRequirementService effectiveDataRequirementService;

  private final String testAccessToken = "test_access_token";
  private static final String CQL_LIBRARY_NAME = "test_cql_library_name";
  private CqlLibraryDetails cqlLibraryDetails;

  @BeforeEach
  void setUp() {
    String cqlData = ResourceUtils.getData("/cv_populations.cql");
    cqlLibraryDetails =
        CqlLibraryDetails.builder()
            .libraryName(CQL_LIBRARY_NAME)
            .cql(cqlData)
            .expressions(Set.of("Initial Population"))
            .build();
  }

  @Test
  public void testGetEffectiveDataRequirementsSuccess() {
    MadieLibrarySourceProvider.setUsing(new CqlTextParser(cqlLibraryDetails.getCql()).getUsing());

    String fhirHelperString = ResourceUtils.getData("/fhirhelpers.cql");
    String suppDataString = ResourceUtils.getData("/SupplementalDataElements.cql");
    String cqlLibrary = ResourceUtils.getData("/cv_populations.cql");

    doReturn(fhirHelperString)
        .when(cqlLibraryService)
        .getLibraryCql(eq("FHIRHelpers"), eq("4.0.001"), nullable(String.class));

    doReturn(suppDataString)
        .when(cqlLibraryService)
        .getLibraryCql(eq("SupplementalDataElementsFHIR4"), eq("4.0.001"), nullable(String.class));

    doReturn(cqlLibrary)
        .when(cqlLibraryService)
        .getLibraryCql(eq("TestCVPopulations"), nullable(String.class), nullable(String.class));

    MadieLibrarySourceProvider.setCqlLibraryService(cqlLibraryService);
    org.hl7.fhir.r5.model.Library r5Library =
        effectiveDataRequirementService.getEffectiveDataRequirements(
            cqlLibraryDetails, false, testAccessToken);
    assertEquals(r5Library.getId(), "effective-data-requirements");
  }

  @Test
  public void testGetEffectiveDataRequirementsStr() {
    MadieLibrarySourceProvider.setUsing(new CqlTextParser(cqlLibraryDetails.getCql()).getUsing());

    String fhirHelperString = ResourceUtils.getData("/fhirhelpers.cql");
    String suppDataString = ResourceUtils.getData("/SupplementalDataElements.cql");
    String cqlLibrary = ResourceUtils.getData("/cv_populations.cql");

    doReturn(fhirHelperString)
        .when(cqlLibraryService)
        .getLibraryCql(eq("FHIRHelpers"), eq("4.0.001"), nullable(String.class));

    doReturn(suppDataString)
        .when(cqlLibraryService)
        .getLibraryCql(eq("SupplementalDataElementsFHIR4"), eq("4.0.001"), nullable(String.class));

    doReturn(cqlLibrary)
        .when(cqlLibraryService)
        .getLibraryCql(eq("TestCVPopulations"), nullable(String.class), nullable(String.class));

    MadieLibrarySourceProvider.setCqlLibraryService(cqlLibraryService);
    org.hl7.fhir.r5.model.Library r5Library =
        effectiveDataRequirementService.getEffectiveDataRequirements(
            cqlLibraryDetails, false, testAccessToken);

    when(fhirContextForR5.newJsonParser()).thenReturn(r5Parser);
    when(r5Parser.setPrettyPrint(true)).thenReturn(jsonParserPrettier);
    when(jsonParserPrettier.encodeResourceToString(any())).thenReturn("test");

    String r5LibraryStr =
        effectiveDataRequirementService.getEffectiveDataRequirementsStr(r5Library);
    assertEquals(r5LibraryStr, "test");
  }
}
