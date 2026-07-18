package org.sitmun.infrastructure.security.filter;

import static org.sitmun.infrastructure.security.core.SecurityRole.MOBILE_EDITION;
import static org.sitmun.infrastructure.security.core.SecurityRole.PROXY;
import static org.sitmun.infrastructure.security.jwt.MobileTokenScopes.springAuthority;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.sitmun.domain.user.User;
import org.sitmun.domain.user.UserRepository;
import org.sitmun.infrastructure.security.core.Rfc9457ResponseWriter;
import org.sitmun.infrastructure.security.service.JsonWebTokenService;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates mobile edition access tokens from {@code Authorization: Bearer}. Skips when the
 * proxy middleware principal is already authenticated so delegated proxy tokens can remain in the
 * Authorization header for proxy-configuration endpoints.
 */
public class EditionBearerTokenFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";

  private final JsonWebTokenService jsonWebTokenService;
  private final UserDetailsService userDetailsService;
  private final UserRepository userRepository;
  private final Rfc9457ResponseWriter responseWriter;

  public EditionBearerTokenFilter(
      JsonWebTokenService jsonWebTokenService,
      UserDetailsService userDetailsService,
      UserRepository userRepository,
      Rfc9457ResponseWriter responseWriter) {
    this.jsonWebTokenService = jsonWebTokenService;
    this.userDetailsService = userDetailsService;
    this.userRepository = userRepository;
    this.responseWriter = responseWriter;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    if (isProxyPrincipalAuthenticated()) {
      filterChain.doFilter(request, response);
      return;
    }

    Optional<String> bearer = extractBearer(request);
    if (bearer.isEmpty()) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      var claims = jsonWebTokenService.parseClaims(bearer.get());
      if (!jsonWebTokenService.isEditionAccessToken(claims)) {
        reject(request, response);
        return;
      }

      String username = claims.getSubject();
      if (!StringUtils.hasText(username)) {
        reject(request, response);
        return;
      }

      UserDetails userDetails = userDetailsService.loadUserByUsername(username);
      if (!userDetails.isAccountNonLocked()) {
        reject(request, response);
        return;
      }

      Optional<User> user = userRepository.findByUsername(username);
      if (user.isEmpty()) {
        reject(request, response);
        return;
      }

      Date lastPasswordChange = user.get().getLastPasswordChange();
      if (!jsonWebTokenService.validateEditionAccessToken(bearer.get(), lastPasswordChange)) {
        reject(request, response);
        return;
      }

      List<GrantedAuthority> authorities = new ArrayList<>();
      authorities.add(new SimpleGrantedAuthority(MOBILE_EDITION.authority()));
      jsonWebTokenService
          .getScopes(claims)
          .forEach(scope -> authorities.add(new SimpleGrantedAuthority(springAuthority(scope))));

      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(username, null, authorities);
      authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
      SecurityContextHolder.getContext().setAuthentication(authentication);
      filterChain.doFilter(request, response);
    } catch (JwtException | IllegalArgumentException e) {
      reject(request, response);
    }
  }

  private static boolean isProxyPrincipalAuthenticated() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null
        && authentication.getAuthorities().stream()
            .anyMatch(a -> PROXY.authority().equals(a.getAuthority()));
  }

  private static Optional<String> extractBearer(HttpServletRequest request) {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (!StringUtils.hasText(header) || !header.startsWith(BEARER_PREFIX)) {
      return Optional.empty();
    }
    String token = header.substring(BEARER_PREFIX.length()).trim();
    return StringUtils.hasText(token) ? Optional.of(token) : Optional.empty();
  }

  private void reject(HttpServletRequest request, HttpServletResponse response) throws IOException {
    SecurityContextHolder.clearContext();
    responseWriter.writeUnauthorized(request, response);
  }
}
