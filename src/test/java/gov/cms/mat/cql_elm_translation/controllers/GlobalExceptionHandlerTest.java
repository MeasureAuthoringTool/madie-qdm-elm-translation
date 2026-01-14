package gov.cms.mat.cql_elm_translation.controllers;

import gov.cms.mat.cql_elm_translation.exceptions.UnsupportedModelException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import gov.cms.mat.cql_elm_translation.service.CqlConversionService;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CqlConversionController.class)
class GlobalExceptionHandlerTest {
  private static final String TEST_USER_ID = "john_doe";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CqlConversionService cqlConversionService;

  @Test
  void givenUnsupportedModelExceptionWhenTranslateThenReturnsBadRequest() throws Exception {
    // given
    when(cqlConversionService.translateCqlToElm(any(), anyBoolean()))
        .thenThrow(new UnsupportedModelException());

    String cqlData = "using BadModel version '1.0.0'";

    // when/then
    mockMvc
        .perform(
            put("/cql/translator/cql")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, TEST_USER_ID)
                .content(cqlData)
                .contentType(MediaType.TEXT_PLAIN))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(
            jsonPath("$.message")
                .value(containsString("Only FHIR-based models are supported at this time")));

    // Verify the service was called and threw the expected exception
    verify(cqlConversionService, times(1)).translateCqlToElm(any(), anyBoolean());
  }
}
