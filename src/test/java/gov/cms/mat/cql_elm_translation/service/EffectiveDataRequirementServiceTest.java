package gov.cms.mat.cql_elm_translation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure.MeasureSupplementalDataComponent;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.parser.JsonParser;
import gov.cms.mat.cql.CqlTextParser;
import gov.cms.mat.cql_elm_translation.cql_translator.MadieLibrarySourceProvider;
import gov.cms.mat.cql_elm_translation.exceptions.ResourceNotFoundException;
import gov.cms.mat.cql_elm_translation.utils.ResourceUtils;

@ExtendWith(MockitoExtension.class)
class EffectiveDataRequirementServiceTest {

  @Mock FhirContext fhirContext;

  @Mock FhirContext fhirContextForR5;

  @Mock CqlLibraryService cqlLibraryService;

  @Mock JsonParser r4Parser;

  @Mock JsonParser r5Parser;

  @Mock JsonParser jsonParserPrettier;

  @Mock CqlConversionService cqlConversionService;

  @InjectMocks EffectiveDataRequirementService effectiveDataRequirementService;

  private org.hl7.fhir.r4.model.Measure measure;

  private Library library;

  private final String testAccessToken = "test_access_token";

  private static final String CQL_LIBRARY_NAME = "test_cql_library_name";

  private org.hl7.fhir.r4.model.Expression expression = new org.hl7.fhir.r4.model.Expression();
  private org.hl7.fhir.r4.model.Measure.MeasureGroupPopulationComponent population =
      new org.hl7.fhir.r4.model.Measure.MeasureGroupPopulationComponent();
  private org.hl7.fhir.r4.model.Measure.MeasureGroupStratifierComponent stratifier =
      new org.hl7.fhir.r4.model.Measure.MeasureGroupStratifierComponent();
  private org.hl7.fhir.r4.model.Measure.MeasureGroupComponent group =
      new org.hl7.fhir.r4.model.Measure.MeasureGroupComponent();
  private List<org.hl7.fhir.r4.model.Measure.MeasureGroupComponent> groups = new ArrayList<>();

  private MeasureSupplementalDataComponent supplementalData =
      new MeasureSupplementalDataComponent();
  private List<MeasureSupplementalDataComponent> supplementalDataList = new ArrayList<>();

  @BeforeEach
  void setUp() {

    expression.setExpression("Initial Population");
    population.setCriteria(expression);
    stratifier.setCriteria(expression);

    group.addPopulation(population);
    group.addStratifier(stratifier);
    groups.add(group);

    supplementalData.setCriteria(expression);
    supplementalDataList.add(supplementalData);

    measure =
        new org.hl7.fhir.r4.model.Measure()
            .setName(CQL_LIBRARY_NAME)
            .setTitle("test_measure_name")
            .setExperimental(true)
            .setUrl("fhirBaseUrl/Measure/" + CQL_LIBRARY_NAME)
            .setVersion("1.0.000")
            .setEffectivePeriod(getPeriodFromDates(new Date(), new Date()))
            .setCopyright("test_copyright")
            .setDisclaimer("test_disclaimer")
            .setGroup(groups)
            .setSupplementalData(supplementalDataList);

    String cqlData = ResourceUtils.getData("/cv_populations.cql");
    library =
        new Library()
            .addContent(new Attachment().setData(cqlData.getBytes()).setContentType("text/cql"));
    library.setId("Library/" + CQL_LIBRARY_NAME);
  }

  public Bundle.BundleEntryComponent getBundleEntryComponent(Resource resource) {
    return new Bundle.BundleEntryComponent().setResource(resource);
  }

  private Period getPeriodFromDates(Date startDate, Date endDate) {
    return new Period()
        .setStart(startDate, TemporalPrecisionEnum.DAY)
        .setEnd(endDate, TemporalPrecisionEnum.DAY);
  }

  @Test
  public void testCreateFhirResourceFromJsonReturnsNull() {
    Bundle result = effectiveDataRequirementService.createFhirResourceFromJson(null, null);
    assertNull(result);
  }

  @Test
  public void testCreateFhirResourceFromJsonSuccess() {

    Bundle.BundleEntryComponent measureBundleEntryComponent = getBundleEntryComponent(measure);
    Bundle.BundleEntryComponent libraryBundleEntryComponent = getBundleEntryComponent(library);
    Bundle bundle =
        new Bundle()
            .setType(Bundle.BundleType.TRANSACTION)
            .addEntry(measureBundleEntryComponent)
            .addEntry(libraryBundleEntryComponent);

    when(effectiveDataRequirementService.getR4Parser()).thenReturn(r4Parser);
    when(r4Parser.parseResource(any(), anyString())).thenReturn(bundle);

    Bundle result =
        effectiveDataRequirementService.createFhirResourceFromJson(
            "bundleResourceJson", Bundle.class);
    assertNotNull(result);
  }

