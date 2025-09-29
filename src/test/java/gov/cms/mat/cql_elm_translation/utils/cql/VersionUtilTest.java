package gov.cms.mat.cql_elm_translation.utils.cql;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class VersionUtilTest {
  private static final String BASELINE = "7.0.0";

  @ParameterizedTest
  @DisplayName("Versions that should be >= baseline 7.0.0")
  @CsvSource({
    "7,true",
    "7.0,true",
    "7.0.0,true",
    "7.0.1,true",
    "7.1,true",
    "7.1.0,true",
    "7.10.5,true",
    "8.0.0,true",
    "10.2.3,true",
    "7.0.0-RC1,true",
    "7.0.1-RC2,true",
    "007.000.001,true"
  })
  void testVersionsAtOrAboveBaseline(String version, boolean expected) {
    assertEquals(
        expected,
        VersionUtil.isVersionAtLeast(version, BASELINE),
        () -> "Expected '" + version + "' >= " + BASELINE + " to be " + expected);
  }

  @ParameterizedTest
  @DisplayName("Versions that should be < baseline 7.0.0")
  @CsvSource({"6,false", "6.9,false", "6.9.9,false", "6.10.0,false", "5.99.99,false"})
  void testVersionsBelowBaseline(String version, boolean expected) {
    assertEquals(
        expected,
        VersionUtil.isVersionAtLeast(version, BASELINE),
        () -> "Expected '" + version + "' >= " + BASELINE + " to be " + expected);
  }

  @Nested
  class EdgeCases {
    @Test
    @DisplayName("Null version should be treated as below baseline")
    void nullVersion() {
      assertFalse(VersionUtil.isVersionAtLeast(null, BASELINE));
    }

    @Test
    @DisplayName("Blank version should be treated as below baseline")
    void blankVersion() {
      assertFalse(VersionUtil.isVersionAtLeast("   ", BASELINE));
    }

    @Test
    @DisplayName("Malformed version with no digits should be below baseline")
    void malformedNoDigits() {
      assertFalse(VersionUtil.isVersionAtLeast("foo", BASELINE));
    }

    @Test
    @DisplayName("Mixed alphanumeric with leading digits still parses major")
    void mixedAlphaNumeric() {
      assertTrue(VersionUtil.isVersionAtLeast("7beta", BASELINE));
    }

    @Test
    @DisplayName("Pre-release tag still counts as underlying numeric version")
    void preReleaseTag() {
      assertTrue(VersionUtil.isVersionAtLeast("7.0.0-SNAPSHOT", BASELINE));
    }

    @Test
    @DisplayName("Exactly baseline returns true")
    void equalBaseline() {
      assertTrue(VersionUtil.isVersionAtLeast(BASELINE, BASELINE));
    }
  }

  @ParameterizedTest
  @DisplayName("Custom baseline comparisons (sanity checks)")
  @CsvSource({
    // version, baseline, expected
    "7.0.0,7.0.0,true",
    "7.0.1,7.0.0,true",
    "7.0.0,7.0.1,false",
    "7.1.0,7.0.5,true",
    "7.0.5,7.1.0,false",
    "8.0.0,7.9.9,true",
    "7.9.9,8.0.0,false"
  })
  void customBaseline(String version, String baseline, boolean expected) {
    assertEquals(
        expected,
        VersionUtil.isVersionAtLeast(version, baseline),
        () -> String.format("Expected %s >= %s to be %s", version, baseline, expected));
  }
}
