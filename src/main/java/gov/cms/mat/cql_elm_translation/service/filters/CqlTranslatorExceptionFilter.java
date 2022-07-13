package gov.cms.mat.cql_elm_translation.service.filters;

import gov.cms.mat.cql.elements.LibraryProperties;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.cqframework.cql.cql2elm.CqlTranslatorException;
import org.hl7.elm.r1.VersionedIdentifier;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class CqlTranslatorExceptionFilter implements CqlLibraryFinder {
  @Getter private final String cqlData;
  private final boolean showWarnings;
  private final List<CqlTranslatorException> cqlTranslatorExceptions;

  public CqlTranslatorExceptionFilter(
      String cqlData, boolean showWarnings, List<CqlTranslatorException> cqlTranslatorExceptions) {
    this.cqlData = cqlData;
    this.showWarnings = showWarnings;
    this.cqlTranslatorExceptions = cqlTranslatorExceptions;
  }

  /*
   * if showWarnings is true, then removes warnings from the cqlTranslatorExceptions List.
   * Then the cqlTranslatorExceptions are filtered out if they are not pointed to the
   * parent library. ( reason: unknown )
   * */
  public List<CqlTranslatorException> filter() {
    if (CollectionUtils.isEmpty(cqlTranslatorExceptions)) {
      log.debug("No CQL Errors found");
      return Collections.emptyList();
    } else {
      List<CqlTranslatorException> filteredCqlTranslatorExceptions = filterOutWarnings();

      if (filteredCqlTranslatorExceptions.isEmpty()) {
        return Collections.emptyList();
      } else {
        return filterByLibrary(filteredCqlTranslatorExceptions);
      }
    }
  }

  private List<CqlTranslatorException> filterOutWarnings() {
    if (showWarnings) {
      return cqlTranslatorExceptions;
    } else {
      return cqlTranslatorExceptions.stream().filter(this::isError).collect(Collectors.toList());
    }
  }

  private boolean isError(CqlTranslatorException cqlTranslatorException) {
    return cqlTranslatorException != null
        && cqlTranslatorException.getSeverity() != null
        && cqlTranslatorException.getSeverity() == CqlTranslatorException.ErrorSeverity.Error;
  }

  /*
   * Few exceptions from CqlTranslator doesn't have a library.version, so they are removed.
   * This filter also removes if there are any errors caught by translator in any of the
   * included libraries
   * */
  private List<CqlTranslatorException> filterByLibrary(
      List<CqlTranslatorException> filteredCqlTranslatorExceptions) {
    var libraryProperties = parseLibrary();

    return filteredCqlTranslatorExceptions.stream()
        .filter(e -> filterOutInclude(e, libraryProperties))
        .collect(Collectors.toList());
  }

  private boolean filterOutInclude(
      CqlTranslatorException cqlTranslatorException, LibraryProperties libraryProperties) {
    if (cqlTranslatorException.getLocator() == null
        || cqlTranslatorException.getLocator().getLibrary() == null) {
      return false;
    } else {
      VersionedIdentifier versionedIdentifier = cqlTranslatorException.getLocator().getLibrary();
      log.debug("versionedIdentifier : {}", versionedIdentifier);
      return isPointingToSameLibrary(libraryProperties, versionedIdentifier);
    }
  }

  private boolean isPointingToSameLibrary(LibraryProperties p, VersionedIdentifier v) {
    return p.getName().equals(v.getId()) && p.getVersion().equals(v.getVersion());
  }
}
