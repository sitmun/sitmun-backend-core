package org.sitmun.infrastructure.security.service;

import static org.sitmun.infrastructure.security.jwt.MobileJwtClaims.*;
import static org.sitmun.infrastructure.security.jwt.MobileTokenScopes.*;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.time.Clock;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JsonWebTokenService {

  @Value("${sitmun.user.secret}")
  private String secret;

  @Value("${sitmun.user.token-validity-in-milliseconds}")
  private int validity;

  @Value("${sitmun.mobile.token-validity-in-milliseconds:3600000}")
  private int mobileValidity;

  @Value("${sitmun.proxy-middleware.token-validity-in-milliseconds}")
  private int proxyValidity;

  private static final String LAST_PASSWORD_CHANGE = "lastPasswordChange";

  private final Clock clock;

  private SecretKey key;

  public JsonWebTokenService(Clock clock) {
    this.clock = clock;
  }

  public int getMobileTokenValidityMillis() {
    return mobileValidity;
  }

  public int getProxyTokenValidityMillis() {
    return proxyValidity;
  }

  public String generateToken(UserDetails userDetails, Date lastPasswordChange) {
    return generateToken(userDetails.getUsername(), now(), lastPasswordChange, validity);
  }

  public String generateToken(UserDetails userDetails) {
    return generateToken(userDetails.getUsername(), now(), null, validity);
  }

  public String generateToken(String username, Date date) {
    return generateToken(username, date, null, validity);
  }

  public String generateToken(String username, Date date, int validity) {
    return generateToken(username, date, null, validity);
  }

  public String generateToken(String username, Date date, Date lastPasswordChange, int validity) {
    long currentTimeMillis = date.getTime();
    JwtBuilder builder =
        Jwts.builder()
            .subject(username)
            .issuedAt(new Date(currentTimeMillis))
            .expiration(new Date(currentTimeMillis + validity))
            .signWith(key);

    if (lastPasswordChange != null) {
      builder.claim(LAST_PASSWORD_CHANGE, lastPasswordChange.toInstant().toEpochMilli());
    }
    return builder.compact();
  }

  public String generateEditionAccessToken(String username, Date lastPasswordChange) {
    return generateScopedToken(
        username,
        now(),
        lastPasswordChange,
        mobileValidity,
        AUDIENCE_MOBILE_API,
        TOKEN_USE_EDITION_ACCESS,
        EDITION_ACCESS);
  }

  public String generateMobileProxyToken(String username, Date lastPasswordChange) {
    return generateScopedToken(
        username,
        now(),
        lastPasswordChange,
        proxyValidity,
        AUDIENCE_PROXY,
        TOKEN_USE_MOBILE_PROXY_ACCESS,
        MOBILE_PROXY_ACCESS);
  }

  private String generateScopedToken(
      String username,
      Date issuedAt,
      Date lastPasswordChange,
      int validityMillis,
      String audience,
      String tokenUse,
      List<String> scopes) {
    long currentTimeMillis = issuedAt.getTime();
    JwtBuilder builder =
        Jwts.builder()
            .subject(username)
            .audience()
            .add(audience)
            .and()
            .issuedAt(new Date(currentTimeMillis))
            .expiration(new Date(currentTimeMillis + validityMillis))
            .claim(TOKEN_USE, tokenUse)
            .claim(APPLICATION_TYPES, List.of(APPLICATION_TYPE_EDITION))
            .claim(SCOPE, scopes)
            .signWith(key);

    if (lastPasswordChange != null) {
      builder.claim(LAST_PASSWORD_CHANGE, lastPasswordChange.toInstant().toEpochMilli());
    }
    return builder.compact();
  }

  public String getUsernameFromToken(String token) {
    return getClaimFromToken(token, Claims::getSubject);
  }

  public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = getAllClaimsFromToken(token);
    return claimsResolver.apply(claims);
  }

  public Date getExpirationDateFromToken(String token) {
    return getClaimFromToken(token, Claims::getExpiration);
  }

  public Claims parseClaims(String token) {
    return getAllClaimsFromToken(token);
  }

  public boolean validateToken(String token, UserDetails userDetails, Date lastPasswordChange) {
    if (token == null || token.isEmpty())
      throw new IllegalArgumentException("Token cannot be null or empty");
    if (userDetails == null) return false;

    String username = getUsernameFromToken(token);
    if (username == null || !username.equals(userDetails.getUsername()) || isTokenExpired(token)) {
      return false;
    }

    if (lastPasswordChange == null) return true;
    Long tokenTimestamp = getClaimFromToken(token, c -> c.get(LAST_PASSWORD_CHANGE, Long.class));
    return tokenTimestamp != null
        && tokenTimestamp.equals(lastPasswordChange.toInstant().toEpochMilli());
  }

  public boolean validateToken(String token, UserDetails userDetails) {
    return validateToken(token, userDetails, null);
  }

  public boolean isEditionAccessToken(Claims claims) {
    return TOKEN_USE_EDITION_ACCESS.equals(claims.get(TOKEN_USE, String.class))
        && hasAudience(claims, AUDIENCE_MOBILE_API);
  }

  public boolean isMobileProxyAccessToken(Claims claims) {
    return TOKEN_USE_MOBILE_PROXY_ACCESS.equals(claims.get(TOKEN_USE, String.class))
        && hasAudience(claims, AUDIENCE_PROXY);
  }

  public boolean isLegacyProxyToken(Claims claims) {
    return claims.get(TOKEN_USE, String.class) == null
        && (claims.getAudience() == null || claims.getAudience().isEmpty());
  }

  public boolean validateEditionAccessToken(String token, Date lastPasswordChange) {
    Claims claims = getAllClaimsFromToken(token);
    return isEditionAccessToken(claims)
        && !isTokenExpired(claims)
        && matchesPasswordChange(claims, lastPasswordChange)
        && hasAllScopes(claims, EDITION_ACCESS)
        && hasApplicationTypeEdition(claims);
  }

  public boolean validateMobileProxyAccessToken(
      String token, Date lastPasswordChange, Collection<String> requiredScopes) {
    Claims claims = getAllClaimsFromToken(token);
    return isMobileProxyAccessToken(claims)
        && !isTokenExpired(claims)
        && matchesPasswordChange(claims, lastPasswordChange)
        && hasApplicationTypeEdition(claims)
        && (requiredScopes == null
            || requiredScopes.isEmpty()
            || hasAllScopes(claims, requiredScopes));
  }

  public boolean validateLegacyProxyToken(String token) {
    Claims claims = getAllClaimsFromToken(token);
    return isLegacyProxyToken(claims)
        && claims.getSubject() != null
        && !claims.getSubject().isBlank()
        && !isTokenExpired(claims);
  }

  public List<String> getScopes(Claims claims) {
    Object raw = claims.get(SCOPE);
    if (raw instanceof Collection<?> collection) {
      return collection.stream().map(String::valueOf).toList();
    }
    return List.of();
  }

  private boolean hasAllScopes(Claims claims, Collection<String> required) {
    Set<String> present = Set.copyOf(getScopes(claims));
    return present.containsAll(required);
  }

  private static boolean hasApplicationTypeEdition(Claims claims) {
    Object raw = claims.get(APPLICATION_TYPES);
    if (raw instanceof Collection<?> collection) {
      return collection.stream().map(String::valueOf).anyMatch(APPLICATION_TYPE_EDITION::equals);
    }
    return false;
  }

  private static boolean hasAudience(Claims claims, String expected) {
    Set<String> audience = claims.getAudience();
    return audience != null && audience.contains(expected);
  }

  private boolean matchesPasswordChange(Claims claims, Date lastPasswordChange) {
    if (lastPasswordChange == null) {
      return true;
    }
    Long tokenTimestamp = claims.get(LAST_PASSWORD_CHANGE, Long.class);
    return tokenTimestamp != null
        && tokenTimestamp.equals(lastPasswordChange.toInstant().toEpochMilli());
  }

  private @NotNull Boolean isTokenExpired(String token) {
    return isTokenExpired(getAllClaimsFromToken(token));
  }

  private boolean isTokenExpired(Claims claims) {
    Date expiration = claims.getExpiration();
    return expiration == null || expiration.before(now());
  }

  private Claims getAllClaimsFromToken(String token) {
    return Jwts.parser()
        .verifyWith(key)
        .clock(() -> Date.from(clock.instant()))
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  private Date now() {
    return Date.from(clock.instant());
  }

  @PostConstruct
  private void buildKey() {
    byte[] keyBytes = secret.getBytes();
    key = Keys.hmacShaKeyFor(keyBytes);
  }
}
