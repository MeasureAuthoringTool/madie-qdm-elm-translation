package gov.cms.mat.cql_elm_translation.exceptions;

import org.cqframework.cql.cql2elm.CqlCompilerException;
import org.cqframework.cql.elm.tracking.TrackBack;
import org.hl7.elm.r1.VersionedIdentifier;

public class DuplicateIncludeCqlCompilerException extends CqlCompilerException {
  private static final String MESSAGE = "Library %s is already in use in this library.";
  private static final String VMESSAGE = "Library %s Version %s is already in use in this library.";

  public DuplicateIncludeCqlCompilerException(
      final String library, VersionedIdentifier identifier, int lineNumber) {
    super(
        String.format(MESSAGE, library),
        CqlCompilerException.ErrorSeverity.Error,
        new TrackBack(identifier, lineNumber, 0, lineNumber, 0));
  }

  public DuplicateIncludeCqlCompilerException(
      final String library, VersionedIdentifier identifier, String version, int lineNumber) {
    super(
        String.format(VMESSAGE, library, version),
        CqlCompilerException.ErrorSeverity.Error,
        new TrackBack(identifier, lineNumber, 0, lineNumber, 0));
  }
}
