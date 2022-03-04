package org.sitmun.security;

import org.sitmun.security.jwt.AuthEntryPointJwt;
import org.sitmun.security.jwt.AuthTokenFilter;
import org.sitmun.security.middleware.MiddlewareKeyFilter;
import org.sitmun.security.services.UserDetailsServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

  private static final String[] AUTH_WHITELIST = {
    "/v3/api-docs/**",
    "/v3/api-docs.*",
    "/swagger-ui/**",
    "/swagger-ui.*",
    "/dist/**",
    "/workspace.html"
  };

  private final AuthEntryPointJwt unauthorizedHandler;
  private final UserDetailsServiceImpl userDetailsService;

  public WebSecurityConfig(UserDetailsServiceImpl userDetailsService, AuthEntryPointJwt unauthorizedHandler) {
    this.userDetailsService = userDetailsService;
    this.unauthorizedHandler = unauthorizedHandler;
  }

  @Bean
  public AuthTokenFilter authenticationJwtTokenFilter() {
    return new AuthTokenFilter();
  }

  @Override
  public void configure(AuthenticationManagerBuilder authenticationManagerBuilder) throws Exception {
    authenticationManagerBuilder.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
  }

  @Bean
  @Override
  public AuthenticationManager authenticationManagerBean() throws Exception {
    return super.authenticationManagerBean();
  }

  @Bean
  public AnonymousAuthenticationFilter anonymousAuthenticationFilter() {
    return new AnonymousAuthenticationFilter(
      "anonymous",
      "public",
      AuthorityUtils.createAuthorityList(Role.ROLE_PUBLIC.name()));
  }

  @Bean
  public MiddlewareKeyFilter middlewareKeyFilter() {
    return new MiddlewareKeyFilter(
      "middleware",
      AuthorityUtils.createAuthorityList(Role.ROLE_MIDDLEWARE.name()));
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Override
  @SuppressWarnings("squid:S4502")
  protected void configure(HttpSecurity http) throws Exception {
    // Configuration for anonymous access
    // https://stackoverflow.com/questions/48173057/customize-spring-security-for-trusted-space
    http.cors().and()
      .csrf().disable().headers().frameOptions().disable().and()
      .anonymous().authenticationFilter(anonymousAuthenticationFilter()).and()
      .exceptionHandling().authenticationEntryPoint(unauthorizedHandler).and()
      .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
      .authorizeRequests()
      .antMatchers(HttpMethod.GET, AUTH_WHITELIST).permitAll()
      .antMatchers(HttpMethod.POST, "/api/authenticate").permitAll()
      .antMatchers(HttpMethod.GET, "/api/languages").permitAll()
      .antMatchers(HttpMethod.GET, "/api/configuration-parameters").permitAll()
      .antMatchers("/api/account").hasAuthority(Role.ROLE_USER.name())
      .antMatchers(HttpMethod.GET, "/api/workspace").hasAnyAuthority(Role.ROLE_USER.name(), Role.ROLE_PUBLIC.name())
      .antMatchers(HttpMethod.GET, "/api/workspace/**").hasAnyAuthority(Role.ROLE_USER.name(), Role.ROLE_PUBLIC.name())
      .antMatchers("/api/**").hasAuthority(Role.ROLE_ADMIN.name())
      .anyRequest().authenticated();

    http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
    http.addFilterBefore(middlewareKeyFilter(), AuthTokenFilter.class);
  }
}