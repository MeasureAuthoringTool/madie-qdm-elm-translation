package gov.cms.mat.cql_elm_translation.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.cqframework.cql.cql2elm.CqlCompilerException;
import org.cqframework.cql.cql2elm.CqlSemanticException;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.LibraryContentType;
import org.cqframework.cql.cql2elm.model.CompiledLibrary;
import org.cqframework.cql.cql2elm.tracking.TrackBack;
import org.cqframework.cql.elm.visiting.BaseElmLibraryVisitor;
import org.hl7.elm.r1.CodeSystemRef;
import org.hl7.elm.r1.Element;
import org.hl7.elm.r1.Library;
import org.hl7.elm.r1.Retrieve;
import org.hl7.elm.r1.VersionedIdentifier;
import org.springframework.stereotype.Service;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import gov.cms.madie.cql_elm_translator.exceptions.InternalServerException;
import gov.cms.madie.cql_elm_translator.service.CqlLibraryService;
import gov.cms.madie.cql_elm_translator.utils.MadieCqlValidator;
import gov.cms.madie.cql_elm_translator.utils.cql.cql_translator.MadieLibrarySourceProvider;
import gov.cms.madie.cql_elm_translator.utils.cql.data.RequestData;
import gov.cms.madie.models.dto.TranslatedLibrary;
import gov.cms.mat.cql.dto.CqlConversionPayload;
import gov.cms.mat.cql_elm_translation.service.filters.CqlTranslatorExceptionFilter;
import gov.cms.mat.cql_elm_translation.service.support.CqlExceptionErrorProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;

@Service
@Slf4j
@RequiredArgsConstructor
public class CqlConversionService extends CqlTooling {

  private static final String LOG_MESSAGE_TEMPLATE = "ErrorSeverity: %s, Message: %s";
  private final CqlLibraryService cqlLibraryService;

  public CqlConversionPayload translateCqlToElm(RequestData requestData) {
    // verify the presence of ^using .*version '[0-9]\.[0-9]\.[0-9]'$ on the cql
    Pattern pattern = Pattern.compile("using .*version '[0-9]\\.[0-9](\\.[0-9])?'");
    Matcher matcher = pattern.matcher(requestData.getCqlData());
    boolean noModelVersion = false;
    if (!matcher.find()) {
      log.debug("cqlTranslatorException: Model and version don't exist");
      log.debug("cqlTranslatorException: \n{}", requestData.getCqlData());
      noModelVersion = true;
    }
    // Gets the translator results
    CqlTranslator cqlTranslator = processCqlData(requestData);

    processForLibraryRulesExceptions(cqlTranslator, requestData.getCqlData());

    CqlTranslatorExceptionFilter cqlTranslatorExceptionFilter =
        new CqlTranslatorExceptionFilter(
            requestData.getCqlData(),
            requestData.getErrorSeverity(),
            cqlTranslator.getExceptions());
    cqlTranslatorExceptionFilter.generateCqlExceptions();
    String jsonWithErrors =
        new CqlExceptionErrorProcessor(
                cqlTranslatorExceptionFilter.getErrorExceptions(),
                cqlTranslatorExceptionFilter.getExternalErrors(),
                cqlTranslator.toJson())
            .addExceptionsToJson();
    if (noModelVersion) {
      // Does jsonWithErrors contain "Model and version don't exist"
      // Looking for both the original error in cqlTranslatorException
      //  and the 'Model and version' error in jsonWithErrors
      DocumentContext jsonContext = JsonPath.parse(jsonWithErrors);
      try {
        JSONArray errorFound =
            jsonContext.read(
                "$.errorExceptions[?(@.message==\"Model Type and version are required\")]");
        if (errorFound.isEmpty()) {
          log.error(
              "cqlTranslatorException: There was a problem finding Model and version, "
                  + "but the error wasn't correctly reported by cqlTranslator?");
          log.warn("Error list {}", cqlTranslatorExceptionFilter.getErrorExceptions());
        }
      } catch (Exception e) {
        log.info("Model missing, but likely an empty CQL file");
      }
    }
    return CqlConversionPayload.builder().json(jsonWithErrors).xml(cqlTranslator.toXml()).build();
  }

