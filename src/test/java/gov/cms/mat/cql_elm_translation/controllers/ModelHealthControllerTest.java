package gov.cms.mat.cql_elm_translation.controllers;

import gov.cms.mat.cql_elm_translation.dto.ModelLoadingInfo;
import gov.cms.mat.cql_elm_translation.dto.ModelLoadingState;
import gov.cms.mat.cql_elm_translation.service.ModelManagerFactory;
import org.hl7.cql.model.ModelIdentifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModelHealthControllerTest {

  @Mock private ModelManagerFactory modelManagerFactory;

  @InjectMocks private ModelHealthController modelHealthController;

  private ModelIdentifier fhirIdentifier;
  private ModelIdentifier qiCoreIdentifier;

  @BeforeEach
  void setUp() {
    fhirIdentifier = new ModelIdentifier("FHIR", null, "4.0.1");
    qiCoreIdentifier = new ModelIdentifier("QICore", null, "7.0.1");
  }

  // ── getKnownModels ────────────────────────────────────────────────────────

  @Test
  void testGetKnownModels_returnsEmptyListWhenNoModelsLoaded() {
    // given
    when(modelManagerFactory.getKnownModelIdentifiers()).thenReturn(Collections.emptyList());

    // when
    List<ModelIdentifier> result = modelHealthController.getKnownModels();

    // then
    assertThat(result, is(notNullValue()));
    assertThat(result, is(empty()));
    verify(modelManagerFactory).getKnownModelIdentifiers();
  }

  @Test
  void testGetKnownModels_returnsSingleIdentifier() {
    // given
    when(modelManagerFactory.getKnownModelIdentifiers()).thenReturn(List.of(fhirIdentifier));

    // when
    List<ModelIdentifier> result = modelHealthController.getKnownModels();

    // then
    assertThat(result, is(notNullValue()));
    assertThat(result, hasSize(1));
    assertThat(result.get(0).getId(), is(equalTo("FHIR")));
    assertThat(result.get(0).getVersion(), is(equalTo("4.0.1")));
    verify(modelManagerFactory).getKnownModelIdentifiers();
  }

  @Test
  void testGetKnownModels_returnsMultipleIdentifiers() {
    // given
    when(modelManagerFactory.getKnownModelIdentifiers())
        .thenReturn(List.of(fhirIdentifier, qiCoreIdentifier));

    // when
    List<ModelIdentifier> result = modelHealthController.getKnownModels();

    // then
    assertThat(result, is(notNullValue()));
    assertThat(result, hasSize(2));
    assertThat(result.get(0).getId(), is(equalTo("FHIR")));
    assertThat(result.get(1).getId(), is(equalTo("QICore")));
    verify(modelManagerFactory).getKnownModelIdentifiers();
  }

  // ── getLoadingInfo ────────────────────────────────────────────────────────

  @Test
  void testGetLoadingInfo_returnsEmptyListWhenNonePresent() {
    // given
    when(modelManagerFactory.getModelLoadingInfos()).thenReturn(Collections.emptyList());

    // when
    List<ModelLoadingInfo> result = modelHealthController.getLoadingInfo();

    // then
    assertThat(result, is(notNullValue()));
    assertThat(result, is(empty()));
    verify(modelManagerFactory).getModelLoadingInfos();
  }

  @Test
  void testGetLoadingInfo_returnsInfoWithDefaultNotLoadedState() {
    // given
    ModelLoadingInfo info = new ModelLoadingInfo(fhirIdentifier);
    when(modelManagerFactory.getModelLoadingInfos()).thenReturn(List.of(info));

    // when
    List<ModelLoadingInfo> result = modelHealthController.getLoadingInfo();

    // then
    assertThat(result, is(notNullValue()));
    assertThat(result, hasSize(1));
    assertThat(result.get(0).getModelIdentifier().getId(), is(equalTo("FHIR")));
    assertThat(result.get(0).getLoadingState(), is(ModelLoadingState.NOT_LOADED));
    verify(modelManagerFactory).getModelLoadingInfos();
  }

  @Test
  void testGetLoadingInfo_returnsInfoWithLoadedState() {
    // given
    ModelLoadingInfo info = new ModelLoadingInfo(fhirIdentifier);
    info.setLoadingState(ModelLoadingState.LOADED);
    when(modelManagerFactory.getModelLoadingInfos()).thenReturn(List.of(info));

    // when
    List<ModelLoadingInfo> result = modelHealthController.getLoadingInfo();

    // then
    assertThat(result, is(notNullValue()));
    assertThat(result, hasSize(1));
    assertThat(result.get(0).getLoadingState(), is(ModelLoadingState.LOADED));
    verify(modelManagerFactory).getModelLoadingInfos();
  }

  @Test
  void testGetLoadingInfo_returnsInfoWithErrorState() {
    // given
    ModelLoadingInfo info = new ModelLoadingInfo(fhirIdentifier);
    info.setLoadingState(ModelLoadingState.ERROR_FAILED);
    info.setErrorMessage("Failed to load FHIR model");
    when(modelManagerFactory.getModelLoadingInfos()).thenReturn(List.of(info));

    // when
    List<ModelLoadingInfo> result = modelHealthController.getLoadingInfo();

    // then
    assertThat(result, is(notNullValue()));
    assertThat(result, hasSize(1));
    assertThat(result.get(0).getLoadingState(), is(ModelLoadingState.ERROR_FAILED));
    assertThat(result.get(0).getErrorMessage(), is(equalTo("Failed to load FHIR model")));
    verify(modelManagerFactory).getModelLoadingInfos();
  }

  @Test
  void testGetLoadingInfo_returnsInfoWithLoadingState() {
    // given
    ModelLoadingInfo info = new ModelLoadingInfo(qiCoreIdentifier);
    info.setLoadingState(ModelLoadingState.LOADING);
    when(modelManagerFactory.getModelLoadingInfos()).thenReturn(List.of(info));

    // when
    List<ModelLoadingInfo> result = modelHealthController.getLoadingInfo();

    // then
    assertThat(result, is(notNullValue()));
    assertThat(result, hasSize(1));
    assertThat(result.get(0).getModelIdentifier().getId(), is(equalTo("QICore")));
    assertThat(result.get(0).getLoadingState(), is(ModelLoadingState.LOADING));
    verify(modelManagerFactory).getModelLoadingInfos();
  }

  @Test
  void testGetLoadingInfo_returnsMultipleInfoEntries() {
    // given
    ModelLoadingInfo info1 = new ModelLoadingInfo(fhirIdentifier);
    info1.setLoadingState(ModelLoadingState.LOADED);

    ModelLoadingInfo info2 = new ModelLoadingInfo(qiCoreIdentifier);
    info2.setLoadingState(ModelLoadingState.ERROR_FAILED);
    info2.setErrorMessage("QICore load error");

    when(modelManagerFactory.getModelLoadingInfos()).thenReturn(List.of(info1, info2));

    // when
    List<ModelLoadingInfo> result = modelHealthController.getLoadingInfo();

    // then
    assertThat(result, is(notNullValue()));
    assertThat(result, hasSize(2));
    assertThat(result.get(0).getLoadingState(), is(ModelLoadingState.LOADED));
    assertThat(result.get(1).getLoadingState(), is(ModelLoadingState.ERROR_FAILED));
    assertThat(result.get(1).getErrorMessage(), is(equalTo("QICore load error")));
    verify(modelManagerFactory).getModelLoadingInfos();
  }
}
