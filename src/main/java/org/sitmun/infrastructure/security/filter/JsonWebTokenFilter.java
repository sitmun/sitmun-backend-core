package org.sitmun.infrastructure.security.filter;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
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
import org.sitmun.infrastructure.security.core.Rfc9457ResponseWriter;
import org.sitmun.infrastructure.security.service.JsonWebTokenService;
import org.springframework.dao.DataAccessException;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
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
  private final Rfc9457ResponseWriter responseWriter;

  public JsonWebTokenFilter(
      UserDetailsService userDetailsService,
      JsonWebTokenService jsonWebTokenService,
      UserRepository userRepository,
      CookieService cookieService,
      Rfc9457ResponseWriter responseWriter) {
    this.userDetailsService = userDetailsService;
    this.jsonWebTokenService = jsonWebTokenService;
    this.userRepository = userRepository;
    this.cookieService = cookieService;
    this.responseWriter = responseWriter;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest httpServletRequest,
      @NonNull HttpServletResponse httpServletResponse,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    Optional<Cookie> accessTokenCookie = getAccessTokenCookie(httpServletRequest);

    if (accessTokenCookie.isEmpty()) {
      filterChain.doFilter(httpServletRequest, httpServletResponse);
      return;
    }

    String jwtToken = accessTokenCookie.get().getValue();
    if (StringUtils.isEmpty(jwtToken)) {
      rejectAuthentication(httpServletRequest, httpServletResponse);
      return;
    }

    try {
      String username = jsonWebTokenService.getUsernameFromToken(jwtToken);
      if (StringUtils.isEmpty(username)) {
        rejectAuthentication(httpServletRequest, httpServletResponse);
        return;
      }
      if (SecurityContextHolder.getContext().getAuthentication() == null) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (!userDetails.isAccountNonLocked()) {
          rejectAuthentication(httpServletRequest, httpServletResponse);
          return;
        }

        Optional<User> user = this.userRepository.findByUsername(username);
        if (user.isEmpty()) {
          rejectAuthentication(httpServletRequest, httpServletResponse);
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
          rejectAuthentication(httpServletRequest, httpServletResponse);
          return;
        }
      }
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid JWT token");
      rejectAuthentication(httpServletRequest, httpServletResponse);
      return;
    } catch (ExpiredJwtException e) {
      logger.debug("JWT token expired");
      rejectAuthentication(httpServletRequest, httpServletResponse);
      return;
    } catch (JwtException e) {
      logger.warn("Invalid JWT token");
      rejectAuthentication(httpServletRequest, httpServletResponse);
      return;
    } catch (DataAccessException | AuthenticationServiceException e) {
      logger.error("Authentication identity store is unavailable", e);
      failAuthenticationService(httpServletRequest, httpServletResponse);
      return;
    } catch (AuthenticationException e) {
      logger.warn("JWT credentials are no longer valid");
      rejectAuthentication(httpServletRequest, httpServletResponse);
      return;
    } catch (RuntimeException e) {
      logger.error("Unexpected JWT authentication failure", e);
      failAuthenticationProcessing(httpServletRequest, httpServletResponse);
      return;
    }
    filterChain.doFilter(httpServletRequest, httpServletResponse);
  }

  private void clearStaleAccessTokenCookie(
      HttpServletRequest request, HttpServletResponse response) {
    cookieService.clearAccessTokenCookie(request, response);
  }

  private void rejectAuthentication(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    SecurityContextHolder.clearContext();
    clearStaleAccessTokenCookie(request, response);
    responseWriter.writeUnauthorized(request, response);
  }

  private void failAuthenticationService(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    SecurityContextHolder.clearContext();
    responseWriter.writeServiceUnavailable(request, response);
  }

  private void failAuthenticationProcessing(
      HttpServletRequest request, HttpServletResponse response) throws IOException {
    SecurityContextHolder.clearContext();
    responseWriter.writeInternalServerError(request, response);
  }

  private Optional<Cookie> getAccessTokenCookie(HttpServletRequest request) {
    if (request.getCookies() == null) {
      return Optional.empty();
    }
    return Arrays.stream(request.getCookies())
        .filter(c -> AuthenticationController.ACCESS_TOKEN_COOKIE_NAME.equals(c.getName()))
        .findFirst();
  }
}
