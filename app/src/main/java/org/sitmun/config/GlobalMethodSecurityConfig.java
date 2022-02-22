package org.sitmun.config;

import org.sitmun.security.CustomPermissionEvaluator;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;

@Configuration
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
public class GlobalMethodSecurityConfig extends GlobalMethodSecurityConfiguration {

  final CustomPermissionEvaluator customPermissionEvaluator;

  public GlobalMethodSecurityConfig(CustomPermissionEvaluator customPermissionEvaluator) {
    this.customPermissionEvaluator = customPermissionEvaluator;
  }

  @Override
  protected MethodSecurityExpressionHandler createExpressionHandler() {
    DefaultMethodSecurityExpressionHandler expressionHandler =
      new DefaultMethodSecurityExpressionHandler();
    expressionHandler.setPermissionEvaluator(customPermissionEvaluator);
    return expressionHandler;
  }
}