package org.sitmun.infrastructure.security.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.sitmun.infrastructure.security.jwt.MobileJwtClaims.*;
import static org.sitmun.infrastructure.security.jwt.MobileTokenScopes.*;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.infrastructure.config.Profiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles(Profiles.TEST)
@Import(JsonWebTokenServiceTest.FixedClockConfig.class)
@DisplayName("JsonWebTokenService Tests")
class JsonWebTokenServiceTest {

  private static final Instant FIXED_NOW = Instant.parse("2026-07-18T12:00:00Z");

  @TestConfiguration
  static class FixedClockConfig {
    @Bean
    @Primary
    Clock fixedClock() {
      return Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    }
  }

  @Autowired private JsonWebTokenService jsonWebTokenService;

  private UserDetails testUser;
  private String validToken;
  private String expiredToken;
  private String futureToken;
  private Date lastPasswordChange;

  @BeforeEach
  void setUp() {
    testUser =
        User.builder().username("testuser").password("password").authorities("ROLE_USER").build();
    lastPasswordChange = Date.from(FIXED_NOW.minusSeconds(3600));

    // Generate valid token
    validToken = jsonWebTokenService.generateToken(testUser);

    // Generate expired token
    Date expiredDate =
        Date.from(LocalDate.parse("1900-01-01").atStartOfDay(ZoneId.systemDefault()).toInstant());
    expiredToken = jsonWebTokenService.generateToken("testuser", expiredDate);

    // Generate future token
    Date futureDate =
        Date.from(LocalDate.parse("2100-01-01").atStartOfDay(ZoneId.systemDefault()).toInstant());
    futureToken = jsonWebTokenService.generateToken("testuser", futureDate);
  }

  @Test
  @DisplayName("Should generate token from UserDetails")
  void generateTokenFromUserDetails() {
    // When
    String token = jsonWebTokenService.generateToken(testUser);

    // Then
    assertNotNull(token);
    assertFalse(token.isEmpty());
    assertEquals("testuser", jsonWebTokenService.getUsernameFromToken(token));
  }

  @Test
  @DisplayName("Should generate token from username and date")
  void generateTokenFromUsernameAndDate() {
    // Given
    String username = "testuser";
    Date date = new Date();

    // When
    String token = jsonWebTokenService.generateToken(username, date);

    // Then
    assertNotNull(token);
    assertFalse(token.isEmpty());
    assertEquals(username, jsonWebTokenService.getUsernameFromToken(token));
  }

  @Test
  @DisplayName("Should extract username from valid token")
  void getUsernameFromValidToken() {
    // When
    String username = jsonWebTokenService.getUsernameFromToken(validToken);

    // Then
    assertEquals("testuser", username);
  }

  @Test
  @DisplayName("Should extract username from future token")
  void getUsernameFromFutureToken() {
    // When
    String username = jsonWebTokenService.getUsernameFromToken(futureToken);

    // Then
    assertEquals("testuser", username);
  }

  @Test
  @DisplayName("Should throw exception when extracting username from invalid token")
  void getUsernameFromInvalidToken() {
    // Given
    String invalidToken = "invalid.token.here";

    // When & Then
    assertThrows(
        MalformedJwtException.class, () -> jsonWebTokenService.getUsernameFromToken(invalidToken));
  }

  @Test
  @DisplayName("Should extract claim from token")
  void getClaimFromToken() {
    // Given
    Function<Claims, String> subjectResolver = Claims::getSubject;
    Function<Claims, Date> issuedAtResolver = Claims::getIssuedAt;
    Function<Claims, Date> expirationResolver = Claims::getExpiration;

    // When
    String subject = jsonWebTokenService.getClaimFromToken(validToken, subjectResolver);
    Date issuedAt = jsonWebTokenService.getClaimFromToken(validToken, issuedAtResolver);
    Date expiration = jsonWebTokenService.getClaimFromToken(validToken, expirationResolver);

    // Then
    assertEquals("testuser", subject);
    assertNotNull(issuedAt);
    assertNotNull(expiration);
    assertTrue(expiration.after(issuedAt));
  }

  @Test
  @DisplayName("Should extract expiration date from token")
  void getExpirationDateFromToken() {
    // When
    Date expiration = jsonWebTokenService.getExpirationDateFromToken(validToken);

    // Then
    assertNotNull(expiration);
    assertTrue(expiration.after(new Date()));
  }

