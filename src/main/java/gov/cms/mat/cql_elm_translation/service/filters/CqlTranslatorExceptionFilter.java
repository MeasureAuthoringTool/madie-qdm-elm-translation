package gov.cms.mat.cql_elm_translation.service.filters;

import gov.cms.mat.cql.elements.LibraryProperties;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.cqframework.cql.cql2elm.CqlCompilerException;
import org.hl7.elm.r1.VersionedIdentifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class CqlTranslatorExceptionFilter implements CqlLibraryFinder {
  @Getter private final String cqlData;
  private final boolean showWarnings;
  private final List<CqlCompilerException> cqlTranslatorExceptions;

  public CqlTranslatorExceptionFilter(
      String cqlData, boolean showWarnings, List<CqlCompilerException> cqlTranslatorExceptions) {
    this.cqlData = cqlData;
    this.showWarnings = showWarnings;
    this.cqlTranslatorExceptions = cqlTranslatorExceptions;
  }

  /*
   * if showWarnings is true, then removes warnings from the cqlTranslatorExceptions List.
   * Then the cqlTranslatorExceptions are filtered out if they are not pointed to the
   * parent library. ( reason: unknown )
   * */
  public List<CqlCompilerException> filter() {
    if (CollectionUtils.isEmpty(cqlTranslatorExceptions)) {
      log.debug("No CQL Errors found");
      return Collections.emptyList();
    }
    List<CqlCompilerException> filteredCqlTranslatorExceptions = filterOutWarnings();

    if (filteredCqlTranslatorExceptions.isEmpty()) {
      return Collections.emptyList();
    }
    List<CqlCompilerException> filteredList = filterByLibrary(filteredCqlTranslatorExceptions);
    List<CqlCompilerException> newList = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(filteredList)) {
      newList.addAll(filteredList);
    }

    filteredList = filterBySyntax(filteredCqlTranslatorExceptions);
    if (CollectionUtils.isNotEmpty(filteredList)) {
      newList.addAll(filteredList);
    }
    return newList;
  }

  private List<CqlCompilerException> filterOutWarnings() {
    if (showWarnings) {
      return cqlTranslatorExceptions;
    } else {
      return cqlTranslatorExceptions.stream().filter(this::isError).collect(Collectors.toList());
    }
  }

  private boolean isError(CqlCompilerException cqlTranslatorException) {
    return cqlTranslatorException != null
        && cqlTranslatorException.getSeverity() != null
        && cqlTranslatorException.getSeverity() == CqlCompilerException.ErrorSeverity.Error;
  }

  /*
   * Few exceptions from CqlTranslator doesn't have a library.version, so they are removed.
   * This filter also removes if there are any errors caught by translator in any of the
   * included libraries
   * */
  private List<CqlCompilerException> filterByLibrary(
      List<CqlCompilerException> filteredCqlTranslatorExceptions) {
    var libraryProperties = parseLibrary();

    return filteredCqlTranslatorExceptions.stream()
        .filter(e -> filterOutInclude(e, libraryProperties))
        .collect(Collectors.toList());
  }

  private boolean filterOutInclude(
      CqlCompilerException cqlTranslatorException, LibraryProperties libraryProperties) {
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
    log.debug(v.toString());
    String id = v.getId();
    String version = v.getVersion();

    return p.getName().equals(id) && p.getVersion().equals(version);
  }

  private List<CqlCompilerException> filterBySyntax(
      List<CqlCompilerException> filteredCqlTranslatorExceptions) {
    return filteredCqlTranslatorExceptions.stream()
        .filter(
            cqlCompilerException ->
                cqlCompilerException
                    .toString()
                    .contains("org.cqframework.cql.cql2elm.CqlSyntaxException"))
        .toList();
  }
}
