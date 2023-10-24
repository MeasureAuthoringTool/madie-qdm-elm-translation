package gov.cms.madie.qdm.humanreadable.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class HumanReadableValuesetModelTest {

  @Test
  void testCreateModel() {
    HumanReadableValuesetModel model =
        new HumanReadableValuesetModel("name", "1.3.6.1.4.1.54392.5.1879", "1.1", "type");
    assertEquals(
        model.getTerminologyDisplay(), "valueset \"name\" (1.3.6.1.4.1.54392.5.1879, version 1.1)");
  }
}
