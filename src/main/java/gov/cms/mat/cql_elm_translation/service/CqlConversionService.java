package gov.cms.mat.cql_elm_translation.service;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import gov.cms.mat.cql.dto.CqlConversionPayload;
import gov.cms.mat.cql_elm_translation.cql_translator.MadieLibrarySourceProvider;
import gov.cms.mat.cql_elm_translation.data.RequestData;
import gov.cms.mat.cql_elm_translation.dto.TranslatedLibrary;
import gov.cms.mat.cql_elm_translation.exceptions.InternalServerException;
import gov.cms.mat.cql_elm_translation.service.filters.AnnotationErrorFilter;
import gov.cms.mat.cql_elm_translation.service.filters.CqlTranslatorExceptionFilter;
import gov.cms.mat.cql_elm_translation.service.support.CqlExceptionErrorProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;
import org.apache.commons.lang3.StringUtils;
import org.cqframework.cql.cql2elm.CqlCompilerException;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.LibraryContentType;
import org.cqframework.cql.cql2elm.model.CompiledLibrary;
import org.cqframework.cql.elm.serializing.ElmLibraryWriterFactory;
import org.hl7.elm.r1.Library;
import org.hl7.elm.r1.VersionedIdentifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class CqlConversionService extends CqlTooling {

  private static final String LOG_MESSAGE_TEMPLATE = "ErrorSeverity: %s, Message: %s";
  private final CqlLibraryService cqlLibraryService;

  public CqlConversionPayload processCqlDataWithErrors(RequestData requestData) {
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

    List<CqlCompilerException> cqlTranslatorExceptions =
        processErrors(
            requestData.getCqlData(), requestData.isShowWarnings(), cqlTranslator.getExceptions());

    AnnotationErrorFilter annotationErrorFilter =
        new AnnotationErrorFilter(
            requestData.getCqlData(), requestData.isShowWarnings(), cqlTranslator.toJson());

    String processedJson = annotationErrorFilter.filter();

    String jsonWithErrors =
        new CqlExceptionErrorProcessor(cqlTranslatorExceptions, processedJson).process();
    if (noModelVersion) {
      // Does jsonWithErrors contain "Model and version don't exist"
      // Looking for both the original error in cqlTranslatorException
      //  and the 'Model and version' error in jsonWithErrors

      DocumentContext jsonContext = JsonPath.parse(jsonWithErrors);
      try {
        JSONArray errorFound =
            jsonContext.read(
                "$.errorExceptions[?(@.message==\"Model Type and version are required\")]");
        if (errorFound.size() == 0) {
          log.error(
              "cqlTranslatorException: There was a problem finding Model and version, "
                  + "but the error wasn't correctly reported by cqlTranslator?");
          log.warn("Error list {}", cqlTranslatorExceptions);
        }
      } catch (Exception e) {
        log.info("Model missing, but likely an empty CQL file");
      }
    }
    return CqlConversionPayload.builder().json(jsonWithErrors).xml(cqlTranslator.toXml()).build();
  }

  public List<TranslatedLibrary> getTranslatedLibrariesForCql(String cql, String accessToken)
      throws IOException {
    if (StringUtils.isBlank(cql)) {
      return Collections.emptyList();
    }
    CqlTranslator translator = runTranslator(cql, accessToken, cqlLibraryService);
    TranslatedLibrary translatedMeasureLib =
        buildTranslatedLibrary(translator.getTranslatedLibrary().getLibrary(), cql);
    Map<String, String> cqlMap =
        getIncludedLibrariesCql(new MadieLibrarySourceProvider(), translator);
    Map<VersionedIdentifier, CompiledLibrary> includedLibraries =
        translator.getTranslatedLibraries();
    // if no included libraries, return only measure library
    if (includedLibraries == null) {
      return List.of(translatedMeasureLib);
    }
    List<TranslatedLibrary> translatedIncludeLibs =
        includedLibraries.values().stream()
            .map(
                compiledLibrary -> {
                  Library library = compiledLibrary.getLibrary();
                  String name = library.getIdentifier().getId();
                  String version = library.getIdentifier().getVersion();
                  try {
                    return buildTranslatedLibrary(library, cqlMap.get(name + "-" + version));
                  } catch (IOException e) {
                    log.error(
                        "Error occurred while building the translated library details for: [{}]",
                        name);
                    throw new InternalServerException(e.getMessage());
                  }
                })
            .toList();
    List<TranslatedLibrary> libraries = new ArrayList<>(translatedIncludeLibs);
    libraries.add(translatedMeasureLib);
    return libraries;
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

  public static String convertToJson(Library library, LibraryContentType contentType)
      throws IOException {
    StringWriter writer = new StringWriter();
    ElmLibraryWriterFactory.getWriter(contentType.mimeType()).write(library, writer);
    return writer.getBuffer().toString();
  }

  private List<CqlCompilerException> processErrors(
      String cqlData, boolean showWarnings, List<CqlCompilerException> cqlTranslatorExceptions) {
    logErrors(cqlTranslatorExceptions);
    return new CqlTranslatorExceptionFilter(cqlData, showWarnings, cqlTranslatorExceptions)
        .filter();
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
