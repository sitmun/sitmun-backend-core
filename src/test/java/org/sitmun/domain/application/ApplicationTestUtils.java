package org.sitmun.domain.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.sitmun.infrastructure.persistence.type.map.HashMapConverter;

/**
 * Shared test utilities for Application tests. Contains common helper methods to reduce code
 * duplication between test classes.
 */
public class ApplicationTestUtils {

  /**
   * Creates a basic test application with minimal required fields.
   *
   * @param name Application name
   * @param type Application type
   * @param title Application title
   * @return A new Application instance
   */
  public static Application createBasicTestApplication(String name, String type, String title) {
    return Application.builder().name(name).type(type).title(title).build();
  }

  /**
   * Creates a test application with custom header parameters.
   *
   * @param name Application name
   * @param type Application type
   * @param title Application title
   * @param headerParams Custom header parameters
   * @return A new Application instance
   */
  public static Application createTestApplicationWithHeaders(
      String name, String type, String title, Map<String, Object> headerParams) {
    Application app = createBasicTestApplication(name, type, title);
    app.setHeaderParams(headerParams);
    return app;
  }

  /**
   * Validates that an application has the expected default boolean values.
   *
   * @param app The application to validate
   */
  public static void validateDefaultBooleanValues(Application app) {
    assertThat(app.getTreeAutoRefresh()).isTrue();
    assertThat(app.getAccessParentTerritory()).isFalse();
    assertThat(app.getAccessChildrenTerritory()).isFalse();
    assertThat(app.getIsUnavailable()).isFalse();
    assertThat(app.getAppPrivate()).isFalse();
  }

  /**
   * Validates that an application has the expected default header parameters structure.
   *
   * @param app The application to validate
   */
  public static void validateDefaultHeaderParameters(Application app) {
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
   * Validates that an application has empty collections by default.
   *
   * @param app The application to validate
   */
  public static void validateEmptyCollections(Application app) {
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
   * Validates that an application has date fields set when saved.
   *
   * @param app The application to validate
   */
  public static void validateDateFields(Application app) {
    assertThat(app.getCreatedDate()).isNotNull();
    assertThat(app.getCreatedDate()).isInstanceOf(Date.class);

    assertThat(app.getLastUpdate()).isNotNull();
    assertThat(app.getLastUpdate()).isInstanceOf(Date.class);
  }

  /**
   * Validates all default values for an application.
   *
   * @param app The application to validate
   */
  public static void validateAllDefaultValues(Application app) {
    validateDefaultBooleanValues(app);
    validateDefaultHeaderParameters(app);
    validateEmptyCollections(app);
    validateDateFields(app);
  }

  /**
   * Creates a test map with custom header parameters.
   *
   * @return A map with test header parameters
   */
  public static Map<String, Object> createCustomHeaderParams() {
    Map<String, Object> customHeaderParams = new HashMap<>();
    customHeaderParams.put("customKey", "customValue");
    customHeaderParams.put("numericValue", 42);
    customHeaderParams.put("booleanValue", true);

    Map<String, Object> nestedObject = new HashMap<>();
    nestedObject.put("nestedKey", "nestedValue");
    customHeaderParams.put("nestedObject", nestedObject);

    return customHeaderParams;
  }

  /**
   * Creates a complex nested header parameters structure for testing.
   *
   * @return A complex nested map structure
   */
  public static Map<String, Object> createComplexHeaderParams() {
    Map<String, Object> complexHeaders = new HashMap<>();
    Map<String, Object> level1 = new HashMap<>();
    Map<String, Object> level2 = new HashMap<>();
    Map<String, Object> level3 = new HashMap<>();

    level3.put("deepValue", "very deep");
    level3.put("arrayValue", new Object[] {1, 2, 3, 4, 5});

    Map<String, Object> mixedTypes = new HashMap<>();
    mixedTypes.put("string", "text");
    mixedTypes.put("number", 123.45);
    mixedTypes.put("boolean", false);
    mixedTypes.put("nullValue", null);
    level3.put("mixedTypes", mixedTypes);

    level2.put("level3", level3);
    level1.put("level2", level2);
    complexHeaders.put("level1", level1);

    return complexHeaders;
  }

  /**
   * Tests the HashMapConverter with various data types.
   *
   * @param converter The HashMapConverter to test
   */
  public static void testHashMapConverter(HashMapConverter converter) {
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

    String jsonString = converter.convertToDatabaseColumn(testParams);
    Map<String, Object> convertedBack = converter.convertToEntityAttribute(jsonString);

    assertThat(jsonString).isNotNull();
    assertThat(jsonString).isNotEmpty();
    assertThat(convertedBack).isNotNull();
    assertThat(convertedBack).containsEntry("stringValue", "test");
    assertThat(convertedBack).containsEntry("intValue", 123);
    assertThat(convertedBack).containsEntry("doubleValue", 45.67);
    assertThat(convertedBack).containsEntry("booleanValue", true);
    assertThat(convertedBack).containsEntry("nullValue", null);
    assertThat(convertedBack).containsKey("nestedMap");

    @SuppressWarnings("unchecked")
    Map<String, Object> retrievedNestedMap = (Map<String, Object>) convertedBack.get("nestedMap");
    assertThat(retrievedNestedMap).containsEntry("nestedString", "nested");
    assertThat(retrievedNestedMap).containsEntry("nestedNumber", 789);
  }
}
