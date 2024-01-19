package gov.cms.mat.cql_elm_translation.service;

import freemarker.template.Template;
import gov.cms.madie.models.common.Organization;
import gov.cms.madie.models.common.Version;
import gov.cms.madie.models.measure.*;
import gov.cms.madie.qdm.humanreadable.model.HumanReadableMeasureInformationModel;
import gov.cms.madie.qdm.humanreadable.model.HumanReadablePopulationCriteriaModel;
import gov.cms.madie.qdm.humanreadable.model.HumanReadablePopulationModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class HumanReadableServiceTest {
  @InjectMocks HumanReadableService humanReadableService;

  @Mock Template template;

  private Measure measure;
  private final Date now = new Date();

  @BeforeEach
  void setUp() {
    measure =
        Measure.builder()
            .id("1234")
            .measureSetId("7890")
            .versionId("56")
            .measureName("measure1")
            .ecqmTitle("ecqm-title")
            .version(Version.parse("0.0.000"))
            .measurementPeriodStart(now)
            .measurementPeriodEnd(now)
            .measureMetaData(
                MeasureMetaData.builder()
                    .draft(true)
                    .clinicalRecommendation("clinical recommendation")
                    .copyright("copyright")
                    .description("a decent description")
                    .disclaimer("a disclaiming disclaimer")
                    .guidance("a helpful guidance")
                    .rationale("here's why we do")
                    .developers(
                        List.of(
                            Organization.builder().name("org1").build(),
                            Organization.builder().name("org2").build()))
                    .steward(Organization.builder().name("stewardOrg").build())
                    .measureDefinitions(
                        List.of(
                            MeasureDefinition.builder()
                                .id("testMeasureDefinitionId")
                                .term("test term")
                                .definition("test definition")
                                .build()))
                    .build())
            .groups(
                List.of(
                    Group.builder()
                        .populationBasis("boolean")
                        .scoring("proportion")
                        .populations(
                            List.of(
                                Population.builder()
                                    .id("p1")
                                    .name(PopulationType.INITIAL_POPULATION)
                                    .definition("define ipp: true")
                                    .build()))
                        .build()))
            .build();
  }

  @Test
  public void generateHumanReadableThrowsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> humanReadableService.generate(null));
  }

  // result is an empty string, Mocking Template doesn't yield expected results.
  @Test
  public void generateHumanReadableSuccessfully() {
    var result = humanReadableService.generate(measure);
    assertNotNull(result);
  }

  @Test
  public void canBuildMeasureInfoFromMeasure() {
    HumanReadableMeasureInformationModel measureInfoModel =
        humanReadableService.buildMeasureInfo(measure);

    assertThat(measureInfoModel.getQdmVersion(), equalTo(5.6));
    assertThat(measureInfoModel.getEcqmTitle(), equalTo(measure.getEcqmTitle()));
    assertThat(measureInfoModel.getEcqmVersionNumber(), equalTo(measure.getVersion().toString()));
    assertThat(measureInfoModel.isCalendarYear(), equalTo(false));
    assertThat(measureInfoModel.getGuid(), equalTo(measure.getMeasureSetId()));
    assertThat(
        measureInfoModel.isPatientBased(),
        equalTo(measure.getGroups().get(0).getPopulationBasis().equals("boolean")));
    assertThat(
        measureInfoModel.getMeasurementPeriodStartDate(),
        equalTo(DateFormat.getDateInstance().format(measure.getMeasurementPeriodStart())));
    assertThat(
        measureInfoModel.getMeasurementPeriodEndDate(),
        equalTo(DateFormat.getDateInstance().format(measure.getMeasurementPeriodEnd())));
    assertThat(
        measureInfoModel.getMeasureScoring(),
        equalTo(
            measure.getGroups().get(0).getScoring())); // All groups expected to have same scoring;
    assertThat(
        measureInfoModel.getDescription(), equalTo(measure.getMeasureMetaData().getDescription()));
    assertThat(
        measureInfoModel.getCopyright(), equalTo(measure.getMeasureMetaData().getCopyright()));
    assertThat(
        measureInfoModel.getDisclaimer(), equalTo(measure.getMeasureMetaData().getDisclaimer()));
    assertThat(
        measureInfoModel.getRationale(), equalTo(measure.getMeasureMetaData().getRationale()));
    assertThat(
        measureInfoModel.getClinicalRecommendationStatement(),
        equalTo(measure.getMeasureMetaData().getClinicalRecommendation()));
    assertThat(
        measureInfoModel.getDefinitions().size(),
        equalTo(measure.getMeasureMetaData().getMeasureDefinitions().size()));
    assertThat(
        measureInfoModel.getDefinitions().get(0).getId(),
        equalTo(measure.getMeasureMetaData().getMeasureDefinitions().get(0).getId()));
    assertThat(
        measureInfoModel.getDefinitions().get(0).getTerm(),
        equalTo(measure.getMeasureMetaData().getMeasureDefinitions().get(0).getTerm()));
    assertThat(
        measureInfoModel.getDefinitions().get(0).getDefinition(),
        equalTo(measure.getMeasureMetaData().getMeasureDefinitions().get(0).getDefinition()));
  }

  @Test
  public void canBuildPopulationCriteriaModelFromMeasure() {
    List<HumanReadablePopulationCriteriaModel> populationCriteriaModels =
        humanReadableService.buildPopCriteria(measure);
    assertThat(populationCriteriaModels.size(), is(equalTo(1)));

    Group group = measure.getGroups().get(0);
    HumanReadablePopulationCriteriaModel popCriteriaModel = populationCriteriaModels.get(0);
    assertThat(popCriteriaModel.getName(), is(equalTo(group.getGroupDescription())));
    assertThat(popCriteriaModel.getPopulations().size(), is(group.getPopulations().size()));

    Population measurePopulation = group.getPopulations().get(0);
    HumanReadablePopulationModel populationModel = popCriteriaModel.getPopulations().get(0);
    assertThat(populationModel.getDisplay(), is(measurePopulation.getName().getDisplay()));
    assertThat(populationModel.getLogic(), is(equalTo(measurePopulation.getDefinition())));
    assertThat(populationModel.getExpressionName(), is(equalTo(measurePopulation.getDefinition())));
  }
}
