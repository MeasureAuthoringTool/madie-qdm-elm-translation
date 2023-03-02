package gov.cms.mat.cql_elm_translation.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.parser.JsonParser;
import gov.cms.madie.models.common.Version;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureMetaData;
import gov.cms.mat.cql_elm_translation.exceptions.ResourceNotFoundException;
import gov.cms.mat.cql_elm_translation.utils.ResourceUtils;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Measure.MeasureSupplementalDataComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HumanReadableServiceTest {

  @Mock FhirContext fhirContext;

  @Mock FhirContext fhirContextForR5;

  @Mock MadieFhirServices madieFhirServices;

  @Mock JsonParser r4Parser;

  @Mock JsonParser r5Parser;

  @Mock JsonParser jsonParserPrettier;

  @Mock CqlConversionService cqlConversionService;

  @InjectMocks HumanReadableService humanReadableService;

  private Measure madieMeasure;

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
    madieMeasure =
        Measure.builder()
            .id("madie-test-id")
            .measureName("test_measure_name")
            .cqlLibraryName(CQL_LIBRARY_NAME)
            .version(new Version(1, 0, 0))
            .measurementPeriodStart(new Date())
            .measurementPeriodEnd(new Date())
            .measureMetaData(
                new MeasureMetaData()
                    .toBuilder()
                    .copyright("test_copyright")
                    .disclaimer("test_disclaimer")
                    .build())
            .build();

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
            .setName(madieMeasure.getCqlLibraryName())
            .setTitle(madieMeasure.getMeasureName())
            .setExperimental(true)
            .setUrl("fhirBaseUrl/Measure/" + madieMeasure.getCqlLibraryName())
            .setVersion(madieMeasure.getVersion().toString())
            .setEffectivePeriod(
                getPeriodFromDates(
                    madieMeasure.getMeasurementPeriodStart(),
                    madieMeasure.getMeasurementPeriodEnd()))
            .setCopyright(madieMeasure.getMeasureMetaData().getCopyright())
            .setDisclaimer(madieMeasure.getMeasureMetaData().getDisclaimer())
            .setGroup(groups)
            .setSupplementalData(supplementalDataList);

    String cqlData = ResourceUtils.getData("/cv_populations.cql");
    library =
        new Library()
            .addContent(new Attachment().setData(cqlData.getBytes()).setContentType("text/cql"));
    library.setId("Library/" + madieMeasure.getCqlLibraryName());
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
    Bundle result = humanReadableService.createFhirResourceFromJson(null, null);
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

    when(humanReadableService.getR4Parser()).thenReturn(r4Parser);
    when(r4Parser.parseResource(any(), anyString())).thenReturn(bundle);

    Bundle result =
        humanReadableService.createFhirResourceFromJson("bundleResourceJson", Bundle.class);
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
    Optional<Bundle.BundleEntryComponent> result = humanReadableService.getMeasureEntry(bundle);

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
        humanReadableService.getMeasureLibraryEntry(bundle, "anotherLibrary");

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
        humanReadableService.getMeasureLibraryEntry(bundle, CQL_LIBRARY_NAME);
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
        humanReadableService.getMeasureEntry(bundle);
    Resource measureResource = measureEntry.get().getResource();

    org.hl7.fhir.r5.model.Measure r5Measure =
        humanReadableService.getR5MeasureFromR4MeasureResource(measureResource);
    assertNotNull(r5Measure);
    assertEquals(r5Measure.getName(), CQL_LIBRARY_NAME);
  }

  @Test
  public void
      testGetEffectiveDataRequirementsThrowsResourceNotFoundExceptionWhenCqlAttachmentHasInvalidContentType() {
    library.getContent().replaceAll(attachment -> attachment.setContentType("invalid"));
    library.setId("Library/" + madieMeasure.getCqlLibraryName());

    Bundle.BundleEntryComponent measureBundleEntryComponent = getBundleEntryComponent(measure);
    Bundle.BundleEntryComponent libraryBundleEntryComponent = getBundleEntryComponent(library);
    Bundle bundle =
        new Bundle()
            .setType(Bundle.BundleType.TRANSACTION)
            .addEntry(measureBundleEntryComponent)
            .addEntry(libraryBundleEntryComponent);

    Optional<Bundle.BundleEntryComponent> measureEntry =
        humanReadableService.getMeasureEntry(bundle);
    Resource measureResource = measureEntry.get().getResource();
    org.hl7.fhir.r5.model.Measure r5Measure =
        humanReadableService.getR5MeasureFromR4MeasureResource(measureResource);

    assertThrows(
        ResourceNotFoundException.class,
        () ->
            humanReadableService.getEffectiveDataRequirements(r5Measure, library, testAccessToken));
  }

  @Test
  public void testGetEffectiveDataRequirementsSuccess() {
    library.getContent().replaceAll(attachment -> attachment.setContentType("text/cql"));
    library.setId("Library/" + madieMeasure.getCqlLibraryName());

    Bundle.BundleEntryComponent measureBundleEntryComponent = getBundleEntryComponent(measure);
    Bundle.BundleEntryComponent libraryBundleEntryComponent = getBundleEntryComponent(library);
    Bundle bundle =
        new Bundle()
            .setType(Bundle.BundleType.TRANSACTION)
            .addEntry(measureBundleEntryComponent)
            .addEntry(libraryBundleEntryComponent);

    Optional<Bundle.BundleEntryComponent> measureEntry =
        humanReadableService.getMeasureEntry(bundle);
    Resource measureResource = measureEntry.get().getResource();
    org.hl7.fhir.r5.model.Measure r5Measure =
        humanReadableService.getR5MeasureFromR4MeasureResource(measureResource);

    org.hl7.fhir.r5.model.Library r5Library =
        humanReadableService.getEffectiveDataRequirements(r5Measure, library, testAccessToken);
    assertEquals(r5Library.getId(), "effective-data-requirements");
  }

  @Test
  public void testGetEffectiveDataRequirementsStr() {
    library.getContent().replaceAll(attachment -> attachment.setContentType("text/cql"));
    library.setId("Library/" + madieMeasure.getCqlLibraryName());

    Bundle.BundleEntryComponent measureBundleEntryComponent = getBundleEntryComponent(measure);
    Bundle.BundleEntryComponent libraryBundleEntryComponent = getBundleEntryComponent(library);
    Bundle bundle =
        new Bundle()
            .setType(Bundle.BundleType.TRANSACTION)
            .addEntry(measureBundleEntryComponent)
            .addEntry(libraryBundleEntryComponent);

    Optional<Bundle.BundleEntryComponent> measureEntry =
        humanReadableService.getMeasureEntry(bundle);
    Resource measureResource = measureEntry.get().getResource();
    org.hl7.fhir.r5.model.Measure r5Measure =
        humanReadableService.getR5MeasureFromR4MeasureResource(measureResource);

    org.hl7.fhir.r5.model.Library r5Library =
        humanReadableService.getEffectiveDataRequirements(r5Measure, library, testAccessToken);
    assertEquals(r5Library.getId(), "effective-data-requirements");

    when(humanReadableService.getR4Parser()).thenReturn(r5Parser);
    when(r5Parser.setPrettyPrint(true)).thenReturn(jsonParserPrettier);
    when(jsonParserPrettier.encodeResourceToString(any())).thenReturn("test");
    String r5LibraryStr = humanReadableService.getEffectiveDataRequirementsStr(r5Library);
    assertEquals(r5LibraryStr, "test");
  }
}
