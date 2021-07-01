package org.sitmun.plugin.core.config;

import org.sitmun.plugin.core.security.AuthoritiesConstants;
import org.sitmun.plugin.core.security.SecurityConstants;
import org.sitmun.plugin.core.security.TokenProvider;
import org.sitmun.plugin.core.security.jwt.JWTFilter;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.data.repository.query.SecurityEvaluationContextExtension;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.annotation.PostConstruct;

@EnableWebSecurity
@Profile("!unsafe")
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

  private static final String[] AUTH_WHITELIST = {
    "/v3/api-docs/**",
    "/v3/api-docs.*",
    "/swagger-ui/**",
    "/swagger-ui.*",
    "/dist/**",
    "/workspace.html"
  };

  private final AuthenticationManagerBuilder authenticationManagerBuilder;
  private final TokenProvider tokenProvider;
  private final UserDetailsService userDetailsService;
  private final PasswordEncoder passwordEncoder;
  private final AnonymousAuthenticationFilter anonymousAuthenticationFilter;

  public WebSecurityConfig(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder,
                           TokenProvider tokenProvider,
                           AuthenticationManagerBuilder authenticationManagerBuilder) {
    super();
    this.authenticationManagerBuilder = authenticationManagerBuilder;
    this.userDetailsService = userDetailsService;
    this.passwordEncoder = passwordEncoder;
    this.tokenProvider = tokenProvider;
    anonymousAuthenticationFilter = new AnonymousAuthenticationFilter(
      "anonymous",
      SecurityConstants.SITMUN_PUBLIC_USERNAME,
      AuthorityUtils.createAuthorityList(AuthoritiesConstants.USUARIO_PUBLICO));
  }

  @Override
  @SuppressWarnings("squid:S4502")
  protected void configure(HttpSecurity http) throws Exception {
    // Configuration for anonymous access
    // https://stackoverflow.com/questions/48173057/customize-spring-security-for-trusted-space
    JWTFilter customFilter = new JWTFilter(tokenProvider);
    http
      .cors()
      .and()
      .anonymous().principal("public")
      .and()
      .addFilterBefore(customFilter, UsernamePasswordAuthenticationFilter.class)
      .exceptionHandling()
      .authenticationEntryPoint(getRestAuthenticationEntryPoint())
      //.accessDeniedHandler(problemSupport)
      .and()
      .csrf()
      .disable()
      .headers()
      .frameOptions()
      .disable()
      .and()
      .sessionManagement()
      .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
      .and()
      .authorizeRequests()
      .antMatchers(HttpMethod.POST, "/api/authenticate").permitAll()
      .antMatchers(HttpMethod.GET, "/api/languages").permitAll()
      .antMatchers(HttpMethod.GET, "/api/workspace").permitAll()
      .antMatchers(HttpMethod.GET, "/api/workspace/**").permitAll()
      .antMatchers(HttpMethod.GET, AUTH_WHITELIST).permitAll()
      .anyRequest().authenticated();
  }

  private AuthenticationEntryPoint getRestAuthenticationEntryPoint() {
    return new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
  }

  @Bean
  public SecurityEvaluationContextExtension securityEvaluationContextExtension() {
    return new SecurityEvaluationContextExtension();
  }

  @PostConstruct
  public void init() {
    try {
      authenticationManagerBuilder.userDetailsService(userDetailsService)
        .passwordEncoder(passwordEncoder);
    } catch (Exception e) {
      throw new BeanInitializationException("Security configuration failed", e);
    }
  }

  @Override
  @Bean
  public AuthenticationManager authenticationManager() throws Exception {
    return super.authenticationManager();
  }
}