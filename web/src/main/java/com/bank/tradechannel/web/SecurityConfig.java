package com.bank.tradechannel.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

/**
 * Deny by default, CSRF on, strict content security policy (no inline script or style, ADR 0006 and
 * 0010). Login (OIDC) and entitlements are added by task T7; until then only the public pages below
 * are reachable.
 */
@Configuration
public class SecurityConfig {

  static final String CONTENT_SECURITY_POLICY =
      "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; "
          + "object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'";

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/", "/css/**", "/actuator/health", "/error")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .csrf(Customizer.withDefaults())
        .headers(
            headers ->
                headers
                    .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))
                    .referrerPolicy(r -> r.policy(ReferrerPolicy.SAME_ORIGIN)))
        .logout(Customizer.withDefaults());
    return http.build();
  }
}
