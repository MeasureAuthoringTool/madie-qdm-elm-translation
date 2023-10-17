package gov.cms.mat.cql_elm_translation.cql_translator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TranslationResourceTest {
  TranslationResource translationResource = TranslationResource.getInstance(true);

  @Test
  void buildTranslator_checkExceptionHandling() {

    Assertions.assertThrows(
        TranslationFailureException.class,
        () -> {
          translationResource.buildTranslator(null, null, null);
        });
  }
}
