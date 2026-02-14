package org.sitmun.infrastructure.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.jspecify.annotations.NonNull;
import org.sitmun.authentication.service.OidcRedirectService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Profile("oidc")
public class SitmunClientFilter extends OncePerRequestFilter {
  @Override
  protected void doFilterInternal(
      HttpServletRequest req, @NonNull HttpServletResponse res, @NonNull FilterChain chain)
      throws ServletException, IOException {
    final String clientType = req.getParameter("client_type");
    if (clientType != null) {
      req.getSession(true).setAttribute(OidcRedirectService.CLIENT_TYPE, clientType);
    }
    chain.doFilter(req, res);
  }
}
