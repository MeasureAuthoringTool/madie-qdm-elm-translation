package gov.cms.mat.cql_elm_translation.service;

import ca.uhn.fhir.context.FhirContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.fhir.npm.LibraryLoader;
import org.cqframework.fhir.npm.NpmModelInfoProvider;
import org.cqframework.fhir.npm.NpmPackageManager;
import org.hl7.cql.model.ModelIdentifier;
import org.hl7.cql.model.ModelInfoProvider;
import org.hl7.fhir.convertors.advisors.impl.BaseAdvisor_40_50;
import org.hl7.fhir.convertors.conv40_50.VersionConvertor_40_50;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r5.context.ILoggingService;
import org.hl7.fhir.r5.model.ImplementationGuide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.file.Paths;
import java.nio.file.Files;

@Slf4j
@Service
public class ModelManagerFactory implements ILoggingService {

  private final Map<ModelIdentifier, ModelManager> modelManagers = new ConcurrentHashMap<>();

  private final Logger logger = LoggerFactory.getLogger(ModelManagerFactory.class);

  public void logMessage(String message) {
    logger.info(message);
  }

  public void logDebugMessage(ILoggingService.LogCategory category, String message) {
    logger.debug(message);
  }

  public ModelManagerFactory() {
    log.info("Initializing ModelManagerFactory");

    try {
      // igs live in resources/igs directory
      var igsDir = ModelManagerFactory.class.getClassLoader().getResource("igs");
      if (igsDir != null) {
        var uri = igsDir.toURI();
        var igsPath = Paths.get(uri);
        Files.list(igsPath)
            // all igs are json files
            .filter(path -> path.toString().endsWith(".json"))
            .forEach(
                path -> {
                  String fileName = "igs/" + path.getFileName().toString();
                  ImplementationGuide ig = loadImplementationGuide(fileName);
                  if (ig != null && ig.hasDependsOn()) {
                    ig.getDependsOn().stream()
                        .filter(dep -> StringUtils.isNotBlank(dep.getId()))
                        .forEach(
                            dep -> {
                              ModelIdentifier identifier =
                                  new ModelIdentifier()
                                      .withId(dep.getId())
                                      .withVersion(dep.getVersion());
                              ModelManager manager = buildModelManager(identifier, fileName);
                              modelManagers.put(identifier, manager);
                              log.info(
                                  "ModelManager created for dependsOn: {}#{}",
                                  dep.getUri(),
                                  dep.getVersion());
                            });
                  }
                });
      }
    } catch (Exception e) {
      log.error("Error initializing ModelManagerFactory IGs", e);
    }
  }

  public ModelManager getModelManager(ModelIdentifier identifier) {
    if (identifier == null || StringUtils.isEmpty(identifier.getId())) {
      log.error("Model name cannot be null or empty");
      throw new IllegalArgumentException("Model name cannot be null or empty");
    }

    // If model is not known, create a new ModelManager without the NpmModelInfoProvider
    return modelManagers.computeIfAbsent(
        identifier,
        key -> {
          log.info("Creating new basic ModelManager for model: {}", key);
          return new ModelManager();
        });
  }

  private ModelManager buildModelManager(ModelIdentifier identifier, String igFilepath) {
    ModelManager modelManager = new ModelManager();
    modelManager
        .getModelInfoLoader()
        .registerModelInfoProvider(buildNpmModelInfoProvider(igFilepath), true);
    modelManager.resolveModel(identifier);
    return modelManager;
  }

  private ImplementationGuide loadImplementationGuide(String resourcePath) {
    Resource igResource =
        (Resource)
            FhirContext.forR4Cached()
                .newJsonParser()
                .parseResource(
                    ModelManagerFactory.class.getClassLoader().getResourceAsStream(resourcePath));

    VersionConvertor_40_50 convertor = new VersionConvertor_40_50(new BaseAdvisor_40_50());
    return (ImplementationGuide) convertor.convertResource(igResource);
  }

  private ModelInfoProvider buildNpmModelInfoProvider(String igPath) {
    ImplementationGuide implementationGuide = loadImplementationGuide(igPath);
    NpmPackageManager packageManager = new NpmPackageManager(implementationGuide);
    LibraryLoader reader = new LibraryLoader("5.0");
    return new NpmModelInfoProvider(packageManager.getNpmList(), reader, this);
  }
}