  @Test
  public void testGetMeasureEntry() {
    Bundle.BundleEntryComponent measureBundleEntryComponent = getBundleEntryComponent(measure);
    Bundle.BundleEntryComponent libraryBundleEntryComponent = getBundleEntryComponent(library);
    Bundle bundle =
        new Bundle()
            .setType(Bundle.BundleType.TRANSACTION)
            .addEntry(measureBundleEntryComponent)
            .addEntry(libraryBundleEntryComponent);
    Optional<Bundle.BundleEntryComponent> result =
        effectiveDataRequirementService.getMeasureEntry(bundle);

    assertTrue(result.isPresent());
  }

  @Test
  public void testGetMeasureLibraryEntryNotFound() {
    Bundle.BundleEntryComponent measureBundleEntryComponent = getBundleEntryComponent(measure);
    Bundle.BundleEntryComponent libraryBundleEntryComponent = getBundleEntryComponent(library);
    Bundle bundle =
        new Bundle()
            .setType(Bundle.BundleType.TRANSACTION)
            .addEntry(measureBundleEntryComponent)
            .addEntry(libraryBundleEntryComponent);
    Optional<Bundle.BundleEntryComponent> result =
        effectiveDataRequirementService.getMeasureLibraryEntry(bundle, "anotherLibrary");

    assertTrue(result.isEmpty());
  }

  @Test
  public void testGetMeasureLibraryEntry() {
    Bundle.BundleEntryComponent measureBundleEntryComponent = getBundleEntryComponent(measure);
    Bundle.BundleEntryComponent libraryBundleEntryComponent = getBundleEntryComponent(library);
    Bundle bundle =
        new Bundle()
            .setType(Bundle.BundleType.TRANSACTION)
            .addEntry(measureBundleEntryComponent)
            .addEntry(libraryBundleEntryComponent);
    Optional<Bundle.BundleEntryComponent> result =
        effectiveDataRequirementService.getMeasureLibraryEntry(bundle, CQL_LIBRARY_NAME);
    Library library = (Library) result.get().getResource();

    assertTrue(result.isPresent());
    assertEquals("Library/" + CQL_LIBRARY_NAME, library.getId());
  }

  @Test
  public void testGetR5MeasureFromR4MeasureResource() {
    Bundle.BundleEntryComponent measureBundleEntryComponent = getBundleEntryComponent(measure);
    Bundle.BundleEntryComponent libraryBundleEntryComponent = getBundleEntryComponent(library);
    Bundle bundle =
        new Bundle()
            .setType(Bundle.BundleType.TRANSACTION)
            .addEntry(measureBundleEntryComponent)
            .addEntry(libraryBundleEntryComponent);

    Optional<Bundle.BundleEntryComponent> measureEntry =
        effectiveDataRequirementService.getMeasureEntry(bundle);
    Resource measureResource = measureEntry.get().getResource();

    org.hl7.fhir.r5.model.Measure r5Measure =
        effectiveDataRequirementService.getR5MeasureFromR4MeasureResource(measureResource);
    assertNotNull(r5Measure);
    assertEquals(r5Measure.getName(), CQL_LIBRARY_NAME);
  }

  @Test
  public void
      testGetEffectiveDataRequirementsThrowsResourceNotFoundExceptionWhenCqlAttachmentHasInvalidContentType() {
    library.getContent().replaceAll(attachment -> attachment.setContentType("invalid"));
    library.setId("Library/" + CQL_LIBRARY_NAME);

    Bundle.BundleEntryComponent measureBundleEntryComponent = getBundleEntryComponent(measure);
    Bundle.BundleEntryComponent libraryBundleEntryComponent = getBundleEntryComponent(library);
    Bundle bundle =
        new Bundle()
            .setType(Bundle.BundleType.TRANSACTION)
            .addEntry(measureBundleEntryComponent)
            .addEntry(libraryBundleEntryComponent);

    Optional<Bundle.BundleEntryComponent> measureEntry =
        effectiveDataRequirementService.getMeasureEntry(bundle);
    Resource measureResource = measureEntry.get().getResource();
    org.hl7.fhir.r5.model.Measure r5Measure =
        effectiveDataRequirementService.getR5MeasureFromR4MeasureResource(measureResource);

    assertThrows(
        ResourceNotFoundException.class,
        () ->
            effectiveDataRequirementService.getEffectiveDataRequirements(
                r5Measure, library, testAccessToken));
  }

