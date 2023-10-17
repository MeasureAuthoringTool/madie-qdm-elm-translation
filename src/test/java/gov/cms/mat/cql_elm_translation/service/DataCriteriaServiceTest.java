package gov.cms.mat.cql_elm_translation.service;

import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.Population;
import gov.cms.mat.cql_elm_translation.ResourceFileUtil;
import gov.cms.mat.cql_elm_translation.cql_translator.TranslationResource;
import gov.cms.mat.cql_elm_translation.data.RequestData;
import gov.cms.mat.cql_elm_translation.dto.SourceDataCriteria;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.LibraryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DataCriteriaServiceTest implements ResourceFileUtil {
  @Mock private CqlConversionService cqlConversionService;

  @InjectMocks private DataCriteriaService dataCriteriaService;

  private final String token = "token";
  private String cql;
  private RequestData requestData;

  @BeforeEach
  void setup() {
    //    cql = getData("/qdm_data_criteria_retrieval_test.cql");
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
    assertThat(sourceDataCriteria.size(), is(equalTo(3)));
    assertThat(sourceDataCriteria.get(1).getOid(), is(equalTo("2.16.840.1.113883.3.666.5.307")));
    assertThat(sourceDataCriteria.get(1).getTitle(), is(equalTo("Encounter Inpatient")));
    assertThat(sourceDataCriteria.get(1).getType(), is(equalTo("EncounterPerformed")));
    assertThat(
        sourceDataCriteria.get(1).getDescription(),
        is(equalTo("Encounter, Performed: Encounter Inpatient")));
    assertFalse(sourceDataCriteria.get(1).isDrc());

    // source data criteria for direct reference code
    assertThat(sourceDataCriteria.size(), is(equalTo(3)));
    assertTrue(sourceDataCriteria.get(2).isDrc());
    assertThat(sourceDataCriteria.get(2).getTitle(), is(equalTo("Clinical Examples")));
    assertThat(sourceDataCriteria.get(2).getType(), is(equalTo("EncounterPerformed")));
    assertThat(
        sourceDataCriteria.get(2).getDescription(),
        is(equalTo("Encounter, Performed: Clinical Examples")));

    // MAT-6210 only setCodeId for direct reference code
    assertThat(sourceDataCriteria.get(0).getCodeId(), is(equalTo(null)));
    assertThat(sourceDataCriteria.get(1).getCodeId(), is(equalTo(null)));
    assertThat(sourceDataCriteria.get(2).getCodeId(), is(equalTo("1021859")));
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
            .buildTranslator(data.getCqlDataInputStream(), data.createMap());

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
    Group group = Group.builder().populations(Collections.singletonList(population)).build();
    Measure measure = Measure.builder().cql(cql).groups(Collections.singletonList(group)).build();
    CqlTranslator translator = TranslationResource.getInstance(false).buildTranslator(requestData);

    Mockito.doNothing()
        .when(cqlConversionService)
        .setUpLibrarySourceProvider(anyString(), anyString());
    when(cqlConversionService.processCqlData(any(RequestData.class))).thenReturn(translator);

    List<SourceDataCriteria> relevantElements =
        dataCriteriaService.getRelevantElements(measure, token);

    // source data criteria for value set
    assertThat(relevantElements.size(), is(equalTo(2)));
    assertThat(relevantElements.get(0).getOid(), is(equalTo("2.16.840.1.113883.3.666.5.307")));
    assertThat(relevantElements.get(0).getTitle(), is(equalTo("Encounter Inpatient")));
    assertThat(relevantElements.get(0).getType(), is(equalTo("EncounterPerformed")));
    assertThat(
        relevantElements.get(0).getDescription(),
        is(equalTo("Encounter, Performed: Encounter Inpatient")));
    assertFalse(relevantElements.get(0).isDrc());

    // source data criteria for direct reference code
    assertTrue(relevantElements.get(1).isDrc());
    assertThat(relevantElements.get(1).getTitle(), is(equalTo("Clinical Examples")));
    assertThat(relevantElements.get(1).getType(), is(equalTo("EncounterPerformed")));
    assertThat(
        relevantElements.get(1).getDescription(),
        is(equalTo("Encounter, Performed: Clinical Examples")));
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
    Group group = Group.builder().populations(Collections.singletonList(population)).build();
    Measure measure = Measure.builder().cql(cql).groups(Collections.singletonList(group)).build();

    RequestData data = requestData.toBuilder().cqlData(cql).build();
    CqlTranslator translator =
        TranslationResource.getInstance(false)
            .buildTranslator(data.getCqlDataInputStream(), data.createMap(), data.getSourceInfo());

    Mockito.doNothing()
        .when(cqlConversionService)
        .setUpLibrarySourceProvider(anyString(), anyString());
    when(cqlConversionService.processCqlData(any(RequestData.class))).thenReturn(translator);

    List<SourceDataCriteria> sourceDataCriteria =
        dataCriteriaService.getRelevantElements(measure, token);

    assertThat(sourceDataCriteria.size(), is(equalTo(0)));
  }

  @Test
  void testGetRelevantElementsWhenNoCqlProvided() {
    Population population = Population.builder().definition("Qualifying Encounters").build();
    Group group = Group.builder().populations(Collections.singletonList(population)).build();
    Measure measure = Measure.builder().cql("").groups(Collections.singletonList(group)).build();
    List<SourceDataCriteria> sourceDataCriteria =
        dataCriteriaService.getRelevantElements(measure, token);
    assertThat(sourceDataCriteria.size(), is(equalTo(0)));
  }
}
