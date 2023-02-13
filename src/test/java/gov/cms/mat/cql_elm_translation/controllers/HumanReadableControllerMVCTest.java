package gov.cms.mat.cql_elm_translation.controllers;

import gov.cms.madie.models.measure.Measure;
import gov.cms.mat.cql_elm_translation.ResourceFileUtil;
import gov.cms.mat.cql_elm_translation.service.HumanReadableService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({HumanReadableController.class})
class HumanReadableControllerMVCTest implements ResourceFileUtil {

  private static final String TEST_USER_ID = "john_doe";

  @Autowired private MockMvc mockMvc;

  @MockBean HumanReadableService humanReadableService;

  @Test
  public void testHumanReadableController() throws Exception {

    String madieMeasureJson = getData("/example_madie_measure.json");

    when(humanReadableService.generateHumanReadable(any(Measure.class), anyString()))
        .thenReturn("Generated Human Readable");
    mockMvc
        .perform(
            MockMvcRequestBuilders.put("/human-readable")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "test-okta")
                .content(madieMeasureJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isOk());
    verify(humanReadableService, times(1)).generateHumanReadable(any(Measure.class), anyString());
  }
}
