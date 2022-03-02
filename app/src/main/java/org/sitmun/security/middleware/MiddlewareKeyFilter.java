package org.sitmun.security.middleware;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class MiddlewareKeyFilter extends OncePerRequestFilter {

  private static final Logger logger = LoggerFactory.getLogger(MiddlewareKeyFilter.class);
  private final String principal;
  private final List<GrantedAuthority> authorities;
  @Value("${security.authentication.middleware.secret}")
  private String middlewareSecret;

  public MiddlewareKeyFilter(String principal, List<GrantedAuthority> authorities) {
    this.principal = principal;
    this.authorities = authorities;
  }

  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
    throws ServletException, IOException {
    try {
      String key = request.getHeader("Key") == null ? "" : request.getHeader("Key");
      logger.info("Trying key: " + key);

      if (Objects.equals(key, middlewareSecret)) {
        UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(
            principal,
            null,
            authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
      }
    } catch (Exception e) {
      logger.error("Cannot set user authentication: {}", e.getMessage());
    }

    filterChain.doFilter(request, response);
  }
}