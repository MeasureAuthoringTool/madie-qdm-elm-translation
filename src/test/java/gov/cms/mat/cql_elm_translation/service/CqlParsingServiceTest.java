package gov.cms.mat.cql_elm_translation.service;

import gov.cms.mat.cql_elm_translation.ResourceFileUtil;
import gov.cms.madie.cql_elm_translator.dto.CqlBuilderLookup;
import gov.cms.madie.cql_elm_translator.utils.cql.parsing.model.CQLDefinition;
import gov.cms.mat.cql.CqlTextParser;
import gov.cms.madie.cql_elm_translator.utils.cql.cql_translator.MadieLibrarySourceProvider;
import gov.cms.madie.cql_elm_translator.service.CqlLibraryService;
import gov.cms.mat.cql_elm_translation.dto.CqlLookups;
import gov.cms.mat.cql_elm_translation.dto.ElementLookup;

import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CqlParsingServiceTest implements ResourceFileUtil {
  @Mock private CqlLibraryService cqlLibraryService;
  @InjectMocks private CqlParsingService cqlParsingService;

  private static String TOKEN = "John Doe";
  private String qiCoreHelperCql;
  private String qiCoreMeasureCql;
  private String qdmMeasureCql;

  @BeforeEach
  void setup() {
    qiCoreHelperCql = getData("/qicore_included_lib.cql");
    qiCoreMeasureCql = getData("/qicore_define_callstack.cql");
    qdmMeasureCql = getData("/qdm_lookup_test_lib.cql");
  }

  @Test
  void testCallstack() {
    MadieLibrarySourceProvider.setUsing(new CqlTextParser(qiCoreMeasureCql).getUsing());
    MadieLibrarySourceProvider.setCqlLibraryService(cqlLibraryService);
    doReturn(qiCoreHelperCql).when(cqlLibraryService).getLibraryCql(any(), any(), any());
    doNothing().when(cqlLibraryService).setUpLibrarySourceProvider(anyString(), anyString());
    Map<String, Set<CQLDefinition>> definitionCallstacks =
        cqlParsingService.getDefinitionCallstacks(qiCoreMeasureCql, "token");

    CQLDefinition define1 =
        CQLDefinition.builder()
            .id("define 1")
            .definitionName("define 1")
            .definitionLogic("define \"define 1\":\n" + "    true")
            .build();

    CQLDefinition define2 =
        CQLDefinition.builder()
            .id("define 2")
            .definitionName("define 2")
            .definitionLogic("define \"define 2\":\n" + "    \"define 1\"")
            .build();

    CQLDefinition function =
        CQLDefinition.builder()
            .id("func")
            .definitionName("func")
            .definitionLogic("define function \"func\":\n" + "    true")
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
    assertThat(definitionCallstacks.get("define 3"), containsInAnyOrder(define1, define2));
    assertThat(definitionCallstacks.get("define 2"), contains(define1));
    assertThat(definitionCallstacks.get("define 4"), containsInAnyOrder(helperDefine, function));
  }

  @Test
  void testGetCqlLookupsWhenMeasureCqlNull() {
    Set<String> measureExpressions = Set.of("Initial Population");
    CqlLookups cqlLookup = cqlParsingService.getCqlLookups(null, measureExpressions, TOKEN);
    assertNull(cqlLookup);
  }

  @Test
  void testGetCqlLookupsWhenMeasureExpressionsNull() {
    CqlLookups cqlLookup = cqlParsingService.getCqlLookups("Test CQL", null, TOKEN);
    assertNull(cqlLookup);
  }

  @Test
  void testGetCqlLookups() {
    Set<String> measureExpressions = Set.of("Initial Population");
    MadieLibrarySourceProvider.setUsing(new CqlTextParser(qdmMeasureCql).getUsing());
    MadieLibrarySourceProvider.setCqlLibraryService(cqlLibraryService);
    doNothing().when(cqlLibraryService).setUpLibrarySourceProvider(anyString(), anyString());
    CqlLookups cqlLookup =
        cqlParsingService.getCqlLookups(qdmMeasureCql, measureExpressions, TOKEN);
    assertThat(cqlLookup.getContext(), is(equalTo("Patient")));
    assertThat(cqlLookup.getLibrary(), is(equalTo("LookupTestLib")));
    assertThat(cqlLookup.getUsingModel(), is(equalTo("QDM")));
    assertThat(cqlLookup.getUsingModelVersion(), is(equalTo("5.6")));
    assertThat(cqlLookup.getParameters().size(), is(equalTo(1)));
    assertThat(cqlLookup.getValueSets().size(), is(equalTo(1)));
    assertThat(cqlLookup.getCodes().size(), is(equalTo(2)));
    assertThat(cqlLookup.getCodeSystems().size(), is(equalTo(1)));
    assertThat(cqlLookup.getDefinitions().size(), is(equalTo(4)));
    assertThat(cqlLookup.getIncludeLibraries().size(), is(equalTo(0)));
    assertThat(cqlLookup.getElementLookups().size(), is(equalTo(3)));
    List<String> definitions =
        cqlLookup.getDefinitions().stream().map(CQLDefinition::getName).toList();
    assertThat(
        definitions,
        containsInAnyOrder(
            "test when then case",
            "More Than One Order",
            "Initial Population",
            "MedicationOrderInjection"));
    List<String> oids = cqlLookup.getElementLookups().stream().map(ElementLookup::getOid).toList();
    assertThat(oids, containsInAnyOrder("204504", "197604", "2.16.840.1.113883.3.464.1003.1065"));
  }

  @Test
  void testGetCqlBuilderLookups() {
    MadieLibrarySourceProvider.setUsing(new CqlTextParser(qiCoreMeasureCql).getUsing());
    MadieLibrarySourceProvider.setCqlLibraryService(cqlLibraryService);
    doReturn(qiCoreHelperCql).when(cqlLibraryService).getLibraryCql(any(), any(), any());
    doNothing().when(cqlLibraryService).setUpLibrarySourceProvider(anyString(), anyString());
    CqlBuilderLookup lookup = cqlParsingService.getCqlBuilderLookups(qiCoreMeasureCql, "token");
    assertThat(lookup.getParameters().size(), is(2));
    assertThat(lookup.getDefinitions().size(), is(5));
    assertThat(lookup.getFunctions().size(), is(1));
    assertThat(lookup.getFluentFunctions().size(), is(1));
  }

  @Test
  void testGetCqlBuilderLookupsForEmptyCql() {
    CqlBuilderLookup lookup = cqlParsingService.getCqlBuilderLookups(null, "token");
    assertThat(lookup, is(nullValue()));
  }
}
