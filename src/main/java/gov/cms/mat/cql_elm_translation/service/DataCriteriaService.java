package gov.cms.mat.cql_elm_translation.service;

import gov.cms.madie.cql_elm_translator.dto.CqlLibraryDetails;
import gov.cms.madie.models.measure.Measure;
import gov.cms.mat.cql_elm_translation.dto.RelevantElement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r5.model.Library;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataCriteriaService {

  private final EffectiveDataRequirementService effectiveDataRequirementService;

  public Set<RelevantElement> getRelevantElements(Measure measure, String accessToken) {
    if (StringUtils.isBlank(measure.getCql())) {
      log.info("Data criteria not found as cql is blank");
      return Collections.emptySet();
    }

    Set<String> usedDefinitions = getUsedDefinitionsFromMeasure(measure);
    CqlLibraryDetails libraryDetails =
        CqlLibraryDetails.builder()
            .libraryName(measure.getCqlLibraryName())
            .cql(measure.getCql())
            .expressions(usedDefinitions)
            .build();
    Library library =
        effectiveDataRequirementService.getEffectiveDataRequirements(
            libraryDetails, true, accessToken);
    if (library == null || CollectionUtils.isEmpty(library.getDataRequirement())) {
      return Set.of();
    }
    return library.getDataRequirement().stream()
        .map(
            dataRequirement ->
                RelevantElement.builder()
                    .type(dataRequirement.getType().getDisplay())
                    .profile(dataRequirement.getProfile().get(0).getValue())
                    .build())
        .collect(Collectors.toSet());
  }

  private Set<String> getUsedDefinitionsFromMeasure(Measure measure) {
    if (CollectionUtils.isEmpty(measure.getGroups())) {
      return Set.of();
    }
    Set<String> usedDefinitions = new HashSet<>();
    measure
        .getGroups()
        .forEach(
            group -> {
              group
                  .getPopulations()
                  .forEach(
                      population -> {
                        if (!population.getDefinition().isEmpty()) {
                          usedDefinitions.add(population.getDefinition());
                        }
                      });
              if (CollectionUtils.isNotEmpty(group.getMeasureObservations())) {
                group
                    .getMeasureObservations()
                    .forEach(
                        measureObservation -> {
                          if (!measureObservation.getDefinition().isEmpty()) {
                            usedDefinitions.add(measureObservation.getDefinition());
                          }
                        });
              }
              if (CollectionUtils.isNotEmpty(group.getStratifications())) {
                group
                    .getStratifications()
                    .forEach(
                        stratification -> {
                          if (!stratification.getCqlDefinition().isEmpty()) {
                            usedDefinitions.add(stratification.getCqlDefinition());
                          }
                        });
              }
            });
    measure
        .getSupplementalData()
        .forEach(defDescPair -> usedDefinitions.add(defDescPair.getDefinition()));
    measure
        .getRiskAdjustments()
        .forEach(defDescPair -> usedDefinitions.add(defDescPair.getDefinition()));
    return usedDefinitions;
  }
}
