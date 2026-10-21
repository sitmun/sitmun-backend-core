package org.sitmun.domain.application;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.util.Date;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.administration.config.AdministrationRestConfigurer;
import org.sitmun.infrastructure.config.Profiles;
import org.sitmun.test.AdditiveActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@DataJpaTest
@DisplayName("Application Default Values Test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AdditiveActiveProfiles(Profiles.TEST)
@Import(AdministrationRestConfigurer.class)
class ApplicationDefaultValuesTest {

  @Autowired private ApplicationRepository applicationRepository;
  @Autowired private EntityManager entityManager;

  private Application application;
  private Application savedApplication;

  @BeforeEach
  void setUp() {
    application =
        Application.builder()
            .name("Test Application")
            .type("I")
            .title("Test Application Title")
            .build();
    savedApplication = null;
  }

  @AfterEach
  void tearDown() {
    // Clean up any applications created during the test
    if (savedApplication != null && savedApplication.getId() != null) {
      applicationRepository.deleteById(savedApplication.getId());
    }
  }

  /** Detached so a later findById reads the row instead of this instance. */
  private Application saveAndTrackApplication(Application app) {
    savedApplication = applicationRepository.save(app);
    entityManager.flush();
    entityManager.detach(savedApplication);
    return savedApplication;
  }

  /**
   * Helper method to validate default boolean values for an application.
   *
   * @param app The application to validate
   */
  private void validateDefaultBooleanValues(Application app) {
    assertThat(app.getTreeAutoRefresh()).isTrue();
    assertThat(app.getAccessParentTerritory()).isFalse();
    assertThat(app.getAccessChildrenTerritory()).isFalse();
    assertThat(app.getIsUnavailable()).isFalse();
    assertThat(app.getAppPrivate()).isFalse();
  }

  /**
   * Helper method to validate default header parameters structure.
   *
   * @param app The application to validate
   */
  private void validateDefaultHeaderParameters(Application app) {
    assertThat(app.getHeaderParams()).isNotNull();

    Map<String, Object> headerParams = app.getHeaderParams();

    assertThat(headerParams).containsKey("headerLeftSection");
    @SuppressWarnings("unchecked")
    Map<String, Object> leftSection = (Map<String, Object>) headerParams.get("headerLeftSection");
    assertThat(leftSection).containsKey("logoSitmun");

    @SuppressWarnings("unchecked")
    Map<String, Object> logoSitmun = (Map<String, Object>) leftSection.get("logoSitmun");
    assertThat(logoSitmun).containsEntry("visible", true);

    assertThat(headerParams).containsKey("headerRightSection");
    @SuppressWarnings("unchecked")
    Map<String, Object> rightSection = (Map<String, Object>) headerParams.get("headerRightSection");
    assertThat(rightSection).containsKey("switchApplication");
    assertThat(rightSection).containsKey("homeMenu");
    assertThat(rightSection).containsKey("switchLanguage");
    assertThat(rightSection).containsKey("profileButton");
    assertThat(rightSection).containsKey("logoutButton");

    assertThat(rightSection.get("switchApplication")).isInstanceOf(Map.class);
    assertThat(rightSection.get("homeMenu")).isInstanceOf(Map.class);
    assertThat(rightSection.get("switchLanguage")).isInstanceOf(Map.class);
    assertThat(rightSection.get("profileButton")).isInstanceOf(Map.class);
    assertThat(rightSection.get("logoutButton")).isInstanceOf(Map.class);

    @SuppressWarnings("unchecked")
    Map<String, Object> switchApplication =
        (Map<String, Object>) rightSection.get("switchApplication");
    assertThat(switchApplication).containsEntry("visible", true);

    @SuppressWarnings("unchecked")
    Map<String, Object> homeMenu = (Map<String, Object>) rightSection.get("homeMenu");
    assertThat(homeMenu).containsEntry("visible", true);

    @SuppressWarnings("unchecked")
    Map<String, Object> switchLanguage = (Map<String, Object>) rightSection.get("switchLanguage");
    assertThat(switchLanguage).containsEntry("visible", true);

    @SuppressWarnings("unchecked")
    Map<String, Object> profileButton = (Map<String, Object>) rightSection.get("profileButton");
    assertThat(profileButton).containsEntry("visible", true);

    @SuppressWarnings("unchecked")
    Map<String, Object> logoutButton = (Map<String, Object>) rightSection.get("logoutButton");
    assertThat(logoutButton).containsEntry("visible", true);
  }

  /**
   * Helper method to validate that collections are empty by default.
   *
   * @param app The application to validate
   */
  private void validateEmptyCollections(Application app) {
    assertThat(app.getParameters()).isNotNull();
    assertThat(app.getParameters()).isEmpty();

    assertThat(app.getAvailableRoles()).isNotNull();
    assertThat(app.getAvailableRoles()).isEmpty();

    assertThat(app.getTrees()).isNotNull();
    assertThat(app.getTrees()).isEmpty();

    assertThat(app.getBackgrounds()).isNotNull();
    assertThat(app.getBackgrounds()).isEmpty();

    assertThat(app.getTerritories()).isNotNull();
    assertThat(app.getTerritories()).isEmpty();
  }

  /**
   * Helper method to validate that date fields are set when saved.
   *
   * @param app The application to validate
   */
  private void validateDateFields(Application app) {
    assertThat(app.getCreatedDate()).isNotNull();
    assertThat(app.getCreatedDate()).isInstanceOf(Date.class);

    assertThat(app.getLastUpdate()).isNotNull();
    assertThat(app.getLastUpdate()).isInstanceOf(Date.class);
  }

  @Test
  @DisplayName("A stored null header map stays null")
  void handlesNullHeaderParameters() {
    // Given
    application.setHeaderParams(null);

    // When
    Application savedApp = saveAndTrackApplication(application);
    Application retrievedApplication =
        applicationRepository.findById(savedApp.getId()).orElse(null);

    // Then
    assertThat(retrievedApplication).isNotNull();
    assertThat(retrievedApplication.getHeaderParams()).isNull();
  }

  @Test
  @DisplayName(
      "A created application stores default booleans, headers, empty collections, and dates")
  void hasAllDefaultValuesWhenCreated() {
    // When
    Application savedApp = saveAndTrackApplication(application);
    Application reloaded = applicationRepository.findById(savedApp.getId()).orElseThrow();

    validateDefaultBooleanValues(reloaded);
    validateDefaultHeaderParameters(reloaded);
    validateEmptyCollections(reloaded);
    validateDateFields(reloaded);
  }
}
