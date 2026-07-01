package gov.cms.mat.cql_elm_translation.config.security;

import gov.cms.mat.cql_elm_translation.clients.UserRoleConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
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
  public SecurityFilterChain filterChain(HttpSecurity http, UserRoleConverter roleConverter)
      throws Exception {

    http.cors(Customizer.withDefaults())
        .csrf(csrf -> csrf.ignoringRequestMatchers(CSRF_WHITELIST))
        .authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers(HttpMethod.PUT, "/mat/translator/cqlToElm/**")
                    .permitAll()
                    .requestMatchers(AUTH_WHITELIST)
                    .permitAll()
                    .requestMatchers("/admin/**")
                    .hasRole("MADIE-ADMIN")
                    .anyRequest()
                    .authenticated())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .oauth2ResourceServer(
            oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(roleConverter)))
        .headers(
            headers ->
                headers
                    .xssProtection(Customizer.withDefaults())
                    .contentSecurityPolicy(csp -> csp.policyDirectives("script-src 'self'")));

    return http.build();
  }
}
