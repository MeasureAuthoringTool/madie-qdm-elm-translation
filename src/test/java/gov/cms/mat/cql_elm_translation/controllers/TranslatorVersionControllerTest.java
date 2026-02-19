package gov.cms.mat.cql_elm_translation.controllers;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import org.cqframework.cql_to_elm.BuildConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
public class TranslatorVersionControllerTest {

  @Spy private TranslatorVersionController translatorVersionController;

  @BeforeEach
  public void setUp() {
    ReflectionTestUtils.setField(translatorVersionController, "translatorPomPropertyVersion", "");
  }

  @Test
  public void testGetTranslatorVersionFromBuildConfig() {
    // Given
    when(translatorVersionController.getBuildConfigVersion()).thenReturn("1.2.3");
    ReflectionTestUtils.setField(translatorVersionController, "translatorPomPropertyVersion", "");

    // When
    ResponseEntity<String> results = translatorVersionController.getTranslatorVersion(true);

    // Then
    assertThat(results.getStatusCode(), is(equalTo(HttpStatus.OK)));
    assertThat(results.getBody(), is(equalTo("1.2.3")));
  }

  @Test
  public void testGetTranslatorVersionFallsBackToPomProperty() {
    // Given
    when(translatorVersionController.getBuildConfigVersion()).thenReturn(null);
    ReflectionTestUtils.setField(
        translatorVersionController, "translatorPomPropertyVersion", "4.2.0");

    // When
    ResponseEntity<String> results = translatorVersionController.getTranslatorVersion(true);

    // Then
    assertThat(results.getStatusCode(), is(equalTo(HttpStatus.OK)));
    assertThat(results.getBody(), is(equalTo("4.2.0")));
  }

  @Test
  public void testGetTranslatorVersionBuildConfigTakesPrecedence() {
    // Given
    when(translatorVersionController.getBuildConfigVersion()).thenReturn("1.2.3");
    ReflectionTestUtils.setField(
        translatorVersionController, "translatorPomPropertyVersion", "4.2.0");

    // When
    ResponseEntity<String> results = translatorVersionController.getTranslatorVersion(true);

    // Then
    assertThat(results.getStatusCode(), is(equalTo(HttpStatus.OK)));
    assertThat(results.getBody(), is(equalTo("1.2.3")));
  }

  @Test
  public void testGetTranslatorVersionFailsWhenBothSourcesBlank() {
    // Given
    when(translatorVersionController.getBuildConfigVersion()).thenReturn("");
    ReflectionTestUtils.setField(translatorVersionController, "translatorPomPropertyVersion", "");

    // When
    ResponseEntity<String> results = translatorVersionController.getTranslatorVersion(true);

    // Then
    assertThat(results.getStatusCode(), is(equalTo(HttpStatus.FAILED_DEPENDENCY)));
  }

  @Test
  public void testGetTranslatorVersionFailsWhenBothSourcesNull() {
    // Given
    when(translatorVersionController.getBuildConfigVersion()).thenReturn(null);
    ReflectionTestUtils.setField(translatorVersionController, "translatorPomPropertyVersion", null);

    // When
    ResponseEntity<String> results = translatorVersionController.getTranslatorVersion(true);

    // Then
    assertThat(results.getStatusCode(), is(equalTo(HttpStatus.FAILED_DEPENDENCY)));
  }

  @Test
  public void testGetTranslatorVersionForDraftFalseThrowsException() {
    // When
    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> translatorVersionController.getTranslatorVersion(false));

    // Then
    assertThat(exception.getStatusCode(), is(equalTo(HttpStatus.BAD_REQUEST)));
    assertThat(exception.getReason(), is(equalTo("Non-draft version is no longer supported.")));
  }

  @Test
  public void testGetBuildConfigVersionReturnsValue() {
    // Given - use real method (no mock)
    TranslatorVersionController realController = new TranslatorVersionController();

    // When
    String version = realController.getBuildConfigVersion();

    // Then - just verify it doesn't throw; value may be null at test time
    // This exercises the real BuildConfig.IMPLEMENTATION_VERSION access
    assertThat(version, is(equalTo(BuildConfig.IMPLEMENTATION_VERSION)));
  }
}
