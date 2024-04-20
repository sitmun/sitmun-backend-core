package org.sitmun.infrastructure.security.config;

import org.sitmun.infrastructure.security.core.SecurityEntryPoint;
import org.sitmun.infrastructure.security.core.SecurityRole;
import org.sitmun.infrastructure.security.core.userdetails.UserDetailsServiceImplementation;
import org.sitmun.infrastructure.security.filter.JsonWebTokenFilter;
import org.sitmun.infrastructure.security.filter.ProxyTokenFilter;
import org.sitmun.infrastructure.security.service.JsonWebTokenService;
import org.sitmun.infrastructure.security.storage.PasswordStorage;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
public class WebSecurityConfigurer extends WebSecurityConfigurerAdapter {

  public static final String PUBLIC_USER_NAME = "public";
  private static final String[] AUTH_WHITELIST = {
    "/v3/api-docs*/**",
    "/v3/api-docs*.*",
    "/swagger-ui/**",
    "/swagger-ui.*",
    "/dist/**",
    "/workspace.html"
  };

  private final SecurityEntryPoint unauthorizedHandler;

  private final UserDetailsServiceImplementation userDetailsService;

  private final JsonWebTokenService jsonWebTokenService;

  private final List<PasswordStorage> passwordStorageList;

  public WebSecurityConfigurer(UserDetailsServiceImplementation userDetailsService,
                               SecurityEntryPoint unauthorizedHandler,
                               JsonWebTokenService jsonWebTokenService,
                               List<PasswordStorage> passwordStorageList) {
    this.userDetailsService = userDetailsService;
    this.unauthorizedHandler = unauthorizedHandler;
    this.jsonWebTokenService = jsonWebTokenService;
    this.passwordStorageList = passwordStorageList;
  }

  @Bean
  public JsonWebTokenFilter authenticationJwtTokenFilter() {
    return new JsonWebTokenFilter(userDetailsService, jsonWebTokenService);
  }

  @Override
  public void configure(AuthenticationManagerBuilder auth) throws Exception {
    auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
  }

  @Bean
  public AuthenticationManager authenticationManagerBean(HttpSecurity http) throws Exception {
    AuthenticationManagerBuilder authenticationManagerBuilder = http
      .getSharedObject(AuthenticationManagerBuilder.class);
    for (PasswordStorage ps : passwordStorageList) {
      ps.addPasswordStorage(authenticationManagerBuilder);
    }
    return authenticationManagerBuilder.build();
  }

  @Bean
  public AnonymousAuthenticationFilter anonymousAuthenticationFilter() {
    return new AnonymousAuthenticationFilter(
      "anonymous",
      PUBLIC_USER_NAME,
      AuthorityUtils.createAuthorityList(SecurityRole.ROLE_PUBLIC.name()));
  }

  @Bean
  public ProxyTokenFilter middlewareKeyFilter() {
    return new ProxyTokenFilter(
      "middleware",
      AuthorityUtils.createAuthorityList(SecurityRole.ROLE_PROXY.name()));
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public CorsFilter corsFilter() {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowCredentials(true);
    config.addAllowedOriginPattern("*");
    config.addAllowedHeader("*");
    config.addAllowedMethod("OPTIONS");
    config.addAllowedMethod("GET");
    config.addAllowedMethod("POST");
    config.addAllowedMethod("PUT");
    config.addAllowedMethod("DELETE");
    source.registerCorsConfiguration("/**", config);
    return new CorsFilter(source);
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
      .antMatchers("/api/account").hasAuthority(SecurityRole.ROLE_USER.name())
      .antMatchers(HttpMethod.GET, "/api/workspace").hasAnyAuthority(SecurityRole.ROLE_USER.name(), SecurityRole.ROLE_PUBLIC.name())
      .antMatchers(HttpMethod.GET, "/api/workspace/**").hasAnyAuthority(SecurityRole.ROLE_USER.name(), SecurityRole.ROLE_PUBLIC.name())
      .antMatchers(HttpMethod.GET, "/api/config/client/**").hasAnyAuthority(SecurityRole.ROLE_USER.name(), SecurityRole.ROLE_PUBLIC.name())
      .antMatchers(HttpMethod.POST, "/api/config/proxy/**").hasAnyAuthority(SecurityRole.ROLE_PROXY.name())
      .antMatchers("/api/**").hasAuthority(SecurityRole.ROLE_ADMIN.name())
      .anyRequest().authenticated();

    http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
    http.addFilterBefore(middlewareKeyFilter(), JsonWebTokenFilter.class);
  }
}