  @Test
  public void testGetEffectiveDataRequirementsSuccess() {
    library.getContent().replaceAll(attachment -> attachment.setContentType("text/cql"));
    library.setId("TestCVPopulations");

    Bundle.BundleEntryComponent measureBundleEntryComponent = getBundleEntryComponent(measure);
    Bundle.BundleEntryComponent libraryBundleEntryComponent = getBundleEntryComponent(library);
    Bundle bundle =
        new Bundle()
            .setType(Bundle.BundleType.TRANSACTION)
            .addEntry(measureBundleEntryComponent)
            .addEntry(libraryBundleEntryComponent);

    Optional<Bundle.BundleEntryComponent> measureEntry =
        effectiveDataRequirementService.getMeasureEntry(bundle);
    Resource measureResource = measureEntry.get().getResource();
    org.hl7.fhir.r5.model.Measure r5Measure =
        effectiveDataRequirementService.getR5MeasureFromR4MeasureResource(measureResource);
    String cql = new String(library.getContentFirstRep().getData(), StandardCharsets.UTF_8);
    MadieLibrarySourceProvider.setUsing(new CqlTextParser(cql).getUsing());

    String fhirHelperString = ResourceUtils.getData("/fhirhelpers.cql");
    String suppDataString = ResourceUtils.getData("/SupplementalDataElements.cql");
    String cqlLibrary = ResourceUtils.getData("/cv_populations.cql");

    doReturn(fhirHelperString)
        .when(cqlLibraryService)
        .getLibraryCql(eq("FHIRHelpers"), eq("4.0.001"), nullable(String.class));
    ;

    doReturn(suppDataString)
        .when(cqlLibraryService)
        .getLibraryCql(eq("SupplementalDataElementsFHIR4"), eq("4.0.001"), nullable(String.class));
    ;

    doReturn(cqlLibrary)
        .when(cqlLibraryService)
        .getLibraryCql(eq("TestCVPopulations"), nullable(String.class), nullable(String.class));
    ;

    MadieLibrarySourceProvider.setCqlLibraryService(cqlLibraryService);
    org.hl7.fhir.r5.model.Library r5Library =
        effectiveDataRequirementService.getEffectiveDataRequirements(
            r5Measure, library, testAccessToken);
    assertEquals(r5Library.getId(), "effective-data-requirements");
  }

  @Test
  public void testGetEffectiveDataRequirementsStr() {
    library.getContent().replaceAll(attachment -> attachment.setContentType("text/cql"));
    library.setId("TestCVPopulations");

    Bundle.BundleEntryComponent measureBundleEntryComponent = getBundleEntryComponent(measure);
    Bundle.BundleEntryComponent libraryBundleEntryComponent = getBundleEntryComponent(library);
    Bundle bundle =
        new Bundle()
            .setType(Bundle.BundleType.TRANSACTION)
            .addEntry(measureBundleEntryComponent)
            .addEntry(libraryBundleEntryComponent);

    Optional<Bundle.BundleEntryComponent> measureEntry =
        effectiveDataRequirementService.getMeasureEntry(bundle);
    Resource measureResource = measureEntry.get().getResource();
    org.hl7.fhir.r5.model.Measure r5Measure =
        effectiveDataRequirementService.getR5MeasureFromR4MeasureResource(measureResource);
    String cql = new String(library.getContentFirstRep().getData(), StandardCharsets.UTF_8);
    MadieLibrarySourceProvider.setUsing(new CqlTextParser(cql).getUsing());

    String fhirHelperString = ResourceUtils.getData("/fhirhelpers.cql");
    String suppDataString = ResourceUtils.getData("/SupplementalDataElements.cql");
    String cqlLibrary = ResourceUtils.getData("/cv_populations.cql");

    doReturn(fhirHelperString)
        .when(cqlLibraryService)
        .getLibraryCql(eq("FHIRHelpers"), eq("4.0.001"), nullable(String.class));
    ;

    doReturn(suppDataString)
        .when(cqlLibraryService)
        .getLibraryCql(eq("SupplementalDataElementsFHIR4"), eq("4.0.001"), nullable(String.class));
    ;

    doReturn(cqlLibrary)
        .when(cqlLibraryService)
        .getLibraryCql(eq("TestCVPopulations"), nullable(String.class), nullable(String.class));
    ;

    MadieLibrarySourceProvider.setCqlLibraryService(cqlLibraryService);
    org.hl7.fhir.r5.model.Library r5Library =
        effectiveDataRequirementService.getEffectiveDataRequirements(
            r5Measure, library, testAccessToken);
    assertEquals(r5Library.getId(), "effective-data-requirements");

    when(effectiveDataRequirementService.getR4Parser()).thenReturn(r5Parser);
    when(r5Parser.setPrettyPrint(true)).thenReturn(jsonParserPrettier);
    when(jsonParserPrettier.encodeResourceToString(any())).thenReturn("test");
    String r5LibraryStr =
        effectiveDataRequirementService.getEffectiveDataRequirementsStr(r5Library);
    assertEquals(r5LibraryStr, "test");
  }
}
