package gov.cms.madie.qdm.humanreadable.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HumanReadablePopulationModel {
  private String id;
  private String name;
  private String logic;
  private String expressionName;
  private String expressionUUID;
  private String aggregateFunction;
  private boolean inGroup;
  private String associatedPopulationName;
  private String display;
  private String type; }
