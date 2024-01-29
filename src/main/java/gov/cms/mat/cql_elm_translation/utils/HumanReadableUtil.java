package gov.cms.mat.cql_elm_translation.utils;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.nimbusds.oauth2.sdk.util.CollectionUtils;

import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.QdmMeasure;

public class HumanReadableUtil {

  public static List<String> getMeasureDevelopers(Measure measure) {
    if (measure.getMeasureMetaData() != null
        && CollectionUtils.isNotEmpty(measure.getMeasureMetaData().getDevelopers())) {
      return measure.getMeasureMetaData().getDevelopers().stream()
          .map(developer -> developer.getName() + "\n")
          .collect(Collectors.toList());
    }
    return null;
  }

  public static String getCbeNumber(Measure measure) {
    if (measure.getMeasureMetaData() != null
        && CollectionUtils.isNotEmpty(measure.getMeasureMetaData().getEndorsements())) {
      return measure.getMeasureMetaData().getEndorsements().stream()
          .map(endorser -> endorser.getEndorsementId())
          .collect(Collectors.joining("\n"));
    }
    return null;
  }

  public static String getEndorsedBy(Measure measure) {
    if (measure.getMeasureMetaData() != null
        && CollectionUtils.isNotEmpty(measure.getMeasureMetaData().getEndorsements())) {
      return measure.getMeasureMetaData().getEndorsements().stream()
          .map(endorser -> endorser.getEndorser())
          .collect(Collectors.joining("\n"));
    }
    return null;
  }

  public static List<String> getMeasureTypes(Measure measure) {
    QdmMeasure qdmMeasure = (QdmMeasure) measure;
    if (CollectionUtils.isNotEmpty(qdmMeasure.getBaseConfigurationTypes())) {
      return qdmMeasure.getBaseConfigurationTypes().stream()
          .map(type -> type.toString())
          .collect(Collectors.toList());
    }
    return null;
  }

  public static String getStratification(Measure measure) {
    if (CollectionUtils.isNotEmpty(measure.getGroups())) {
      for (Group group : measure.getGroups()) {
        if (CollectionUtils.isNotEmpty(group.getStratifications())) {
          return group.getStratifications().stream()
              .map(strat -> strat.getCqlDefinition())
              .collect(Collectors.joining("\n"));
        }
      }
    }
    return null;
  }

  public static String getDefinitions(Measure measure) {
    if (measure.getMeasureMetaData() != null
        && CollectionUtils.isNotEmpty(measure.getMeasureMetaData().getMeasureDefinitions())) {
      return measure.getMeasureMetaData().getMeasureDefinitions().stream()
          .map(definition -> definition.getDefinition())
          .collect(Collectors.joining("\n"));
    }
    return null;
  }

  public static String getPopulationDescription(Measure measure, String populationType) {
    StringBuilder sb = new StringBuilder();
    if (CollectionUtils.isNotEmpty(measure.getGroups())) {
      measure.getGroups().stream()
          .forEach(
              group -> {
                if (CollectionUtils.isNotEmpty(group.getPopulations())) {
                  group.getPopulations().stream()
                      .forEach(
                          population -> {
                            if (StringUtils.isNotBlank(population.getDefinition())
                                && populationType.equalsIgnoreCase(population.getName().name())) {
                              sb.append(population.getDescription() + "\n");
                            }
                          });
                }
              });
    }
    return sb.toString();
  }
}
