package gov.cms.mat.cql_elm_translation.service;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.PopulationType;
import gov.cms.madie.models.measure.QdmMeasure;
import gov.cms.madie.qdm.humanreadable.model.HumanReadable;
import gov.cms.madie.qdm.humanreadable.model.HumanReadableExpressionModel;
import gov.cms.madie.qdm.humanreadable.model.HumanReadableMeasureInformationModel;
import gov.cms.madie.qdm.humanreadable.model.HumanReadablePopulationCriteriaModel;
import gov.cms.madie.qdm.humanreadable.model.HumanReadablePopulationModel;
import gov.cms.madie.qdm.humanreadable.model.HumanReadableTerminologyModel;
import gov.cms.madie.qdm.humanreadable.model.HumanReadableValuesetModel;
import gov.cms.mat.cql_elm_translation.dto.SourceDataCriteria;
import gov.cms.mat.cql_elm_translation.utils.HumanReadableDateUtil;
import gov.cms.mat.cql_elm_translation.utils.HumanReadableUtil;
import gov.cms.mat.cql_elm_translation.utils.cql.parsing.model.CQLDefinition;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@AllArgsConstructor
@Slf4j
public class HumanReadableService {

  private Template baseHumanReadableTemplate;

  private final DataCriteriaService dataCriteriaService;
  private final CqlParsingService cqlParsingService;

  /**
   * Generates the QDM Human Readable HTML from a MADiE Measure.
   *
   * @param measure MADiE Measure
   * @return QDM Human Readable HTML
   */
  public String generate(Measure measure, String accessToken) {
    if (measure == null) {
      throw new IllegalArgumentException("Measure cannot be null.");
    }

    List<SourceDataCriteria> sourceDataCriteria =
        dataCriteriaService.getSourceDataCriteria(measure.getCql(), accessToken);

    HumanReadable hr =
        HumanReadable.builder()
            .measureInformation(buildMeasureInfo(measure))
            .populationCriteria(buildPopCriteria(measure))
            .definitions(buildDefinitions(measure, accessToken))
            .functions(buildFunctions(measure, accessToken))
            .valuesetTerminologyList(buildValuesetTerminologyList(sourceDataCriteria))
            .codeTerminologyList(buildCodeTerminologyList(sourceDataCriteria))
            .build();
    hr.setValuesetAndCodeDataCriteriaList(
        Stream.concat(
                hr.getValuesetTerminologyList().stream(), hr.getCodeTerminologyList().stream())
            .toList());
    hr.setSupplementalDataElements(buildSupplementalDataElements(measure, hr.getDefinitions()));
    hr.setRiskAdjustmentVariables(buildRiskAdjustmentVariables(measure, hr.getDefinitions()));
    return generate(hr);
  }

  private String generate(HumanReadable model) {
    Map<String, Object> paramsMap = new HashMap<>();
    paramsMap.put("model", model);
    setMeasurementPeriodForQdm(model.getMeasureInformation());
    try {
      return FreeMarkerTemplateUtils.processTemplateIntoString(
          baseHumanReadableTemplate, paramsMap);
    } catch (IOException | TemplateException e) {
      throw new IllegalStateException("Unable to process Human Readable from Measure", e);
    }
  }

  private void setMeasurementPeriodForQdm(HumanReadableMeasureInformationModel model) {
    boolean isCalendarYear = model.isCalendarYear();
    String measurementPeriodStartDate = model.getMeasurementPeriodStartDate();
    String measurementPeriodEndDate = model.getMeasurementPeriodEndDate();
    model.setMeasurementPeriod(
        HumanReadableDateUtil.getFormattedMeasurementPeriod(
            isCalendarYear, measurementPeriodStartDate, measurementPeriodEndDate));
  }

