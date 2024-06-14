package gov.cms.mat.cql_elm_translation.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gov.cms.mat.cql_elm_translation.config.TranslatorVersionConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/translator-version")
public class TranslatorVersionController {

  private TranslatorVersionConfig translatorVersionConfig;

  @GetMapping()
  public ResponseEntity<String> getTranslatorVersion(
      @RequestParam(required = true, name = "draft") boolean draft) {
    log.info(
        "Current translator version: " + translatorVersionConfig.getCurrentTranslatorVersion());
    log.info(
        "Most recent translator version: "
            + translatorVersionConfig.getMostRecentTranslatorVersion());
    String result =
        draft
            ? translatorVersionConfig.getMostRecentTranslatorVersion()
            : translatorVersionConfig.getCurrentTranslatorVersion();
    return ResponseEntity.status(HttpStatus.OK).body(result);
  }
}
