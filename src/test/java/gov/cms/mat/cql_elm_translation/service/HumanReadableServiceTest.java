package gov.cms.mat.cql_elm_translation.service;

import freemarker.template.Template;
import gov.cms.madie.models.common.ModelType;
import gov.cms.madie.models.common.Organization;
import gov.cms.madie.models.common.Version;
import gov.cms.madie.models.measure.*;
import gov.cms.madie.qdm.humanreadable.model.HumanReadableCodeModel;
import gov.cms.madie.qdm.humanreadable.model.HumanReadableExpressionModel;
import gov.cms.madie.qdm.humanreadable.model.HumanReadableMeasureInformationModel;
import gov.cms.madie.qdm.humanreadable.model.HumanReadablePopulationCriteriaModel;
import gov.cms.madie.qdm.humanreadable.model.HumanReadablePopulationModel;
import gov.cms.madie.qdm.humanreadable.model.HumanReadableTerminologyModel;
import gov.cms.madie.qdm.humanreadable.model.HumanReadableValuesetModel;
import gov.cms.mat.cql_elm_translation.dto.SourceDataCriteria;
import gov.cms.mat.cql_elm_translation.utils.cql.parsing.model.CQLCode;
import gov.cms.mat.cql_elm_translation.utils.cql.parsing.model.CQLDefinition;
import gov.cms.mat.cql_elm_translation.utils.cql.parsing.model.CQLValueSet;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.text.Collator;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HumanReadableServiceTest {
  @InjectMocks HumanReadableService humanReadableService;

  @Mock Template template;

  @Mock DataCriteriaService dataCriteriaService;
  @Mock CqlParsingService cqlParsingService;
  @Mock Collator collator;

  private QdmMeasure measure;
  private final Date now = new Date();
  private Set<CQLDefinition> allDefinitions;
  private Set<CQLDefinition> onlyDefinitions;
  private CQLDefinition function;
  private Set<String> usedFunctionIds;

  private SourceDataCriteria sourceDataCriteria1;
  private SourceDataCriteria sourceDataCriteria2;
  private SourceDataCriteria sourceDataCriteria3;
  private SourceDataCriteria sourceDataCriteria4;

  @BeforeEach
  void setUp() {
    measure =
        QdmMeasure.builder()
            .id("1234")
            .model(ModelType.QDM_5_6.getValue())
            .cql("test CQL")
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
                    .references(
                        List.of(
                            Reference.builder()
                                .id("ref-123")
                                .referenceType("Citation")
                                .referenceText("Example Citation Reference Text")
                                .build(),
                            Reference.builder()
                                .id("ref-xyz")
                                .referenceType("Justification")
                                .referenceText("Example < Justification Reference Text")
                                .build()))
                    .steward(Organization.builder().name("stewardOrg").build())
                    .definition("test definition")
                    .endorsements(
                        List.of(
                            Endorsement.builder()
                                .endorsementId("test endorsement id")
                                .endorser("test endorser")
                                .build()))
                    .measureSetTitle("test measure title")
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
                                    .definition("Initial Population")
                                    .build(),
                                Population.builder()
                                    .id("p2")
                                    .name(PopulationType.DENOMINATOR)
                                    .build()))
                        .stratifications(
                            List.of(
                                Stratification.builder()
                                    .id("testStrat1Id")
                                    .cqlDefinition(PopulationType.INITIAL_POPULATION.name())
                                    .build(),
                                Stratification.builder()
                                    .id("testStrat2Id")
                                    .cqlDefinition("")
                                    .build()))
                        .measureObservations(
                            List.of(
                                MeasureObservation.builder()
                                    .definition(PopulationType.INITIAL_POPULATION.name())
                                    .build(),
                                MeasureObservation.builder().build()))
                        .build()))
            .baseConfigurationTypes(List.of(BaseConfigurationTypes.OUTCOME))
            .riskAdjustmentDescription("test risk adjustment")
            .supplementalDataDescription("test supplemental data elements description")
            .rateAggregation("test rate aggregation")
            .improvementNotation("test improvment notation")
            .supplementalData(
                List.of(
                    DefDescPair.builder()
                        .definition("SDE Ethnicity")
                        .description("test SDE Ethnicity")
                        .build()))
            .riskAdjustments(
                List.of(
                    DefDescPair.builder()
                        .definition("SDE Ethnicity")
                        .description("test SDE Ethnicity")
                        .build()))
            .build();

    CQLDefinition definition1 =
        CQLDefinition.builder()
            .id("Initial Population")
            .definitionName("Initial Population")
            .parentLibrary(null)
            .definitionLogic(
                "define \"Initial Population\":\n  \"Encounter with Opioid Administration Outside of Operating Room\"")
            .build();
    CQLDefinition definition2 =
        CQLDefinition.builder()
            .id("Opioid Administration")
            .definitionName("Opioid Administration")
            .definitionLogic(
                "define \"Opioid Administration\":\n  [\"Medication, Administered\": \"Opioids, All\"]")
            .parentLibrary(null)
            .build();
    CQLDefinition definition3 =
        CQLDefinition.builder()
            .id("SDE Ethnicity")
            .definitionName("SDE Ethnicity")
            .definitionLogic(
                "define \"SDE Ethnicity\":\n  [\"Patient Characteristic Ethnicity\": \"Ethnicity\"]")
            .parentLibrary(null)
            .build();
    CQLDefinition definition4 =
            CQLDefinition.builder()
                    .id("Fake")
                    .definitionName("Fake")
                    .definitionLogic(
                            "define \"Fake\":\n  [\"Patient Characteristic Ethnicity\": \"Ethnicity\"]")
                    .parentLibrary("MATGlobalCommonFunctionsQDM")
                    .build();
    function =
        CQLDefinition.builder()
            .id("MATGlobalCommonFunctionsQDM-1.0.000|Global|NormalizeInterval")
            .definitionName("NormalizeInterval")
            .definitionLogic(
                "define function \"NormalizeInterval\"(pointInTime DateTime, period Interval<DateTime> ):\n  if pointInTime is not null then Interval[pointInTime, pointInTime]\n    else if period is not null then period \n    else null as Interval<DateTime>")
            .parentLibrary("MATGlobalCommonFunctionsQDM")
            .isFunction(true)
            .build();
    onlyDefinitions = new HashSet<>(Arrays.asList(definition1, definition2, definition3, definition4));
    allDefinitions = new HashSet<>(Arrays.asList(definition1, definition2, function, definition3, definition4));
    usedFunctionIds = new HashSet<>(Arrays.asList(function.getId()));

    sourceDataCriteria1 =
        SourceDataCriteria.builder()
            .oid("2.16.840.1.113762.1.4.1248.119")
            .title("Opioid Antagonist")
            .description("Medication, Administered: Opioid Antagonist")
            .type("MedicationAdministered")
            .name("Opioid Antagonist")
            .build();
    sourceDataCriteria2 =
        SourceDataCriteria.builder()
            .oid("2.16.840.1.113883.3.666.5.307")
            .title("Encounter Inpatient")
            .description("Encounter, Performed: Encounter Inpatient")
            .type("EncounterPerformed")
            .name("Encounter Inpatient")
            .build();
    sourceDataCriteria3 =
        SourceDataCriteria.builder()
            .oid("1096-7")
            .title("Operating Room/Suite")
            .description("Operating Room/Suite")
            .type("EncounterPerformed")
            .name("Encounter Inpatient")
            .codeId("testCodeId")
            .build();
    sourceDataCriteria4 =
        SourceDataCriteria.builder()
            .oid("2.16.840.1.114222.4.11.837")
            .title("Ethnicity")
            .description("Patient Characteristic Ethnicity: Ethnicity")
            .type("PatientCharacteristicEthnicity")
            .name("Ethnicity")
            .build();
  }

  @Test
  public void generateHumanReadableThrowsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class, () -> humanReadableService.generate(null, "accessToken"));
  }

  // result is an empty string, Mocking Template doesn't yield expected results.
  @Test
  public void generateHumanReadableSuccessfully() {
    var result = humanReadableService.generate(measure, "accessToken");
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
        measureInfoModel.getMeasureDevelopers().size(),
        equalTo(measure.getMeasureMetaData().getDevelopers().size()));
    assertThat(
        measureInfoModel.getMeasureSteward(),
        equalTo(measure.getMeasureMetaData().getSteward().getName()));
    assertThat(
        measureInfoModel.getRiskAdjustment(), equalTo(measure.getRiskAdjustmentDescription()));
    assertThat(
        measureInfoModel.getSupplementalDataElements(),
        equalTo(measure.getSupplementalDataDescription()));
    assertThat(measureInfoModel.getRateAggregation(), equalTo(measure.getRateAggregation()));
    assertThat(
        measureInfoModel.getImprovementNotation(), equalTo(measure.getImprovementNotation()));
    assertThat(
        measureInfoModel.getReferences().size(),
        equalTo(measure.getMeasureMetaData().getReferences().size()));
    assertThat(
        measureInfoModel.getDefinition(), equalTo(measure.getMeasureMetaData().getDefinition()));
    assertThat(measureInfoModel.getGuidance(), equalTo(measure.getMeasureMetaData().getGuidance()));
    assertThat(
        measureInfoModel.getCbeNumber(),
        equalTo(measure.getMeasureMetaData().getEndorsements().get(0).getEndorsementId()));
    assertThat(
        measureInfoModel.getEndorsedBy(),
        equalTo(measure.getMeasureMetaData().getEndorsements().get(0).getEndorser()));
    assertThat(
        measureInfoModel.getMeasureSet(),
        equalTo(measure.getMeasureMetaData().getMeasureSetTitle()));
  }

  @Test
  public void testBuildMeasureInfoSomeMeasureMetaDataNull() {
    measure.getMeasureMetaData().setSteward(null);
    measure.getMeasureMetaData().setGuidance(null);
    HumanReadableMeasureInformationModel measureInfoModel =
        humanReadableService.buildMeasureInfo(measure);
    assertNull(measureInfoModel.getMeasureSteward());
    assertNull(measureInfoModel.getGuidance());

    assertThat(
        measureInfoModel.getDefinition(), equalTo(measure.getMeasureMetaData().getDefinition()));
    assertEquals(
        measure.getMeasureMetaData().getReferences().get(0),
        measureInfoModel.getReferences().get(0));
    // assertNotEquals as the "<" will be escaped and replaced by &lt
    assertNotEquals(
        measure.getMeasureMetaData().getReferences().get(1),
        measureInfoModel.getReferences().get(1));
  }

  @Test
  public void testBuildMeasureInfoWhenReferenceIsNull() {
    measure.getMeasureMetaData().setReferences(null);
    HumanReadableMeasureInformationModel measureInfoModel =
        humanReadableService.buildMeasureInfo(measure);
    assertNull(measureInfoModel.getReferences());
  }

  @Test
  public void testBuildMeasureInfoWhenReferenceTextIsEmpty() {
    measure.getMeasureMetaData().getReferences().get(0).setReferenceText("");
    HumanReadableMeasureInformationModel measureInfoModel =
        humanReadableService.buildMeasureInfo(measure);
    assertEquals("", measureInfoModel.getReferences().get(0).getReferenceText());
  }

  @Test
  public void canBuildPopulationCriteriaModelFromMeasure() {

    List<HumanReadablePopulationCriteriaModel> populationCriteriaModels =
        humanReadableService.buildPopCriteria(measure, allDefinitions);
    assertThat(populationCriteriaModels.size(), is(equalTo(1)));

    Group group = measure.getGroups().get(0);
    HumanReadablePopulationCriteriaModel popCriteriaModel = populationCriteriaModels.get(0);
    assertThat(popCriteriaModel.getName(), is(equalTo(group.getGroupDescription())));
    assertThat(
        popCriteriaModel.getPopulations().size(),
        is(
            group.getPopulations().size()
                + group.getStratifications().size()
                + group.getMeasureObservations().size()));

    Population measurePopulation = group.getPopulations().get(0);
    HumanReadablePopulationModel populationModel = popCriteriaModel.getPopulations().get(0);
    assertThat(populationModel.getDisplay(), is(measurePopulation.getName().getDisplay()));
    assertThat(
        populationModel.getLogic(),
        is(equalTo("  \"Encounter with Opioid Administration Outside of Operating Room\"")));
    assertThat(populationModel.getExpressionName(), is(equalTo(measurePopulation.getDefinition())));
  }

  @Test
  public void testBuildDefinitions() {
    List<HumanReadableExpressionModel> definitions =
        humanReadableService.buildDefinitions(onlyDefinitions);
    assertThat(definitions.size(), is(equalTo(3)));
  }

  @Test
  public void testBuildDefinitionsWithFunctionsRemoved() {
    List<HumanReadableExpressionModel> definitions =
            humanReadableService.buildDefinitions(allDefinitions);
    assertThat(definitions.size(), is(equalTo(3)));
  }

  @Test
  public void testBuildFunctions() {
    when(cqlParsingService.getAllDefinitions(anyString(), anyString())).thenReturn(allDefinitions);
    Map<String, Set<String>> usedFunctions = new HashMap<>();
    usedFunctions.put(function.getId(), usedFunctionIds);
    when(cqlParsingService.getUsedFunctions(anyString(), anyString())).thenReturn(usedFunctions);

    List<HumanReadableExpressionModel> functions =
        humanReadableService.buildFunctions(measure, "accessToken");
    assertThat(functions.size(), is(equalTo(1)));
  }

  @Test
  public void testBuildFunctionsNotFound() {
    when(cqlParsingService.getAllDefinitions(anyString(), anyString())).thenReturn(allDefinitions);
    when(cqlParsingService.getUsedFunctions(anyString(), anyString())).thenReturn(null);

    List<HumanReadableExpressionModel> functions =
        humanReadableService.buildFunctions(measure, "accessToken");
    assertThat(functions.size(), is(equalTo(0)));
  }

  @Test
  public void testBuildValuesetDataCriteriaList() {
    when(dataCriteriaService.getUsedValuesets(anyString(), anyString()))
        .thenReturn(List.of("Opioid Antagonist", "Encounter Inpatient", "Ethnicity"));

    List<HumanReadableValuesetModel> result =
        humanReadableService.buildValuesetDataCriteriaList(
            List.of(
                sourceDataCriteria1, sourceDataCriteria2, sourceDataCriteria3, sourceDataCriteria4),
            measure,
            "accessToken");
    assertThat(result.size(), is(equalTo(3)));
  }

  @Test
  public void testBuildValuesetDataCriteriaListNullUsedValuesets() {
    when(dataCriteriaService.getUsedValuesets(anyString(), anyString())).thenReturn(null);

    List<HumanReadableValuesetModel> result =
        humanReadableService.buildValuesetDataCriteriaList(
            List.of(
                sourceDataCriteria1, sourceDataCriteria2, sourceDataCriteria3, sourceDataCriteria4),
            measure,
            "accessToken");
    assertThat(result.size(), is(equalTo(0)));
  }

  @Test
  public void testBuildValuesetDataCriteriaListEmptyUsedValuesets() {
    when(dataCriteriaService.getUsedValuesets(anyString(), anyString())).thenReturn(List.of());

    List<HumanReadableValuesetModel> result =
        humanReadableService.buildValuesetDataCriteriaList(
            List.of(
                sourceDataCriteria1, sourceDataCriteria2, sourceDataCriteria3, sourceDataCriteria4),
            measure,
            "accessToken");
    assertThat(result.size(), is(equalTo(0)));
  }

  @Test
  public void testBuildValuesetDataCriteriaListNotFound() {
    when(dataCriteriaService.getUsedValuesets(anyString(), anyString()))
        .thenReturn(List.of("different value set"));

    List<HumanReadableValuesetModel> result =
        humanReadableService.buildValuesetDataCriteriaList(
            List.of(
                sourceDataCriteria1, sourceDataCriteria2, sourceDataCriteria3, sourceDataCriteria4),
            measure,
            "accessToken");
    assertThat(result.size(), is(equalTo(0)));
  }

  @Test
  public void testBuildSupplementalDataElements() {
    List<HumanReadableExpressionModel> definitions =
        humanReadableService.buildDefinitions(allDefinitions);
    List<HumanReadableExpressionModel> supplementalData =
        humanReadableService.buildSupplementalDataElements(measure, definitions);
    assertThat(supplementalData.size(), is(equalTo(1)));
  }

  @Test
  public void testBuildSupplementalDataElementsNull() {
    List<HumanReadableExpressionModel> definitions =
        humanReadableService.buildDefinitions(allDefinitions);
    measure.setSupplementalData(null);
    List<HumanReadableExpressionModel> supplementalData =
        humanReadableService.buildSupplementalDataElements(measure, definitions);
    assertNull(supplementalData);
  }

  @Test
  public void testBuildRiskAdjustmentVariablesNull() {
    List<HumanReadableExpressionModel> definitions =
        humanReadableService.buildDefinitions(allDefinitions);
    measure.setRiskAdjustments(null);
    List<HumanReadableExpressionModel> riskAdjustment =
        humanReadableService.buildRiskAdjustmentVariables(measure, definitions);
    assertNull(riskAdjustment);
  }

  @Test
  public void testIsUsedFunctionNull() {
    when(cqlParsingService.getUsedFunctions(anyString(), anyString())).thenReturn(null);
    boolean result = humanReadableService.isUsedFunction(measure, "accessToken", "testId");
    assertFalse(result);
  }

  @Test
  public void testIsUsedFunctionEmpty() {
    when(cqlParsingService.getUsedFunctions(anyString(), anyString())).thenReturn(new HashMap<>());
    boolean result = humanReadableService.isUsedFunction(measure, "accessToken", "another testId");
    assertFalse(result);
  }

  @Test
  public void testIsUsedFunctionNotFound() {
    Map<String, Set<String>> usedFunctions = new HashMap<>();
    usedFunctions.put("testId", new HashSet<>(Arrays.asList("testId")));
    when(cqlParsingService.getUsedFunctions(anyString(), anyString())).thenReturn(usedFunctions);

    boolean result = humanReadableService.isUsedFunction(measure, "accessToken", "another testId");

    assertFalse(result);
  }

  @Test
  public void testBuildCodeDataCriteriaList() {
    CQLCode cqlCode = CQLCode.builder().build();
    List<HumanReadableCodeModel> result =
        humanReadableService.buildCodeDataCriteriaList(List.of(cqlCode));
    assertThat(result.size(), is(equalTo(1)));
  }

  @Test
  public void testBuildCodeDataCriteriaListNull() {
    List<HumanReadableCodeModel> result = humanReadableService.buildCodeDataCriteriaList(List.of());
    assertThat(result.size(), is(equalTo(0)));
  }

  @Test
  public void testFindUsedCQLValueSet() {
    CQLValueSet cqlValueSet =
        CQLValueSet.builder()
            .name("Routes of Administration for Opioid Antagonists")
            .oid("2.16.840.1.113762.1.4.1248.187")
            .build();
    when(dataCriteriaService.getUsedCQLValuesets(anyString(), anyString()))
        .thenReturn(List.of(cqlValueSet));

    Set<HumanReadableValuesetModel> result =
        humanReadableService.findUsedCQLValueSet(
            measure,
            "accessToken",
            List.of(
                sourceDataCriteria1,
                sourceDataCriteria2,
                sourceDataCriteria3,
                sourceDataCriteria4));
    assertThat(result.size(), is(equalTo(1)));
    assertThat(result.iterator().next().getName(), is(equalTo(cqlValueSet.getName())));
  }

  @Test
  public void testFindUsedCQLValueSetNoOtherValueSet() {
    CQLValueSet cqlValueSet =
        CQLValueSet.builder()
            .name("Encounter Inpatient")
            .oid("2.16.840.1.113883.3.666.5.307")
            .build();
    when(dataCriteriaService.getUsedCQLValuesets(anyString(), anyString()))
        .thenReturn(List.of(cqlValueSet));

    Set<HumanReadableValuesetModel> result =
        humanReadableService.findUsedCQLValueSet(
            measure,
            "accessToken",
            List.of(
                sourceDataCriteria1,
                sourceDataCriteria2,
                sourceDataCriteria3,
                sourceDataCriteria4));
    assertThat(result.size(), is(equalTo(0)));
  }

  @Test
  public void testBuildValuesetTerminologyList() {
    when(dataCriteriaService.getUsedValuesets(anyString(), anyString()))
        .thenReturn(
            List.of("Opioid Antagonist", "Routes of Administration for Opioid Antagonists"));

    List<HumanReadableValuesetModel> valuesetModels =
        humanReadableService.buildValuesetDataCriteriaList(
            List.of(
                sourceDataCriteria1, sourceDataCriteria2, sourceDataCriteria3, sourceDataCriteria4),
            measure,
            "accessToken");
    System.out.println(
        "valuesetModels = " + (valuesetModels != null ? valuesetModels.toString() : " null"));

    CQLValueSet cqlValueSet1 =
        CQLValueSet.builder()
            .name("Opioid Antagonist")
            .oid("2.16.840.1.113762.1.4.1248.119")
            .build();
    CQLValueSet cqlValueSet2 =
        CQLValueSet.builder()
            .name("Routes of Administration for Opioid Antagonists")
            .oid("2.16.840.1.113762.1.4.1248.187")
            .build();
    when(dataCriteriaService.getUsedCQLValuesets(anyString(), anyString()))
        .thenReturn(List.of(cqlValueSet1, cqlValueSet2));

    List<HumanReadableTerminologyModel> terminologyModels =
        humanReadableService.buildValuesetTerminologyList(
            valuesetModels,
            measure,
            "accessToken",
            List.of(
                sourceDataCriteria1,
                sourceDataCriteria2,
                sourceDataCriteria3,
                sourceDataCriteria4));
    assertThat(terminologyModels.size(), is(equalTo(2)));
    assertThat(terminologyModels.get(0).getName(), is(equalTo("Opioid Antagonist")));
    assertThat(
        terminologyModels.get(1).getName(),
        is(equalTo("Routes of Administration for Opioid Antagonists")));
  }

  @Test
  public void testbuildStratificationWithNoStratifications() {
    Group group = measure.getGroups().get(0);
    group.setStratifications(null);
    List<HumanReadablePopulationModel> model =
        humanReadableService.buildStratification(group, allDefinitions);
    assertTrue(CollectionUtils.isEmpty(model));
  }

  @Test
  public void testBuildMeasureObservationWithoutMeasureObservations() {
    Group group = measure.getGroups().get(0);
    group.setMeasureObservations(null);
    List<HumanReadablePopulationModel> model =
        humanReadableService.buildMeasureObservation(group, allDefinitions);
    assertTrue(CollectionUtils.isEmpty(model));
  }
}
