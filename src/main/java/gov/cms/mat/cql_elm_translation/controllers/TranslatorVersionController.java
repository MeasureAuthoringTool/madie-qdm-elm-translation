package gov.cms.mat.cql_elm_translation.controllers;

import gov.cms.mat.cql_elm_translation.service.VersionService;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.server.ResponseStatusException;


@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/translator-version")
public class TranslatorVersionController {

  private VersionService versionService;

  @GetMapping()
  public ResponseEntity<String> getTranslatorVersion(
          @RequestParam(required = true, name = "draft") boolean draft) {
    if (!draft) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Non-draft version is no longer supported.");
    }

    try {
      return ResponseEntity.ok(versionService.getTranslatorVersion());
    } catch (IllegalStateException e) {
      return ResponseEntity.status(HttpStatus.FAILED_DEPENDENCY)
          .body("Unable to determine translator version.");
    }
  }
}
