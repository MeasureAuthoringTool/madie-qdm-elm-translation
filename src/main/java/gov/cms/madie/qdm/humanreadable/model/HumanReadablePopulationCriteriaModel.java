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
public class HumanReadablePopulationCriteriaModel {
  private String name;
  private String id;
  private int sequence;
  private List<HumanReadablePopulationModel> populations;
  @Builder.Default
  private String scoreUnit = "";
}
