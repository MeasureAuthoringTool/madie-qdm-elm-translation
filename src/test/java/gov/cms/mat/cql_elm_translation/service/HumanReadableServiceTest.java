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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HumanReadableServiceTest {

  @Mock FhirContext fhirContext;

  @Mock MadieFhirServices madieFhirServices;

  @Mock JsonParser jsonParser;

  @Mock CqlConversionService cqlConversionService;

  @InjectMocks HumanReadableService humanReadableService;

  private Measure madieMeasure;

  private org.hl7.fhir.r4.model.Measure measure;

  private Library library;

  private final String testAccessToken = "test_access_token";

  @BeforeEach
  void setUp() {
    madieMeasure =
        Measure.builder()
            .id("madie-test-id")
            .measureName("test_measure_name")
            .cqlLibraryName("test_cql_library_name")
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

    measure = new org.hl7.fhir.r4.model.Measure()
        .setName(madieMeasure.getCqlLibraryName())
        .setTitle(madieMeasure.getMeasureName())
        .setExperimental(true)
        .setUrl("fhirBaseUrl/Measure/" + madieMeasure.getCqlLibraryName())
        .setVersion(madieMeasure.getVersion().toString())
        .setEffectivePeriod(
            getPeriodFromDates(
                madieMeasure.getMeasurementPeriodStart(), madieMeasure.getMeasurementPeriodEnd()))
        .setCopyright(madieMeasure.getMeasureMetaData().getCopyright())
        .setDisclaimer(madieMeasure.getMeasureMetaData().getDisclaimer());


    String cqlData = ResourceUtils.getData("/cv_populations.cql");
    library = new Library().addContent(new Attachment().setData(cqlData.getBytes()).setContentType("text/cql"));
    library.setId("Library/" + madieMeasure.getCqlLibraryName());

    when(madieFhirServices.getFhirMeasureBundle(any(), anyString()))
        .thenReturn("bundleResourceJson");
    when(fhirContext.newJsonParser()).thenReturn(jsonParser);
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
  void generateHumanReadable() {
    Bundle.BundleEntryComponent measureBundleEntryComponent = getBundleEntryComponent(measure);
    Bundle.BundleEntryComponent libraryBundleEntryComponent = getBundleEntryComponent(library);
    Bundle bundle = new Bundle()
        .setType(Bundle.BundleType.TRANSACTION)
        .addEntry(measureBundleEntryComponent)
        .addEntry(libraryBundleEntryComponent);

    doNothing().when(cqlConversionService).setUpLibrarySourceProvider(anyString(), anyString());
    when(jsonParser.parseResource(any(), anyString())).thenReturn(bundle);

    String generatedHumanReadable =
        humanReadableService.generateHumanReadable(madieMeasure, testAccessToken);
    assertNotNull(generatedHumanReadable);
    assertTrue(generatedHumanReadable.contains("test_measure_name"));
    verify(cqlConversionService, times(1)).setUpLibrarySourceProvider(anyString(), anyString());
  }

  @Test
  void generateHumanReadableThrowsResourceNotFoundExceptionForNoBundle() {
    when(jsonParser.parseResource(any(), anyString())).thenReturn(null);
    assertThrows(
        ResourceNotFoundException.class,
        () -> humanReadableService.generateHumanReadable(madieMeasure, testAccessToken));
  }

  @Test
  void generateHumanReadableThrowsResourceNotFoundExceptionForNoEntry() {
    when(jsonParser.parseResource(any(), anyString())).thenReturn(new Bundle());
    assertThrows(
        ResourceNotFoundException.class,
        () -> humanReadableService.generateHumanReadable(madieMeasure, testAccessToken));
  }

  @Test
  void generateHumanReadableThrowsResourceNotFoundExceptionForNoMeasureResource() {
    Bundle bundle =
        new Bundle()
            .setType(Bundle.BundleType.TRANSACTION)
            .addEntry(getBundleEntryComponent(new Library()));
    when(jsonParser.parseResource(any(), anyString())).thenReturn(bundle);
    assertThrows(
        ResourceNotFoundException.class,
        () -> humanReadableService.generateHumanReadable(madieMeasure, testAccessToken));
  }

  @Test
  void generateHumanReadableThrowsResourceNotFoundExceptionForNoMeasureLibraryResource() {
    Bundle.BundleEntryComponent measureBundleEntryComponent = getBundleEntryComponent(measure);
    Bundle bundle = new Bundle()
        .setType(Bundle.BundleType.TRANSACTION)
        .addEntry(measureBundleEntryComponent);
    when(jsonParser.parseResource(any(), anyString())).thenReturn(bundle);
    assertThrows(
        ResourceNotFoundException.class,
        () -> humanReadableService.generateHumanReadable(madieMeasure, testAccessToken));
  }

  @Test
  void generateHumanReadableThrowsResourceNotFoundExceptionWhenCqlAttachmentIsNull() {
    library.getContent().replaceAll(attachment -> attachment.setContentType(null));
    library.setId("Library/" + madieMeasure.getCqlLibraryName());

    Bundle.BundleEntryComponent measureBundleEntryComponent = getBundleEntryComponent(measure);
    Bundle.BundleEntryComponent libraryBundleEntryComponent = getBundleEntryComponent(library);
    Bundle bundle = new Bundle()
        .setType(Bundle.BundleType.TRANSACTION)
        .addEntry(measureBundleEntryComponent)
        .addEntry(libraryBundleEntryComponent);

    when(jsonParser.parseResource(any(), anyString())).thenReturn(bundle);

    when(jsonParser.parseResource(any(), anyString())).thenReturn(bundle);
    assertThrows(
        ResourceNotFoundException.class,
        () -> humanReadableService.generateHumanReadable(madieMeasure, testAccessToken));
  }

  // todo need to write a test case to handle FHIRException

}
