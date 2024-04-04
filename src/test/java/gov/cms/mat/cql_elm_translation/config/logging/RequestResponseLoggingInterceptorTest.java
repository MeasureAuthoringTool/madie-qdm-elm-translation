package gov.cms.mat.cql_elm_translation.config.logging;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalMatchers;
import org.mockito.Mock;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;

import lombok.extern.slf4j.Slf4j;

import org.mockito.junit.jupiter.MockitoExtension;

@Slf4j
@ExtendWith(MockitoExtension.class)
class RequestResponseLoggingInterceptorTest {

  @Mock private ClientHttpRequestExecution mockClientHttpRequestExecution;

  @Test
  void test() {
    RequestResponseLoggingInterceptor interceptor = new TestLoggingInterceptor();
    HttpRequest mockRequest = new MockClientHttpRequest();
    try {
      doReturn(null)
          .when(mockClientHttpRequestExecution)
          .execute(any(HttpRequest.class), AdditionalMatchers.aryEq("".getBytes()));
    } catch (IOException e) {
      fail();
    }
    try {
      ClientHttpResponse response =
          interceptor.intercept(mockRequest, "".getBytes(), mockClientHttpRequestExecution);
      assertNull(response);
    } catch (IOException e) {
      fail();
    }
  }

  class TestLoggingInterceptor extends RequestResponseLoggingInterceptor {

    @Override
    protected void processHeaders(HttpRequest request) {
      // doesn't do anything ; but needs body to prevent codacy errors
      log.debug("just to have something in this method for codacy purposes");
    }
  }
}
