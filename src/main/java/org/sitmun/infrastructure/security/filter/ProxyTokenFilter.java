package org.sitmun.infrastructure.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class ProxyTokenFilter extends OncePerRequestFilter {

  public static final String X_SITMUN_PROXY_KEY = "X-SITMUN-Proxy-Key";
  private final String principal;
  private final List<GrantedAuthority> authorities;
  @Value("${sitmun.proxy-middleware.secret}")
  private String middlewareSecret;

  public ProxyTokenFilter(String principal, List<GrantedAuthority> authorities) {
    this.principal = principal;
    this.authorities = authorities;
  }

  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest httpServletRequest, @NonNull HttpServletResponse httpServletResponse, @NonNull FilterChain filterChain)
    throws ServletException, IOException {
    try {
      String key = httpServletRequest.getHeader(X_SITMUN_PROXY_KEY) == null ? "" : httpServletRequest.getHeader(X_SITMUN_PROXY_KEY);

      if (Objects.equals(key, middlewareSecret)) {
        UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(
            principal,
            null,
            authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(httpServletRequest));
        SecurityContextHolder.getContext().setAuthentication(authentication);
      }
    } catch (Exception e) {
      String msg = String.format("Cannot set user authentication: %s", e.getMessage());
      logger.error(msg, e);
    }

    filterChain.doFilter(httpServletRequest, httpServletResponse);
  }
}