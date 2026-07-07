package org.sitmun.infrastructure.security.filter;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.sitmun.authentication.controller.AuthenticationController;
import org.sitmun.authentication.service.CookieService;
import org.sitmun.domain.user.User;
import org.sitmun.domain.user.UserRepository;
import org.sitmun.infrastructure.security.service.JsonWebTokenService;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

public class JsonWebTokenFilter extends OncePerRequestFilter {

  private final JsonWebTokenService jsonWebTokenService;
  private final UserDetailsService userDetailsService;
  private final UserRepository userRepository;
  private final CookieService cookieService;

  public JsonWebTokenFilter(
      UserDetailsService userDetailsService,
      JsonWebTokenService jsonWebTokenService,
      UserRepository userRepository,
      CookieService cookieService) {
    this.userDetailsService = userDetailsService;
    this.jsonWebTokenService = jsonWebTokenService;
    this.userRepository = userRepository;
    this.cookieService = cookieService;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest httpServletRequest,
      @NonNull HttpServletResponse httpServletResponse,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    final String jwtToken = getTokenFromRequest(httpServletRequest);

    if (StringUtils.isEmpty(jwtToken)) {
      filterChain.doFilter(httpServletRequest, httpServletResponse);
      return;
    }

    try {
      String username = jsonWebTokenService.getUsernameFromToken(jwtToken);
      if (StringUtils.isNotEmpty(username)
          && SecurityContextHolder.getContext().getAuthentication() == null) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (!userDetails.isAccountNonLocked()) {
          SecurityContextHolder.clearContext();
          cookieService.clearAccessTokenCookie(httpServletRequest, httpServletResponse);
          httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          return;
        }

        Optional<User> user = this.userRepository.findByUsername(username);
        if (user.isEmpty()) {
          clearStaleAccessTokenCookie(httpServletRequest, httpServletResponse);
          filterChain.doFilter(httpServletRequest, httpServletResponse);
          return;
        }
        Date lastPasswordChange = user.get().getLastPasswordChange();
        if (jsonWebTokenService.validateToken(jwtToken, userDetails, lastPasswordChange)) {
          UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
              new UsernamePasswordAuthenticationToken(
                  userDetails, null, userDetails.getAuthorities());
          usernamePasswordAuthenticationToken.setDetails(
              new WebAuthenticationDetailsSource().buildDetails(httpServletRequest));
          SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
        } else {
          clearStaleAccessTokenCookie(httpServletRequest, httpServletResponse);
        }
      }
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid JWT token");
      clearStaleAccessTokenCookie(httpServletRequest, httpServletResponse);
    } catch (ExpiredJwtException e) {
      logger.debug("JWT token expired");
      clearStaleAccessTokenCookie(httpServletRequest, httpServletResponse);
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
    filterChain.doFilter(httpServletRequest, httpServletResponse);
  }

  private void clearStaleAccessTokenCookie(
      HttpServletRequest request, HttpServletResponse response) {
    cookieService.clearAccessTokenCookie(request, response);
  }

  private String getTokenFromRequest(HttpServletRequest request) {
    if (request.getCookies() == null) {
      return null;
    }
    return Arrays.stream(request.getCookies())
        .filter(c -> AuthenticationController.ACCESS_TOKEN_COOKIE_NAME.equals(c.getName()))
        .findFirst()
        .map(Cookie::getValue)
        .orElse(null);
  }
}
