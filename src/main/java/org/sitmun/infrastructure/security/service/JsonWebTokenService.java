package org.sitmun.infrastructure.security.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Service
public class JsonWebTokenService {

  @Value("${security.authentication.jwt.secret}")
  private String secret;

  @Value("${security.authentication.jwt.token-validity-in-miliseconds}")
  private int validity;

  private Key key;

  public String generateToken(UserDetails userDetails) {
    return generateToken(userDetails.getUsername(), new Date());
  }

  public String generateToken(String username, Date date) {
    long currentTimeMillis = date.getTime();
    return Jwts.builder()
      .setSubject(username)
      .setIssuedAt(new Date(currentTimeMillis))
      .setExpiration(new Date(currentTimeMillis + validity))
      .signWith(key)
      .compact();
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

  public boolean validateToken(String token, UserDetails userDetails) {
    final String username = getUsernameFromToken(token);
    return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
  }

  private Boolean isTokenExpired(String token) {
    final Date expiration = getExpirationDateFromToken(token);
    return expiration.before(new Date());
  }

  private Claims getAllClaimsFromToken(String token) {
    return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
  }

  @PostConstruct
  private void buildKey() {
    byte[] keyBytes = secret.getBytes();
    key = Keys.hmacShaKeyFor(keyBytes);
  }
}