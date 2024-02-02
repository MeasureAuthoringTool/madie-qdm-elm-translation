package gov.cms.mat.cql_elm_translation.controllers;

import gov.cms.mat.cql_elm_translation.dto.CqlLibraryDetails;
import gov.cms.mat.cql_elm_translation.service.EffectiveDataRequirementService;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({EffectiveDataRequirementController.class})
class EffectiveDataRequirementControllerMVCTest {

  private static final String TEST_USER_ID = "john_doe";

  @Autowired private MockMvc mockMvc;

  @MockBean EffectiveDataRequirementService effectiveDataRequirementService;

  @Mock org.hl7.fhir.r5.model.Library r5Libray;

  @Test
  public void testGetEffectiveDataRequirementsThrowsExceptionWhenLibraryDetailsIsnull()
      throws Exception {
    var results =
        mockMvc
            .perform(
                MockMvcRequestBuilders.put("/effective-data-requirements")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .header(HttpHeaders.AUTHORIZATION, "test-okta")
                    .param("recursive", "true")
                    .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().is4xxClientError())
            .andReturn();
    assertThat(results.getResponse().getStatus(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));
  }

  @Test
  public void testGetEffectiveDataRequirementsThrowsExceptionWhenCqlIsNull() throws Exception {
    var results =
        mockMvc
            .perform(
                MockMvcRequestBuilders.put("/effective-data-requirements")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .header(HttpHeaders.AUTHORIZATION, "test-okta")
                    .content("{\"cql\": null, \"libraryName\": \"Test\", \"expressions\": []}")
                    .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().is4xxClientError())
            .andReturn();
    assertThat(results.getResponse().getStatus(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));
  }

  @Test
  public void testGetEffectiveDataRequirementsSuccess() throws Exception {
    when(effectiveDataRequirementService.getEffectiveDataRequirements(
            any(CqlLibraryDetails.class), anyBoolean(), anyString()))
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
                .content("{\"cql\": \"Test CQL\", \"libraryName\": \"Test\", \"expressions\": []}")
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isOk());
    verify(effectiveDataRequirementService, times(1))
        .getEffectiveDataRequirements(any(CqlLibraryDetails.class), anyBoolean(), anyString());
    verify(effectiveDataRequirementService, times(1))
        .getEffectiveDataRequirementsStr(any(org.hl7.fhir.r5.model.Library.class));
  }
}
