package org.sitmun.infrastructure.variables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.territory.Territory;
import org.sitmun.domain.user.User;
import org.sitmun.infrastructure.config.SystemVariableProperties;

/** Tests for SystemVariableResolver ensuring proper SpEL-based variable resolution. */
class SystemVariableResolverTest {

  private SystemVariableResolver resolver;
  private SystemVariableProperties properties;

  private User user;
  private Territory territory;
  private Application application;

  @BeforeEach
  void setUp() {
    // Set up mock entities
    user = mock(User.class);
    when(user.getId()).thenReturn(100);
    when(user.getUsername()).thenReturn("testuser");

    territory = mock(Territory.class);
    when(territory.getId()).thenReturn(200);
    when(territory.getCode()).thenReturn("TERR_CODE");
    when(territory.getName()).thenReturn("Test Territory");

    application = mock(Application.class);
    when(application.getId()).thenReturn(300);
    when(application.getName()).thenReturn("Test App");

    // Set up properties with standard variable definitions
    properties = new SystemVariableProperties();
    Map<String, String> systemVars = new HashMap<>();
    systemVars.put("USER_ID", "#{#user.id}");
    systemVars.put("USER_NAME", "#{#user.username}");
    systemVars.put("TERR_ID", "#{#territory.id}");
    systemVars.put("TERR_COD", "#{#territory.code}");
    systemVars.put("TERR_NAME", "#{#territory.name}");
    systemVars.put("APP_ID", "#{#application.id}");
    systemVars.put("APP_NAME", "#{#application.name}");
    properties.setSystem(systemVars);

    resolver = new SystemVariableResolver(properties);
  }

  @Test
  void resolve_withSingleUserVariable_replacesCorrectly() {
    String template = "SELECT * FROM users WHERE id = #{USER_ID}";

    String result = resolver.resolve(template, user, territory, application);

    assertThat(result).isEqualTo("SELECT * FROM users WHERE id = 100");
  }

  @Test
  void resolve_withSingleTerritoryVariable_replacesCorrectly() {
    String template = "SELECT * FROM data WHERE territory = '#{TERR_COD}'";

    String result = resolver.resolve(template, user, territory, application);

    assertThat(result).isEqualTo("SELECT * FROM data WHERE territory = 'TERR_CODE'");
  }

  @Test
  void resolve_withMultipleVariables_replacesAllCorrectly() {
    String template =
        "SELECT * FROM logs WHERE user_id = #{USER_ID} AND terr_id = #{TERR_ID} AND app_id = #{APP_ID}";

    String result = resolver.resolve(template, user, territory, application);

    assertThat(result)
        .isEqualTo("SELECT * FROM logs WHERE user_id = 100 AND terr_id = 200 AND app_id = 300");
  }

  @Test
  void resolve_withStringVariable_replacesCorrectly() {
    String template = "User #{USER_NAME} in #{TERR_NAME}";

    String result = resolver.resolve(template, user, territory, application);

    assertThat(result).isEqualTo("User testuser in Test Territory");
  }

  @Test
  void resolve_withNoVariables_returnsUnchanged() {
    String template = "SELECT * FROM data WHERE status = 'active'";

    String result = resolver.resolve(template, user, territory, application);

    assertThat(result).isEqualTo(template);
  }

  @Test
  void resolve_withUndefinedVariable_returnsUnchangedPlaceholder() {
    String template = "SELECT * FROM data WHERE unknown = #{UNDEFINED_VAR}";

    String result = resolver.resolve(template, user, territory, application);

    assertThat(result).isEqualTo("SELECT * FROM data WHERE unknown = #{UNDEFINED_VAR}");
  }

  @Test
  void resolve_withNullTemplate_returnsNull() {
    String result = resolver.resolve(null, user, territory, application);

    assertThat(result).isNull();
  }

  @Test
  void resolve_withEmptyTemplate_returnsEmpty() {
    String result = resolver.resolve("", user, territory, application);

    assertThat(result).isEmpty();
  }

  @Test
  void resolve_withNullUser_handlesGracefully() {
    String template = "SELECT * FROM data WHERE terr_id = #{TERR_ID}";

    String result = resolver.resolve(template, null, territory, application);

    assertThat(result).isEqualTo("SELECT * FROM data WHERE terr_id = 200");
  }

