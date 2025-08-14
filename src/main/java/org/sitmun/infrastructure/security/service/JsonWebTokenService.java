package org.sitmun.infrastructure.security.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.util.Date;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JsonWebTokenService {

  @Value("${sitmun.user.secret}")
  private String secret;

  @Value("${sitmun.user.token-validity-in-milliseconds}")
  private int validity;

  private final String LAST_PASSWORD_CHANGE = "lastPasswordChange";

  private SecretKey key;

  public String generateToken(UserDetails userDetails, Date lastPasswordChange) {
    return generateToken(userDetails.getUsername(), new Date(), lastPasswordChange);
  }

  public String generateToken(String username, Date date, Date lastPasswordChange) {

    long currentTimeMillis = date.getTime();
    JwtBuilder builder = Jwts.builder()
      .subject(username)
      .issuedAt(new Date(currentTimeMillis))
      .expiration(new Date(currentTimeMillis + validity))
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

  public boolean validateToken(String token, UserDetails userDetails, Date lastPasswordChange) {
    if (token == null || userDetails == null) return false;

    String username = getUsernameFromToken(token);
    if (username == null || !username.equals(userDetails.getUsername()) || isTokenExpired(token)) {
      return false;
    }

    if (lastPasswordChange == null) return true;
    Long tokenTimestamp = getClaimFromToken(token, c -> c.get(LAST_PASSWORD_CHANGE, Long.class));
    return tokenTimestamp != null
      && tokenTimestamp.equals(lastPasswordChange.toInstant().toEpochMilli());
  }


  private Boolean isTokenExpired(String token) {
    final Date expiration = getExpirationDateFromToken(token);
    return expiration.before(new Date());
  }

  private Claims getAllClaimsFromToken(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }

  @PostConstruct
  private void buildKey() {
    byte[] keyBytes = secret.getBytes();
    key = Keys.hmacShaKeyFor(keyBytes);
  }
}
