package gov.cms.mat.cql_elm_translation.service;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.qdm.humanreadable.model.HumanReadable;
import gov.cms.madie.qdm.humanreadable.model.HumanReadableExpressionModel;
import gov.cms.madie.qdm.humanreadable.model.HumanReadableMeasureInformationModel;
import gov.cms.madie.qdm.humanreadable.model.HumanReadablePopulationCriteriaModel;
import gov.cms.madie.qdm.humanreadable.model.HumanReadablePopulationModel;
import gov.cms.madie.qdm.humanreadable.model.HumanReadableValuesetModel;
import gov.cms.mat.cql_elm_translation.utils.HumanReadableDateUtil;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.IOException;
import java.text.DateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class HumanReadableService {
  private Configuration freemarkerConfiguration;

  /**
   * Transforms MADiE Measure to the QDM HumanReadable data model, then generates the HR HTML.
   * @param measure MADiE Measure
   * @return String QDM Human Readable HTML
   * @throws TemplateException bad end
   * @throws IOException bad end
   */
  public String generate(Measure measure) throws TemplateException, IOException {

    HumanReadable hr =
        HumanReadable.builder()
            .measureInformation(buildMeasureInfo(measure))
            .populationCriteria(buildPopCriteria(measure))
            // TODO Value Set HR Model transform
            // .valuesetDataCriteriaList(buildValueSetCriteriaList(measure))
            // TODO Definition HR Model transform
            // .definitions(buildDefinitions(measure))
            .build();
    return generate(hr);
  }

  /**
   * Full HR Generation.
   * @param model
   * @return
   * @throws IOException
   * @throws TemplateException
   */
  public String generate(HumanReadable model) throws IOException, TemplateException {
    Map<String, Object> paramsMap = new HashMap<>();
    paramsMap.put("model", model);
    setMeasurementPeriodForQdm(model.getMeasureInformation());
    return FreeMarkerTemplateUtils.processTemplateIntoString(freemarkerConfiguration.getTemplate("humanreadable/human_readable.ftl"), paramsMap);
  }

  public String generateSinglePopulation(HumanReadablePopulationModel population) throws IOException, TemplateException {
    Map<String, Object> paramsMap = new HashMap<>();
    paramsMap.put("population", population);
    return FreeMarkerTemplateUtils.processTemplateIntoString(freemarkerConfiguration.getTemplate("humanreadable/population_human_readable.ftl"), paramsMap);
  }

  /**
   * Truncated HR originally displayed on MAT's Measure Details page.
   * @param measureInformationModel
   * @param measureModel
   * @return
   * @throws IOException
   * @throws TemplateException
   */
  public String generate(HumanReadableMeasureInformationModel measureInformationModel, String measureModel) throws IOException, TemplateException {
    HumanReadable model = new HumanReadable();
    model.setMeasureInformation(measureInformationModel);
    Map<String, Object> paramsMap = new HashMap<>();
    paramsMap.put("model", model);
    return FreeMarkerTemplateUtils.processTemplateIntoString(freemarkerConfiguration.getTemplate("humanreadable/human_readable_measuredetails.ftl"), paramsMap);
  }

  private void setMeasurementPeriodForQdm(HumanReadableMeasureInformationModel model) {
    boolean isCalendarYear = model.isCalendarYear();
    String measurementPeriodStartDate = model.getMeasurementPeriodStartDate();
    String measurementPeriodEndDate = model.getMeasurementPeriodEndDate();
    model.setMeasurementPeriod(HumanReadableDateUtil.getFormattedMeasurementPeriod(isCalendarYear, measurementPeriodStartDate, measurementPeriodEndDate));
  }

  private HumanReadableMeasureInformationModel buildMeasureInfo(Measure measure) {
    //TODO Needs safety checks
    return HumanReadableMeasureInformationModel.builder()
        .qdmVersion(5.6) // TODO Replace hardcode
        .ecqmTitle(measure.getEcqmTitle())
        .ecqmVersionNumber(measure.getVersion().toString())
        // TODO What is dis? Maybe MAT's "Calendar Year (January 1, 20XX through December 31, 20XX)"
        .calendarYear(false)
        .guid(measure.getMeasureSetId())
        // TODO needs safety check
        .patientBased(measure.getGroups().get(0).getPopulationBasis().equals("boolean"))
        .measurementPeriodStartDate(
            DateFormat.getDateInstance().format(measure.getMeasurementPeriodStart()))
        .measurementPeriodEndDate(
            DateFormat.getDateInstance().format(measure.getMeasurementPeriodEnd()))
        .measureScoring(measure.getGroups().get(0).getScoring()) //All groups expected to have same scoring
        .description(measure.getMeasureMetaData().getDescription())
        .copyright(measure.getMeasureMetaData().getCopyright())
        .disclaimer(measure.getMeasureMetaData().getDisclaimer())
        .rationale(measure.getMeasureMetaData().getRationale())
        .clinicalRecommendationStatement(measure.getMeasureMetaData().getClinicalRecommendation())
        .build();
  }

  private List<HumanReadablePopulationCriteriaModel> buildPopCriteria(Measure measure) {
    return measure.getGroups().stream()
        .map(group -> HumanReadablePopulationCriteriaModel.builder()
            .id(group.getId())
            .name(group.getGroupDescription())
            .populations(buildPopulations(group))
            .build())
        .collect(Collectors.toList());
  }

  private List<HumanReadablePopulationModel> buildPopulations(Group group) {
    return group.getPopulations().stream()
        .map(population -> HumanReadablePopulationModel.builder()
            .name(population.getName().name())
            .id(population.getId())
            .display(population.getName().getDisplay())
            .logic(population.getDefinition())
            .expressionName(population.getDefinition())
            .build())
        .collect(Collectors.toList());
  }

  private List<HumanReadableExpressionModel> buildDefinitions(Measure measure) {
    return List.of(new HumanReadableExpressionModel());
  }

  private List<HumanReadableValuesetModel> buildValueSetCriteriaList(Measure measure) {
    return List.of(new HumanReadableValuesetModel());
  }

}
