package gov.cms.mat.cql_elm_translation.controllers;

import gov.cms.mat.cql_elm_translation.exceptions.ResourceNotFoundException;
import gov.cms.mat.cql_elm_translation.service.EffectiveDataRequirementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
public class EffectiveDataRequirementController {
  private final EffectiveDataRequirementService effectiveDataRequirementService;

  @PutMapping(
      value = "/effective-data-requirements",
      produces = {MediaType.APPLICATION_JSON_VALUE},
      consumes = {MediaType.TEXT_PLAIN_VALUE})
  public ResponseEntity<String> getEffectiveDataRequirements(
      Principal principal,
      @RequestBody String bundleStr,
      @RequestHeader("Authorization") String accessToken,
      @RequestParam(name = "libraryName") String libraryName,
      @RequestParam(name = "measureId") String measureId) {

    Bundle bundleResource =
        effectiveDataRequirementService.createFhirResourceFromJson(bundleStr, Bundle.class);

    log.info(
        "User {} trying to get effective data requirements for bundle: {} ",
        principal.getName(),
        (bundleResource != null ? bundleResource.getId() : " bundleResouce Id is null"));
    if (bundleResource == null || CollectionUtils.isEmpty(bundleResource.getEntry())) {
      log.error(
          "BundleResource is null or unable to find entry for bundle {}",
          (bundleResource != null ? bundleResource.getId() : " null"));
      throw new ResourceNotFoundException("bundle resource or entry ", measureId);
    }

    Optional<Bundle.BundleEntryComponent> measureEntry =
        effectiveDataRequirementService.getMeasureEntry(bundleResource);
    if (measureEntry.isEmpty()) {
      log.error("Unable to find measure entry for bundle {}", bundleResource.getId());
      throw new ResourceNotFoundException("measure entry ", measureId);
    }
    Resource measureResource = measureEntry.get().getResource();

    Optional<Bundle.BundleEntryComponent> measureLibraryEntry =
        effectiveDataRequirementService.getMeasureLibraryEntry(bundleResource, libraryName);
    if (measureLibraryEntry.isEmpty()) {
      log.error("Unable to find library entry for bundle {}", bundleResource.getId());
      throw new ResourceNotFoundException("library entry ", measureId);
    }
    Library library = (Library) measureLibraryEntry.get().getResource();

    org.hl7.fhir.r5.model.Measure r5Measure =
        effectiveDataRequirementService.getR5MeasureFromR4MeasureResource(measureResource);

    org.hl7.fhir.r5.model.Library r5Library =
        effectiveDataRequirementService.getEffectiveDataRequirements(
            r5Measure, library, accessToken);

    String r5LibraryStr =
        effectiveDataRequirementService.getEffectiveDataRequirementsStr(r5Library);

    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(r5LibraryStr);
  }
}
