package org.sitmun.domain.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@DisplayName("ApplicationPointOfContactValidation")
class ApplicationPointOfContactValidationTest {

  @Autowired private ApplicationPointOfContactValidation validation;

  @Autowired private ApplicationRepository applicationRepository;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName("null incoming creator is always valid")
  @WithMockUser(roles = "ADMIN")
  @Transactional
  void nullCreatorAlwaysValid() {
    assertThat(validation.isValidCreatorChange(null, null)).isTrue();
    assertThat(validation.isValidCreatorChange(1, null)).isTrue();
  }

  @Test
  @DisplayName("preserving existing persisted creator is valid even if now ineligible")
  @WithMockUser(roles = "ADMIN")
  @Transactional
  void preservingExistingCreatorAllowed() {
    // Use the built-in public user id from the database
    Integer publicUserId =
        jdbcTemplate.queryForObject(
            "SELECT USE_ID FROM STM_USER WHERE USE_USER = 'public'", Integer.class);
    assertThat(publicUserId).isNotNull();

    // Insert an app with APP_CREATORID = publicUserId
    jdbcTemplate.update(
        "INSERT INTO STM_APP (APP_ID, APP_NAME, APP_TYPE, APP_PRIVATE, APP_CREATORID) "
            + "VALUES (99999, 'test-poc', 'I', FALSE, ?)",
        publicUserId);

    User publicUser =
        User.builder()
            .id(publicUserId)
            .username("public")
            .blocked(false)
            .administrator(false)
            .build();
    // Even though public is ineligible, same ID → allowed
    assertThat(validation.isValidCreatorChange(99999, publicUser)).isTrue();

    jdbcTemplate.update("DELETE FROM STM_APP WHERE APP_ID = 99999");
  }

  @Test
  @DisplayName("replacing with eligible user is valid")
  @WithMockUser(roles = "ADMIN")
  @Transactional
  void replacingWithEligibleUserIsValid() {
    Integer adminId =
        jdbcTemplate.queryForObject(
            "SELECT USE_ID FROM STM_USER WHERE USE_USER = 'admin'", Integer.class);
    assertThat(adminId).isNotNull();

    jdbcTemplate.update(
        "INSERT INTO STM_APP (APP_ID, APP_NAME, APP_TYPE, APP_PRIVATE, APP_CREATORID) "
            + "VALUES (99998, 'test-poc-2', 'I', FALSE, ?)",
        adminId);

    // Replace admin (built-in) with a new ordinary user
    User ordinary =
        User.builder()
            .id(adminId + 1000)
            .username("ordinary")
            .blocked(false)
            .administrator(true)
            .build();
    // New ordinary user is eligible
    assertThat(validation.isValidCreatorChange(99998, ordinary)).isTrue();

    jdbcTemplate.update("DELETE FROM STM_APP WHERE APP_ID = 99998");
  }

  @Test
  @DisplayName("replacing with ineligible user is invalid")
  @WithMockUser(roles = "ADMIN")
  @Transactional
  void replacingWithIneligibleUserIsInvalid() {
    Integer adminId =
        jdbcTemplate.queryForObject(
            "SELECT USE_ID FROM STM_USER WHERE USE_USER = 'admin'", Integer.class);
    assertThat(adminId).isNotNull();

    jdbcTemplate.update(
        "INSERT INTO STM_APP (APP_ID, APP_NAME, APP_TYPE, APP_PRIVATE, APP_CREATORID) "
            + "VALUES (99997, 'test-poc-3', 'I', FALSE, ?)",
        adminId);

    // Try to set built-in public as new creator (ineligible)
    Integer publicId =
        jdbcTemplate.queryForObject(
            "SELECT USE_ID FROM STM_USER WHERE USE_USER = 'public'", Integer.class);
    User publicUser =
        User.builder().id(publicId).username("public").blocked(false).administrator(false).build();

    assertThat(validation.isValidCreatorChange(99997, publicUser)).isFalse();

    jdbcTemplate.update("DELETE FROM STM_APP WHERE APP_ID = 99997");
  }
}
