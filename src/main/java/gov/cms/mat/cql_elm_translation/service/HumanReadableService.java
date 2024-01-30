package gov.cms.mat.cql_elm_translation.service;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.Reference;
import gov.cms.madie.qdm.humanreadable.model.HumanReadable;
import gov.cms.madie.qdm.humanreadable.model.HumanReadableExpressionModel;
import gov.cms.madie.qdm.humanreadable.model.HumanReadableMeasureInformationModel;
import gov.cms.madie.qdm.humanreadable.model.HumanReadablePopulationCriteriaModel;
import gov.cms.madie.qdm.humanreadable.model.HumanReadablePopulationModel;
import gov.cms.mat.cql_elm_translation.utils.HumanReadableDateUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.IOException;
import java.text.DateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.web.util.HtmlUtils.htmlEscape;

@Component
@AllArgsConstructor
@Slf4j
public class HumanReadableService {

  private Template baseHumanReadableTemplate;

  /**
   * Generates the QDM Human Readable HTML from a MADiE Measure.
   *
   * @param measure MADiE Measure
   * @return QDM Human Readable HTML
   */
  public String generate(Measure measure) {
    if (measure == null) {
      throw new IllegalArgumentException("Measure cannot be null.");
    }

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
        .definitions(
            measure.getMeasureMetaData() != null
                ? measure.getMeasureMetaData().getMeasureDefinitions()
                : null)
        .references(
            measure.getMeasureMetaData().getReferences().stream()
                .map(
                    reference ->
                        new Reference()
                            .toBuilder()
                                .id(reference.getId())
                                .referenceText(escapeStr(reference.getReferenceText()))
                                .referenceType(reference.getReferenceType())
                                .build())
                .collect(Collectors.toList()))
        .build();
  }

  List<HumanReadablePopulationCriteriaModel> buildPopCriteria(Measure measure) {
    return measure.getGroups().stream()
        .map(
            group ->
                HumanReadablePopulationCriteriaModel.builder()
                    .id(group.getId())
                    .name(group.getGroupDescription())
                    .populations(buildPopulations(group))
                    .build())
        .collect(Collectors.toList());
  }

  private List<HumanReadablePopulationModel> buildPopulations(Group group) {
    return group.getPopulations().stream()
        .map(
            population ->
                HumanReadablePopulationModel.builder()
                    .name(population.getName().name())
                    .id(population.getId())
                    .display(population.getName().getDisplay())
                    .logic(population.getDefinition())
                    .expressionName(population.getDefinition())
                    .build())
        .collect(Collectors.toList());
  }

  List<HumanReadableExpressionModel> buildDefinitions(Measure measure) {
    return List.of(new HumanReadableExpressionModel());
  }

  private String escapeStr(String val) {
    if (val != null && !val.isEmpty()) {
      return htmlEscape(val);
    }
    return val;
  }
}
