package gov.cms.mat.cql_elm_translation.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@ExtendWith(MockitoExtension.class)
class VersionServiceTest {

  @InjectMocks VersionService versionService;

  @Test
  void testGetTranslatorVersion() {
    // When
    String version = versionService.getTranslatorVersion();

    // Then
    assertThat(version, is(notNullValue()));
    assertThat(version.isBlank(), is(false));
    assertThat(version.matches("\\d+\\.\\d+\\.\\d+"), is(true));
  }
}
