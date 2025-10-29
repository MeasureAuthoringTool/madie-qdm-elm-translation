package gov.cms.mat.cql_elm_translation.service;

import gov.cms.madie.cql_elm_translator.dto.CqlLibraryDetails;
import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureObservation;
import gov.cms.madie.models.measure.Population;
import gov.cms.madie.models.measure.Stratification;
import gov.cms.mat.cql_elm_translation.ResourceFileUtil;
import gov.cms.mat.cql_elm_translation.dto.RelevantElement;
import org.hl7.fhir.r5.model.DataRequirement;
import org.hl7.fhir.r5.model.Enumerations;
import org.hl7.fhir.r5.model.Library;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DataCriteriaServiceTest implements ResourceFileUtil {
  @Mock private EffectiveDataRequirementService effectiveDataRequirementService;
  @InjectMocks private DataCriteriaService dataCriteriaService;

  private final String token = "token";
  private String cql;

  @BeforeEach
  void setup() {
    cql = getData("/qdm_data_criteria_retrieval_test.cql");
  }

  @Test
  void testGetRelevantElements() {
    Population population = Population.builder().definition("Qualifying Encounters").build();
    MeasureObservation observation =
        MeasureObservation.builder().definition("Test Observation").build();
    Stratification stratification = new Stratification();
    stratification.setCqlDefinition("test stratification");
    Group group =
        Group.builder()
            .populations(Collections.singletonList(population))
            .measureObservations(Collections.singletonList(observation))
            .stratifications(Collections.singletonList(stratification))
            .build();
    DataRequirement dataRequirement1 =
        new DataRequirement()
            .addProfile("http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-patient")
            .setType(Enumerations.FHIRTypes.PATIENT);
    DataRequirement dataRequirement2 =
        new DataRequirement()
            .addProfile(
                "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-condition-encounter-diagnosis")
            .setType(Enumerations.FHIRTypes.CONDITION);
    Library library = new Library();
    library.getDataRequirement().addAll(List.of(dataRequirement1, dataRequirement2));
    Measure measure = Measure.builder().cql(cql).groups(Collections.singletonList(group)).build();
    when(effectiveDataRequirementService.getEffectiveDataRequirements(
            any(CqlLibraryDetails.class), anyBoolean(), anyString()))
        .thenReturn(library);
    Set<RelevantElement> relevantElements = dataCriteriaService.getRelevantElements(measure, token);

    // source data criteria for value set
    assertThat(relevantElements.size(), is(equalTo(2)));
    RelevantElement firstData = relevantElements.stream().toList().get(0);
    assertThat(
        firstData.getProfile(), is(equalTo(dataRequirement1.getProfile().get(0).getValue())));
    assertThat(firstData.getType(), is(equalTo(dataRequirement1.getType().getDisplay())));

    // source data criteria for direct reference code
    RelevantElement secondData = relevantElements.stream().toList().get(1);
    ;
    assertThat(
        secondData.getProfile(), is(equalTo(dataRequirement2.getProfile().get(0).getValue())));
    assertThat(secondData.getType(), is(equalTo(dataRequirement2.getType().getDisplay())));
  }

  @Test
  void testGetRelevantElementsWhenNoSourceCriteriaFound() {
    String cql =
        "library DataCriteriaRetrivalTest version '0.0.000'\n"
            + "using QDM version '5.6'\n"
            + "valueset \"Encounter Inpatient\": 'urn:oid:2.16.840.1.113883.3.666.5.307'\n"
            + "parameter \"Measurement Period\" Interval<DateTime>\n"
            + "context Patient\n"
            + "define \"Qualifying Encounters\":\n true";

    Population population = Population.builder().definition("Qualifying Encounters").build();
    MeasureObservation observation =
        MeasureObservation.builder().definition("Test Observation").build();
    Stratification stratification = new Stratification();
    stratification.setCqlDefinition("test stratification");
    Group group =
        Group.builder()
            .populations(Collections.singletonList(population))
            .measureObservations(Collections.singletonList(observation))
            .stratifications(Collections.singletonList(stratification))
            .build();
    Measure measure = Measure.builder().cql(cql).groups(Collections.singletonList(group)).build();
    when(effectiveDataRequirementService.getEffectiveDataRequirements(
            any(CqlLibraryDetails.class), anyBoolean(), anyString()))
        .thenReturn(new Library());

    Set<RelevantElement> sourceDataCriteria =
        dataCriteriaService.getRelevantElements(measure, token);

    assertThat(sourceDataCriteria.size(), is(equalTo(0)));
  }

  @Test
  void testGetRelevantElementsWhenNoCqlProvided() {
    Population population = Population.builder().definition("Qualifying Encounters").build();
    Group group = Group.builder().populations(Collections.singletonList(population)).build();
    Measure measure = Measure.builder().cql("").groups(Collections.singletonList(group)).build();
    Set<RelevantElement> sourceDataCriteria =
        dataCriteriaService.getRelevantElements(measure, token);
    assertThat(sourceDataCriteria.size(), is(equalTo(0)));
  }

  @Test
  void testGetRelevantElementsWhenNoGroupsPresent() {
    Measure measure = Measure.builder().cql(cql).build();
    Set<RelevantElement> sourceDataCriteria =
        dataCriteriaService.getRelevantElements(measure, token);
    assertThat(sourceDataCriteria.size(), is(equalTo(0)));
  }

  @Test
  void testGetRelevantElementsWhenNullMeasure() {
    Set<RelevantElement> sourceDataCriteria = dataCriteriaService.getRelevantElements(null, token);
    assertThat(sourceDataCriteria.size(), is(equalTo(0)));
  }
}
