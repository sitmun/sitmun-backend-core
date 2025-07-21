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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.AuthorityUtils;
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

import java.util.List;

@Configuration
@EnableWebSecurity
public class WebSecurityConfigurer {

  public static final String PUBLIC_USER_NAME = "public";


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

  @Bean
  public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
    AuthenticationManagerBuilder authenticationManagerBuilder = http
      .getSharedObject(AuthenticationManagerBuilder.class);
    authenticationManagerBuilder.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
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
    .headers(headers -> headers.frameOptions().disable())
    .anonymous(anonymous -> anonymous.authenticationFilter(anonymousAuthenticationFilter()))
    .exceptionHandling(exceptionHandling -> exceptionHandling.authenticationEntryPoint(unauthorizedHandler))
    .sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    .authorizeHttpRequests(authz -> authz
      .requestMatchers(new AntPathRequestMatcher("/v3/api-docs*/**")).permitAll()
      .requestMatchers(new AntPathRequestMatcher("/v3/api-docs*.*")).permitAll()
      .requestMatchers(new AntPathRequestMatcher("/swagger-ui/**")).permitAll()
      .requestMatchers(new AntPathRequestMatcher("/swagger-ui.*")).permitAll()
      .requestMatchers(new AntPathRequestMatcher("/api/profile")).permitAll()
      .requestMatchers(new AntPathRequestMatcher("/api/profile/**")).permitAll()
      .requestMatchers(new AntPathRequestMatcher("/api/authenticate", "POST")).permitAll()
      .requestMatchers(new AntPathRequestMatcher("/api/recover-password", "POST")).permitAll()
      .requestMatchers(new AntPathRequestMatcher("/api/recover-password", "PUT")).permitAll()
      .requestMatchers(new AntPathRequestMatcher("/api/userTokenValid", "GET")).permitAll()
      .requestMatchers(new AntPathRequestMatcher("/api/languages", "GET")).permitAll()
      .requestMatchers(new AntPathRequestMatcher("/api/configuration-parameters", "GET")).permitAll()
      .requestMatchers(new AntPathRequestMatcher("/api/account/public/**", "GET")).permitAll()
      .requestMatchers(new AntPathRequestMatcher("/api/account")).hasAuthority(SecurityRole.ROLE_USER.name())
      .requestMatchers(new AntPathRequestMatcher("/api/account/all", "GET")).hasAnyAuthority(SecurityRole.ROLE_ADMIN.name())
      .requestMatchers(new AntPathRequestMatcher("/api/workspace", "GET")).hasAnyAuthority(SecurityRole.ROLE_USER.name(), SecurityRole.ROLE_PUBLIC.name())
      .requestMatchers(new AntPathRequestMatcher("/api/workspace/**", "GET")).hasAnyAuthority(SecurityRole.ROLE_USER.name(), SecurityRole.ROLE_PUBLIC.name())
      .requestMatchers(new AntPathRequestMatcher("/api/config/client/**", "GET")).hasAnyAuthority(SecurityRole.ROLE_USER.name(), SecurityRole.ROLE_PUBLIC.name())
      .requestMatchers(new AntPathRequestMatcher("/api/config/client/**", "PUT")).hasAnyAuthority(SecurityRole.ROLE_USER.name(), SecurityRole.ROLE_PUBLIC.name())
      .requestMatchers(new AntPathRequestMatcher("/api/user-verification/**", "POST")).hasAuthority(SecurityRole.ROLE_USER.name())
      .requestMatchers(new AntPathRequestMatcher("/api/config/proxy/**", "POST")).hasAnyAuthority(SecurityRole.ROLE_PROXY.name())
      .requestMatchers(new AntPathRequestMatcher("/api/dashboard/health")).permitAll()
      .requestMatchers(new AntPathRequestMatcher("/api/dashboard/info")).hasAuthority(SecurityRole.ROLE_ADMIN.name())
      .requestMatchers(new AntPathRequestMatcher("/api/**")).hasAuthority(SecurityRole.ROLE_ADMIN.name())
      .anyRequest().authenticated()
    );

    http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
    http.addFilterBefore(middlewareKeyFilter(), JsonWebTokenFilter.class);
    
    return http.build();
  }
}