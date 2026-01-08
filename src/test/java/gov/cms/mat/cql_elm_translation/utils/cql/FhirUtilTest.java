package gov.cms.mat.cql_elm_translation.utils.cql;

import gov.cms.mat.cql.elements.UsingProperties;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class FhirUtilTest {
  private final FhirUtil fhirUtil = new FhirUtil();

  @Test
  void isFhirModelShouldReturnTrueForFhirModels() {
    // given
    // no mocks needed

    // when
    boolean fhir = fhirUtil.isFhirModel("FHIR");
    boolean uscore = fhirUtil.isFhirModel("uscore");
    boolean qicore = fhirUtil.isFhirModel("QICORE");

    // then
    assertThat(fhir, is(true));
    assertThat(uscore, is(true));
    assertThat(qicore, is(true));
  }

  @Test
  void isFhirModelShouldReturnFalseForUnknownModels() {
    // given
    // no mocks needed

    // when
    boolean result = fhirUtil.isFhirModel("notamodel");

    // then
    assertThat(result, is(false));
  }

  @Test
  void isFhirModelShouldReturnFalseForNullOrEmpty() {
    // given
    // no mocks needed

    // when
    boolean nullResult = fhirUtil.isFhirModel(null);
    boolean emptyResult = fhirUtil.isFhirModel("");

    // then
    assertThat(nullResult, is(false));
    assertThat(emptyResult, is(false));
  }

  @Test
  void getMostSpecificFhirModelShouldReturnMostSpecific() {
    // given
    UsingProperties fhir = Mockito.mock(UsingProperties.class);
    Mockito.when(fhir.getLibraryType()).thenReturn("FHIR");
    UsingProperties uscore = Mockito.mock(UsingProperties.class);
    Mockito.when(uscore.getLibraryType()).thenReturn("USCore");
    UsingProperties qicore = Mockito.mock(UsingProperties.class);
    Mockito.when(qicore.getLibraryType()).thenReturn("QICore");
    List<UsingProperties> list = Arrays.asList(fhir, uscore, qicore);

    // when
    UsingProperties result = fhirUtil.getMostSpecificFhirModel(list);

    // then
    assertThat(result, is(equalTo(qicore)));
  }

  @Test
  void getMostSpecificFhirModelShouldReturnNullIfNoneMatch() {
    // given
    UsingProperties notFhir = Mockito.mock(UsingProperties.class);
    Mockito.when(notFhir.getLibraryType()).thenReturn("NotAModel");
    List<UsingProperties> list = Collections.singletonList(notFhir);

    // when
    UsingProperties result = fhirUtil.getMostSpecificFhirModel(list);

    // then
    assertThat(result, is(nullValue()));
  }

  @Test
  void getMostSpecificFhirModelShouldReturnNullForNullOrEmptyList() {
    // given
    // no mocks needed

    // when
    UsingProperties nullResult = fhirUtil.getMostSpecificFhirModel(null);
    UsingProperties emptyResult = fhirUtil.getMostSpecificFhirModel(Collections.emptyList());

    // then
    assertThat(nullResult, is(nullValue()));
    assertThat(emptyResult, is(nullValue()));
  }
}
