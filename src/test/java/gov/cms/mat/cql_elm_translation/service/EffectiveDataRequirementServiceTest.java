package gov.cms.mat.cql_elm_translation.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.JsonParser;
import gov.cms.madie.cql_elm_translator.dto.CqlLibraryDetails;
import gov.cms.madie.cql_elm_translator.service.CqlLibraryService;
import gov.cms.madie.cql_elm_translator.utils.ResourceUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

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
}
