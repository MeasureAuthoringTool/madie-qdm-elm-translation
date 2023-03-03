package gov.cms.mat.cql_elm_translation.controllers;

import gov.cms.mat.cql_elm_translation.service.EffectiveDataRequirementService;

import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.JsonParser;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@WebMvcTest({EffectiveDataRequirementController.class})
class EffectiveDataRequirementControllerMVCTest {

  private static final String TEST_USER_ID = "john_doe";

  @Autowired private MockMvc mockMvc;

  @MockBean EffectiveDataRequirementService effectiveDataRequirementService;

  @MockBean private FhirContext fhirContextForR5;

  @Mock JsonParser jsonParser;

  @Mock org.hl7.fhir.r5.model.Library r5Libray;

  @Mock org.hl7.fhir.r4.model.Bundle bundle;
  @Mock org.hl7.fhir.r4.model.Bundle.BundleEntryComponent entryComponent;
  @Mock org.hl7.fhir.r4.model.Bundle.BundleEntryComponent measureEntryComponent;
  @Mock Resource measureResource;
  @Mock org.hl7.fhir.r4.model.Bundle.BundleEntryComponent libraryEntryComponent;
  @Mock org.hl7.fhir.r4.model.Library library;
  @Mock org.hl7.fhir.r5.model.Measure r5Measure;
  private List<BundleEntryComponent> entries = new ArrayList<>();

  @BeforeEach
  void setUp() {

    when(fhirContextForR5.newJsonParser()).thenReturn(jsonParser);

    entries.add(measureEntryComponent);
    entries.add(libraryEntryComponent);
  }

