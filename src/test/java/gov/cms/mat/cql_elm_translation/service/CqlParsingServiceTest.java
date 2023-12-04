package gov.cms.mat.cql_elm_translation.service;

import gov.cms.mat.cql.CqlTextParser;
import gov.cms.mat.cql_elm_translation.ResourceFileUtil;
import gov.cms.mat.cql_elm_translation.cql_translator.MadieLibrarySourceProvider;
import gov.cms.mat.cql_elm_translation.cql_translator.TranslationResource;
import gov.cms.mat.cql_elm_translation.data.RequestData;
import gov.cms.mat.cql_elm_translation.utils.cql.parsing.model.CQLDefinition;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.LibraryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CqlParsingServiceTest implements ResourceFileUtil {
  @Mock private CqlConversionService cqlConversionService;
  @Mock private CqlLibraryService cqlLibraryService;
  @InjectMocks private CqlParsingService cqlParsingService;

  private String cql;
  private String helperCql;
  private RequestData requestData;

  @BeforeEach
  void setup() {
    helperCql = getData("/qicore_included_lib.cql");
    cql = getData("/qicore_define_callstack.cql");

    requestData =
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
  }

  @Test
  void testCallstack() {
    MadieLibrarySourceProvider.setUsing(new CqlTextParser(cql).getUsing());
    MadieLibrarySourceProvider.setCqlLibraryService(cqlLibraryService);
    doReturn(helperCql).when(cqlLibraryService).getLibraryCql(any(), any(), any());

    CqlTranslator translator = TranslationResource.getInstance(true).buildTranslator(requestData);
    verify(cqlLibraryService).getLibraryCql(any(), any(), any());
    doNothing().when(cqlConversionService).setUpLibrarySourceProvider(anyString(), anyString());
    when(cqlConversionService.processCqlData(any(RequestData.class))).thenReturn(translator);

    Map<String, Set<CQLDefinition>> definitionCallstacks =
        cqlParsingService.getDefinitionCallstacks(cql, "token");

    CQLDefinition measure1 =
        CQLDefinition.builder()
            .id("measure 1")
            .definitionName("measure 1")
            .definitionLogic("define \"measure 1\":\n" + "    true")
            .build();

    CQLDefinition measure2 =
        CQLDefinition.builder()
            .id("measure 2")
            .definitionName("measure 2")
            .definitionLogic("define \"measure 2\":\n" + "    \"measure 1\"")
            .build();

    CQLDefinition helperDefine =
        CQLDefinition.builder()
            .id("HelperLibrary-0.0.000|Helper|Inpatient Encounter")
            .definitionName("Inpatient Encounter")
            .definitionLogic(
                "define \"Inpatient Encounter\":\n"
                    + "  [Encounter: \"Encounter Inpatient\"] EncounterInpatient\n"
                    + "\t\twhere EncounterInpatient.status = 'finished'\n"
                    + "\t\tand EncounterInpatient.period ends during day of \"Measurement Period\"")
            .parentLibrary("HelperLibrary")
            .libraryDisplayName("Helper")
            .libraryVersion("0.0.000")
            .build();

    assertThat(definitionCallstacks.keySet().size(), is(3));
    assertThat(definitionCallstacks.get("measure 3"), containsInAnyOrder(measure1, measure2));
    assertThat(definitionCallstacks.get("measure 2"), contains(measure1));
    // assertThat(definitionCallstacks.get("measure 4"), contains(helperDefine));
  }
}