  @Test
  @DisplayName("Should throw exception when extracting expiration date from expired token")
  void getExpirationDateFromExpiredToken() {
    // When & Then
    assertThrows(
        ExpiredJwtException.class,
        () -> jsonWebTokenService.getExpirationDateFromToken(expiredToken));
  }

  @Test
  @DisplayName("Should validate token with correct user details")
  void validateTokenWithCorrectUserDetails() {
    // When
    boolean isValid = jsonWebTokenService.validateToken(validToken, testUser);

    // Then
    assertTrue(isValid);
  }

  @Test
  @DisplayName("Should validate future token with correct user details")
  void validateFutureTokenWithCorrectUserDetails() {
    // When
    boolean isValid = jsonWebTokenService.validateToken(futureToken, testUser);

    // Then
    assertTrue(isValid);
  }

  @Test
  @DisplayName("Should not validate expired token")
  void validateExpiredToken() {
    // When & Then
    assertThrows(
        ExpiredJwtException.class, () -> jsonWebTokenService.validateToken(expiredToken, testUser));
  }

  @Test
  @DisplayName("Should not validate token with wrong username")
  void validateTokenWithWrongUsername() {
    // Given
    UserDetails wrongUser =
        User.builder().username("wronguser").password("password").authorities("ROLE_USER").build();

    // When
    boolean isValid = jsonWebTokenService.validateToken(validToken, wrongUser);

    // Then
    assertFalse(isValid);
  }

  @Test
  @DisplayName("Should not validate invalid token")
  void validateInvalidToken() {
    // Given
    String invalidToken = "invalid.token.here";

    // When & Then
    assertThrows(
        MalformedJwtException.class,
        () -> jsonWebTokenService.validateToken(invalidToken, testUser));
  }

  @Test
  @DisplayName("Should not validate null token")
  void validateNullToken() {
    // When & Then
    assertThrows(
        IllegalArgumentException.class, () -> jsonWebTokenService.validateToken(null, testUser));
  }

  @Test
  @DisplayName("Should not validate empty token")
  void validateEmptyToken() {
    // When & Then
    assertThrows(
        IllegalArgumentException.class, () -> jsonWebTokenService.validateToken("", testUser));
  }

  @Test
  @DisplayName("Should handle token with different username case")
  void handleTokenWithDifferentUsernameCase() {
    // Given
    UserDetails upperCaseUser =
        User.builder().username("TESTUSER").password("password").authorities("ROLE_USER").build();

    // When
    boolean isValid = jsonWebTokenService.validateToken(validToken, upperCaseUser);

    // Then
    assertFalse(isValid);
  }

  @Test
  @DisplayName("Should generate tokens with different usernames")
  void generateTokensWithDifferentUsernames() {
    // Given
    String username1 = "user1";
    String username2 = "user2";
    Date date = new Date();

    // When
    String token1 = jsonWebTokenService.generateToken(username1, date);
    String token2 = jsonWebTokenService.generateToken(username2, date);

    // Then
    assertNotEquals(token1, token2);
    assertEquals(username1, jsonWebTokenService.getUsernameFromToken(token1));
    assertEquals(username2, jsonWebTokenService.getUsernameFromToken(token2));
  }

  @Test
  @DisplayName("Should generate tokens with different dates")
  void generateTokensWithDifferentDates() {
    // Given
    String username = "testuser";
    Date date1 = new Date();
    Date date2 = new Date(date1.getTime() + 1000); // 1 second later

    // When
    String token1 = jsonWebTokenService.generateToken(username, date1);
    String token2 = jsonWebTokenService.generateToken(username, date2);

    // Then
    assertNotEquals(token1, token2);
    assertEquals(username, jsonWebTokenService.getUsernameFromToken(token1));
    assertEquals(username, jsonWebTokenService.getUsernameFromToken(token2));
  }

  @Test
  @DisplayName("Should handle malformed JWT token")
  void handleMalformedJwtToken() {
    // Given
    String malformedToken = "not.a.valid.jwt.token";

    // When & Then
    assertThrows(
        MalformedJwtException.class,
        () -> jsonWebTokenService.getUsernameFromToken(malformedToken));
  }

