package gov.cms.mat.cql_elm_translation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CqlParsingService extends CqlTooling {
  private final CqlConversionService cqlConversionService;
}
