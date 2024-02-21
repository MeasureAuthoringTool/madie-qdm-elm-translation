package gov.cms.madie.qdm.humanreadable.model;

import java.util.List;

import gov.cms.madie.models.measure.MeasureDefinition;
import gov.cms.madie.models.measure.Reference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HumanReadableMeasureInformationModel {

  private double qdmVersion;
  private String ecqmTitle;
  private String ecqmIdentifier;
  private String ecqmVersionNumber;
  private String cbeNumber;
  private String guid;
  private String measurementPeriod;
  private boolean calendarYear;
  private String measurementPeriodStartDate;
  private String measurementPeriodEndDate;
  private String measureSteward;
  private List<String> measureDevelopers;
  private String endorsedBy;
  private String description;
  private String copyright;
  private String disclaimer;
  private String compositeScoringMethod;
  private String measureScoring;
  private List<String> measureTypes;
  private List<HumanReadableComponentMeasureModel> componentMeasures;
  private String stratification;
  private String stratificationDescription;
  private String riskAdjustment;
  private String rateAggregation;
  private String rationale;
  private String clinicalRecommendationStatement;
  private String improvementNotation;
  private List<Reference> references;

  private List<MeasureDefinition> definitions;
  private String guidance;
  private String transmissionFormat;
  private String initialPopulation;
  private String denominator;
  private String denominatorExclusions;
  private String denominatorExceptions;
  private String numerator;
  private String numeratorExclusions;
  private String measurePopulation;
  private String measurePopulationExclusions;
  private String measureObservations;
  private String supplementalDataElements;
  private String measureSet;
  private boolean patientBased;
  private boolean experimental;
  private String populationBasis;
}