  @Test
  public void testGetEffectiveDataRequirementsThrowsExceptionWhenBundleIsNull() throws Exception {
    when(effectiveDataRequirementService.createFhirResourceFromJson(anyString(), any()))
        .thenReturn(null);
    mockMvc
        .perform(
            MockMvcRequestBuilders.put("/effective-data-requirements")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "test-okta")
                .content("testBundleStr")
                .param("libraryName", "CMS104")
                .param("measureId", "CMS104")
                .contentType(MediaType.TEXT_PLAIN_VALUE))
        .andExpect(status().is4xxClientError());
    verify(effectiveDataRequirementService, times(1))
        .createFhirResourceFromJson(anyString(), any());
  }

  @Test
  public void testGetEffectiveDataRequirementsThrowsExceptionWhenBundleEntryIsNull()
      throws Exception {
    when(effectiveDataRequirementService.createFhirResourceFromJson(anyString(), any()))
        .thenReturn(bundle);
    when(bundle.getEntry()).thenReturn(new ArrayList<>());
    mockMvc
        .perform(
            MockMvcRequestBuilders.put("/effective-data-requirements")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "test-okta")
                .content("testBundleStr")
                .param("libraryName", "CMS104")
                .param("measureId", "CMS104")
                .contentType(MediaType.TEXT_PLAIN_VALUE))
        .andExpect(status().is4xxClientError());
    verify(effectiveDataRequirementService, times(1))
        .createFhirResourceFromJson(anyString(), any());
  }

  @Test
  public void testGetEffectiveDataRequirementsThrowsExceptionWhenMeasureEntryIsNull()
      throws Exception {
    when(effectiveDataRequirementService.createFhirResourceFromJson(anyString(), any()))
        .thenReturn(bundle);
    when(bundle.getEntry()).thenReturn(entries);
    when(effectiveDataRequirementService.getMeasureEntry(any(org.hl7.fhir.r4.model.Bundle.class)))
        .thenReturn(Optional.empty());
    mockMvc
        .perform(
            MockMvcRequestBuilders.put("/effective-data-requirements")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "test-okta")
                .content("testBundleStr")
                .param("libraryName", "CMS104")
                .param("measureId", "CMS104")
                .contentType(MediaType.TEXT_PLAIN_VALUE))
        .andExpect(status().is4xxClientError());
    verify(effectiveDataRequirementService, times(1))
        .createFhirResourceFromJson(anyString(), any());
    verify(effectiveDataRequirementService, times(1))
        .getMeasureEntry(any(org.hl7.fhir.r4.model.Bundle.class));
  }

  @Test
  public void testGetEffectiveDataRequirementsThrowsExceptionWhenLibraryEntryIsNull()
      throws Exception {
    when(effectiveDataRequirementService.createFhirResourceFromJson(anyString(), any()))
        .thenReturn(bundle);
    when(bundle.getEntry()).thenReturn(entries);
    when(effectiveDataRequirementService.getMeasureEntry(any(org.hl7.fhir.r4.model.Bundle.class)))
        .thenReturn(Optional.of(measureEntryComponent));
    when(measureEntryComponent.getResource()).thenReturn(measureResource);
    when(effectiveDataRequirementService.getMeasureLibraryEntry(
            any(org.hl7.fhir.r4.model.Bundle.class), anyString()))
        .thenReturn(Optional.empty());

    mockMvc
        .perform(
            MockMvcRequestBuilders.put("/effective-data-requirements")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "test-okta")
                .content("testBundleStr")
                .param("libraryName", "CMS104")
                .param("measureId", "CMS104")
                .contentType(MediaType.TEXT_PLAIN_VALUE))
        .andExpect(status().is4xxClientError());
    verify(effectiveDataRequirementService, times(1))
        .createFhirResourceFromJson(anyString(), any());
    verify(effectiveDataRequirementService, times(1))
        .getMeasureEntry(any(org.hl7.fhir.r4.model.Bundle.class));
    verify(effectiveDataRequirementService, times(1))
        .getMeasureLibraryEntry(any(org.hl7.fhir.r4.model.Bundle.class), anyString());
  }

  @Test
  public void testGetEffectiveDataRequirementsSuccess() throws Exception {

    when(effectiveDataRequirementService.createFhirResourceFromJson(anyString(), any()))
        .thenReturn(bundle);
    when(bundle.getEntry()).thenReturn(entries);
    when(effectiveDataRequirementService.getMeasureEntry(any(org.hl7.fhir.r4.model.Bundle.class)))
        .thenReturn(Optional.of(measureEntryComponent));
    when(measureEntryComponent.getResource()).thenReturn(measureResource);
    when(effectiveDataRequirementService.getMeasureLibraryEntry(
            any(org.hl7.fhir.r4.model.Bundle.class), anyString()))
        .thenReturn(Optional.of(libraryEntryComponent));
    when(libraryEntryComponent.getResource()).thenReturn(library);
    when(effectiveDataRequirementService.getR5MeasureFromR4MeasureResource(any(Resource.class)))
        .thenReturn(r5Measure);
    when(effectiveDataRequirementService.getEffectiveDataRequirements(
            any(org.hl7.fhir.r5.model.Measure.class),
            any(org.hl7.fhir.r4.model.Library.class),
            anyString()))
        .thenReturn(r5Libray);
    when(effectiveDataRequirementService.getEffectiveDataRequirementsStr(
            any(org.hl7.fhir.r5.model.Library.class)))
        .thenReturn("test");
    mockMvc
        .perform(
            MockMvcRequestBuilders.put("/effective-data-requirements")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "test-okta")
                .content("testBundleStr")
                .param("libraryName", "testLibraryName")
                .param("measureId", "testMeasureId")
                .contentType(MediaType.TEXT_PLAIN_VALUE))
        .andExpect(status().isOk());
    verify(effectiveDataRequirementService, times(1))
        .getMeasureEntry(any(org.hl7.fhir.r4.model.Bundle.class));
    verify(effectiveDataRequirementService, times(1))
        .getMeasureLibraryEntry(any(org.hl7.fhir.r4.model.Bundle.class), anyString());
    verify(effectiveDataRequirementService, times(1))
        .getEffectiveDataRequirements(
            any(org.hl7.fhir.r5.model.Measure.class),
            any(org.hl7.fhir.r4.model.Library.class),
            anyString());
  }
}
