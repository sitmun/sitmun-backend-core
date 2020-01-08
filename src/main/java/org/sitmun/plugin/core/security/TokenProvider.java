package org.sitmun.plugin.core.security;

import static org.sitmun.plugin.core.security.SecurityConstants.TOKEN_PREFIX;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

@Component
public class TokenProvider {

  private static final String AUTHORITIES_KEY = "auth";
  private final Logger log = LoggerFactory.getLogger(TokenProvider.class);
  @Value("${security.authentication.jwt.secret}")
  private String secretKey;
  @Value("${security.authentication.jwt.token-validity-in-miliseconds}")
  private long tokenValidityInMilliseconds;

  public String getSecretKey() {
    return secretKey;
  }

  public long getTokenValidityInMilliseconds() {
    return tokenValidityInMilliseconds;
  }

  public Authentication getAuthentication(String token) {
    Claims claims = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody();
    Collection<? extends GrantedAuthority> authorities = new ArrayList<>();
    /*
     * Collection<? extends GrantedAuthority> authorities =
     * Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
     * .map(SimpleGrantedAuthority::new) .collect(Collectors.toList());
     */
    User principal = new User(claims.getSubject(), "", authorities);

    return new UsernamePasswordAuthenticationToken(principal, token, authorities);
  }

  public boolean validateToken(String authToken) {
    try {
      Jwts.parser().setSigningKey(secretKey).parseClaimsJws(authToken);
      return true;
    } catch (SignatureException e) {
      log.info("Invalid JWT signature.");
      log.trace("Invalid JWT signature trace: {}", e);
    } catch (MalformedJwtException e) {
      log.info("Invalid JWT token.");
      log.trace("Invalid JWT token trace: {}", e);
    } catch (ExpiredJwtException e) {
      log.info("Expired JWT token.");
      log.trace("Expired JWT token trace: {}", e);
    } catch (UnsupportedJwtException e) {
      log.info("Unsupported JWT token.");
      log.trace("Unsupported JWT token trace: {}", e);
    } catch (IllegalArgumentException e) {
      log.info("JWT token compact of handler are invalid.");
      log.trace("JWT token compact of handler are invalid trace: {}", e);
    }
    return false;
  }

  public String createToken(Authentication authentication) {
    /*
     * String authorities = authentication.getAuthorities().stream()
     * .map(GrantedAuthority::getAuthority) .collect(Collectors.joining(","));
     */
    long now = (new Date()).getTime();
    Date validity;

    validity = new Date(now + this.tokenValidityInMilliseconds);

    return Jwts.builder().setSubject(authentication.getName())
               // .claim(AUTHORITIES_KEY, authorities)
               .signWith(SignatureAlgorithm.HS512, secretKey).setExpiration(validity).compact();
  }

  public String createToken(String username) {

    long now = (new Date()).getTime();
    Date validity;

    validity = new Date(now + this.tokenValidityInMilliseconds);

    return Jwts.builder().setSubject(username)

               // .claim(AUTHORITIES_KEY, "")
               .signWith(SignatureAlgorithm.HS512, secretKey).setExpiration(validity).compact();
  }

  public String getUserFromToken(String token) {
    try {
      return Jwts.parser().setSigningKey(secretKey.getBytes()).parseClaimsJws(token.replace(TOKEN_PREFIX, ""))
                 .getBody().getSubject();
    } catch (Exception ex) {
      return null;
    }
  }
}
