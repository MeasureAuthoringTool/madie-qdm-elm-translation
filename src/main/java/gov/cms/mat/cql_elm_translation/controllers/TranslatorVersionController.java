package gov.cms.mat.cql_elm_translation.controllers;

import org.cqframework.cql_to_elm.BuildConfig;
import org.springframework.beans.factory.annotation.Value;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequestMapping("/translator-version")
public class TranslatorVersionController {

  @Value("${cql-translator.version:}")
  private String translatorPomPropertyVersion;

  @GetMapping()
  public ResponseEntity<String> getTranslatorVersion(
      @RequestParam(required = true, name = "draft") boolean draft) {
    if (!draft) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Non-draft version is no longer supported.");
    }

    var translatorVersion =
        StringUtils.firstNonBlank(getBuildConfigVersion(), translatorPomPropertyVersion);
    if (StringUtils.isNotBlank(translatorVersion)) {
      return ResponseEntity.ok(translatorVersion);
    } else {
      return ResponseEntity.status(HttpStatus.FAILED_DEPENDENCY)
          .body("Unable to determine translator version.");
    }
  }

  String getBuildConfigVersion() {
    return BuildConfig.IMPLEMENTATION_VERSION;
  }
}
