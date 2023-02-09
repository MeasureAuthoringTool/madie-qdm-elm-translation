package gov.cms.mat.cql_elm_translation.service;

import ca.uhn.fhir.context.FhirContext;
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
import org.apache.commons.lang3.StringUtils;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.CqlCompilerException;
import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.model.CompiledLibrary;
import org.cqframework.cql.elm.requirements.fhir.DataRequirementsProcessor;
import org.hl7.fhir.convertors.advisors.impl.BaseAdvisor_40_50;
import org.hl7.fhir.convertors.conv40_50.VersionConvertor_40_50;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r5.model.Extension;
import org.hl7.fhir.r5.model.Measure;
import org.hl7.fhir.r5.test.utils.TestingUtilities;
import org.hl7.fhir.r5.utils.LiquidEngine;
// import org.opencds.cqf.cql.engine.fhir.searchparam.SearchParameterResolver;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CqlConversionService {

  private static final String LOG_MESSAGE_TEMPLATE = "ErrorSeverity: %s, Message: %s";
  private static final String EXTENSION_URL_FHIR_QUERY_PATTERN =
      "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-fhirQueryPattern";
  private final MadieFhirServices madieFhirServices;
  //  private final SearchParameterResolver searchParameterResolver;

  /* MadieLibrarySourceProvider places version and service in thread local */
  public void setUpLibrarySourceProvider(String cql, String accessToken) {
    MadieLibrarySourceProvider.setUsing(new CqlTextParser(cql).getUsing());
    MadieLibrarySourceProvider.setFhirServicesService(madieFhirServices);
    MadieLibrarySourceProvider.setAccessToken(accessToken);
  }

  public CqlConversionPayload processCqlDataWithErrors(RequestData requestData) {
    // Gets the translator results
    CqlTranslator cqlTranslator = processCqlData(requestData);

    getModuleDefinitionLibraryR4(requestData);
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

  public org.hl7.fhir.r4.model.Library getModuleDefinitionLibraryR4(RequestData requestData) {
    String fhirMeasureJson = getData("/templates/liquid_measure.json");
    org.hl7.fhir.r4.model.Measure measure =
        createFhirResourceFromJson(fhirMeasureJson, org.hl7.fhir.r4.model.Measure.class);

    String libraryJson = getData("/templates/liquid_library.json");
    Library library = createFhirResourceFromJson(libraryJson, Library.class);

    Attachment attachment =
        library.getContent().stream()
            .filter(content -> "text/cql".equals(content.getContentType()))
            .findFirst()
            .orElse(null);

    String cql = new String(attachment.getData());

    TranslationResource translationResource = TranslationResource.getInstance(true);
    CqlTranslator cqlTranslator =
        translationResource.buildTranslator(
            new ByteArrayInputStream(cql.getBytes()), requestData.createMap());
    CompiledLibrary translatedLibrary = cqlTranslator.getTranslatedLibrary();
    LibraryManager libraryManager = translationResource.getLibraryManager();

    VersionConvertor_40_50 versionConvertor_40_50 =
        new VersionConvertor_40_50(new BaseAdvisor_40_50());
    org.hl7.fhir.r5.model.Measure r5Measure =
        (org.hl7.fhir.r5.model.Measure) versionConvertor_40_50.convertResource(measure);
    Set<String> expressionList = getExpressions(r5Measure);
    DataRequirementsProcessor dqReqTrans = new DataRequirementsProcessor();
    CqlTranslatorOptions options = CqlTranslatorOptions.defaultOptions();

    org.hl7.fhir.r5.model.Library effectiveDataRequirements =
        dqReqTrans.gatherDataRequirements(
            libraryManager, translatedLibrary, options, expressionList, true);
    //    org.hl7.fhir.r4.model.Library r4EffectiveDataRequirements =
    //        (org.hl7.fhir.r4.model.Library)
    //            versionConvertor_40_50.convertResource(effectiveDataRequirements);
    //    ModelResolver modelResolver = new R4FhirModelResolver();
    //
    //    r4EffectiveDataRequirements =
    //        addDataRequirementFhirQueries(
    //            r4EffectiveDataRequirements, searchParameterResolver, null, modelResolver, null);
    ;
    effectiveDataRequirements.setContent(createAttachment(library.getContent()));
    String template = getData("/templates/Measure.liquid");
    LiquidEngine engine = new LiquidEngine(TestingUtilities.context(), null);
    effectiveDataRequirements.setId("effective-data-requirements");
    r5Measure.addContained(effectiveDataRequirements);
    r5Measure.getExtension().add(createExtension());
    LiquidEngine.LiquidDocument doc = engine.parse(template, "test-script");

    String output = engine.evaluate(doc, r5Measure, null);

    return null;
  }

  private Extension createExtension() {
    return new Extension("http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-effectiveDataRequirements");
  }

  private List<org.hl7.fhir.r5.model.Attachment> createAttachment(List<Attachment> r4attachments) {
    List<org.hl7.fhir.r5.model.Attachment> attachments;
    attachments =
        r4attachments.stream()
            .map(
                attachment ->
                    new org.hl7.fhir.r5.model.Attachment()
                        .setContentType(attachment.getContentType())
                        .setData(attachment.getData()))
            .collect(Collectors.toList());
    return attachments;
  }

  private Set<String> getExpressions(Measure measureToUse) {
    Set<String> expressionSet = new HashSet<>();
    measureToUse
        .getSupplementalData()
        .forEach(
            supData -> {
              expressionSet.add(supData.getCriteria().getExpression());
            });
    measureToUse
        .getGroup()
        .forEach(
            groupMember -> {
              groupMember
                  .getPopulation()
                  .forEach(
                      population -> {
                        expressionSet.add(population.getCriteria().getExpression());
                      });
              groupMember
                  .getStratifier()
                  .forEach(
                      stratifier -> {
                        expressionSet.add(stratifier.getCriteria().getExpression());
                      });
            });
    return expressionSet;
  }

  public <T extends Resource> T createFhirResourceFromJson(String json, Class<T> clazz) {
    if (StringUtils.isEmpty(json)) {
      return null;
    }
    return FhirContext.forR4().newJsonParser().parseResource(clazz, json);
  }

  public String getData(String resource) {
    File inputXmlFile = new File(this.getClass().getResource(resource).getFile());

    try {
      return new String(Files.readAllBytes(inputXmlFile.toPath()));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  //  private org.hl7.fhir.r4.model.Library addDataRequirementFhirQueries(
  //      org.hl7.fhir.r4.model.Library library,
  //      SearchParameterResolver searchParameterResolver,
  //      TerminologyProvider terminologyProvider,
  //      ModelResolver modelResolver,
  //      IBaseConformance capStatement) {
  //    List<org.hl7.fhir.r4.model.DataRequirement> dataReqs = library.getDataRequirement();
  //
  //    try {
  //      BaseFhirQueryGenerator fhirQueryGenerator =
  //          new R4FhirQueryGenerator(searchParameterResolver, terminologyProvider, modelResolver);
  //
  //      Map<String, Object> contextValues = new HashMap<String, Object>();
  //      SubjectContext contextValue = getContextForSubject(library.getSubject());
  //      if (contextValue != null) {
  //        contextValues.put(contextValue.getContextType(), contextValue.getContextValue());
  //      }
  //
  //      for (org.hl7.fhir.r4.model.DataRequirement drq : dataReqs) {
  //        List<String> queries =
  //            fhirQueryGenerator.generateFhirQueries(drq, null, contextValues, null, null);
  //        for (String query : queries) {
  //          org.hl7.fhir.r4.model.Extension ext = new org.hl7.fhir.r4.model.Extension();
  //          ext.setUrl(EXTENSION_URL_FHIR_QUERY_PATTERN);
  //          ext.setValue(new org.hl7.fhir.r4.model.StringType(query));
  //          drq.getExtension().add(ext);
  //        }
  //      }
  //    } catch (FhirVersionMisMatchException e) {
  //      log.debug("Error attempting to generate FHIR queries: {}", e.getMessage());
  //    }
  //
  //    return library;
  //  }

  //  private SubjectContext getContextForSubject(Type subject) {
  //    String contextType = "Patient";
  //
  //    if (subject instanceof CodeableConcept) {
  //      for (Coding c : ((CodeableConcept) subject).getCoding()) {
  //        if ("http://hl7.org/fhir/resource-types".equals(c.getSystem())) {
  //          contextType = c.getCode();
  //        }
  //      }
  //    }
  //    return new SubjectContext(
  //        contextType, String.format("{{context.%sId}}", contextType.toLowerCase()));
  //  }

  //  class SubjectContext {
  //    public SubjectContext(String contextType, Object contextValue) {
  //      this.contextType = contextType;
  //      this.contextValue = contextValue;
  //    }
  //
  //    private String contextType;
  //
  //    public String getContextType() {
  //      return contextType;
  //    }
  //
  //    private Object contextValue;
  //
  //    public Object getContextValue() {
  //      return contextValue;
  //    }
  //  }
}