  @Test
  @DisplayName("Should handle unsupported JWT token")
  void handleUnsupportedJwtToken() {
    // Given
    String unsupportedToken = "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJzdWIiOiJ0ZXN0dXNlciJ9.";

    // When & Then
    assertThrows(
        UnsupportedJwtException.class,
        () -> jsonWebTokenService.getUsernameFromToken(unsupportedToken));
  }

  @Test
  @DisplayName("Should throw exception when extracting claim from expired token")
  void handleExpiredJwtTokenForClaimExtraction() {
    // Given
    Function<Claims, String> subjectResolver = Claims::getSubject;

    // When & Then
    assertThrows(
        ExpiredJwtException.class,
        () -> jsonWebTokenService.getClaimFromToken(expiredToken, subjectResolver));
  }

  @Test
  @DisplayName("Should throw exception when extracting expiration from expired token")
  void handleExpiredJwtTokenForExpirationExtraction() {
    // When & Then
    assertThrows(
        ExpiredJwtException.class,
        () -> jsonWebTokenService.getExpirationDateFromToken(expiredToken));
  }

  @Test
  @DisplayName("Edition access token has expected audience, use, types and scopes")
  void editionAccessTokenClaims() {
    String token =
        jsonWebTokenService.generateEditionAccessToken("edition-user", lastPasswordChange);
    Claims claims = jsonWebTokenService.parseClaims(token);

    assertThat(claims.getSubject()).isEqualTo("edition-user");
    assertThat(claims.getAudience()).containsExactly(AUDIENCE_MOBILE_API);
    assertThat(claims.get(TOKEN_USE, String.class)).isEqualTo(TOKEN_USE_EDITION_ACCESS);
    assertThat(claims.get(APPLICATION_TYPES, List.class)).containsExactly(APPLICATION_TYPE_EDITION);
    assertThat(jsonWebTokenService.getScopes(claims))
        .containsExactlyInAnyOrderElementsOf(EDITION_ACCESS);
    assertThat(claims.getIssuedAt()).isEqualTo(Date.from(FIXED_NOW));
    assertThat(claims.getExpiration())
        .isEqualTo(
            Date.from(FIXED_NOW.plusMillis(jsonWebTokenService.getMobileTokenValidityMillis())));
    assertThat(jsonWebTokenService.validateEditionAccessToken(token, lastPasswordChange)).isTrue();
  }

  @Test
  @DisplayName("Mobile proxy token has expected audience, use, types and scopes")
  void mobileProxyTokenClaims() {
    String token = jsonWebTokenService.generateMobileProxyToken("edition-user", lastPasswordChange);
    Claims claims = jsonWebTokenService.parseClaims(token);

    assertThat(claims.getSubject()).isEqualTo("edition-user");
    assertThat(claims.getAudience()).containsExactly(AUDIENCE_PROXY);
    assertThat(claims.get(TOKEN_USE, String.class)).isEqualTo(TOKEN_USE_MOBILE_PROXY_ACCESS);
    assertThat(jsonWebTokenService.getScopes(claims))
        .containsExactlyInAnyOrderElementsOf(MOBILE_PROXY_ACCESS);
    assertThat(
            jsonWebTokenService.validateMobileProxyAccessToken(
                token, lastPasswordChange, List.of(MBTILES_ESTIMATE)))
        .isTrue();
  }

  @Test
  @DisplayName("Edition access validation rejects wrong password change")
  void editionAccessRejectsPasswordChangeMismatch() {
    String token =
        jsonWebTokenService.generateEditionAccessToken("edition-user", lastPasswordChange);
    Date otherChange = Date.from(FIXED_NOW.minusSeconds(10));
    assertThat(jsonWebTokenService.validateEditionAccessToken(token, otherChange)).isFalse();
  }

  @Test
  @DisplayName("Legacy proxy tokens remain valid without audience claims")
  void legacyProxyTokenRemainsValid() {
    String token = jsonWebTokenService.generateToken("admin", Date.from(FIXED_NOW), 900_000);
    Claims claims = jsonWebTokenService.parseClaims(token);
    assertThat(jsonWebTokenService.isLegacyProxyToken(claims)).isTrue();
    assertThat(jsonWebTokenService.validateLegacyProxyToken(token)).isTrue();
    assertThat(jsonWebTokenService.isEditionAccessToken(claims)).isFalse();
    assertThat(jsonWebTokenService.isMobileProxyAccessToken(claims)).isFalse();
  }
}
