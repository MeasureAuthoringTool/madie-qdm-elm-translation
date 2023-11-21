package gov.cms.mat.cql_elm_translation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CqlParsingService extends CqlTooling {
  private final CqlConversionService cqlConversionService;

  public List<String> parseUsedDefinitionsFromCql(String cql, List<String> populations,String accessToken) {
    Set<String> combinedSet = new HashSet<>(populations);
    combinedSet.addAll(parseCql(cql, accessToken, cqlConversionService).getUsedDefinitions());

    return parseCql(cql, accessToken, cqlConversionService).getUsedDefinitions();
  }
}
