package gov.cms.mat.cql_elm_translation.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HumanReadableDateUtilTest {

  @Test
  void getFormattedMeasurementPeriod() {
    var result = HumanReadableDateUtil.getFormattedMeasurementPeriod(false, "20230101", "20231231");
    assertEquals("January 1, 2023 through December 31, 2023", result);
  }

  @Test
  void testInvalidDate() {
    var result = HumanReadableDateUtil.getFormattedMeasurementPeriod(false, "2023", "20231231");
    assertEquals("  through December 31, 2023", result);
  }

  @Test
  void testYearEndingWith0000() {
    var result = HumanReadableDateUtil.getFormattedMeasurementPeriod(false, "00000101", "20231231");
    assertEquals("January 1, 20XX through December 31, 2023", result);
  }

  @Test
  void testWhenEndYearIsNull() {
    var result = HumanReadableDateUtil.getFormattedMeasurementPeriod(false, "00000101", "");
    assertEquals("January 1, 20XX ", result);
  }

  @Test
  void testCalendarYearPeriod() {
    var result = HumanReadableDateUtil.getFormattedMeasurementPeriod(true, "20230101", "20231231");
    assertEquals("January 1, 20XX through December 31, 20XX", result);
  }

  @Test
  void getFormattedMeasurementPeriodForFhir() {
    var result =
        HumanReadableDateUtil.getFormattedMeasurementPeriodForFhir("01/01/2023", "12/31/2023");
    assertEquals("January 01, 2023 through December 31, 2023", result);
  }
}
