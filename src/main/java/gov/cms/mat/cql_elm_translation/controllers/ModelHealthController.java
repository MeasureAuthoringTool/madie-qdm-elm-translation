package gov.cms.mat.cql_elm_translation.controllers;

import gov.cms.mat.cql_elm_translation.service.ModelManagerFactory;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/models")
@Tag(name = "Model-Health-Controller", description = "API for checking the health of the models used in CQL to ELM translation.")
@Slf4j
@RequiredArgsConstructor
public class ModelHealthController {

  private final ModelManagerFactory modelManagerFactory;

  @GetMapping("/known")
  public Object getKnownModels() {
    return modelManagerFactory.getKnownModelIdentifiers();
  }

  @GetMapping("/loading-info")
  public Object getLoadingInfo() {
    return modelManagerFactory.getModelLoadingInfos();
  }

}
