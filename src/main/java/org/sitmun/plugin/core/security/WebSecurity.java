package org.sitmun.plugin.core.security;

import javax.annotation.PostConstruct;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@EnableWebSecurity
public class WebSecurity extends WebSecurityConfigurerAdapter {
  private final AuthenticationManagerBuilder authenticationManagerBuilder;
  private final TokenProvider tokenProvider;
  private UserDetailsService userDetailsService;
  private PasswordEncoder passwordEncoder;
  private AnonymousAuthenticationFilter anonymousAuthenticationFilter;


  public WebSecurity(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder,
                     TokenProvider tokenProvider, AuthenticationManagerBuilder authenticationManagerBuilder) {
    super();
    this.authenticationManagerBuilder = authenticationManagerBuilder;
    this.userDetailsService = userDetailsService;
    this.passwordEncoder = passwordEncoder;
    this.tokenProvider = tokenProvider;
    anonymousAuthenticationFilter = new AnonymousAuthenticationFilter("anonymous", SecurityConstants.SITMUN_PUBLIC_USERNAME, AuthorityUtils.createAuthorityList(AuthoritiesConstants.USUARIO_PUBLICO));
  }

  @Override
  @SuppressWarnings("squid:S4502")
  protected void configure(HttpSecurity http) throws Exception {
    //COnfigurar acceso anńimo
    //https://stackoverflow.com/questions/48173057/customize-spring-security-for-trusted-space

    http
        .addFilterBefore(corsFilter(), UsernamePasswordAuthenticationFilter.class)
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
        .antMatchers("/api/users").authenticated()
        .antMatchers("/api/account").authenticated()
        .antMatchers("/api/**").permitAll()

        .and()
        .apply(securityConfigurerAdapter())
        .and()
        .anonymous().authenticationFilter(anonymousAuthenticationFilter);

  }

  @Bean
  public CorsFilter corsFilter() {
    final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    CorsConfiguration corsConfig = new CorsConfiguration().applyPermitDefaultValues();
    corsConfig.addAllowedOrigin("http://localhost:4200");
    corsConfig.addAllowedMethod("POST");
    corsConfig.addAllowedMethod("PUT");
    corsConfig.addAllowedMethod("DELETE");
    corsConfig.addAllowedMethod("HEAD");
    corsConfig.addAllowedMethod("OPTIONS");
    corsConfig.setAllowCredentials(true);
    source.registerCorsConfiguration("/**", corsConfig);
    return new CorsFilter(source);
  }

  private JWTConfigurer securityConfigurerAdapter() {
    return new JWTConfigurer(tokenProvider);
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
      authenticationManagerBuilder.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder);
    } catch (Exception e) {
      throw new BeanInitializationException("Security configuration failed", e);
    }
  }

}