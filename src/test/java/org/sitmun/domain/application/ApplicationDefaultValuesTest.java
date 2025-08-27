package org.sitmun.domain.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.infrastructure.persistence.type.map.HashMapConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.sitmun.administration.config.AdministrationRestConfigurer;

@DataJpaTest
@DisplayName("Application Default Values Test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(AdministrationRestConfigurer.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ApplicationDefaultValuesTest {

  @Autowired
  private ApplicationRepository applicationRepository;

  private Application application;
  private Application savedApplication;
  private HashMapConverter hashMapConverter;

  @BeforeEach
  void setUp() {
    hashMapConverter = new HashMapConverter();

    application = Application.builder()
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
      try {
        applicationRepository.deleteById(savedApplication.getId());
      } catch (Exception e) {
        // Ignore deletion errors as the database might be reset by @DirtiesContext
      }
    }
  }

  /**
   * Helper method to save and track an application for cleanup.
   * 
   * @param app The application to save
   * @return The saved application
   */
  private Application saveAndTrackApplication(Application app) {
    savedApplication = applicationRepository.save(app);
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
    
    // Check headerLeftSection
    assertThat(headerParams).containsKey("headerLeftSection");
    @SuppressWarnings("unchecked")
    Map<String, Object> leftSection = (Map<String, Object>) headerParams.get("headerLeftSection");
    assertThat(leftSection).containsKey("logoSitmun");

    // Check logoSitmun
    @SuppressWarnings("unchecked")
    Map<String, Object> logoSitmun = (Map<String, Object>) leftSection.get("logoSitmun");
    assertThat(logoSitmun).containsEntry("visible", true);

    // Check headerRightSection
    assertThat(headerParams).containsKey("headerRightSection");
    @SuppressWarnings("unchecked")
    Map<String, Object> rightSection = (Map<String, Object>) headerParams.get("headerRightSection");
    assertThat(rightSection).containsKey("switchApplication");
    assertThat(rightSection).containsKey("homeMenu");
    assertThat(rightSection).containsKey("switchLanguage");
    assertThat(rightSection).containsKey("profileButton");
    assertThat(rightSection).containsKey("logoutButton");

    // Check that all right section elements are visible by default
    assertThat(rightSection.get("switchApplication")).isInstanceOf(Map.class);
    assertThat(rightSection.get("homeMenu")).isInstanceOf(Map.class);
    assertThat(rightSection.get("switchLanguage")).isInstanceOf(Map.class);
    assertThat(rightSection.get("profileButton")).isInstanceOf(Map.class);
    assertThat(rightSection.get("logoutButton")).isInstanceOf(Map.class);
    
    @SuppressWarnings("unchecked")
    Map<String, Object> switchApplication = (Map<String, Object>) rightSection.get("switchApplication");
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
  @DisplayName("Should have default values for boolean fields when created")
  void shouldHaveDefaultValuesForBooleanFields() {
    // When
    Application savedApp = saveAndTrackApplication(application);
    
    // Then
    validateDefaultBooleanValues(savedApp);
  }

  @Test
  @DisplayName("Should have default header parameters when created")
  void shouldHaveDefaultHeaderParameters() {
    // When
    Application savedApp = saveAndTrackApplication(application);
    
    // Then
    validateDefaultHeaderParameters(savedApp);
  }

  @Test
  @DisplayName("Should persist and retrieve custom header parameters")
  void shouldPersistAndRetrieveCustomHeaderParameters() {
    // Given
    Map<String, Object> customHeaderParams = new HashMap<>();
    customHeaderParams.put("customKey", "customValue");
    customHeaderParams.put("numericValue", 42);
    customHeaderParams.put("booleanValue", true);
    
    Map<String, Object> nestedObject = new HashMap<>();
    nestedObject.put("nestedKey", "nestedValue");
    customHeaderParams.put("nestedObject", nestedObject);
    
    application.setHeaderParams(customHeaderParams);
    
    // When
    Application savedApp = saveAndTrackApplication(application);
    Application retrievedApplication = applicationRepository.findById(savedApp.getId()).orElse(null);
    
    // Then
    assertThat(retrievedApplication).isNotNull();
    assertThat(retrievedApplication.getHeaderParams()).isNotNull();
    assertThat(retrievedApplication.getHeaderParams()).containsEntry("customKey", "customValue");
    assertThat(retrievedApplication.getHeaderParams()).containsEntry("numericValue", 42);
    assertThat(retrievedApplication.getHeaderParams()).containsEntry("booleanValue", true);
    assertThat(retrievedApplication.getHeaderParams()).containsKey("nestedObject");
    
    @SuppressWarnings("unchecked")
    Map<String, Object> retrievedNestedObject = (Map<String, Object>) retrievedApplication.getHeaderParams().get("nestedObject");
    assertThat(retrievedNestedObject).containsEntry("nestedKey", "nestedValue");
  }

  @Test
  @DisplayName("Should handle null header parameters")
  void shouldHandleNullHeaderParameters() {
    // Given
    application.setHeaderParams(null);
    
    // When
    Application savedApp = saveAndTrackApplication(application);
    Application retrievedApplication = applicationRepository.findById(savedApp.getId()).orElse(null);
    
    // Then
    assertThat(retrievedApplication).isNotNull();
    assertThat(retrievedApplication.getHeaderParams()).isNull();
  }

  @Test
  @DisplayName("Should handle empty header parameters")
  void shouldHandleEmptyHeaderParameters() {
    // Given
    application.setHeaderParams(new HashMap<>());
    
    // When
    Application savedApp = saveAndTrackApplication(application);
    Application retrievedApplication = applicationRepository.findById(savedApp.getId()).orElse(null);
    
    // Then
    assertThat(retrievedApplication).isNotNull();
    assertThat(retrievedApplication.getHeaderParams()).isNotNull();
    assertThat(retrievedApplication.getHeaderParams()).isEmpty();
  }

  @Test
  @DisplayName("Should have created date when saved")
  void shouldHaveCreatedDateWhenSaved() {
    // When
    Application savedApp = saveAndTrackApplication(application);
    
    // Then
    assertThat(savedApp.getCreatedDate()).isNotNull();
    assertThat(savedApp.getCreatedDate()).isInstanceOf(Date.class);
  }

  @Test
  @DisplayName("Should have last update date when saved")
  void shouldHaveLastUpdateDateWhenSaved() {
    // When
    Application savedApp = saveAndTrackApplication(application);
    
    // Then
    assertThat(savedApp.getLastUpdate()).isNotNull();
    assertThat(savedApp.getLastUpdate()).isInstanceOf(Date.class);
  }

  @Test
  @DisplayName("Should have empty collections as defaults")
  void shouldHaveEmptyCollectionsAsDefaults() {
    // When
    Application savedApp = saveAndTrackApplication(application);
    
    // Then
    validateEmptyCollections(savedApp);
  }

  @Test
  @DisplayName("Should convert header parameters to JSON and back correctly")
  void shouldConvertHeaderParametersToJsonAndBack() {
    // Given
    Map<String, Object> testParams = new HashMap<>();
    testParams.put("stringValue", "test");
    testParams.put("intValue", 123);
    testParams.put("doubleValue", 45.67);
    testParams.put("booleanValue", true);
    testParams.put("nullValue", null);
    
    Map<String, Object> nestedMap = new HashMap<>();
    nestedMap.put("nestedString", "nested");
    nestedMap.put("nestedNumber", 789);
    testParams.put("nestedMap", nestedMap);
    
    // When
    String jsonString = hashMapConverter.convertToDatabaseColumn(testParams);
    Map<String, Object> convertedBack = hashMapConverter.convertToEntityAttribute(jsonString);
    
    // Then
    assertThat(jsonString).isNotNull().isNotEmpty();
    assertThat(convertedBack).isNotNull()
      .containsEntry("stringValue", "test")
      .containsEntry("intValue", 123)
      .containsEntry("doubleValue", 45.67)
      .containsEntry("booleanValue", true)
      .containsEntry("nullValue", null)
      .containsKey("nestedMap");
    
    @SuppressWarnings("unchecked")
    Map<String, Object> retrievedNestedMap = (Map<String, Object>) convertedBack.get("nestedMap");
    assertThat(retrievedNestedMap).containsEntry("nestedString", "nested")
      .containsEntry("nestedNumber", 789);
  }

  @Test
  @DisplayName("Should handle null and empty string in converter")
  void shouldHandleNullAndEmptyStringInConverter() {
    // When & Then
    assertThat(hashMapConverter.convertToDatabaseColumn(null)).isNull();
    assertThat(hashMapConverter.convertToEntityAttribute(null)).isNull();
    assertThat(hashMapConverter.convertToEntityAttribute("")).isNull();
    assertThat(hashMapConverter.convertToEntityAttribute("   ")).isNull();
  }

  @Test
  @DisplayName("Should have all default values when created")
  void shouldHaveAllDefaultValuesWhenCreated() {
    // When
    Application savedApp = saveAndTrackApplication(application);
    
    // Then
    validateDefaultBooleanValues(savedApp);
    validateDefaultHeaderParameters(savedApp);
    validateEmptyCollections(savedApp);
    validateDateFields(savedApp);
  }
}