  public void processForLibraryRulesExceptions(CqlTranslator cqlTranslator, String cql) {
    VersionedIdentifier identifier =
        cqlTranslator.getTranslatedLibrary().getLibrary().getIdentifier();
    if (StringUtils.isNotBlank(cql)) {
      if (identifier != null) {
        Library.Includes includes = cqlTranslator.getTranslatedLibrary().getLibrary().getIncludes();

        new MadieCqlValidator().checkNoDuplicateIncludes(cqlTranslator, includes);
      }
    }
    validateRetrieve(cqlTranslator);
  }

  public List<TranslatedLibrary> getTranslatedLibrariesForCql(
      String cql, String accessToken, CqlCompilerException.ErrorSeverity errorSeverity)
      throws IOException {
    if (StringUtils.isBlank(cql)) {
      return Collections.emptyList();
    }
    TranslationArtifacts translationArtifacts =
        buildTranslationArtifacts(cql, accessToken, cqlLibraryService, errorSeverity);
    CqlTranslator translator = translationArtifacts.getTranslator();
    TranslatedLibrary translatedMeasureLib =
        buildTranslatedLibrary(translator.getTranslatedLibrary().getLibrary(), cql);
    Map<String, CompiledLibrary> includedLibraries = translationArtifacts.getTranslatedLibraries();
    List<TranslatedLibrary> libraries = new ArrayList<>();
    libraries.add(translatedMeasureLib);
    // if no included libraries, return only measure library
    if (MapUtils.isEmpty(includedLibraries)) {
      return libraries;
    }
    // get the cql for included libraries
    Map<String, String> cqlMap =
        getIncludedLibrariesCql(new MadieLibrarySourceProvider(), includedLibraries);

    // create TranslatedLibrary for each included library
    List<TranslatedLibrary> translatedIncludeLibs =
        includedLibraries.values().stream()
            .filter(
                compiledLibrary ->
                    !isMainLibrary(
                        compiledLibrary,
                        translatedMeasureLib.getName(),
                        translatedMeasureLib.getVersion()))
            .map(compiledLibrary -> buildTranslatedLibrary(compiledLibrary, cqlMap))
            .toList();
    libraries.addAll(translatedIncludeLibs);
    log.info("getTranslatedLibrariesForCql: libraries size = " + libraries.size());
    return libraries;
  }

  public TranslatedLibrary buildTranslatedLibrary(
      CompiledLibrary compiledLibrary, Map<String, String> cqlMap) {
    if (compiledLibrary == null) {
      return null;
    }
    Library library = compiledLibrary.getLibrary();
    String name = library.getIdentifier().getId();
    String version = library.getIdentifier().getVersion();
    try {
      return buildTranslatedLibrary(library, cqlMap.get(name + "-" + version));
    } catch (IOException e) {
      log.error("Error occurred while building the translated library artifacts: ", e);
      throw new InternalServerException(
          "An error occurred while building translated artifacts for library " + name);
    }
  }

  /**
   * This method validate retrieves for presence of value set or code filter. If one doesn't have
   * it, create a CqlCompilerException and add it to the compiler exceptions
   *
   * @param cqlTranslator - an instance of CqlTranslator
   */
  public void validateRetrieve(CqlTranslator cqlTranslator) {
    List<Retrieve> retrieves = getRetrieves(cqlTranslator.toELM());
    if (!CollectionUtils.isEmpty(retrieves)) {
      List<CqlCompilerException> exceptions =
          retrieves.stream()
              .filter(
                  retrieve ->
                      !isPatientRetrieve(retrieve)
                          && StringUtils.isNotBlank(retrieve.getLocator())
                          && (retrieve.getCodes() == null
                              || retrieve.getCodes() instanceof CodeSystemRef)
                          && CollectionUtils.isEmpty(retrieve.getCodeFilter()))
              .map(
                  retrieve -> {
                    TrackBack trackable =
                        buildTrackBack(retrieve, cqlTranslator.toELM().getIdentifier());
                    return (CqlCompilerException)
                        new CqlSemanticException(
                            "Retrieves must contain a code or value set filter",
                            trackable,
                            CqlCompilerException.ErrorSeverity.Error,
                            null);
                  })
              .toList();
      if (!CollectionUtils.isEmpty(exceptions)) {
        cqlTranslator.getExceptions().addAll(exceptions);
      }
    }
  }

