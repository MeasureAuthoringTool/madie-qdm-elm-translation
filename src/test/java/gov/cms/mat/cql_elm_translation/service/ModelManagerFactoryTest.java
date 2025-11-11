package gov.cms.mat.cql_elm_translation.service;

import org.cqframework.cql.cql2elm.ModelManager;
import org.hl7.cql.model.ModelIdentifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Disabled("Enabled this test case when issue with resolving USCore dependency is fixed.")
@ExtendWith(MockitoExtension.class)
public class ModelManagerFactoryTest {

  private static ModelManagerFactory modelManagerFactory;

  @BeforeAll
  static void setUp() {
    modelManagerFactory = new ModelManagerFactory();
  }

  @Test
  void testConstructorInitializesSuccessfully() {
    assertNotNull(modelManagerFactory);
  }

  @Test
  void testGetModelManagerWithValidIdentifier() {
    ModelIdentifier identifier = new ModelIdentifier().withId("FHIR").withVersion("4.0.1");

    ModelManager modelManager = modelManagerFactory.getModelManager(identifier);

    assertNotNull(modelManager);
  }

  @Test
  void testGetModelManagerWithQICoreIdentifier() {
    ModelIdentifier identifier = new ModelIdentifier().withId("QICore").withVersion("7.0.1");

    ModelManager modelManager = modelManagerFactory.getModelManager(identifier);

    assertNotNull(modelManager);
    assertThat(modelManager, is(notNullValue()));
  }

  @Test
  void testGetModelManagerWithCqlIdentifier() {
    ModelIdentifier identifier =
        new ModelIdentifier().withId("hl7.fhir.us.cql").withVersion("1.0.0");

    ModelManager modelManager = modelManagerFactory.getModelManager(identifier);

    assertNotNull(modelManager);
  }

  @Test
  void testGetModelManagerWithNullIdentifierThrowsException() {
    Exception exception =
        assertThrows(
            IllegalArgumentException.class, () -> modelManagerFactory.getModelManager(null));

    assertThat(exception.getMessage(), equalTo("Model name cannot be null or empty"));
  }

  @Test
  void testGetModelManagerWithNullIdThrowsException() {
    ModelIdentifier identifier = new ModelIdentifier().withId(null).withVersion("1.0.0");

    Exception exception =
        assertThrows(
            IllegalArgumentException.class, () -> modelManagerFactory.getModelManager(identifier));

    assertThat(exception.getMessage(), equalTo("Model name cannot be null or empty"));
  }

  @Test
  void testGetModelManagerWithEmptyIdThrowsException() {
    ModelIdentifier identifier = new ModelIdentifier().withId("").withVersion("1.0.0");

    Exception exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              modelManagerFactory.getModelManager(identifier);
            });

    assertThat(exception.getMessage(), equalTo("Model name cannot be null or empty"));
  }

  @Test
  void testGetModelManagerReturnsSameInstanceForSameIdentifier() {
    ModelIdentifier identifier = new ModelIdentifier().withId("FHIR").withVersion("4.0.1");

    ModelManager modelManager1 = modelManagerFactory.getModelManager(identifier);
    ModelManager modelManager2 = modelManagerFactory.getModelManager(identifier);

    assertNotNull(modelManager1);
    assertNotNull(modelManager2);
    assertSame(modelManager1, modelManager2);
  }

  @Test
  void testGetModelManagerReturnsDifferentInstancesForDifferentIdentifiers() {
    ModelIdentifier identifier1 = new ModelIdentifier().withId("FHIR").withVersion("4.0.1");
    ModelIdentifier identifier2 = new ModelIdentifier().withId("FHIR").withVersion("5.0.0");

    ModelManager modelManager1 = modelManagerFactory.getModelManager(identifier1);
    ModelManager modelManager2 = modelManagerFactory.getModelManager(identifier2);

    assertNotNull(modelManager1);
    assertNotNull(modelManager2);
    assertNotSame(modelManager1, modelManager2);
  }

  @Test
  void testGetModelManagerCreatesNewManagerForUnknownModel() {
    ModelIdentifier identifier = new ModelIdentifier().withId("UnknownModel").withVersion("1.0.0");

    ModelManager modelManager = modelManagerFactory.getModelManager(identifier);

    assertNotNull(modelManager);
  }

  @Test
  void testLogDebugMessageWithNullCategory() {
    assertDoesNotThrow(
        () -> {
          modelManagerFactory.logDebugMessage(null, "Test debug message with null category");
        });
  }

  @Test
  void testLogMessageWithNullMessage() {
    assertDoesNotThrow(
        () -> {
          modelManagerFactory.logMessage(null);
        });
  }

  @Test
  void testMultipleModelManagersCanCoexist() {
    ModelIdentifier identifier1 = new ModelIdentifier().withId("Model1").withVersion("1.0.0");
    ModelIdentifier identifier2 = new ModelIdentifier().withId("Model2").withVersion("2.0.0");
    ModelIdentifier identifier3 = new ModelIdentifier().withId("Model3").withVersion("3.0.0");

    ModelManager modelManager1 = modelManagerFactory.getModelManager(identifier1);
    ModelManager modelManager2 = modelManagerFactory.getModelManager(identifier2);
    ModelManager modelManager3 = modelManagerFactory.getModelManager(identifier3);

    assertNotNull(modelManager1);
    assertNotNull(modelManager2);
    assertNotNull(modelManager3);
    assertNotSame(modelManager1, modelManager2);
    assertNotSame(modelManager2, modelManager3);
    assertNotSame(modelManager1, modelManager3);
  }
}
