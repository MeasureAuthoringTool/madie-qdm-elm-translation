package gov.cms.mat.cql_elm_translation.service;

import org.cqframework.cql.cql2elm.CqlTranslator;
import org.springframework.stereotype.Service;

@Service
public class VersionService {

  public String getTranslatorVersion() {
    Package translatorPackage = CqlTranslator.class.getPackage();
    if (translatorPackage != null && translatorPackage.getImplementationVersion() != null) {
      return translatorPackage.getImplementationVersion();
    } else {
      throw new IllegalStateException("Unable to determine translator version.");
    }
  }
}
