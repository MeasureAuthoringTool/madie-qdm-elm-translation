package gov.cms.mat.cql_elm_translation.cql_translator;

import org.cqframework.cql.cql2elm.CqlCompilerException;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.CqlTranslatorOptions;

import org.cqframework.cql.cql2elm.LibraryBuilder;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.fhir.ucum.UcumEssenceService;
import org.fhir.ucum.UcumException;
import org.fhir.ucum.UcumService;
import org.springframework.stereotype.Service;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TranslationResource {
  public enum ModelType {
    FHIR,
    QICore;
  }

  private static final MultivaluedMap<String, CqlTranslatorOptions.Options> PARAMS_TO_OPTIONS_MAP =
      new MultivaluedHashMap<>() {
        {
          putSingle(
              "date-range-optimization", CqlTranslatorOptions.Options.EnableDateRangeOptimization);
          putSingle("annotations", CqlTranslatorOptions.Options.EnableAnnotations);
          putSingle("locators", CqlTranslatorOptions.Options.EnableLocators);
          putSingle("result-types", CqlTranslatorOptions.Options.EnableResultTypes);
          putSingle("detailed-errors", CqlTranslatorOptions.Options.EnableDetailedErrors);
          putSingle("disable-list-traversal", CqlTranslatorOptions.Options.DisableListTraversal);
          putSingle("disable-list-demotion", CqlTranslatorOptions.Options.DisableListDemotion);
          putSingle("disable-list-promotion", CqlTranslatorOptions.Options.DisableListPromotion);
          putSingle(
              "enable-interval-demotion", CqlTranslatorOptions.Options.EnableIntervalDemotion);
          putSingle(
              "enable-interval-promotion", CqlTranslatorOptions.Options.EnableIntervalPromotion);
          putSingle(
              "disable-method-invocation", CqlTranslatorOptions.Options.DisableMethodInvocation);
          putSingle("require-from-keyword", CqlTranslatorOptions.Options.RequireFromKeyword);

          // Todo Do we even use these consolidated options ?
          put(
              "strict",
              Arrays.asList(
                  CqlTranslatorOptions.Options.DisableListTraversal,
                  CqlTranslatorOptions.Options.DisableListDemotion,
                  CqlTranslatorOptions.Options.DisableListPromotion,
                  CqlTranslatorOptions.Options.DisableMethodInvocation));
          put(
              "debug",
              Arrays.asList(
                  CqlTranslatorOptions.Options.EnableAnnotations,
                  CqlTranslatorOptions.Options.EnableLocators,
                  CqlTranslatorOptions.Options.EnableResultTypes));
          put(
              "mat",
              Arrays.asList(
                  CqlTranslatorOptions.Options.EnableAnnotations,
                  CqlTranslatorOptions.Options.EnableLocators,
                  CqlTranslatorOptions.Options.DisableListDemotion,
                  CqlTranslatorOptions.Options.DisableListPromotion,
                  CqlTranslatorOptions.Options.DisableMethodInvocation));
        }
      };

  private ModelManager modelManager;
  private LibraryManager libraryManager;

  static TranslationResource instance = null;

  private TranslationResource(boolean isFhir) {
    modelManager = new ModelManager();

    if (isFhir) {
      modelManager.resolveModel("FHIR", "4.0.1");
    }

    this.libraryManager = new LibraryManager(modelManager);
  }

  public static TranslationResource getInstance(boolean model) {
    instance = new TranslationResource(model);
    // returns the singleton object
    return instance;
  }

  /*sets the options and calls cql-elm-translator using MatLibrarySourceProvider,
  which helps the translator to fetch the CQL of the included libraries from HAPI FHIR Server*/
  public CqlTranslator buildTranslator(
      InputStream cqlStream, MultivaluedMap<String, String> params) {
    try {
      UcumService ucumService = null;
      LibraryBuilder.SignatureLevel signatureLevel = LibraryBuilder.SignatureLevel.None;
      List<CqlTranslatorOptions.Options> optionsList = new ArrayList<>();

      for (String key : params.keySet()) {
        if (PARAMS_TO_OPTIONS_MAP.containsKey(key) && Boolean.parseBoolean(params.getFirst(key))) {
          optionsList.addAll(PARAMS_TO_OPTIONS_MAP.get(key));
        } else if (key.equals("validate-units") && Boolean.parseBoolean(params.getFirst(key))) {
          ucumService = getUcumService();
        } else if (key.equals("signatures")) {
          signatureLevel = LibraryBuilder.SignatureLevel.valueOf(params.getFirst("signatures"));
        }
      }

      CqlTranslatorOptions.Options[] options =
          optionsList.toArray(new CqlTranslatorOptions.Options[0]);

      libraryManager.getLibrarySourceLoader().registerProvider(new MadieLibrarySourceProvider());

      return CqlTranslator.fromStream(
          cqlStream,
          modelManager,
          libraryManager,
          ucumService,
          CqlCompilerException.ErrorSeverity.Error,
          signatureLevel,
          options);

    } catch (Exception e) {
      throw new TranslationFailureException("Unable to read request", e);
    }
  }

  UcumService getUcumService() throws UcumException {
    return new UcumEssenceService(
        UcumEssenceService.class.getResourceAsStream("/ucum-essence.xml"));
  }
}
