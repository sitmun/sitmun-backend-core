package org.sitmun.infrastructure.security.filter;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.sitmun.domain.user.User;
import org.sitmun.domain.user.UserRepository;
import org.sitmun.infrastructure.security.service.JsonWebTokenService;
import org.springframework.http.HttpHeaders;
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

  public JsonWebTokenFilter(
      UserDetailsService userDetailsService,
      JsonWebTokenService jsonWebTokenService,
      UserRepository userRepository) {
    this.userDetailsService = userDetailsService;
    this.jsonWebTokenService = jsonWebTokenService;
    this.userRepository = userRepository;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest httpServletRequest,
      @NonNull HttpServletResponse httpServletResponse,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    final String requestTokenHeader = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);
    if (StringUtils.isEmpty(requestTokenHeader)
        || !StringUtils.startsWith(requestTokenHeader, "Bearer ")) {
      filterChain.doFilter(httpServletRequest, httpServletResponse);
      return;
    }
    try {
      String jwtToken = requestTokenHeader.substring(7);
      String username = jsonWebTokenService.getUsernameFromToken(jwtToken);
      if (StringUtils.isNotEmpty(username)
          && SecurityContextHolder.getContext().getAuthentication() == null) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        Optional<User> user = this.userRepository.findByUsername(username);
        if (user.isEmpty()) {
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
        }
      }
    } catch (IllegalArgumentException e) {
      logger.error("Unable to fetch JWT Token");
    } catch (ExpiredJwtException e) {
      logger.error("JWT Token is expired");
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
    filterChain.doFilter(httpServletRequest, httpServletResponse);
  }
}
