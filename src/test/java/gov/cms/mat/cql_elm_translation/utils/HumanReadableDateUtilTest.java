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
  void getFormattedMeasurementPeriod2() {
    var result =
        HumanReadableDateUtil.getFormattedMeasurementPeriod(false, "Jan 20, 2024", "Dec 20, 2024");
    assertEquals("Jan 20, 2024 through Dec 20, 2024", result);
  }

  @Test
  void testInvalidDate() {
    var result = HumanReadableDateUtil.getFormattedMeasurementPeriod(false, "2023", "20231231");
    assertEquals("  through December 31, 2023", result);
  }

  @Test
  void testYearEndingWith0000() {
    var result = HumanReadableDateUtil.getFormattedMeasurementPeriod(false, "00000101", "20231231");
    assertEquals("January 1, 0000 through December 31, 2023", result);
  }

  @Test
  void testWhenEndYearIsNull() {
    var result = HumanReadableDateUtil.getFormattedMeasurementPeriod(false, "00000101", "");
    assertEquals("January 1, 0000 ", result);
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

  @Test
  void getFormattedMeasurementPeriodForFhir2() {
    var result =
        HumanReadableDateUtil.getFormattedMeasurementPeriodForFhir("02/01/2023", "11/30/2023");
    assertEquals("February 01, 2023 through November 30, 2023", result);
  }

  @Test
  void getFormattedMeasurementPeriodForFhir3() {
    var result =
        HumanReadableDateUtil.getFormattedMeasurementPeriodForFhir("03/01/2023", "10/31/2023");
    assertEquals("March 01, 2023 through October 31, 2023", result);
  }

  @Test
  void getFormattedMeasurementPeriodForFhir4() {
    var result =
        HumanReadableDateUtil.getFormattedMeasurementPeriodForFhir("04/01/2023", "09/30/2023");
    assertEquals("April 01, 2023 through September 30, 2023", result);
  }

  @Test
  void getFormattedMeasurementPeriodForFhir5() {
    var result =
        HumanReadableDateUtil.getFormattedMeasurementPeriodForFhir("05/01/2023", "08/31/2023");
    assertEquals("May 01, 2023 through August 31, 2023", result);
  }

  @Test
  void getFormattedMeasurementPeriodForFhir6() {
    var result =
        HumanReadableDateUtil.getFormattedMeasurementPeriodForFhir("06/01/2023", "07/31/2023");
    assertEquals("June 01, 2023 through July 31, 2023", result);
  }
}
