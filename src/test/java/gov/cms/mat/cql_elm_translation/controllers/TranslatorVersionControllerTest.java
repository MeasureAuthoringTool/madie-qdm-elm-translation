package gov.cms.mat.cql_elm_translation.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import gov.cms.mat.cql_elm_translation.config.TranslatorVersionConfig;

@ExtendWith(MockitoExtension.class)
public class TranslatorVersionControllerTest {

  @Mock private TranslatorVersionConfig translatorVersionConfig;
  @InjectMocks private TranslatorVersionController translatorVersionController;

  private static final String CURRENT_VERSION = "3.3.2";
  private static final String MOST_RECENT_VERSION = "3.10.0";

  @BeforeEach
  void beforeEach() {
    lenient()
        .when(translatorVersionConfig.getCurrentTranslatorVersion())
        .thenReturn(CURRENT_VERSION);
    lenient()
        .when(translatorVersionConfig.getMostRecentTranslatorVersion())
        .thenReturn(MOST_RECENT_VERSION);
  }

  @Test
  public void testGetTranslatorVersionIsDraft() {
    ResponseEntity<String> results = translatorVersionController.getTranslatorVersion(true);
    assertTrue(results.getStatusCode().equals(HttpStatus.OK));
    assertEquals(results.getBody(), MOST_RECENT_VERSION);
  }

  @Test
  public void testGetTranslatorVersionVersioned() {
    ResponseEntity<String> results = translatorVersionController.getTranslatorVersion(false);
    assertTrue(results.getStatusCode().equals(HttpStatus.OK));
    assertEquals(results.getBody(), CURRENT_VERSION);
  }
}
