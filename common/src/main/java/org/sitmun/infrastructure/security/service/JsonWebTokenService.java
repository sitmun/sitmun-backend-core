package org.sitmun.infrastructure.security.service;

import io.jsonwebtoken.*;
import org.sitmun.infrastructure.security.core.userdetails.UserDetailsImplementation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class JsonWebTokenService {
  private static final Logger logger = LoggerFactory.getLogger(JsonWebTokenService.class);

  @Value("${security.authentication.jwt.secret}")
  private String jwtSecret;

  @Value("${security.authentication.jwt.token-validity-in-miliseconds}")
  private int jwtExpirationMs;

  public String generateToken(String userName, Date date) {
    return Jwts.builder()
      .setSubject(userName)
      .setIssuedAt(date)
      .setExpiration(new Date(date.getTime() + jwtExpirationMs))
      .signWith(SignatureAlgorithm.HS512, jwtSecret)
      .compact();
  }

  public String generateToken(Authentication authentication) {

    UserDetailsImplementation userPrincipal = (UserDetailsImplementation) authentication.getPrincipal();

    return generateToken(userPrincipal.getUsername(), new Date());
  }

  public String getUserNameFromJwtToken(String token) {
    return Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token).getBody().getSubject();
  }

  public boolean validateJwtToken(String authToken) {
    try {
      Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(authToken);
      return true;
    } catch (SignatureException e) {
      logger.error("Invalid JWT signature: {}", e.getMessage());
    } catch (MalformedJwtException e) {
      logger.error("Invalid JWT token: {}", e.getMessage());
    } catch (ExpiredJwtException e) {
      logger.error("JWT token is expired: {}", e.getMessage());
    } catch (UnsupportedJwtException e) {
      logger.error("JWT token is unsupported: {}", e.getMessage());
    } catch (IllegalArgumentException e) {
      logger.error("JWT claims string is empty: {}", e.getMessage());
    }

    return false;
  }
}