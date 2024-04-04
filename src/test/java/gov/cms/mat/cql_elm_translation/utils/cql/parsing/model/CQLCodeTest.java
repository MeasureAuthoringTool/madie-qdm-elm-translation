package gov.cms.mat.cql_elm_translation.utils.cql.parsing.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CQLCodeTest {

  @Test
  void test() {
    CQLCode code = new CQLCode();
    code.setId("ABCD");
    code.setIsCodeSystemVersionIncluded(true);
    assertEquals(
        "CQLCode{id='ABCD', codeName='null', codeSystemName='null', codeSystemVersion='null', codeSystemVersionUri='null', codeSystemOID='null', codeOID='null', displayName='null', codeIdentifier='null', isUsed=false, readOnly=false, suffix='null', isCodeSystemVersionIncluded=true, isValidatedWithVsac=VALID}",
        code.toString());
    assertEquals(code, code);
  }
}
