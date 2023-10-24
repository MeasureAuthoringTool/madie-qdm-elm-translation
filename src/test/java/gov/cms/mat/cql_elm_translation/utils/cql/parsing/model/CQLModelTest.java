package gov.cms.mat.cql_elm_translation.utils.cql.parsing.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class CQLModelTest {

  @Test
  void testCqlModel() {
    CQLIncludeLibrary cqlIncludeLibrary = new CQLIncludeLibrary();
    cqlIncludeLibrary.setCqlLibraryName("This");
    cqlIncludeLibrary.setVersion("One");

    CQLModel libraryModel = new CQLModel();
    List<CQLCode> codeList = new ArrayList<>();
    CQLCode code = new CQLCode();
    code.setDisplayName("Time");
    codeList.add(code);

    libraryModel.setCodeList(codeList);
    Map<CQLIncludeLibrary, CQLModel> includedLibraries = new HashMap<>();
    includedLibraries.put(cqlIncludeLibrary, libraryModel);

    CQLModel model = new CQLModel();

    model.setIncludedLibraries(includedLibraries);

    assertEquals(0, model.getIncludedDef().size());
    assertEquals(0, model.getCQLIdentifierDefinitions().size());
    assertEquals(0, model.getIncludedFunc().size());
    assertEquals(0, model.getCQLIdentifierFunctions().size());
    assertEquals(0, model.getIncludedValueSet().size());
    assertEquals(0, model.getCQLIdentifierValueSet().size());
    assertEquals(0, model.getIncludedParam().size());
    assertEquals(0, model.getCQLIdentifierParam().size());
    assertEquals(1, model.getIncludedCode().size());
    assertEquals(1, model.getCQLIdentifierCode().size());
    assertEquals("Time", model.getCodeByName("This-One|AndOnly|Time").getDisplayName());
  }
}
