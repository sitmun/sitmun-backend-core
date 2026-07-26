package org.sitmun.authorization.proxy.service;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import org.sitmun.authorization.access.UserApplicationAccessPolicy;
import org.sitmun.domain.user.User;
import org.sitmun.domain.user.UserRepository;
import org.sitmun.infrastructure.security.core.SecurityConstants;
import org.sitmun.infrastructure.security.service.JsonWebTokenService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Validates delegated JWTs on proxy-configuration endpoints. Accepts legacy viewer proxy tokens (no
 * aud/token_use) and mobile_proxy_access tokens; rejects edition_access tokens.
 */
@Service
public class ProxyDelegatedTokenAuthenticator {

  public enum Failure {
    INVALID,
    EXPIRED,
    BLOCKED,
    WRONG_KIND
  }

  public record Result(String username, long expirationTimeMillis) {}

  public sealed interface Outcome {
    record Success(Result result) implements Outcome {}

    record PublicPrincipal(Result result) implements Outcome {}

    record Denied(Failure failure) implements Outcome {}
  }

  private final JsonWebTokenService jsonWebTokenService;
  private final UserRepository userRepository;
  private final UserApplicationAccessPolicy userApplicationAccessPolicy;

  public ProxyDelegatedTokenAuthenticator(
      JsonWebTokenService jsonWebTokenService,
      UserRepository userRepository,
      UserApplicationAccessPolicy userApplicationAccessPolicy) {
    this.jsonWebTokenService = jsonWebTokenService;
    this.userRepository = userRepository;
    this.userApplicationAccessPolicy = userApplicationAccessPolicy;
  }

  public Outcome authenticate(String bearerToken, Collection<String> requiredScopes) {
    return authenticate(bearerToken, requiredScopes, false);
  }

  /**
   * @param requireMobileProxy when true, only {@code mobile_proxy_access} tokens are accepted
   *     (MBTiles); legacy viewer proxy tokens are rejected.
   */
  public Outcome authenticate(
      String bearerToken, Collection<String> requiredScopes, boolean requireMobileProxy) {
    if (!StringUtils.hasText(bearerToken)) {
      return new Outcome.PublicPrincipal(new Result(SecurityConstants.PUBLIC_PRINCIPAL, 0L));
    }

    try {
      var claims = jsonWebTokenService.parseClaims(bearerToken);
      if (jsonWebTokenService.isEditionAccessToken(claims)) {
        return new Outcome.Denied(Failure.WRONG_KIND);
      }

      String username = claims.getSubject();
      if (!StringUtils.hasText(username)) {
        return new Outcome.Denied(Failure.INVALID);
      }

      if (userApplicationAccessPolicy.isBlockedAccount(username)) {
        return new Outcome.Denied(Failure.BLOCKED);
      }

      Date lastPasswordChange =
          userRepository.findByUsername(username).map(User::getLastPasswordChange).orElse(null);

      if (jsonWebTokenService.isMobileProxyAccessToken(claims)) {
        if (!jsonWebTokenService.validateMobileProxyAccessToken(
            bearerToken, lastPasswordChange, requiredScopes)) {
          return new Outcome.Denied(Failure.INVALID);
        }
        return new Outcome.Success(
            new Result(
                username, jsonWebTokenService.getExpirationDateFromToken(bearerToken).getTime()));
      }

      if (!requireMobileProxy
          && jsonWebTokenService.isLegacyProxyToken(claims)
          && jsonWebTokenService.validateLegacyProxyToken(bearerToken)) {
        return new Outcome.Success(
            new Result(
                username, jsonWebTokenService.getExpirationDateFromToken(bearerToken).getTime()));
      }

      return new Outcome.Denied(requireMobileProxy ? Failure.WRONG_KIND : Failure.INVALID);
    } catch (ExpiredJwtException e) {
      return new Outcome.Denied(Failure.EXPIRED);
    } catch (JwtException | IllegalArgumentException e) {
      return new Outcome.Denied(Failure.INVALID);
    }
  }

  public Optional<String> extractBearer(String authorizationHeader) {
    if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
      return Optional.empty();
    }
    String token = authorizationHeader.substring(7).trim();
    return StringUtils.hasText(token) ? Optional.of(token) : Optional.empty();
  }
}
