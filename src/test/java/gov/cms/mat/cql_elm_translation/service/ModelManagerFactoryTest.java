package gov.cms.mat.cql_elm_translation.service;

import gov.cms.madie.cql_elm_translator.utils.FhirUtil;
import gov.cms.madie.cql_elm_translator.utils.ImplementationGuideLoader;
import gov.cms.mat.cql_elm_translation.dto.ModelLoadingInfo;
import gov.cms.mat.cql_elm_translation.dto.ModelLoadingState;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.cqframework.cql.cql2elm.ModelManager;
import org.hl7.cql.model.ModelIdentifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.hl7.fhir.r5.model.ImplementationGuide;
import org.hl7.fhir.r5.model.ImplementationGuide.ImplementationGuideDependsOnComponent;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class ModelManagerFactoryTest {

  @Mock FhirUtil fhirUtil;
  private ModelManagerFactory modelManagerFactory;

  @BeforeEach
  void setUp() throws IOException {
    File file = new File("/tmp/fake-cache");
    if (!file.exists()) {
      FileUtils.forceMkdir(file);
    }
    reset(fhirUtil);
    // Constructor no longer calls ImplementationGuideLoader.load() — that moved to
    // onApplicationReady()
    modelManagerFactory = new ModelManagerFactory("/tmp/fake-cache", "classpath:igs/*.json");
  }

  @Test
  void testConstructorInitializesSuccessfully() {
    // given / when — constructor called in setUp()

    // then
    assertThat(modelManagerFactory, is(notNullValue()));
  }

  @Test
  void testGetModelManagerWithValidIdentifier() {
    // given
    ModelIdentifier identifier = new ModelIdentifier("FHIR", null, "4.0.1");

    // when
    ModelManager modelManager = modelManagerFactory.getModelManager(identifier);

    // then
    assertThat(modelManager, is(notNullValue()));
  }

  @Test
  void testGetModelManagerWithQICoreIdentifier() {
    // given
    ModelIdentifier identifier = new ModelIdentifier("QICore", null, "7.0.1");

    // when
    ModelManager modelManager = modelManagerFactory.getModelManager(identifier);

    // then
    assertThat(modelManager, is(notNullValue()));
  }

  @Test
  void testGetModelManagerWithCqlIdentifier() {
    // given
    ModelIdentifier identifier = new ModelIdentifier("hl7.fhir.us.cql", null, "1.0.0");

    // when
    ModelManager modelManager = modelManagerFactory.getModelManager(identifier);

    // then
    assertThat(modelManager, is(notNullValue()));
  }

  @Test
  void testGetModelManagerWithNullIdentifierThrowsExceptionForNull() {
    // given / when
    Exception exception =
        assertThrows(
            IllegalArgumentException.class, () -> modelManagerFactory.getModelManager(null));

    // then
    assertThat(exception.getMessage(), is(equalTo("Model name cannot be null or empty")));
  }

  @Test
  void testGetModelManagerReturnsSameInstanceForSameIdentifier() {
    // given
    ModelIdentifier identifier = new ModelIdentifier("FHIR", null, "4.0.1");

    // when
    ModelManager modelManager1 = modelManagerFactory.getModelManager(identifier);
    ModelManager modelManager2 = modelManagerFactory.getModelManager(identifier);

    // then
    assertThat(modelManager1, is(notNullValue()));
    assertThat(modelManager2, is(sameInstance(modelManager1)));
  }

  @Test
  void testGetModelManagerReturnsDifferentInstancesForDifferentIdentifiers() {
    // given
    ModelIdentifier identifier1 = new ModelIdentifier("FHIR", null, "4.0.1");
    ModelIdentifier identifier2 = new ModelIdentifier("FHIR", null, "5.0.0");

    // when
    ModelManager modelManager1 = modelManagerFactory.getModelManager(identifier1);
    ModelManager modelManager2 = modelManagerFactory.getModelManager(identifier2);

    // then
    assertThat(modelManager1, is(notNullValue()));
    assertThat(modelManager2, is(notNullValue()));
    assertThat(modelManager1, is(not(sameInstance(modelManager2))));
  }

  @Test
  void testGetModelManagerCreatesNewManagerForUnknownModel() {
    // given
    ModelIdentifier identifier = new ModelIdentifier("UnknownModel", null, "1.0.0");

    // when
    ModelManager modelManager = modelManagerFactory.getModelManager(identifier);

    // then
    assertThat(modelManager, is(notNullValue()));
  }

  @Test
  void testLogDebugMessageWithNullCategory() {
    // given / when / then
    assertDoesNotThrow(
        () -> modelManagerFactory.logDebugMessage(null, "Test debug message with null category"));
  }

  @Test
  void testLogMessageWithNullMessage() {
    // given / when / then
    assertDoesNotThrow(() -> modelManagerFactory.logMessage(null));
  }

  @Test
  void testMultipleModelManagersCanCoexist() {
    // given
    ModelIdentifier identifier1 = new ModelIdentifier("Model1", null, "1.0.0");
    ModelIdentifier identifier2 = new ModelIdentifier("Model2", null, "2.0.0");
    ModelIdentifier identifier3 = new ModelIdentifier("Model3", null, "3.0.0");

    // when
    ModelManager modelManager1 = modelManagerFactory.getModelManager(identifier1);
    ModelManager modelManager2 = modelManagerFactory.getModelManager(identifier2);
    ModelManager modelManager3 = modelManagerFactory.getModelManager(identifier3);

    // then
    assertThat(modelManager1, is(notNullValue()));
    assertThat(modelManager2, is(notNullValue()));
    assertThat(modelManager3, is(notNullValue()));
    assertThat(modelManager1, is(not(sameInstance(modelManager2))));
    assertThat(modelManager2, is(not(sameInstance(modelManager3))));
    assertThat(modelManager1, is(not(sameInstance(modelManager3))));
  }

  @Test
  void testErrorLoggingWhenBuildModelManagerThrows() throws IOException {
    // given
    ImplementationGuide ig = new ImplementationGuide();
    ig.setId("MainIG");
    ig.setVersion("2.0.0");
    ImplementationGuideDependsOnComponent dep = new ImplementationGuideDependsOnComponent();
    dep.setId("DepIG");
    dep.setVersion("1.0.0");
    ig.setDependsOn(List.of(dep));

    try (MockedStatic<ImplementationGuideLoader> mockedLoader =
        mockStatic(ImplementationGuideLoader.class)) {
      mockedLoader.when(() -> ImplementationGuideLoader.load(anyString())).thenReturn(List.of(ig));
      ModelManagerFactory factory =
          spy(new ModelManagerFactory("/tmp/fake-cache", "classpath:igs/*.json"));
      doReturn(mock(ModelManager.class)).when(factory).buildModelManager(any(), any());

      // when / then
      assertDoesNotThrow(
          () -> factory.processImplementationGuide(ig),
          "processImplementationGuide should handle exceptions internally and not throw");
    }
  }

  @Test
  void testProcessImplementationGuideWithNoDependencies() {
    // given
    ImplementationGuide ig = new ImplementationGuide();
    ig.setId("TestIG");
    ig.setVersion("1.0.0");
    ModelManagerFactory factory = spy(modelManagerFactory);

    // when
    factory.processImplementationGuide(ig);

    // then
    assertThat(factory.getKnownModelIdentifiers().size(), is(0));
  }

  @Test
  void testProcessImplementationGuideWithDependencies() throws IOException {
    // given
    ImplementationGuide ig = new ImplementationGuide();
    ig.setId("MainIG");
    ig.setVersion("2.0.0");
    ImplementationGuideDependsOnComponent dep = new ImplementationGuideDependsOnComponent();
    dep.setId("DepIG");
    dep.setVersion("1.0.0");
    ig.setDependsOn(List.of(dep));

    ModelManagerFactory factory =
        spy(new ModelManagerFactory("/tmp/fake-cache", "classpath:igs/*.json"));
    doAnswer(
            inv -> {
              // mock the behavior of CQFramework mutating the system of the input identifier
              ModelIdentifier inputIdentifier = inv.getArgument(0);
              inputIdentifier.setSystem("some-system");
              return mock(ModelManager.class);
            })
        .when(factory)
        .buildModelManager(any(), any());

    // when
    factory.processImplementationGuide(ig);

    // then — both the mutated-system entry and the null-system duplicate should be present
    assertThat(
        factory.getKnownModelIdentifiers(),
        hasItem(
            allOf(
                hasProperty("id", is("DepIG")),
                hasProperty("system", is("some-system")),
                hasProperty("version", is("1.0.0")))));
    assertThat(
        factory.getKnownModelIdentifiers(),
        hasItem(
            allOf(
                hasProperty("id", is("DepIG")),
                hasProperty("system", is(nullValue())),
                hasProperty("version", is("1.0.0")))));
    assertThat(factory.getKnownModelIdentifiers().size(), is(2));
  }

  @Test
  void testProcessImplementationGuideWithMalformedDependency() {
    // given
    ImplementationGuide ig = new ImplementationGuide();
    ig.setId("MainIG");
    ig.setVersion("2.0.0");
    ImplementationGuideDependsOnComponent dep = new ImplementationGuideDependsOnComponent();
    // No id set — filtered out before processing
    ig.setDependsOn(List.of(dep));
    ModelManagerFactory factory = spy(modelManagerFactory);

    // when
    factory.processImplementationGuide(ig);

    // then
    assertThat(factory.getKnownModelIdentifiers().size(), is(0));
  }

  /**
   * Verifies that when resolveModel throws a runtime exception for one dependency, processing
   * continues and the next dependency is still registered successfully.
   */
  @Test
  void testProcessImplementationGuideResolveModelExceptionDoesNotHaltNextDependency()
      throws IOException {
    // given
    ImplementationGuide ig = new ImplementationGuide();
    ig.setId("MainIG");
    ig.setVersion("2.0.0");

    ImplementationGuideDependsOnComponent failingDep = new ImplementationGuideDependsOnComponent();
    failingDep.setId("FailingDep");
    failingDep.setVersion("1.0.0");

    ImplementationGuideDependsOnComponent successDep = new ImplementationGuideDependsOnComponent();
    successDep.setId("SuccessfulDep");
    successDep.setVersion("2.0.0");

    ig.setDependsOn(List.of(failingDep, successDep));

    ModelManagerFactory factory =
        spy(new ModelManagerFactory("/tmp/fake-cache", "classpath:igs/*.json"));
    ModelManager successManager = mock(ModelManager.class);
    doThrow(new RuntimeException("Simulated resolveModel failure"))
        .doReturn(successManager)
        .when(factory)
        .buildModelManager(any(), any());

    // when / then — exception must be swallowed and loop must continue
    assertDoesNotThrow(() -> factory.processImplementationGuide(ig));

    assertThat(
        factory.getKnownModelIdentifiers().stream()
            .noneMatch(id -> id.getId().equals("FailingDep")),
        is(true));
    assertThat(
        factory.getKnownModelIdentifiers(),
        hasItem(
            allOf(hasProperty("id", is("SuccessfulDep")), hasProperty("version", is("2.0.0")))));
  }

  // ── getModelLoadingInfos ──────────────────────────────────────────────────

  @Test
  void testGetModelLoadingInfosIsEmptyInitially() {
    // given — fresh factory from setUp()

    // when
    List<ModelLoadingInfo> infos = modelManagerFactory.getModelLoadingInfos();

    // then
    assertThat(infos.size(), is(0));
  }

  @Test
  void testGetModelLoadingInfosAfterSuccessfulProcessing() throws IOException {
    // given
    ImplementationGuide ig = new ImplementationGuide();
    ig.setId("MainIG");
    ig.setVersion("1.0.0");
    ImplementationGuideDependsOnComponent dep = new ImplementationGuideDependsOnComponent();
    dep.setId("QICore");
    dep.setVersion("7.0.0");
    ig.setDependsOn(List.of(dep));

    ModelManagerFactory factory =
        spy(new ModelManagerFactory("/tmp/fake-cache", "classpath:igs/*.json"));
    doReturn(mock(ModelManager.class)).when(factory).buildModelManager(any(), any());
    factory.processImplementationGuide(ig);

    // when
    List<ModelLoadingInfo> infos = factory.getModelLoadingInfos();

    // then
    assertThat(infos.size(), is(1));
    assertThat(infos.get(0).getModelIdentifier().getId(), is(equalTo("QICore")));
    assertThat(infos.get(0).getLoadingState(), is(ModelLoadingState.LOADED));
    assertThat(infos.get(0).getErrorMessage(), is(nullValue()));
  }

  @Test
  void testGetModelLoadingInfosAfterFailedProcessing() throws IOException {
    // given
    ImplementationGuide ig = new ImplementationGuide();
    ig.setId("MainIG");
    ig.setVersion("1.0.0");
    ImplementationGuideDependsOnComponent dep = new ImplementationGuideDependsOnComponent();
    dep.setId("BadModel");
    dep.setVersion("1.0.0");
    ig.setDependsOn(List.of(dep));

    ModelManagerFactory factory =
        spy(new ModelManagerFactory("/tmp/fake-cache", "classpath:igs/*.json"));
    doThrow(new RuntimeException("Model load failed"))
        .when(factory)
        .buildModelManager(any(), any());
    factory.processImplementationGuide(ig);

    // when
    List<ModelLoadingInfo> infos = factory.getModelLoadingInfos();

    // then
    assertThat(infos.size(), is(1));
    assertThat(infos.get(0).getModelIdentifier().getId(), is(equalTo("BadModel")));
    assertThat(infos.get(0).getLoadingState(), is(ModelLoadingState.ERROR_FAILED));
    assertThat(infos.get(0).getErrorMessage(), is(equalTo("Model load failed")));
  }

  @Test
  void testGetModelLoadingInfosMalformedDepIsNotTracked() {
    // given
    ImplementationGuide ig = new ImplementationGuide();
    ig.setId("MainIG");
    ig.setVersion("1.0.0");
    ImplementationGuideDependsOnComponent dep = new ImplementationGuideDependsOnComponent();
    // blank id — filtered out before any tracking occurs
    ig.setDependsOn(List.of(dep));
    modelManagerFactory.processImplementationGuide(ig);

    // when
    List<ModelLoadingInfo> infos = modelManagerFactory.getModelLoadingInfos();

    // then
    assertThat(infos.size(), is(0));
  }

  @Test
  void testGetModelLoadingInfosTracksMultipleDependencies() throws IOException {
    // given
    ImplementationGuide ig = new ImplementationGuide();
    ig.setId("MainIG");
    ig.setVersion("1.0.0");
    ImplementationGuideDependsOnComponent dep1 = new ImplementationGuideDependsOnComponent();
    dep1.setId("ModelA");
    dep1.setVersion("1.0.0");
    ImplementationGuideDependsOnComponent dep2 = new ImplementationGuideDependsOnComponent();
    dep2.setId("ModelB");
    dep2.setVersion("2.0.0");
    ig.setDependsOn(List.of(dep1, dep2));

    ModelManagerFactory factory =
        spy(new ModelManagerFactory("/tmp/fake-cache", "classpath:igs/*.json"));
    doReturn(mock(ModelManager.class))
        .doReturn(mock(ModelManager.class))
        .when(factory)
        .buildModelManager(any(), any());
    factory.processImplementationGuide(ig);

    // when
    List<ModelLoadingInfo> infos = factory.getModelLoadingInfos();

    // then
    assertThat(infos.size(), is(2));
    assertThat(
        infos,
        hasItem(
            allOf(
                hasProperty("modelIdentifier", hasProperty("id", is("ModelA"))),
                hasProperty("loadingState", is(ModelLoadingState.LOADED)))));
    assertThat(
        infos,
        hasItem(
            allOf(
                hasProperty("modelIdentifier", hasProperty("id", is("ModelB"))),
                hasProperty("loadingState", is(ModelLoadingState.LOADED)))));
  }

  @Test
  void testGetModelLoadingInfosMixedSuccessAndFailure() throws IOException {
    // given
    ImplementationGuide ig = new ImplementationGuide();
    ig.setId("MainIG");
    ig.setVersion("1.0.0");
    ImplementationGuideDependsOnComponent dep1 = new ImplementationGuideDependsOnComponent();
    dep1.setId("GoodModel");
    dep1.setVersion("1.0.0");
    ImplementationGuideDependsOnComponent dep2 = new ImplementationGuideDependsOnComponent();
    dep2.setId("BadModel");
    dep2.setVersion("2.0.0");
    ig.setDependsOn(List.of(dep1, dep2));

    ModelManagerFactory factory =
        spy(new ModelManagerFactory("/tmp/fake-cache", "classpath:igs/*.json"));
    doReturn(mock(ModelManager.class))
        .doThrow(new RuntimeException("Failed to build"))
        .when(factory)
        .buildModelManager(any(), any());
    factory.processImplementationGuide(ig);

    // when
    List<ModelLoadingInfo> infos = factory.getModelLoadingInfos();

    // then
    assertThat(infos.size(), is(2));
    assertThat(
        infos,
        hasItem(
            allOf(
                hasProperty("modelIdentifier", hasProperty("id", is("GoodModel"))),
                hasProperty("loadingState", is(ModelLoadingState.LOADED)),
                hasProperty("errorMessage", is(nullValue())))));
    assertThat(
        infos,
        hasItem(
            allOf(
                hasProperty("modelIdentifier", hasProperty("id", is("BadModel"))),
                hasProperty("loadingState", is(ModelLoadingState.ERROR_FAILED)),
                hasProperty("errorMessage", is(equalTo("Failed to build"))))));
  }
}
