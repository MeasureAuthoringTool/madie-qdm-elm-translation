package gov.cms.madie.qdm.humanreadable.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HumanReadableExpressionModel {
  private String name;
  private String logic;
  private String id;
}
