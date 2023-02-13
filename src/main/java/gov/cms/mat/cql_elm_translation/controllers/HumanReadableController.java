package gov.cms.mat.cql_elm_translation.controllers;

import gov.cms.madie.models.measure.Measure;
import gov.cms.mat.cql_elm_translation.service.HumanReadableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import java.security.Principal;

@Slf4j
@RestController
@RequiredArgsConstructor
public class HumanReadableController {
  private final HumanReadableService humanReadableService;

  @PutMapping(
      value = "/human-readable",
      produces = {MediaType.TEXT_HTML_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE})
  public String generateHumanReadable(
      @RequestBody @Validated(Measure.ValidationSequence.class) Measure measure,
      Principal principal,
      @RequestHeader("Authorization") String accessToken) {
    log.info(
        "User {} trying to generate Human Readable for measure: {} ",
        principal.getName(),
        measure.getId());
    return humanReadableService.generateHumanReadable(measure, accessToken);
  }
}
