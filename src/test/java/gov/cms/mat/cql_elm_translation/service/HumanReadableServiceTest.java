package gov.cms.mat.cql_elm_translation.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.parser.JsonParser;
import gov.cms.madie.models.common.Version;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureMetaData;
import gov.cms.mat.cql_elm_translation.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r4.model.Bundle;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HumanReadableServiceTest {

  @Mock FhirContext fhirContext;

  @Mock MadieFhirServices madieFhirServices;

  @Mock JsonParser jsonParser;

  @InjectMocks HumanReadableService humanReadableService;

  private Measure madieMeasure;

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

    org.hl7.fhir.r4.model.Measure measure = new org.hl7.fhir.r4.model.Measure();
    measure
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
    Bundle.BundleEntryComponent bundleEntryComponent = getBundleEntryComponent(measure);
    Bundle bundle =
        new Bundle().setType(Bundle.BundleType.TRANSACTION).addEntry(bundleEntryComponent);

    when(madieFhirServices.getFhirMeasureBundle(any(), anyString()))
        .thenReturn("bundleResourceJson");
    when(fhirContext.newJsonParser()).thenReturn(jsonParser);
    when(jsonParser.parseResource(any(), anyString())).thenReturn(bundle);
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
    String generatedHumanReadable =
        humanReadableService.generateHumanReadable(madieMeasure, testAccessToken);
    assertNotNull(generatedHumanReadable);
    assertTrue(generatedHumanReadable.contains("test_measure_name"));
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

  // todo need to write a test case to handle FHIRException

}
