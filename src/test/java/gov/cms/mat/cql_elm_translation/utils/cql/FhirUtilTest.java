package gov.cms.mat.cql_elm_translation.utils.cql;

import gov.cms.mat.cql.elements.UsingProperties;
import org.hl7.fhir.r5.model.ImplementationGuide;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;

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
    when(fhir.getLibraryType()).thenReturn("FHIR");
    UsingProperties uscore = Mockito.mock(UsingProperties.class);
    when(uscore.getLibraryType()).thenReturn("USCore");
    UsingProperties qicore = Mockito.mock(UsingProperties.class);
    when(qicore.getLibraryType()).thenReturn("QICore");
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
    when(notFhir.getLibraryType()).thenReturn("NotAModel");
    List<UsingProperties> list = Collections.singletonList(notFhir);

    // when
    UsingProperties result = fhirUtil.getMostSpecificFhirModel(list);

    // then
    assertThat(result, is(nullValue()));
  }

  @Test
  void getMostSpecificFhirModelShouldIgnoreUnknownModelAndReturnQiCore() {
    // given
    UsingProperties notFhir = Mockito.mock(UsingProperties.class);
    when(notFhir.getLibraryType()).thenReturn("NotAModel");
    UsingProperties qicore = Mockito.mock(UsingProperties.class);
    when(qicore.getLibraryType()).thenReturn("QICore");
    List<UsingProperties> list = Arrays.asList(notFhir, qicore);

    // when
    UsingProperties result = fhirUtil.getMostSpecificFhirModel(list);

    // then
    assertThat(result, is(equalTo(qicore)));
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

  @Test
  void getMostSpecificFhirModelShouldSkipNullEntriesAndChooseSpecific() {
    // given
    UsingProperties qicore = Mockito.mock(UsingProperties.class);
    when(qicore.getLibraryType()).thenReturn("QICore");
    List<UsingProperties> list = Arrays.asList(null, qicore);

    // when
    UsingProperties result = fhirUtil.getMostSpecificFhirModel(list);

    // then
    assertThat(result, is(equalTo(qicore)));
  }

  @Test
  void getMostSpecificFhirModelShouldSkipEntriesWithNullLibraryType() {
    // given
    UsingProperties nullType = Mockito.mock(UsingProperties.class);
    when(nullType.getLibraryType()).thenReturn(null);
    UsingProperties qicore = Mockito.mock(UsingProperties.class);
    when(qicore.getLibraryType()).thenReturn("QICore");
    List<UsingProperties> list = Arrays.asList(nullType, qicore);

    // when
    UsingProperties result = fhirUtil.getMostSpecificFhirModel(list);

    // then
    assertThat(result, is(equalTo(qicore)));
  }

  @Test
  void getMostSpecificFhirModelShouldKeepFirstMostSpecificWhenLessSpecificFollows() {
    // given
    UsingProperties qicore = Mockito.mock(UsingProperties.class);
    when(qicore.getLibraryType()).thenReturn("QICore");
    UsingProperties uscore = Mockito.mock(UsingProperties.class);
    when(uscore.getLibraryType()).thenReturn("USCore");
    List<UsingProperties> list = Arrays.asList(qicore, uscore);

    // when
    UsingProperties result = fhirUtil.getMostSpecificFhirModel(list);

    // then
    assertThat(result, is(equalTo(qicore)));
  }

  @Test
  void getMostSpecificFhirModelShouldKeepFirstMostSpecificWhenLessSpecificFollowsZZZ() {
    // given
    UsingProperties qicore = Mockito.mock(UsingProperties.class);
    when(qicore.getLibraryType()).thenReturn("OtherModel");
    UsingProperties uscore = Mockito.mock(UsingProperties.class);
    when(uscore.getLibraryType()).thenReturn("USCore");
    UsingProperties bad = Mockito.mock(UsingProperties.class);
    when(bad.getLibraryType()).thenReturn("BadModel");
    List<UsingProperties> list = Arrays.asList(qicore, uscore, bad);
    Object map = ReflectionTestUtils.getField(FhirUtil.class, "MODEL_MAP");
    if (map instanceof Map) {
      Map<String, ModelNode> modelMap = (Map<String, ModelNode>) map;
      modelMap.put("BADMODEL", new ModelNode("BADMODEL", null));
    }

    // when
    UsingProperties result = fhirUtil.getMostSpecificFhirModel(list);

    // then
    assertThat(result, is(equalTo(uscore)));
  }

  @Test
  void loadImplementationGuideShouldReturnGuideWithExpectedAttributes() {
    // given
    String resourcePath = "igs/qicore-7-madie-ig.json";

    // when
    ImplementationGuide implementationGuide = fhirUtil.loadImplementationGuide(resourcePath);

    // then
    assertThat(implementationGuide, is(notNullValue()));
    assertThat(implementationGuide.getId(), is(equalTo("ImplementationGuide/cms.fhir.us.madieig")));
    assertThat(
        implementationGuide.getUrl(),
        is(
            equalTo(
                "http://madie.cms.gov/fhir/us/madieig/ImplementationGuide/cms.fhir.us.madieig")));
    assertThat(implementationGuide.getContactFirstRep().getName(), is(equalTo("CMS")));
  }

  @Test
  void loadImplementationGuideShouldIncludeDependencies() {
    // given
    String resourcePath = "igs/qicore-7-madie-ig.json";

    // when
    ImplementationGuide implementationGuide = fhirUtil.loadImplementationGuide(resourcePath);

    // then
    assertThat(implementationGuide.getDependsOn(), hasSize(greaterThanOrEqualTo(2)));
    assertThat(
        implementationGuide.getDependsOn().get(0).getPackageId(),
        is(equalTo("hl7.fhir.us.qicore")));
  }
}
