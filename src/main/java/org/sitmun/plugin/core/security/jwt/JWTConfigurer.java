package org.sitmun.plugin.core.security.jwt;

import org.sitmun.plugin.core.security.TokenProvider;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

public class JWTConfigurer
  extends SecurityConfigurerAdapter<DefaultSecurityFilterChain, HttpSecurity> {

  public static final String AUTHORIZATION_HEADER = "Authorization";

  private final TokenProvider tokenProvider;

  public JWTConfigurer(TokenProvider tokenProvider) {
    this.tokenProvider = tokenProvider;
  }

  @Override
  public void configure(HttpSecurity http) {
    JWTFilter customFilter = new JWTFilter(tokenProvider);
    http.addFilterBefore(customFilter, UsernamePasswordAuthenticationFilter.class);
  }
}