  HumanReadableMeasureInformationModel buildMeasureInfo(Measure measure) {
    // TODO Needs safety checks
    return HumanReadableMeasureInformationModel.builder()
        .qdmVersion(5.6) // TODO Replace hardcode
        .ecqmTitle(measure.getEcqmTitle())
        .ecqmVersionNumber(measure.getVersion().toString())
        .calendarYear(false) // Unsupported MAT feature, default to false
        .guid(measure.getMeasureSetId())
        .cbeNumber(HumanReadableUtil.getCbeNumber(measure))
        .endorsedBy(HumanReadableUtil.getEndorsedBy(measure))
        // TODO needs safety check
        .patientBased(measure.getGroups().get(0).getPopulationBasis().equals("boolean"))
        .measurementPeriodStartDate(
            DateFormat.getDateInstance().format(measure.getMeasurementPeriodStart()))
        .measurementPeriodEndDate(
            DateFormat.getDateInstance().format(measure.getMeasurementPeriodEnd()))
        .measureScoring(
            measure.getGroups().get(0).getScoring()) // All groups expected to have same scoring
        .description(measure.getMeasureMetaData().getDescription())
        .copyright(measure.getMeasureMetaData().getCopyright())
        .disclaimer(measure.getMeasureMetaData().getDisclaimer())
        .rationale(measure.getMeasureMetaData().getRationale())
        .clinicalRecommendationStatement(measure.getMeasureMetaData().getClinicalRecommendation())
        .measureDevelopers(HumanReadableUtil.getMeasureDevelopers(measure))
        .measureSteward(
            measure.getMeasureMetaData().getSteward() != null
                ? measure.getMeasureMetaData().getSteward().getName()
                : null)
        .measureTypes(HumanReadableUtil.getMeasureTypes(measure))
        .stratification(HumanReadableUtil.getStratification(measure))
        .riskAdjustment(measure.getRiskAdjustmentDescription())
        .supplementalDataElements(measure.getSupplementalDataDescription())
        .rateAggregation(((QdmMeasure) measure).getRateAggregation())
        .improvementNotation(((QdmMeasure) measure).getImprovementNotation())
        .references(measure.getMeasureMetaData().getReferences())
        .definition(HumanReadableUtil.getDefinitions(measure))
        .guidance(measure.getMeasureMetaData().getGuidance())
        .transmissionFormat(measure.getMeasureMetaData().getTransmissionFormat())
        .initialPopulation(
            HumanReadableUtil.getPopulationDescription(
                measure, PopulationType.INITIAL_POPULATION.name()))
        .denominator(
            HumanReadableUtil.getPopulationDescription(measure, PopulationType.DENOMINATOR.name()))
        .denominatorExceptions(
            HumanReadableUtil.getPopulationDescription(
                measure, PopulationType.DENOMINATOR_EXCLUSION.name()))
        .numerator(
            HumanReadableUtil.getPopulationDescription(measure, PopulationType.NUMERATOR.name()))
        .numeratorExclusions(
            HumanReadableUtil.getPopulationDescription(
                measure, PopulationType.NUMERATOR_EXCLUSION.name()))
        .denominatorExceptions(
            HumanReadableUtil.getPopulationDescription(
                measure, PopulationType.DENOMINATOR_EXCEPTION.name()))
        .build();
  }

  List<HumanReadablePopulationCriteriaModel> buildPopCriteria(Measure measure) {
    return measure.getGroups().stream()
        .map(
            group ->
                HumanReadablePopulationCriteriaModel.builder()
                    .id(group.getId())
                    .name(group.getGroupDescription())
                    .populations(
                        Stream.concat(
                                buildPopulations(group).stream(),
                                buildStratification(group).stream())
                            .toList())
                    .build())
        .collect(Collectors.toList());
  }

  List<HumanReadablePopulationModel> buildPopulations(Group group) {
    return group.getPopulations().stream()
        .map(
            population ->
                HumanReadablePopulationModel.builder()
                    .name(population.getName().name())
                    .id(population.getId())
                    .display(population.getName().getDisplay())
                    .logic(population.getDefinition())
                    .expressionName(population.getDefinition())
                    .inGroup(StringUtils.isBlank(population.getDefinition()) ? false : true)
                    .build())
        .collect(Collectors.toList());
  }

  List<HumanReadablePopulationModel> buildStratification(Group group) {
    return group.getStratifications().stream()
        .map(
            stratification ->
                HumanReadablePopulationModel.builder()
                    .name("Stratification")
                    .id(stratification.getId())
                    .display("Stratification")
                    .logic(stratification.getCqlDefinition())
                    .expressionName(stratification.getCqlDefinition())
                    .inGroup(StringUtils.isBlank(stratification.getCqlDefinition()) ? false : true)
                    .build())
        .collect(Collectors.toList());
  }

  List<HumanReadableExpressionModel> buildDefinitions(Measure measure, String accessToken) {
    Set<CQLDefinition> allDefinitions =
        cqlParsingService.getAllDefinitions(measure.getCql(), accessToken);
    List<CQLDefinition> definitions =
        allDefinitions.stream()
            .filter(definition -> definition.getParentLibrary() == null)
            .collect(Collectors.toList());

    List<HumanReadableExpressionModel> expressions =
        definitions.stream()
            .map(
                definition ->
                    HumanReadableExpressionModel.builder()
                        .id(definition.getId())
                        .name(definition.getName())
                        .logic(
                            definition
                                .getLogic()
                                .substring(definition.getLogic().indexOf('\n') + 1))
                        .build())
            .collect(Collectors.toList());
    expressions.sort(Comparator.comparing(HumanReadableExpressionModel::getName));
    return expressions;
  }

