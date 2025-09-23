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
import org.springframework.http.HttpMethod;
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
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
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
              authz
          ) {
    var builder = PathPatternRequestMatcher.withDefaults();
    return authz
        .requestMatchers(builder.matcher( HttpMethod.GET,"/v3/api-docs"))
        .permitAll()
        .requestMatchers(builder.matcher(HttpMethod.GET,"/v3/api-docs.yaml"))
        .permitAll()
        .requestMatchers(builder.matcher(HttpMethod.GET,"/v3/api-docs/**"))
        .permitAll()
        .requestMatchers(builder.matcher(HttpMethod.GET,"/swagger-ui/**"))
        .permitAll()
        .requestMatchers(builder.matcher(HttpMethod.GET,"/api/profile"))
        .permitAll()
        .requestMatchers(builder.matcher(HttpMethod.GET,"/api/profile/**"))
        .permitAll()
        .requestMatchers(builder.matcher(HttpMethod.GET,"/api/dashboard/health"))
        .permitAll()
        .requestMatchers(builder.matcher(HttpMethod.POST, "/api/authenticate"))
        .permitAll()
        .requestMatchers(builder.matcher(HttpMethod.POST, "/api/password-reset/**"))
        .permitAll()
        .requestMatchers(builder.matcher(HttpMethod.PUT, "/api/password-reset/**"))
        .permitAll()
        .requestMatchers(builder.matcher(HttpMethod.GET,"/api/languages"))
        .permitAll()
        .requestMatchers(builder.matcher(HttpMethod.GET, "/api/configuration-parameters"))
        .permitAll()
        .requestMatchers(builder.matcher(HttpMethod.GET, "/api/account/public/**"))
        .permitAll();
  }

  /**
   * Configures authorization for user-specific endpoints. These endpoints require USER role
   * authentication and include:
   * <ul>
   * <li>/api/account: (GET, POST) User account management</li>
   * <li>/api/account/** (GET): User account information retrieval</li>
   * <li>/api/user-verification/** (POST): User verification processes</li>
   * </ul>
   * @param authz The authorization configuration
   * @return The updated authorization configuration
   */
  private AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
      configureUser(
          AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
              authz) {
    var builder = PathPatternRequestMatcher.withDefaults();
    return authz
        .requestMatchers(builder.matcher(HttpMethod.GET,"/api/account"))
        .hasRole(USER.name())
        .requestMatchers(builder.matcher(HttpMethod.POST,"/api/account"))
        .hasRole(USER.name())
        .requestMatchers(builder.matcher(HttpMethod.GET,"/api/account/**"))
        .hasRole(USER.name())
        .requestMatchers(builder.matcher(HttpMethod.POST, "/api/user-verification/**"))
        .hasRole(USER.name())
        .requestMatchers(builder.matcher(HttpMethod.GET, "/api/user/details"))
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
    var builder = PathPatternRequestMatcher.withDefaults();
    return authz
        .requestMatchers(builder.matcher(HttpMethod.GET, "/api/config/languages"))
        .hasAnyRole(USER.name(), PUBLIC.name())
        .requestMatchers(builder.matcher(HttpMethod.GET, "/api/config/client/**"))
        .hasAnyRole(USER.name(), PUBLIC.name())
        .requestMatchers(builder.matcher(HttpMethod.POST,"/api/config/client/territory/position"))
        .hasAnyRole(USER.name(), PUBLIC.name());
  }

  private AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
      configureProxy(
          AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
              authz) {
    var builder = PathPatternRequestMatcher.withDefaults();
    return authz
        .requestMatchers(builder.matcher(HttpMethod.POST, "/api/config/proxy/**"))
        .hasRole(PROXY.name());
  }

  private AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
      configureAdmin(
          AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
              authz) {
    var builder = PathPatternRequestMatcher.withDefaults();
    return authz.requestMatchers(builder.matcher("/api/**")).hasRole(ADMIN.name());
  }
}
