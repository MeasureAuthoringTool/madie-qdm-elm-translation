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
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.CqlCompilerException;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CqlConversionService {

  private static final String LOG_MESSAGE_TEMPLATE = "ErrorSeverity: %s, Message: %s";
  private final MadieFhirServices madieFhirServices;

  /* MadieLibrarySourceProvider places version and service in thread local */
  public void setUpLibrarySourceProvider(String cql, String accessToken) {
    MadieLibrarySourceProvider.setUsing(new CqlTextParser(cql).getUsing());
    MadieLibrarySourceProvider.setFhirServicesService(madieFhirServices);
    MadieLibrarySourceProvider.setAccessToken(accessToken);
  }

  public CqlConversionPayload processCqlDataWithErrors(RequestData requestData) {
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

    return CqlConversionPayload.builder().json(jsonWithErrors).xml(cqlTranslator.toXml()).build();
  }

  @SneakyThrows
  public CqlTranslator processCqlData(RequestData requestData) {
    CqlTextParser cqlTextParser = new CqlTextParser(requestData.getCqlData());
    UsingProperties usingProperties = cqlTextParser.getUsing();
    return TranslationResource.getInstance(
            usingProperties != null && "FHIR".equals(usingProperties.getLibraryType()))
        .buildTranslator(requestData.getCqlDataInputStream(), requestData.createMap());
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
