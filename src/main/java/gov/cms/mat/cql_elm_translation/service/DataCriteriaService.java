package gov.cms.mat.cql_elm_translation.service;

import gov.cms.mat.cql_elm_translation.utils.cql.CQLFilter;
import gov.cms.mat.cql_elm_translation.utils.cql.parsing.CqlParserListener;
import gov.cms.mat.cql_elm_translation.utils.cql.parsing.model.CQLModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataCriteriaService {

  public Map<String, Set<String>> parseDataCriteriaFromCql(String cql, Map<String,String> includedLibraries, CqlTranslator cqlTranslator) {

    CQLFilter cqlFilter = new CQLFilter(
        cql,
        includedLibraries,
        getExpressionsFromCql(cql),
        cqlTranslator
    );

    try {
      cqlFilter.filter();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return buildDataCriteria(cqlFilter.getExpressionNameToValuesetDataTypeMap(),
        cqlFilter.getExpressionNameToCodeDataTypeMap());
  }

  private Map<String, Set<String>> buildDataCriteria(Map<String, Map<String, Set<String>>> valueSetBackedDataCriteriaByExpression,
                                                            Map<String, Map<String, Set<String>>> codeBackedDataCriteriaByExpression) {
    Map<String, Set<String>> dataCriteria = new HashMap<>();
    condense(valueSetBackedDataCriteriaByExpression, dataCriteria);
    condense(codeBackedDataCriteriaByExpression, dataCriteria);
    return dataCriteria;
  }

   private void condense(Map<String, Map<String, Set<String>>> expressionMap,
                         Map<String, Set<String>> target) {
     // key -> Definition/Function (aka Expressions)
     // Value -> Map
     //    Key -> Expression
     //    Value -> Map
     //      Key -> ValueSet/Code
     //      Value -> Set
     //        Key -> ValueSet/Code
     //        Value -> QDM Data Type
    expressionMap.values().forEach(ex -> {
      for (String terminology : ex.keySet()) {
        if (target.get(terminology) != null) {
          target.get(terminology).addAll(ex.get(terminology));
        } else {
          target.put(terminology, ex.get(terminology));
        }
      }
    });
   }

  private List<String> getExpressionsFromCql(String cql) {
    CQLModel cqlModel;
    try {
      CqlParserListener listener = new CqlParserListener(cql);
      cqlModel = listener.getCQLModel();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return cqlModel.getExpressionListFromCqlModel();
  }
}
