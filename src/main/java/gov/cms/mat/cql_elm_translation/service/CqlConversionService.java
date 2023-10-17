package gov.cms.mat.cql_elm_translation.service;

import gov.cms.mat.cql.CqlTextParser;
import gov.cms.mat.cql.dto.CqlConversionPayload;
import gov.cms.mat.cql.elements.UsingProperties;
import gov.cms.mat.cql_elm_translation.cql_translator.MadieLibrarySourceProvider;
import gov.cms.mat.cql_elm_translation.cql_translator.TranslationResource;
import gov.cms.mat.cql_elm_translation.data.RequestData;
import gov.cms.mat.cql_elm_translation.service.filters.AnnotationErrorFilter;
import gov.cms.mat.cql_elm_translation.service.filters.CqlTranslatorExceptionFilter;
import gov.cms.mat.cql_elm_translation.service.support.CqlExceptionErrorProcessor;
import org.apache.commons.lang3.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.CqlCompilerException;
import org.cqframework.cql.cql2elm.LibraryBuilder;
import org.cqframework.cql.cql2elm.LibraryContentType;
import org.cqframework.cql.cql2elm.model.CompiledLibrary;
import org.cqframework.cql.elm.serializing.ElmLibraryWriterFactory;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.elm.r1.Library;
import org.springframework.stereotype.Service;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class CqlConversionService {

  private static final String LOG_MESSAGE_TEMPLATE = "ErrorSeverity: %s, Message: %s";
  private final CqlLibraryService cqlLibraryService;

  /* MadieLibrarySourceProvider places version and service in thread local */
  public void setUpLibrarySourceProvider(String cql, String accessToken) {
    MadieLibrarySourceProvider.setUsing(new CqlTextParser(cql).getUsing());
    MadieLibrarySourceProvider.setCqlLibraryService(cqlLibraryService);
    MadieLibrarySourceProvider.setAccessToken(accessToken);
  }

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

  @SneakyThrows
  public CqlTranslator processCqlData(RequestData requestData) {
    CqlTextParser cqlTextParser = new CqlTextParser(requestData.getCqlData());
    UsingProperties usingProperties = cqlTextParser.getUsing();
    return TranslationResource.getInstance(
            usingProperties != null && "FHIR".equals(usingProperties.getLibraryType()))
        .buildTranslator(requestData);
  }

  public List<String> getElmForCql(String cql, String accessToken) throws IOException {
    if (StringUtils.isBlank(cql)) {
      return Collections.emptyList();
    }

    RequestData requestData =
        RequestData.builder()
            .cqlData(cql)
            .showWarnings(false)
            .signatures(LibraryBuilder.SignatureLevel.All)
            .annotations(true)
            .locators(true)
            .disableListDemotion(true)
            .disableListPromotion(true)
            .disableMethodInvocation(false)
            .validateUnits(true)
            .resultTypes(true)
            .build();

    setUpLibrarySourceProvider(cql, accessToken);
    CqlTranslator translator = processCqlData(requestData);
    String library = translator.toJson();
    var result = new HashMap<VersionedIdentifier, String>();
    for (Map.Entry<VersionedIdentifier, CompiledLibrary> entry :
        translator.getTranslatedLibraries().entrySet()) {
      result.put(entry.getKey(), convertToXml(entry.getValue().getLibrary()));
    }

    List<String> libraries = new ArrayList<>(result.values());
    libraries.add(library);
    return libraries;
  }

  public static String convertToXml(Library library) throws IOException {
    StringWriter writer = new StringWriter();
    ElmLibraryWriterFactory.getWriter(LibraryContentType.XML.mimeType()).write(library, writer);
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
