package org.sitmun.infrastructure.security.config;

import static org.sitmun.infrastructure.security.core.SecurityConstants.*;
import static org.sitmun.infrastructure.security.core.SecurityRole.*;

import java.util.List;

import org.sitmun.domain.user.UserRepository;
import org.sitmun.infrastructure.security.core.SecurityEntryPoint;
import org.sitmun.infrastructure.security.core.userdetails.UserDetailsServiceImplementation;
import org.sitmun.infrastructure.security.filter.JsonWebTokenFilter;
import org.sitmun.infrastructure.security.filter.ProxyTokenFilter;
import org.sitmun.infrastructure.security.service.JsonWebTokenService;
import org.sitmun.infrastructure.security.storage.PasswordStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Security configuration for the SITMUN application.
 *
 * <p>This class configures: - JWT and Proxy token authentication filters - CORS settings for
 * cross-origin requests - Role-based access control for different endpoints - Password encoding and
 * storage strategies - Stateless session management
 *
 * <p>Authentication can be done via: 1. JWT tokens for regular users 2. Proxy tokens for middleware
 * communication
 *
 * <p>Roles supported: - ADMIN: Full access to all endpoints - USER: Access to user-specific
 * endpoints - PUBLIC: Limited access to public endpoints - PROXY: Special access for middleware
 * communication
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfigurer {

  private final SecurityEntryPoint unauthorizedHandler;

  private final UserDetailsServiceImplementation userDetailsService;

  private final JsonWebTokenService jsonWebTokenService;

  private final UserRepository userRepository;

  private final List<PasswordStorage> passwordStorageList;

  @Value("${sitmun.proxy-middleware.secret}")
  private String secret;

  public WebSecurityConfigurer(
      UserDetailsServiceImplementation userDetailsService,
      SecurityEntryPoint unauthorizedHandler,
      JsonWebTokenService jsonWebTokenService,
      List<PasswordStorage> passwordStorageList,
      UserRepository userRepository) {
    this.userDetailsService = userDetailsService;
    this.unauthorizedHandler = unauthorizedHandler;
    this.jsonWebTokenService = jsonWebTokenService;
    this.passwordStorageList = passwordStorageList;
    this.userRepository = userRepository;
  }

  @Bean
  public JsonWebTokenFilter authenticationJwtTokenFilter() {
    return new JsonWebTokenFilter(userDetailsService, jsonWebTokenService, userRepository);
  }

  @Bean
  public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
    AuthenticationManagerBuilder authenticationManagerBuilder =
        http.getSharedObject(AuthenticationManagerBuilder.class);
    authenticationManagerBuilder
        .userDetailsService(userDetailsService)
        .passwordEncoder(passwordEncoder());
    for (PasswordStorage ps : passwordStorageList) {
      ps.addPasswordStorage(authenticationManagerBuilder);
    }
    return authenticationManagerBuilder.build();
  }

  @Bean
  public AnonymousAuthenticationFilter anonymousAuthenticationFilter() {
    return new AnonymousAuthenticationFilter(
        PUBLIC_KEY, PUBLIC_PRINCIPAL, createAuthorityList(PUBLIC));
  }

  @Bean
  public ProxyTokenFilter middlewareKeyFilter() {
    return new ProxyTokenFilter(
        PROXY_MIDDLEWARE_KEY, PROXY_MIDDLEWARE_PRINCIPAL, createAuthorityList(PROXY), secret);
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowCredentials(true);
    config.addAllowedOriginPattern("*");
    config.addAllowedHeader("*");
    config.addAllowedMethod("OPTIONS");
    config.addAllowedMethod("GET");
    config.addAllowedMethod("POST");
    config.addAllowedMethod("PUT");
    config.addAllowedMethod("DELETE");
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  @Bean
  public CorsFilter corsFilter() {
    return new CorsFilter(corsConfigurationSource());
  }

  @Bean
  @SuppressWarnings("squid:S4502")
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(AbstractHttpConfigurer::disable)
        .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
        .anonymous(anonymous -> anonymous.authenticationFilter(anonymousAuthenticationFilter()))
        .exceptionHandling(
            exceptionHandling -> exceptionHandling.authenticationEntryPoint(unauthorizedHandler))
        .sessionManagement(
            sessionManagement ->
                sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            authz -> {
              authz = configurePermitAll(authz);
              authz = configureUser(authz);
              authz = configureUserOrPublic(authz);
              authz = configureProxy(authz);
              authz = configureAdmin(authz);
              authz.anyRequest().denyAll();
            });

    http.addFilterBefore(
        authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
    http.addFilterBefore(middlewareKeyFilter(), JsonWebTokenFilter.class);

    return http.build();
  }

  // TODO: Confirm the logic for authorization of open endpoints
  private AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
      configurePermitAll(
          AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
              authz) {
    return authz
        .requestMatchers(new AntPathRequestMatcher("/v3/api-docs"))
        .permitAll()
        .requestMatchers(new AntPathRequestMatcher("/v3/api-docs.yaml"))
        .permitAll()
        .requestMatchers(new AntPathRequestMatcher("/v3/api-docs/**"))
        .permitAll()
        .requestMatchers(new AntPathRequestMatcher("/swagger-ui/**"))
        .permitAll()
        .requestMatchers(new AntPathRequestMatcher("/api/profile"))
        .permitAll()
        .requestMatchers(new AntPathRequestMatcher("/api/profile/**"))
        .permitAll()
        .requestMatchers(new AntPathRequestMatcher("/api/dashboard/health"))
        .permitAll()
        .requestMatchers(new AntPathRequestMatcher("/api/authenticate", "POST"))
        .permitAll()
        .requestMatchers(new AntPathRequestMatcher("/api/password-reset/**", "POST"))
        .permitAll()
        .requestMatchers(new AntPathRequestMatcher("/api/languages", "GET"))
        .permitAll()
        .requestMatchers(new AntPathRequestMatcher("/api/configuration-parameters", "GET"))
        .permitAll()
        .requestMatchers(new AntPathRequestMatcher("/api/account/public/**", "GET"))
        .permitAll();
  }

  /**
   * Configures authorization for user-specific endpoints. These endpoints require USER role
   * authentication and include: - /api/account: User account management - /api/account/** (GET):
   * User account information retrieval - /api/user-verification/** (POST): User verification
   * processes
   *
   * @param authz The authorization configuration
   * @return The updated authorization configuration
   */
  private AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
      configureUser(
          AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
              authz) {
    return authz
        .requestMatchers(new AntPathRequestMatcher("/api/account"))
        .hasRole(USER.name())
        .requestMatchers(new AntPathRequestMatcher("/api/account/**", "GET"))
        .hasRole(USER.name())
        .requestMatchers(new AntPathRequestMatcher("/api/user-verification/**", "POST"))
        .hasRole(USER.name())
        .requestMatchers(new AntPathRequestMatcher("/api/user/details", "GET"))
        .hasRole(USER.name());
  }

  /**
   * Configures authorization for endpoints accessible by both USER and PUBLIC roles. These
   * endpoints include: - /api/config/client/** (GET): Client configuration retrieval -
   * /api/config/client/** (PUT): Client configuration updates
   *
   * @param authz The authorization configuration
   * @return The updated authorization configuration
   */
  private AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
      configureUserOrPublic(
          AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
              authz) {
    return authz
        .requestMatchers(new AntPathRequestMatcher("/api/config/client/**", "GET"))
        .hasAnyRole(USER.name(), PUBLIC.name())
        .requestMatchers(new AntPathRequestMatcher("/api/config/client/**", "PUT"))
        .hasAnyRole(USER.name(), PUBLIC.name());
  }

  private AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
      configureProxy(
          AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
              authz) {
    return authz
        .requestMatchers(new AntPathRequestMatcher("/api/config/proxy/**", "POST"))
        .hasRole(PROXY.name());
  }

  private AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
      configureAdmin(
          AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
              authz) {
    return authz.requestMatchers(new AntPathRequestMatcher("/api/**")).hasRole(ADMIN.name());
  }
}
