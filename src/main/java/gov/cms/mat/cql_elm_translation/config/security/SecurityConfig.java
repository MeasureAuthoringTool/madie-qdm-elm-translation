package gov.cms.mat.cql_elm_translation.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  private static final String[] AUTH_WHITELIST = {
    "/v3/api-docs/**",
    "/swagger/**",
    "/swagger-ui/**",
    "/actuator/**",
    "/mat/translator/cqlToElm/**"
    // other public endpoints of your API may be appended to this array
  };

  private static final String[] CSRF_WHITELIST = {"/mat/translator/cqlToElm/**"};

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

    http.cors()
        .and()
        .csrf()
        .ignoringRequestMatchers(CSRF_WHITELIST)
        .and()
        .authorizeHttpRequests()
        .requestMatchers(HttpMethod.PUT, "/mat/translator/cqlToElm/**")
        .permitAll()
        .requestMatchers(AUTH_WHITELIST)
        .permitAll()
        .and()
        .authorizeHttpRequests()
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

    return http.build();
  }
}
