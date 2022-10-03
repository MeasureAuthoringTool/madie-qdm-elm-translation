package gov.cms.mat.cql_elm_translation.config.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

  private static final String[] AUTH_WHITELIST = {
    "/v3/api-docs/**",
    "/swagger/**",
    "/swagger-ui/**",
    "/actuator/**",
    "/mat/translator/cqlToElm/**"
    // other public endpoints of your API may be appended to this array
  };

  private static final String[] CSRF_WHITELIST = {"/mat/translator/cqlToElm/**"};

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.cors()
        .and()
        .csrf()
        .ignoringAntMatchers(CSRF_WHITELIST)
        .and()
        .authorizeRequests()
        .antMatchers(HttpMethod.PUT, "/mat/translator/cqlToElm/**")
        .permitAll()
        .antMatchers(AUTH_WHITELIST)
        .permitAll()
        .and()
        .authorizeRequests()
        .anyRequest()
        .authenticated()
        .and()
        .sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        .and()
        .oauth2ResourceServer()
        .jwt()
        .and()
        .and()
        .headers()
        .xssProtection()
        .and()
        .contentSecurityPolicy("script-src 'self'");
  }
}
