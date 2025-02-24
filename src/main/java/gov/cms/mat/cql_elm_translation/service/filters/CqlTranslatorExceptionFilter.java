package gov.cms.mat.cql_elm_translation.service.filters;

import gov.cms.mat.cql.elements.LibraryProperties;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.cqframework.cql.cql2elm.CqlCompilerException;
import org.hl7.elm.r1.VersionedIdentifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class CqlTranslatorExceptionFilter implements CqlLibraryFinder {
  @Getter private final String cqlData;
  private final CqlCompilerException.ErrorSeverity errorSeverity;
  private final List<CqlCompilerException> cqlTranslatorExceptions;

  @Getter private final List<CqlCompilerException> errorExceptions = new ArrayList<>();
  @Getter private final List<CqlCompilerException> externalErrors = new ArrayList<>();

  public CqlTranslatorExceptionFilter(
      String cqlData,
      CqlCompilerException.ErrorSeverity errorSeverity,
      List<CqlCompilerException> cqlTranslatorExceptions) {
    this.cqlData = cqlData;
    this.errorSeverity = errorSeverity;
    this.cqlTranslatorExceptions = cqlTranslatorExceptions;
  }

  /**
   * Generates CQL exceptions by: 1. Removing warnings if errorSeverity is 'Error'. 2. Partitioning
   * by library scope. 3. Filtering out specific errors. 4. Filtering out syntax Exceptions.
   */
  public void generateCqlExceptions() {
    if (CollectionUtils.isEmpty(cqlTranslatorExceptions)) {
      log.debug("No CQL Exceptions found");
      return;
    }
    List<CqlCompilerException> filteredOutWarnings = filterOutWarnings();

    if (CollectionUtils.isEmpty(filteredOutWarnings)) {
      return;
    }
    Map<Boolean, List<CqlCompilerException>> partitionedExceptions =
        filterByLibrary(filteredOutWarnings);
    List<CqlCompilerException> exceptionsForCurrentCql =
        partitionedExceptions.getOrDefault(true, Collections.emptyList());
    externalErrors.addAll(partitionedExceptions.getOrDefault(false, Collections.emptyList()));

    /*
     * MAT-7995: error: "No viable alternative at input 'define :'"
     * should be customized as: "Definition is missing a name."
     * This is done in cql-antlr-parse, so on the frontend we don't want a duplicate error message
     * therefore we are filtering it out here.
     */
    if (CollectionUtils.isNotEmpty(exceptionsForCurrentCql)) {
      List<CqlCompilerException> filteredOutSyntaxAndSpecificExceptions =
          exceptionsForCurrentCql.stream()
              .filter(
                  cqlCompilerException ->
                      !cqlCompilerException
                              .toString()
                              .contains("org.cqframework.cql.cql2elm.CqlSyntaxException")
                          && !StringUtils.containsIgnoreCase(
                              cqlCompilerException.getMessage(),
                              "no viable alternative at input 'define")
                          && !StringUtils.containsIgnoreCase(
                              cqlCompilerException.getMessage(),
                              "mismatched input 'display' expecting 'from'"))
              .toList();
      errorExceptions.addAll(filteredOutSyntaxAndSpecificExceptions);
    }
  }

  private List<CqlCompilerException> filterOutWarnings() {
    if (CqlCompilerException.ErrorSeverity.Error.equals(errorSeverity)) {
      return cqlTranslatorExceptions.stream().filter(this::isError).collect(Collectors.toList());
    }
    return cqlTranslatorExceptions;
  }

  private boolean isError(CqlCompilerException cqlTranslatorException) {
    return cqlTranslatorException != null
        && cqlTranslatorException.getSeverity() == CqlCompilerException.ErrorSeverity.Error;
  }

  /**
   * Filters out exceptions into 2 Collections Returns a Map with Key: True - The List of exceptions
   * which are part of current CQL Key: False - The List of exceptions which are not part of current
   * CQL ( Could be exceptions related to Included Libraries )
   */
  private Map<Boolean, List<CqlCompilerException>> filterByLibrary(
      List<CqlCompilerException> filteredOutWarnings) {
    var libraryProperties = parseLibrary();
    return filteredOutWarnings.stream()
        .collect(Collectors.partitioningBy(e -> filterOutInclude(e, libraryProperties)));
  }

  /**
   * Todo Potential bug (GitHub Issue #1492): Currently, if a CQL exception is a Warning, it lacks
   * Locator information. As a result, such warnings are filtered out, which is not the intention of
   * filterOutInclude. If Exception lacks Locator info, Should we consider it as a current CQL Error
   * or External Error
   */
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
    return p.getName().equals(v.getId()) && p.getVersion().equals(v.getVersion());
  }
}
