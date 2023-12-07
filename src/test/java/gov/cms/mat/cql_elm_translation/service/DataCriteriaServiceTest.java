package gov.cms.mat.cql_elm_translation.service;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import gov.cms.madie.models.measure.*;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.LibraryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import gov.cms.mat.cql.CqlTextParser;
import gov.cms.mat.cql_elm_translation.ResourceFileUtil;
import gov.cms.mat.cql_elm_translation.cql_translator.MadieLibrarySourceProvider;
import gov.cms.mat.cql_elm_translation.cql_translator.TranslationResource;
import gov.cms.mat.cql_elm_translation.data.RequestData;
import gov.cms.mat.cql_elm_translation.dto.SourceDataCriteria;

@ExtendWith(MockitoExtension.class)
public class DataCriteriaServiceTest implements ResourceFileUtil {
  @Mock private CqlConversionService cqlConversionService;
  @Mock private CqlLibraryService cqlLibraryService;

  @InjectMocks private DataCriteriaService dataCriteriaService;

  private final String token = "token";
  private String cql;
  private String matGlobalCql;
  private RequestData requestData;

  @BeforeEach
  void setup() {
    matGlobalCql = getData("/mat_global_common_functions.cql");
    cql = getData("/qdm_data_criteria_retrieval_test.cql");

    requestData =
        RequestData.builder()
            .cqlData(cql)
            .showWarnings(false)
            .signatures(LibraryBuilder.SignatureLevel.All)
            .annotations(true)
            .locators(true)
            .disableListDemotion(true)
            .disableListPromotion(true)
            .disableMethodInvocation(false)
            .validateUnits(true)
            .resultTypes(true)
            .build();
  }

  @Test
  void testGetSourceDataCriteria() {
    CqlTranslator translator = TranslationResource.getInstance(false).buildTranslator(requestData);

    Mockito.doNothing()
        .when(cqlConversionService)
        .setUpLibrarySourceProvider(anyString(), anyString());
    when(cqlConversionService.processCqlData(any(RequestData.class))).thenReturn(translator);

    List<SourceDataCriteria> sourceDataCriteria =
        dataCriteriaService.getSourceDataCriteria(cql, token);

    // source data criteria for value set

    SourceDataCriteria singleSdc =
        sourceDataCriteria.stream()
            .filter(sdc -> sdc.getTitle().equals("Encounter Inpatient"))
            .collect(Collectors.toList())
            .get(0);
    assertThat(singleSdc.getOid(), is(equalTo("2.16.840.1.113883.3.666.5.307")));
    assertThat(singleSdc.getTitle(), is(equalTo("Encounter Inpatient")));
    assertThat(singleSdc.getType(), is(equalTo("EncounterPerformed")));
    assertThat(
        singleSdc.getDescription(), is(equalTo("Encounter, Performed: Encounter Inpatient")));
    assertFalse(singleSdc.isDrc());
    //
    //    // source data criteria for direct reference code
    //    assertThat(sourceDataCriteria.size(), is(equalTo(3)));
    //    assertTrue(sourceDataCriteria.get(2).isDrc());
    //    assertThat(sourceDataCriteria.get(2).getTitle(), is(equalTo("Clinical Examples")));
    //    assertThat(sourceDataCriteria.get(2).getType(), is(equalTo("EncounterPerformed")));
    //    assertThat(
    //        sourceDataCriteria.get(2).getDescription(),
    //        is(equalTo("Encounter, Performed: Clinical Examples")));
    //
    //    // MAT-6210 only setCodeId for direct reference code
    //    assertThat(sourceDataCriteria.get(0).getCodeId(), is(equalTo(null)));
    //    assertThat(sourceDataCriteria.get(1).getCodeId(), is(equalTo(null)));
    //    assertThat(sourceDataCriteria.get(2).getCodeId(), is(equalTo("1021859")));
  }

  @Test
  void testGetSourceDataCriteriaWhenNoSourceCriteriaFound() {
    String cql =
        "library DataCriteriaRetrivalTest version '0.0.000'\n"
            + "using QDM version '5.6'\n"
            + "valueset \"Encounter Inpatient\": 'urn:oid:2.16.840.1.113883.3.666.5.307'\n"
            + "parameter \"Measurement Period\" Interval<DateTime>\n"
            + "context Patient\n"
            + "define \"Qualifying Encounters\":\n true";

    RequestData data = requestData.toBuilder().cqlData(cql).build();
    CqlTranslator translator = TranslationResource.getInstance(false).buildTranslator(data);

    Mockito.doNothing()
        .when(cqlConversionService)
        .setUpLibrarySourceProvider(anyString(), anyString());
    when(cqlConversionService.processCqlData(any(RequestData.class))).thenReturn(translator);

    List<SourceDataCriteria> sourceDataCriteria =
        dataCriteriaService.getSourceDataCriteria(cql, token);

    assertThat(sourceDataCriteria.size(), is(equalTo(0)));
  }

