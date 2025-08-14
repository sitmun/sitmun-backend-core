package org.sitmun.resetPassword.Filtres;

import org.sitmun.infrastructure.security.core.Filter.RateLimitingFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResetPasswordLimiting {
  @Bean
  public FilterRegistrationBean<RateLimitingFilter> passwordResetResendLimiting() {
    FilterRegistrationBean<RateLimitingFilter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(new RateLimitingFilter(3, 15));
    registrationBean.addUrlPatterns("/api/password-reset/resend");
    return registrationBean;
  }

  @Bean
  public FilterRegistrationBean<RateLimitingFilter> passwordResetRequestLimiting() {
    FilterRegistrationBean<RateLimitingFilter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(new RateLimitingFilter(5, 15));
    registrationBean.addUrlPatterns("/api/password-reset/request");
    return registrationBean;
  }
}
