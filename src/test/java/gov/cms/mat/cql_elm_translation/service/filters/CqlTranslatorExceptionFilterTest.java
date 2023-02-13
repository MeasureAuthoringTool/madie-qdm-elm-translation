package gov.cms.mat.cql_elm_translation.service.filters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.cqframework.cql.cql2elm.CqlCompilerException;
import org.cqframework.cql.cql2elm.CqlCompilerException.ErrorSeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import com.fasterxml.jackson.core.JsonProcessingException;

@SpringBootTest
public class CqlTranslatorExceptionFilterTest {

  // @Autowired private CqlTranslatorExceptionFilter filter;

  private String cqlData = "";
  private List<CqlCompilerException> cqlTranslatorExceptions = null;
  private CqlCompilerException syntaxException = null;
  private CqlCompilerException warning = null;

  @BeforeEach
  void setUp() throws JsonProcessingException {
    cqlData = StringUtils.EMPTY;
    File cqlFile = new File(this.getClass().getResource("/syntaxError.cql").getFile());

    try {
      cqlData = new String(Files.readAllBytes(cqlFile.toPath()));

    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    cqlTranslatorExceptions = new ArrayList<CqlCompilerException>();
    syntaxException =
        new CqlCompilerException(
            "org.cqframework.cql.cql2elm.CqlSyntaxException: extraneous input ')' expecting {<EOF>, 'using', 'include', 'public', 'private', 'parameter', 'codesystem', 'valueset', 'code', 'concept', 'define', 'context'}");
    warning = new CqlCompilerException("just warning", ErrorSeverity.Warning);
  }

  @Test
  public void testFilter_noExceptions() {
    CqlTranslatorExceptionFilter filter =
        new CqlTranslatorExceptionFilter(cqlData, false, cqlTranslatorExceptions);
    List<CqlCompilerException> filteredExceptions = filter.filter();
    assertTrue(filteredExceptions.size() == 0);
  }

  @Test
  public void testFilter_warnings() {
    cqlTranslatorExceptions.add(warning);
    CqlTranslatorExceptionFilter filter =
        new CqlTranslatorExceptionFilter(cqlData, true, cqlTranslatorExceptions);
    List<CqlCompilerException> filteredExceptions = filter.filter();
    assertTrue(filteredExceptions.size() == 0);
  }

  @Test
  public void testFilterSyntaxException() {
    cqlTranslatorExceptions.add(syntaxException);
    CqlTranslatorExceptionFilter filter =
        new CqlTranslatorExceptionFilter(cqlData, false, cqlTranslatorExceptions);

    List<CqlCompilerException> filteredExceptions = filter.filter();
    assertTrue(filteredExceptions.size() == 1);

    CqlCompilerException syntaxException = filteredExceptions.get(0);
    assertEquals(
        syntaxException.getClass(), org.cqframework.cql.cql2elm.CqlCompilerException.class);
  }
}
