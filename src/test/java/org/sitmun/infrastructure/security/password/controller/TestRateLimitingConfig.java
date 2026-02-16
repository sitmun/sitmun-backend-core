package org.sitmun.infrastructure.security.password.controller;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test configuration that disables rate limiting for password reset endpoints. This prevents rate
 * limit counters from accumulating across tests.
 */
@TestConfiguration
public class TestRateLimitingConfig {

  /** No-op filter that does nothing - effectively disables rate limiting for tests. */
  private static class NoOpFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
      chain.doFilter(request, response);
    }
  }

  /** Disables rate limiting for password reset resend endpoint in tests. */
  @Bean
  @Primary
  public FilterRegistrationBean<Filter> passwordResetResendLimiting() {
    FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(new NoOpFilter());
    registrationBean.addUrlPatterns("/api/password-reset/resend");
    registrationBean.setOrder(1);
    return registrationBean;
  }

  /** Disables rate limiting for password reset request endpoint in tests. */
  @Bean
  @Primary
  public FilterRegistrationBean<Filter> passwordResetRequestLimiting() {
    FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(new NoOpFilter());
    registrationBean.addUrlPatterns("/api/password-reset/request");
    registrationBean.setOrder(1);
    return registrationBean;
  }
}
