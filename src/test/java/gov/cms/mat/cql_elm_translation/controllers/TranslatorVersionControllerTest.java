package gov.cms.mat.cql_elm_translation.controllers;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import gov.cms.mat.cql_elm_translation.service.VersionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
public class TranslatorVersionControllerTest {

  @Mock private VersionService versionService;
  @InjectMocks private TranslatorVersionController translatorVersionController;

  @Test
  public void testGetTranslatorVersionIsDraft() {
    // Given
    when(versionService.getTranslatorVersion()).thenReturn("1.2.3");

    // When
    ResponseEntity<String> results = translatorVersionController.getTranslatorVersion(true);

    // Then
    assertThat(results.getStatusCode(), is(equalTo(HttpStatus.OK)));
    assertThat(results.getBody(), is(equalTo("1.2.3")));
  }

  @Test
  public void testGetTranslatorVersionWithVersionLookupFailure() {
    // Given
    when(versionService.getTranslatorVersion())
        .thenThrow(new IllegalStateException("Unable to determine translator version."));

    // When
    ResponseEntity<String> results = translatorVersionController.getTranslatorVersion(true);

    // Then
    assertThat(results.getStatusCode(), is(equalTo(HttpStatus.FAILED_DEPENDENCY)));
  }

  @Test
  public void testGetTranslatorVersionForDraftFalse() {
    // When
    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> {
              translatorVersionController.getTranslatorVersion(false);
            });

    // Then
    assertThat(exception.getReason(), is("Non-draft version is no longer supported."));
  }
}
