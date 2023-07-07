package gov.cms.mat.cql_elm_translation.service;

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
    CqlTranslator translator =
        TranslationResource.getInstance(false)
            .buildTranslator(requestData.getCqlDataInputStream(), requestData.createMap());

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
    CqlTranslator translator =
        TranslationResource.getInstance(false)
            .buildTranslator(data.getCqlDataInputStream(), data.createMap());

    Mockito.doNothing()
        .when(cqlConversionService)
        .setUpLibrarySourceProvider(anyString(), anyString());
    when(cqlConversionService.processCqlData(any(RequestData.class))).thenReturn(translator);

    List<SourceDataCriteria> sourceDataCriteria =
        dataCriteriaService.getSourceDataCriteria(cql, token);

    assertThat(sourceDataCriteria.size(), is(equalTo(0)));
  }

  @Test
  void testGetSourceDataCriteriaWhenNoCqlProvided() {
    List<SourceDataCriteria> sourceDataCriteria =
        dataCriteriaService.getSourceDataCriteria("", token);
    assertThat(sourceDataCriteria.size(), is(equalTo(0)));
  }
}