  private TranslatedLibrary buildTranslatedLibrary(Library library, String cql) throws IOException {
    VersionedIdentifier identifier = library.getIdentifier();
    String elmJson = convertToJson(library, LibraryContentType.JSON);
    String elmXml = convertToJson(library, LibraryContentType.XML);
    String name = identifier.getId();
    String version = identifier.getVersion();
    return TranslatedLibrary.builder()
        .name(name)
        .version(version)
        .cql(cql)
        .elmJson(elmJson)
        .elmXml(elmXml)
        .build();
  }

  public String convertToJson(Library library, LibraryContentType contentType) throws IOException {
    if (contentType == LibraryContentType.XML) {
      return CqlTranslator.convertToXml(library);
    }
    return CqlTranslator.convertToJson(library);
  }

  private boolean isMainLibrary(CompiledLibrary compiledLibrary, String name, String version) {
    VersionedIdentifier identifier = compiledLibrary.getIdentifier();
    return identifier != null
        && StringUtils.equals(identifier.getId(), name)
        && StringUtils.equals(identifier.getVersion(), version);
  }

  private List<Retrieve> getRetrieves(Library rootLibrary) {
    if (rootLibrary == null) {
      return Collections.emptyList();
    }

    RetrieveCollector collector = new RetrieveCollector();
    collector.visitLibrary(rootLibrary, null);
    return collector.retrieves;
  }

  private boolean isPatientRetrieve(Retrieve retrieve) {
    return retrieve != null
        && retrieve.getDataType() != null
        && "Patient".equals(retrieve.getDataType().getLocalPart());
  }

  private TrackBack buildTrackBack(Retrieve retrieve, VersionedIdentifier libraryIdentifier) {
    int[] position = parseLocator(retrieve.getLocator());
    return new TrackBack(libraryIdentifier, position[0], position[1], position[2], position[3]);
  }

  private int[] parseLocator(String locator) {
    if (StringUtils.isBlank(locator)) {
      return new int[] {0, 0, 0, 0};
    }

    Matcher rangeMatcher = Pattern.compile("(\\d+):(\\d+)-(\\d+):(\\d+)").matcher(locator);
    if (rangeMatcher.find()) {
      return new int[] {
        Integer.parseInt(rangeMatcher.group(1)),
        Integer.parseInt(rangeMatcher.group(2)),
        Integer.parseInt(rangeMatcher.group(3)),
        Integer.parseInt(rangeMatcher.group(4))
      };
    }

    Matcher startMatcher = Pattern.compile("(\\d+):(\\d+)").matcher(locator);
    if (startMatcher.find()) {
      int startLine = Integer.parseInt(startMatcher.group(1));
      int startChar = Integer.parseInt(startMatcher.group(2));
      return new int[] {startLine, startChar, startLine, startChar};
    }

    return new int[] {0, 0, 0, 0};
  }

  private static class RetrieveCollector extends BaseElmLibraryVisitor<Void, Void> {
    private final List<Retrieve> retrieves = new ArrayList<>();

    @Override
    protected Void defaultResult(Element elm, Void context) {
      return null;
    }

    @Override
    public Void visitRetrieve(Retrieve elm, Void context) {
      retrieves.add(elm);
      return super.visitRetrieve(elm, context);
    }
  }

  private void logErrors(List<CqlCompilerException> exceptions) {
    exceptions.forEach(e -> log.debug(formatMessage(e)));
  }

  private String formatMessage(CqlCompilerException e) {
    return String.format(
        LOG_MESSAGE_TEMPLATE,
        e.getSeverity() != null ? e.getSeverity().name() : null,
        e.getMessage());
  }
}
