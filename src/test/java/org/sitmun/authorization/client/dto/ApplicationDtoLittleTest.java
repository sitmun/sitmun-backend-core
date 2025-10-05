package org.sitmun.authorization.client.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ApplicationDtoLittle DTO test")
class ApplicationDtoLittleTest {

  @Test
  @DisplayName("ApplicationDtoLittle should have all required fields with getters and setters")
  void applicationDtoLittleShouldHaveAllRequiredFields() {
    // Given
    ApplicationDtoLittle dto = new ApplicationDtoLittle();
    Date testDate = new Date();
    Map<String, String> testConfig = new HashMap<>();
    testConfig.put("mbtilesUrl", "https://example.com/mbtiles");
    Map<String, Object> testHeaderParams = new HashMap<>();
    testHeaderParams.put("headerKey", "headerValue");

    // When
    dto.setId(1);
    dto.setTitle("Test Application");
    dto.setType("T");
    dto.setLogo("logo.png");
    dto.setDescription("Test Description");
    dto.setMaintenanceInformation("Maintenance info");
    dto.setIsUnavailable(false);
    dto.setAppPrivate(false);
    dto.setLastUpdate(testDate);
    dto.setCreator("Test Creator");
    dto.setHeaderParams(testHeaderParams);
    dto.setConfig(testConfig);

    // Then
    assertThat(dto.getId()).isEqualTo(1);
    assertThat(dto.getTitle()).isEqualTo("Test Application");
    assertThat(dto.getType()).isEqualTo("T");
    assertThat(dto.getLogo()).isEqualTo("logo.png");
    assertThat(dto.getDescription()).isEqualTo("Test Description");
    assertThat(dto.getMaintenanceInformation()).isEqualTo("Maintenance info");
    assertThat(dto.getIsUnavailable()).isFalse();
    assertThat(dto.getAppPrivate()).isFalse();
    assertThat(dto.getLastUpdate()).isEqualTo(testDate);
    assertThat(dto.getCreator()).isEqualTo("Test Creator");
    assertThat(dto.getHeaderParams()).isEqualTo(testHeaderParams);
    assertThat(dto.getConfig()).isEqualTo(testConfig);
  }

  @Test
  @DisplayName("ApplicationDtoLittle config field should support mbtiles URL")
  void applicationDtoLittleConfigFieldShouldSupportMbtilesUrl() {
    // Given
    ApplicationDtoLittle dto = new ApplicationDtoLittle();
    Map<String, String> config = new HashMap<>();
    String mbtilesUrl = "https://localhost:8080/mbtiles";

    // When
    config.put("mbtilesUrl", mbtilesUrl);
    dto.setConfig(config);

    // Then
    assertThat(dto.getConfig()).isNotNull();
    assertThat(dto.getConfig()).containsEntry("mbtilesUrl", mbtilesUrl);
    assertThat(dto.getConfig()).containsKey("mbtilesUrl");
  }

  @Test
  @DisplayName("ApplicationDtoLittle config field should support multiple configuration values")
  void applicationDtoLittleConfigFieldShouldSupportMultipleConfigurationValues() {
    // Given
    ApplicationDtoLittle dto = new ApplicationDtoLittle();
    Map<String, String> config = new HashMap<>();

    // When
    config.put("mbtilesUrl", "https://localhost:8080/mbtiles");
    config.put("apiUrl", "https://api.example.com");
    config.put("version", "1.0.0");
    dto.setConfig(config);

    // Then
    assertThat(dto.getConfig()).hasSize(3);
    assertThat(dto.getConfig()).containsEntry("mbtilesUrl", "https://localhost:8080/mbtiles");
    assertThat(dto.getConfig()).containsEntry("apiUrl", "https://api.example.com");
    assertThat(dto.getConfig()).containsEntry("version", "1.0.0");
  }

  @Test
  @DisplayName("ApplicationDtoLittle should handle null config field")
  void applicationDtoLittleShouldHandleNullConfigField() {
    // Given
    ApplicationDtoLittle dto = new ApplicationDtoLittle();

    // When
    dto.setConfig(null);

    // Then
    assertThat(dto.getConfig()).isNull();
  }

  @Test
  @DisplayName("ApplicationDtoLittle should handle empty config field")
  void applicationDtoLittleShouldHandleEmptyConfigField() {
    // Given
    ApplicationDtoLittle dto = new ApplicationDtoLittle();
    Map<String, String> emptyConfig = new HashMap<>();

    // When
    dto.setConfig(emptyConfig);

    // Then
    assertThat(dto.getConfig()).isNotNull();
    assertThat(dto.getConfig()).isEmpty();
  }

  @Test
  @DisplayName("ApplicationDtoLittle should support Boolean fields with null values")
  void applicationDtoLittleShouldSupportBooleanFieldsWithNullValues() {
    // Given
    ApplicationDtoLittle dto = new ApplicationDtoLittle();

    // When
    dto.setIsUnavailable(null);
    dto.setAppPrivate(null);

    // Then
    assertThat(dto.getIsUnavailable()).isNull();
    assertThat(dto.getAppPrivate()).isNull();
  }

  @Test
  @DisplayName("ApplicationDtoLittle should support Boolean fields with true values")
  void applicationDtoLittleShouldSupportBooleanFieldsWithTrueValues() {
    // Given
    ApplicationDtoLittle dto = new ApplicationDtoLittle();

    // When
    dto.setIsUnavailable(true);
    dto.setAppPrivate(true);

    // Then
    assertThat(dto.getIsUnavailable()).isTrue();
    assertThat(dto.getAppPrivate()).isTrue();
  }
}
