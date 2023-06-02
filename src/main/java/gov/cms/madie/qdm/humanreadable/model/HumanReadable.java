package gov.cms.madie.qdm.humanreadable.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HumanReadable {
  private HumanReadableMeasureInformationModel measureInformation;
  private List<HumanReadablePopulationCriteriaModel> populationCriteria;
  private List<HumanReadableExpressionModel> supplementalDataElements;
  private List<HumanReadableExpressionModel> riskAdjustmentVariables;
  private List<HumanReadableTerminologyModel> codeTerminologyList;
  private List<HumanReadableTerminologyModel> valuesetTerminologyList;
  private List<HumanReadableCodeModel> codeDataCriteriaList;
  private List<HumanReadableValuesetModel> valuesetDataCriteriaList;
  private List<HumanReadableTerminologyModel> valuesetAndCodeDataCriteriaList;
  private List<HumanReadableExpressionModel> definitions;
  private List<HumanReadableExpressionModel> functions;
}
