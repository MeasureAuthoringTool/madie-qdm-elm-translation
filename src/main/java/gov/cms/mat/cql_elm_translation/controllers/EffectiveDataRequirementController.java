package gov.cms.mat.cql_elm_translation.controllers;

import gov.cms.madie.cql_elm_translator.dto.CqlLibraryDetails;
import gov.cms.madie.cql_elm_translator.exceptions.CqlFormatException;
import gov.cms.mat.cql_elm_translation.service.EffectiveDataRequirementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class EffectiveDataRequirementController {
  private final EffectiveDataRequirementService effectiveDataRequirementService;

  @PutMapping(
      value = "/effective-data-requirements",
      produces = {MediaType.APPLICATION_JSON_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<String> getEffectiveDataRequirements(
      @RequestBody CqlLibraryDetails libraryDetails,
      @RequestParam(value = "recursive", defaultValue = "true") boolean recursive,
      @RequestHeader("Authorization") String accessToken) {

    if (libraryDetails == null || StringUtils.isBlank(libraryDetails.getCql())) {
      log.error("Invalid cql provided for library");
      throw new CqlFormatException("Invalid cql provided for library");
    }
    log.info(
        "building the effective data requirements for library: {}",
        libraryDetails.getLibraryName());

    org.hl7.fhir.r5.model.Library r5Library =
        effectiveDataRequirementService.getEffectiveDataRequirements(
            libraryDetails, recursive, accessToken);

    String r5LibraryStr =
        effectiveDataRequirementService.getEffectiveDataRequirementsStr(r5Library);

    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(r5LibraryStr);
  }
}
