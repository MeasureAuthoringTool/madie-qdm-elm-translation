package gov.cms.mat.cql_elm_translation.controllers;

import gov.cms.mat.cql_elm_translation.service.HumanReadableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/human-readable")
public class HumanReadableController {
  @Autowired private HumanReadableService humanReadableService;

  @GetMapping("/")
  public String index() {
    return humanReadableService.generateHumanReadable();
  }
}