  @Test
  void testGetSourceDataCriteriaWithCriteriaWithCodes() {
    String cql =
        "library DRCTest version '0.0.000'\n"
            + "using QDM version '5.6'\n"
            + "codesystem \"LOINC\": 'urn:oid:2.16.840.1.113883.6.1'\n"
            + "valueset \"Palliative Care Encounter\": 'urn:oid:2.16.840.1.113883.3.464.1003.101.12.1090'\n"
            + "code \"Functional Assessment of Chronic Illness Therapy - Palliative Care Questionnaire (FACIT-Pal)\": '71007-9' from \"LOINC\" display 'Functional Assessment of Chronic Illness Therapy - Palliative Care Questionnaire (FACIT-Pal)'\n"
            + "parameter \"Measurement Period\" Interval<DateTime>\n"
            + "context Patient\n"
            + "define \"Palliative Care in the Measurement Period\":\n"
            + "( [\"Encounter, Performed\": \"Functional Assessment of Chronic Illness Therapy - Palliative Care Questionnaire (FACIT-Pal)\"]\n"
            + ")";

    RequestData data = requestData.toBuilder().cqlData(cql).build();
    CqlTranslator translator =
        TranslationResource.getInstance(false)
            .buildTranslator(data.getCqlDataInputStream(), data.createMap(), data.getSourceInfo());

    Mockito.doNothing()
        .when(cqlConversionService)
        .setUpLibrarySourceProvider(anyString(), anyString());
    when(cqlConversionService.processCqlData(any(RequestData.class))).thenReturn(translator);

    List<SourceDataCriteria> sourceDataCriteria =
        dataCriteriaService.getSourceDataCriteria(cql, token);

    assertThat(sourceDataCriteria.size(), is(equalTo(1)));
  }

  @Test
  void testGetSourceDataCriteriaWhenNoCqlProvided() {
    List<SourceDataCriteria> sourceDataCriteria =
        dataCriteriaService.getSourceDataCriteria("", token);
    assertThat(sourceDataCriteria.size(), is(equalTo(0)));
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
    Measure measure = Measure.builder().cql(cql).groups(Collections.singletonList(group)).build();
    MadieLibrarySourceProvider.setUsing(new CqlTextParser(cql).getUsing());
    MadieLibrarySourceProvider.setCqlLibraryService(cqlLibraryService);
    doReturn(matGlobalCql).when(cqlLibraryService).getLibraryCql(any(), any(), any());

    CqlTranslator translator = TranslationResource.getInstance(false).buildTranslator(requestData);
    verify(cqlLibraryService).getLibraryCql(any(), any(), any());
    Mockito.doNothing()
        .when(cqlConversionService)
        .setUpLibrarySourceProvider(anyString(), anyString());
    when(cqlConversionService.processCqlData(any(RequestData.class))).thenReturn(translator);

    Set<SourceDataCriteria> relevantElements =
        dataCriteriaService.getRelevantElements(measure, token);

    // source data criteria for value set
    assertThat(relevantElements.size(), is(equalTo(2)));
    SourceDataCriteria firstData = ((TreeSet<SourceDataCriteria>) relevantElements).first();
    assertThat(firstData.getOid(), is(equalTo("2.16.840.1.113883.3.666.5.307")));
    assertThat(firstData.getTitle(), is(equalTo("Encounter Inpatient")));
    assertThat(firstData.getType(), is(equalTo("EncounterPerformed")));
    assertThat(
        firstData.getDescription(), is(equalTo("Encounter, Performed: Encounter Inpatient")));
    assertFalse(firstData.isDrc());

    // source data criteria for direct reference code
    SourceDataCriteria secondData = ((TreeSet<SourceDataCriteria>) relevantElements).last();
    assertTrue(secondData.isDrc());
    assertThat(secondData.getTitle(), is(equalTo("Clinical Examples")));
    assertThat(secondData.getType(), is(equalTo("EncounterPerformed")));
    assertThat(secondData.getDescription(), is(equalTo("Encounter, Performed: Clinical Examples")));
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

    RequestData data = requestData.toBuilder().cqlData(cql).build();
    CqlTranslator translator =
        TranslationResource.getInstance(false)
            .buildTranslator(data.getCqlDataInputStream(), data.createMap(), data.getSourceInfo());

    Mockito.doNothing()
        .when(cqlConversionService)
        .setUpLibrarySourceProvider(anyString(), anyString());
    when(cqlConversionService.processCqlData(any(RequestData.class))).thenReturn(translator);

    Set<SourceDataCriteria> sourceDataCriteria =
        dataCriteriaService.getRelevantElements(measure, token);

    assertThat(sourceDataCriteria.size(), is(equalTo(0)));
  }

  @Test
  void testGetRelevantElementsWhenNoCqlProvided() {
    Population population = Population.builder().definition("Qualifying Encounters").build();
    Group group = Group.builder().populations(Collections.singletonList(population)).build();
    Measure measure = Measure.builder().cql("").groups(Collections.singletonList(group)).build();
    Set<SourceDataCriteria> sourceDataCriteria =
        dataCriteriaService.getRelevantElements(measure, token);
    assertThat(sourceDataCriteria.size(), is(equalTo(0)));
  }
}
