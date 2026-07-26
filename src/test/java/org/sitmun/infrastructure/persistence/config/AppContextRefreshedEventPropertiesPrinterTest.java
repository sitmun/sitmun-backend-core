package org.sitmun.infrastructure.persistence.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.sitmun.test.LogTestUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

@DisplayName("AppContextRefreshedEventPropertiesPrinter")
class AppContextRefreshedEventPropertiesPrinterTest {

  private static final String LOGGER_NAME =
      "org.sitmun.infrastructure.persistence.config.AppContextRefreshedEventPropertiesPrinter";
  private static final String SECRET_MARKER = "super-secret-value-XYZ";

  private LogTestUtils logCapture;

  @BeforeEach
  void setUp() {
    logCapture = new LogTestUtils(LOGGER_NAME);
  }

  @AfterEach
  void tearDown() {
    if (logCapture != null) {
      logCapture.stopCapturing();
    }
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "SITMUN_BOOTSTRAP_ADMIN_PASSWORD",
        "SITMUN_USER_SECRET",
        "sitmun.user.secret",
        "sitmun.startup.built-in-users.admin-password",
        "SPRING_DATASOURCE_PASSWORD",
        "spring.datasource.password",
        "SITMUN_PROXY_MIDDLEWARE_SECRET",
        "recovery.jwt.token",
        "ldap.credential"
      })
  @DisplayName("sensitive keys are redacted")
  void sensitiveKeysAreRedacted(String key) {
    assertThat(AppContextRefreshedEventPropertiesPrinter.displayValue(key, "super-secret-value"))
        .isEqualTo("<redacted>");
  }

  @ParameterizedTest
  @CsvSource({
    "server.port,8080",
    "spring.application.name,SITMUN",
    "sitmun.language,en",
    "logging.level.ROOT,INFO"
  })
  @DisplayName("non-sensitive keys keep their values")
  void nonSensitiveKeysKeepValues(String key, String value) {
    assertThat(AppContextRefreshedEventPropertiesPrinter.displayValue(key, value)).isEqualTo(value);
  }

  @Test
  @DisplayName("null values stay null")
  void nullValuesStayNull() {
    assertThat(AppContextRefreshedEventPropertiesPrinter.displayValue("server.port", null))
        .isNull();
  }

  @Test
  @DisplayName("isSensitiveKey is case-insensitive")
  void sensitiveKeyDetectionIsCaseInsensitive() {
    assertThat(AppContextRefreshedEventPropertiesPrinter.isSensitiveKey("SiTmUn_UsEr_SeCrEt"))
        .isTrue();
    assertThat(AppContextRefreshedEventPropertiesPrinter.isSensitiveKey("server.port")).isFalse();
  }

  @Test
  @DisplayName("listener logs redacted secret and keeps non-sensitive values")
  void listenerLogsRedactedSecrets() {
    Map<String, Object> source = new HashMap<>();
    source.put("SITMUN_BOOTSTRAP_ADMIN_PASSWORD", SECRET_MARKER);
    source.put("server.port", "18083");

    MutablePropertySources propertySources = new MutablePropertySources();
    propertySources.addFirst(new MapPropertySource("test", source));

    ConfigurableEnvironment env = mock(ConfigurableEnvironment.class);
    when(env.getPropertySources()).thenReturn(propertySources);
    when(env.getProperty("SITMUN_BOOTSTRAP_ADMIN_PASSWORD")).thenReturn(SECRET_MARKER);
    when(env.getProperty("server.port")).thenReturn("18083");

    ApplicationContext applicationContext = mock(ApplicationContext.class);
    when(applicationContext.getEnvironment()).thenReturn(env);

    logCapture.startCapturing();
    new AppContextRefreshedEventPropertiesPrinter()
        .handleContextRefreshed(new ContextRefreshedEvent(applicationContext));

    assertThat(logCapture.getLogMessagesContaining("SITMUN_BOOTSTRAP_ADMIN_PASSWORD=<redacted>"))
        .isNotEmpty();
    assertThat(logCapture.getLogMessagesContaining("server.port=18083")).isNotEmpty();
    assertThat(logCapture.getLogMessagesContaining(SECRET_MARKER)).isEmpty();
  }
}
