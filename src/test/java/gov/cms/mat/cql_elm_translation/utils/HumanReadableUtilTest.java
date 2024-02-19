package gov.cms.mat.cql_elm_translation.utils;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.MeasureMetaData;
import gov.cms.madie.models.measure.Population;
import gov.cms.madie.models.measure.PopulationType;
import gov.cms.madie.models.measure.QdmMeasure;

public class HumanReadableUtilTest {

  private QdmMeasure measure = QdmMeasure.builder().build();

  @Test
  void testGetMeasureDevelopersMetaDataNull() {
    var result = HumanReadableUtil.getMeasureDevelopers(measure);
    assertNull(result);
  }

  @Test
  void testGetMeasureDevelopersNullDevelopers() {
    measure.setMeasureMetaData(MeasureMetaData.builder().build());
    var result = HumanReadableUtil.getMeasureDevelopers(measure);
    assertNull(result);
  }

  @Test
  void testGetCbeNumberMetaDataNull() {
    measure.setMeasureMetaData(null);
    var result = HumanReadableUtil.getCbeNumber(measure);
    assertNull(result);
  }

  @Test
  void testGetCbeNumberCbeNumberNull() {
    measure.setMeasureMetaData(MeasureMetaData.builder().build());
    var result = HumanReadableUtil.getCbeNumber(measure);
    assertNull(result);
  }

  @Test
  void testGetEndorsedByMetaDataNull() {
    measure.setMeasureMetaData(null);
    var result = HumanReadableUtil.getEndorsedBy(measure);
    assertNull(result);
  }

  @Test
  void testGetEndorsedByEndorsementNull() {
    measure.setMeasureMetaData(MeasureMetaData.builder().build());
    var result = HumanReadableUtil.getEndorsedBy(measure);
    assertNull(result);
  }

  @Test
  void testGetMeasureTypesMetaDataNull() {
    measure.setMeasureMetaData(null);
    var result = HumanReadableUtil.getMeasureTypes(measure);
    assertNull(result);
  }

  @Test
  void testGetStratificationNullGroups() {
    var result = HumanReadableUtil.getStratification(measure);
    assertNull(result);
  }

  @Test
  void testGetStratificationNullStratificationss() {
    measure.setGroups(List.of(Group.builder().build()));
    var result = HumanReadableUtil.getStratification(measure);
    assertNull(result);
  }

  @Test
  void testGetMeasureTypesMeasureTypesaNull() {
    measure.setMeasureMetaData(MeasureMetaData.builder().build());
    var result = HumanReadableUtil.getMeasureTypes(measure);
    assertNull(result);
  }

  @Test
  void testEscapeHtmlNullString() {
    measure.setMeasureMetaData(null);
    var result = HumanReadableUtil.escapeHtmlString(null);
    assertNull(result);
  }

  @Test
  void testEscapeHtmlString() {
    measure.setMeasureMetaData(MeasureMetaData.builder().build());
    var result = HumanReadableUtil.escapeHtmlString("this is <html> string");
    assertThat(result, is(equalTo("this is &lt;html&gt; string")));
  }

  @Test
  void testBuildReferencesMetaDataNull() {
    measure.setMeasureMetaData(null);
    var result = HumanReadableUtil.buildReferences(measure.getMeasureMetaData());
    assertNull(result);
  }

  @Test
  void testBuildReferencesMeasureDefinitionsNull() {
    measure.setMeasureMetaData(MeasureMetaData.builder().build());
    var result = HumanReadableUtil.buildReferences(measure.getMeasureMetaData());
    assertNull(result);
  }

  @Test
  void testGetPopulationDescriptionGroupsNull() {
    var result =
        HumanReadableUtil.getPopulationDescription(
            measure, PopulationType.INITIAL_POPULATION.name());
    assertTrue(StringUtils.isBlank(result));
  }

  @Test
  void testGetPopulationDescriptionPopulationsNull() {
    measure.setGroups(List.of(Group.builder().build()));
    var result =
        HumanReadableUtil.getPopulationDescription(
            measure, PopulationType.INITIAL_POPULATION.name());
    assertTrue(StringUtils.isBlank(result));
  }

  @Test
  void testGetPopulationDescriptionNoDefinition() {
    measure.setGroups(
        List.of(Group.builder().populations(List.of(Population.builder().build())).build()));
    var result =
        HumanReadableUtil.getPopulationDescription(
            measure, PopulationType.INITIAL_POPULATION.name());
    assertTrue(StringUtils.isBlank(result));
  }
}
