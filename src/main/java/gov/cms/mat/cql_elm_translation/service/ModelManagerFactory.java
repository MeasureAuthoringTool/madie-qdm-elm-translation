package gov.cms.mat.cql_elm_translation.service;

import gov.cms.madie.cql_elm_translator.utils.ImplementationGuideLoader;
import gov.cms.mat.cql_elm_translation.dto.ModelLoadingInfo;
import gov.cms.mat.cql_elm_translation.dto.ModelLoadingState;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.fhir.npm.LibraryLoader;
import org.cqframework.fhir.npm.NpmModelInfoProvider;
import org.cqframework.fhir.npm.NpmPackageManager;
import org.hl7.cql.model.ModelIdentifier;
import org.hl7.cql.model.ModelInfoProvider;
import org.hl7.fhir.r5.context.ILoggingService;
import org.hl7.fhir.r5.model.ImplementationGuide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ModelManagerFactory implements ILoggingService {

  private final String fhirCachePath;

  private final Map<ModelIdentifier, ModelManager> modelManagers = new ConcurrentHashMap<>();
  private final Map<ModelIdentifier, ModelLoadingInfo> modelLoadingInfos =
      new ConcurrentHashMap<>();

  private final Logger logger = LoggerFactory.getLogger(ModelManagerFactory.class);

  public void logMessage(String message) {
    logger.info(message);
  }

  public void logDebugMessage(LogCategory category, String message) {
    logger.debug(message);
  }

  public ModelManagerFactory(@Value("${madie.fhir-cache}") String fhirCachePath) {
    log.info("Initializing ModelManagerFactory");
    this.fhirCachePath = fhirCachePath;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void onApplicationReady() {
    ImplementationGuideLoader.load().forEach(this::processImplementationGuide);
  }

  public void processImplementationGuide(ImplementationGuide ig) {
    if (ig != null && ig.hasDependsOn()) {
      ig.getDependsOn().stream()
          .filter(dep -> StringUtils.isNotBlank(dep.getId()))
          .forEach(
              dep -> {
                ModelIdentifier identifier =
                    new ModelIdentifier(dep.getId(), null, dep.getVersion());
                ModelLoadingInfo info = new ModelLoadingInfo(identifier);
                ;
                try {
                  info.setLoadingState(ModelLoadingState.LOADING);
                  modelLoadingInfos.put(identifier, info);
                  ModelManager modelManager = buildModelManager(identifier, ig);
                  modelManagers.put(identifier, modelManager);

                  // cqframework seems to be updating the system of the ModelIdentifier input
                  // parameter
                  if (StringUtils.isNotBlank(identifier.getSystem())) {
                    modelManagers.put(
                        new ModelIdentifier(dep.getId(), null, dep.getVersion()), modelManager);
                  }
                  log.info(
                      "ModelManager created for dependsOn: {}#{}", dep.getUri(), dep.getVersion());
                  info.setLoadingState(ModelLoadingState.LOADED);
                } catch (Exception e) {
                  log.error(
                      "Error occurred and failed to create ModelManager for dependsOn: {}#{}, skipping "
                          + "and continuing with next dependency.",
                      dep.getUri(),
                      dep.getVersion(),
                      e);
                  info.setLoadingState(ModelLoadingState.ERROR_FAILED);
                  info.setErrorMessage(e.getMessage());
                }
              });
    }
  }

  public List<ModelLoadingInfo> getModelLoadingInfos() {
    return modelLoadingInfos.values().stream().toList();
  }

  public ModelManager getModelManager(ModelIdentifier identifier) {
    if (identifier == null) {
      log.error("Model name cannot be null or empty");
      throw new IllegalArgumentException("Model name cannot be null or empty");
    }

    log.info("Retrieving ModelManager for model: {}", identifier);

    // If model is not known, create a new ModelManager without the NpmModelInfoProvider
    return modelManagers.computeIfAbsent(
        identifier,
        key -> {
          log.info("Creating new basic ModelManager for model: {}", key);
          return new ModelManager();
        });
  }

  public List<ModelIdentifier> getKnownModelIdentifiers() {
    return this.modelManagers.keySet().stream()
        .map(
            modelIdentifier ->
                new ModelIdentifier(
                    modelIdentifier.getId(),
                    modelIdentifier.getSystem(),
                    modelIdentifier.getVersion()))
        .toList();
  }

  protected ModelManager buildModelManager(
      ModelIdentifier identifier, ImplementationGuide implementationGuide) throws IOException {
    ModelManager modelManager = new ModelManager();
    modelManager
        .getModelInfoLoader()
        .registerModelInfoProvider(buildNpmModelInfoProvider(implementationGuide), true);
    modelManager.resolveModel(identifier);
    log.info("ModelManager built and model resolved for model: {}", identifier);
    return modelManager;
  }

  private ModelInfoProvider buildNpmModelInfoProvider(ImplementationGuide implementationGuide)
      throws IOException {
    NpmPackageManager packageManager =
        ImplementationGuideLoader.buildPackageManager(fhirCachePath, implementationGuide);
    LibraryLoader reader = new LibraryLoader("5.0");
    return new NpmModelInfoProvider(packageManager.getNpmList(), reader, this);
  }
}