  List<HumanReadableExpressionModel> buildFunctions(Measure measure, String accessToken) {
    Set<CQLDefinition> allDefinitions =
        cqlParsingService.getAllDefinitions(measure.getCql(), accessToken);
    List<CQLDefinition> functions =
        allDefinitions.stream()
            .filter(
                definition ->
                    definition.isFunction() == true
                        && findUsedFunction(measure, accessToken, definition.getId()))
            .collect(Collectors.toList());

    List<HumanReadableExpressionModel> expressions =
        functions.stream()
            .map(
                definition ->
                    HumanReadableExpressionModel.builder()
                        .id(definition.getId())
                        .name(definition.getLibraryDisplayName() + "." + definition.getName())
                        .logic(
                            definition
                                .getLogic()
                                .substring(definition.getLogic().indexOf('\n') + 1))
                        .build())
            .collect(Collectors.toList());
    expressions.sort(Comparator.comparing(HumanReadableExpressionModel::getName));
    return expressions;
  }

  boolean findUsedFunction(Measure measure, String accessToken, String id) {
    Map<String, Set<String>> usedFunctions =
        cqlParsingService.getUsedFunctions(measure.getCql(), accessToken);
    return usedFunctions != null && !usedFunctions.isEmpty() && usedFunctions.containsKey(id);
  }

  List<HumanReadableTerminologyModel> buildValuesetTerminologyList(
      List<SourceDataCriteria> sourceDataCriteria) {
    List<SourceDataCriteria> valuesetsSourceDataCriteria =
        sourceDataCriteria.stream()
            .filter(valueset -> StringUtils.isBlank(valueset.getCodeId()))
            .collect(Collectors.toList());
    Set<HumanReadableValuesetModel> valuesets =
        valuesetsSourceDataCriteria.stream()
            .map(
                criteria ->
                    new HumanReadableValuesetModel(
                        criteria.getTitle(),
                        criteria.getOid(),
                        "",
                        criteria.getDescription().split(":")[0]))
            .collect(Collectors.toSet());
    List<HumanReadableTerminologyModel> valuesetList = new ArrayList<>(valuesets);
    valuesetList.sort(Comparator.comparing(HumanReadableTerminologyModel::getDataCriteriaDisplay));
    return valuesetList;
  }

  List<HumanReadableTerminologyModel> buildCodeTerminologyList(
      List<SourceDataCriteria> sourceDataCriteria) {
    List<SourceDataCriteria> codeSourceDataCriteria =
        sourceDataCriteria.stream()
            .filter(valueset -> StringUtils.isNotBlank(valueset.getCodeId()))
            .collect(Collectors.toList());
    Set<HumanReadableValuesetModel> codes =
        codeSourceDataCriteria.stream()
            .map(
                criteria ->
                    new HumanReadableValuesetModel(
                        criteria.getTitle(),
                        criteria.getOid(),
                        "",
                        criteria.getDescription().split(":")[0]))
            .collect(Collectors.toSet());
    List<HumanReadableTerminologyModel> codeList = new ArrayList<>(codes);
    codeList.sort(Comparator.comparing(HumanReadableTerminologyModel::getDataCriteriaDisplay));
    return codeList;
  }

  List<HumanReadableExpressionModel> buildSupplementalDataElements(
      Measure measure, List<HumanReadableExpressionModel> definitions) {
    if (!CollectionUtils.isEmpty(measure.getSupplementalData())) {
      return measure.getSupplementalData().stream()
          .map(
              supplementalData ->
                  HumanReadableExpressionModel.builder()
                      .id(UUID.randomUUID().toString())
                      .name(supplementalData.getDefinition())
                      .logic(getLogic(supplementalData.getDefinition(), definitions))
                      .build())
          .collect(Collectors.toList());
    }
    return null;
  }

  List<HumanReadableExpressionModel> buildRiskAdjustmentVariables(
      Measure measure, List<HumanReadableExpressionModel> definitions) {
    if (!CollectionUtils.isEmpty(measure.getRiskAdjustments())) {
      return measure.getRiskAdjustments().stream()
          .map(
              riskAdjustment ->
                  HumanReadableExpressionModel.builder()
                      .id(UUID.randomUUID().toString())
                      .name(riskAdjustment.getDefinition())
                      .logic(
                          "[" + getLogic(riskAdjustment.getDefinition(), definitions).trim() + "]")
                      .build())
          .collect(Collectors.toList());
    }
    return null;
  }

  String getLogic(String definition, List<HumanReadableExpressionModel> definitions) {
    for (HumanReadableExpressionModel humanReadableDefinition : definitions) {
      if (definition.equalsIgnoreCase(humanReadableDefinition.getName())) {
        return humanReadableDefinition.getLogic();
      }
    }
    return "";
  }
}
