package gov.cms.mat.cql_elm_translation.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class SecurityFilterTest {

  @InjectMocks SecurityFilter securityFilter;

  private final MockHttpServletRequest servletRequest = new MockHttpServletRequest();
  private final MockHttpServletResponse servletResponse = new MockHttpServletResponse();
  private FilterChain filterChain;
  private FilterConfig filterConfig;

  @BeforeEach
  void init() {
    ReflectionTestUtils.setField(securityFilter, "matApiKey", "Enabled");
    securityFilter.init(filterConfig);
  }

  @AfterEach
  void destroy() {
    securityFilter.destroy();
  }

  // This can't be tested as servletRequest header value cannot be set to null
  // @Test
  void testDoFilterForNullHeader() throws ServletException, IOException {
    servletRequest.setRequestURI("ExampleUri");
    servletRequest.setMethod("Example-method");
    servletRequest.addHeader("MAT-API-KEY", null);
    securityFilter.doFilter(servletRequest, servletResponse, filterChain);
    assertEquals(403, servletResponse.getStatus());
  }

  @Test
  void testDoFilterIfDisabled() throws ServletException, IOException {
    servletRequest.setRequestURI("ExampleUri");
    servletRequest.setMethod("Example-method");
    servletRequest.addHeader("MAT-API-KEY", "Disabled");
    securityFilter.doFilter(servletRequest, servletResponse, filterChain);
    assertEquals(403, servletResponse.getStatus());
  }

  @Test
  void testDoFilterIfEnabled() throws ServletException, IOException {
    filterChain = mock(FilterChain.class);
    servletRequest.setRequestURI("ExampleUri");
    servletRequest.setMethod("Example-method");
    servletRequest.addHeader("MAT-API-KEY", "Enabled");
    securityFilter.doFilter(servletRequest, servletResponse, filterChain);
    assertEquals(200, servletResponse.getStatus());
  }

  @Test
  void testDoFilterIfRequestIsWhiteListed() throws ServletException, IOException {
    filterChain = mock(FilterChain.class);
    servletRequest.setRequestURI("/actuator/health");
    servletRequest.setMethod("GET");
    securityFilter.doFilter(servletRequest, servletResponse, filterChain);
    assertEquals(200, servletResponse.getStatus());
  }
}