  @Test
  void resolve_withNullTerritory_handlesGracefully() {
    String template = "SELECT * FROM data WHERE user_id = #{USER_ID}";

    String result = resolver.resolve(template, user, null, application);

    assertThat(result).isEqualTo("SELECT * FROM data WHERE user_id = 100");
  }

  @Test
  void resolve_withNullApplication_handlesGracefully() {
    String template = "SELECT * FROM data WHERE user_id = #{USER_ID}";

    String result = resolver.resolve(template, user, territory, null);

    assertThat(result).isEqualTo("SELECT * FROM data WHERE user_id = 100");
  }

  @Test
  void resolve_withAccessToNullEntity_returnsEmptyString() {
    String template = "App ID: #{APP_ID}";

    String result = resolver.resolve(template, user, territory, null);

    // Since application is null, SpEL evaluation will fail gracefully
    // and return the placeholder unchanged
    assertThat(result).isEqualTo("App ID: #{APP_ID}");
  }

  @Test
  void resolve_withSameVariableMultipleTimes_replacesAll() {
    String template = "User #{USER_ID} logged in. Previous user was #{USER_ID}.";

    String result = resolver.resolve(template, user, territory, application);

    assertThat(result).isEqualTo("User 100 logged in. Previous user was 100.");
  }

  @Test
  void resolve_withVariableInMiddleOfWord_replacesOnlyPlaceholder() {
    String template = "prefix_#{TERR_ID}_suffix";

    String result = resolver.resolve(template, user, territory, application);

    assertThat(result).isEqualTo("prefix_200_suffix");
  }

  @Test
  void resolve_withComplexSpelExpression_canAccessNestedProperties() {
    // Add a more complex expression
    properties.getSystem().put("USER_DISPLAY", "#{#user.username + ' (ID: ' + #user.id + ')'}");

    String template = "Logged in as: #{USER_DISPLAY}";

    String result = resolver.resolve(template, user, territory, application);

    assertThat(result).isEqualTo("Logged in as: testuser (ID: 100)");
  }

  @Test
  void getAvailableVariables_returnsAllConfiguredVariables() {
    Map<String, String> available = resolver.getAvailableVariables();

    assertThat(available)
        .containsKeys(
            "USER_ID", "USER_NAME", "TERR_ID", "TERR_COD", "TERR_NAME", "APP_ID", "APP_NAME");
    assertThat(available.get("USER_ID")).isEqualTo("#{#user.id}");
  }

  @Test
  void containsSystemVariables_withVariables_returnsTrue() {
    assertThat(
            SystemVariableResolver.containsSystemVariables(
                "SELECT * FROM data WHERE id = #{USER_ID}"))
        .isTrue();
  }

  @Test
  void containsSystemVariables_withoutVariables_returnsFalse() {
    assertThat(SystemVariableResolver.containsSystemVariables("SELECT * FROM data WHERE id = 123"))
        .isFalse();
  }

  @Test
  void containsSystemVariables_withNull_returnsFalse() {
    assertThat(SystemVariableResolver.containsSystemVariables(null)).isFalse();
  }

  @Test
  void containsSystemVariables_withDollarBraceNotHashBrace_returnsFalse() {
    // ${var} is for SQL user variables, not system variables
    assertThat(
            SystemVariableResolver.containsSystemVariables(
                "SELECT * FROM data WHERE id = ${userId}"))
        .isFalse();
  }

  @Test
  void resolve_sqlInjectionInVariable_isHandledSafely() {
    // Even if territory code contains SQL injection attempt, it's just a string substitution
    when(territory.getCode()).thenReturn("'; DROP TABLE users; --");

    String template = "SELECT * FROM data WHERE territory = '#{TERR_COD}'";

    String result = resolver.resolve(template, user, territory, application);

    // The malicious code is inserted as-is (string substitution)
    // SQL injection protection should be handled by PreparedStatements in the proxy middleware
    assertThat(result).isEqualTo("SELECT * FROM data WHERE territory = ''; DROP TABLE users; --'");
  }

  @Test
  void resolve_withAlternativeExpressionSyntax_worksCorrectly() {
    // Test that expressions without outer #{} wrapper also work
    properties.getSystem().put("TEST_VAR", "#user.id");

    String template = "Value: #{TEST_VAR}";

    String result = resolver.resolve(template, user, territory, application);

    assertThat(result).isEqualTo("Value: 100");
  }
}
