package gov.cms.mat.cql_elm_translation.config.logging;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@Slf4j
@ExtendWith(MockitoExtension.class)
class RequestResponseLoggingInterceptorTest {

  @Mock private ClientHttpRequestExecution mockClientHttpRequestExecution;

  @Test
  void testHttpRequestIntercept() throws IOException {
    RequestResponseLoggingInterceptor interceptor = new TestLoggingInterceptor();
    HttpRequest mockRequest = new MockClientHttpRequest();
    ClientHttpResponse mockResponse = new MockClientHttpResponse();
    doReturn(mockResponse)
        .when(mockClientHttpRequestExecution)
        .execute(any(HttpRequest.class), AdditionalMatchers.aryEq("".getBytes()));

    ClientHttpResponse response =
        interceptor.intercept(mockRequest, "".getBytes(), mockClientHttpRequestExecution);
    assertNotNull(response);
    assertTrue(response.getStatusCode().is2xxSuccessful());
  }

  static class TestLoggingInterceptor extends RequestResponseLoggingInterceptor {

    @Override
    protected void processHeaders(HttpRequest request) {
      // doesn't do anything ; but needs body to prevent codacy errors
      log.debug("just to have something in this method for codacy purposes");
    }
  }
}
