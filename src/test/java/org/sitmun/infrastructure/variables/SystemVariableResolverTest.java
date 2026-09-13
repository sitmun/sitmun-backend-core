package org.sitmun.infrastructure.variables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sitmun.authorization.proxy.service.RequestCoordinates;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.territory.Territory;
import org.sitmun.domain.user.User;
import org.sitmun.infrastructure.config.SystemVariableProperties;
import org.sitmun.infrastructure.persistence.type.envelope.Envelope;
import org.springframework.context.i18n.LocaleContextHolder;

/** Tests for SystemVariableResolver ensuring proper SpEL-based variable resolution. */
class SystemVariableResolverTest {

  private static final Instant FIXED_INSTANT = Instant.parse("2026-09-13T16:54:00Z");

  private SystemVariableResolver resolver;
  private SystemVariableProperties properties;

  private User user;
  private Territory territory;
  private Application application;

  @BeforeEach
  void setUp() {
    user = mock(User.class);
    when(user.getId()).thenReturn(100);
    when(user.getUsername()).thenReturn("testuser");

    Envelope extent = Envelope.builder().minX(1.0).minY(2.0).maxX(3.0).maxY(4.0).build();
    Territory child = mock(Territory.class);
    when(child.getCode()).thenReturn("08021");

    territory = mock(Territory.class);
    when(territory.getId()).thenReturn(200);
    when(territory.getCode()).thenReturn("08019");
    when(territory.getName()).thenReturn("Test Territory");
    when(territory.getExtent()).thenReturn(extent);
    when(territory.getMembers()).thenReturn(Set.of(child));

    application = mock(Application.class);
    when(application.getId()).thenReturn(300);
    when(application.getName()).thenReturn("Test App");
    when(application.getSrs()).thenReturn("EPSG:23031");
    when(application.getAccessChildrenTerritory()).thenReturn(false);

    properties = new SystemVariableProperties();
    Map<String, String> systemVars = new HashMap<>();
    systemVars.put("USER_ID", "#{#user.id}");
    systemVars.put("USER_NAME", "#{#user.username}");
    systemVars.put("TERR_ID", "#{#territory.id}");
    systemVars.put("TERR_COD", "#{#territory.code}");
    systemVars.put("TERR_NAME", "#{#territory.name}");
    systemVars.put("APP_ID", "#{#application.id}");
    systemVars.put("APP_NAME", "#{#application.name}");
    systemVars.put("APP_CODIGO", "#{#application.id}");
    systemVars.put("TER_CODIGO", "#{#territory.id}");
    systemVars.put("USU_CODIGO", "#{#user.id}");
    systemVars.put("USUARIO", "#{#user.username}");
    systemVars.put("MUN_INE", "#{#territory.code}");
    systemVars.put(
        "MUN_INES",
        "#{T(org.sitmun.infrastructure.variables.SystemVariableSupport).munInes(#application, #territory)}");
    systemVars.put("PROYECCION", "#{#application.srs}");
    systemVars.put("EXTENSION_MAX_X0", "#{#territory.extent.minX}");
    systemVars.put("EXTENSION_MAX_Y0", "#{#territory.extent.minY}");
    systemVars.put("EXTENSION_MAX_X1", "#{#territory.extent.maxX}");
    systemVars.put("EXTENSION_MAX_Y1", "#{#territory.extent.maxY}");
    systemVars.put(
        "DATE",
        "#{#now.format(T(java.time.format.DateTimeFormatter).ofPattern('dd/MM/yyyy HH:mm:ss'))}");
    systemVars.put("LANG", "#{#language}");
    systemVars.put("LANGUAGE", "#{#language}");
    properties.setSystem(systemVars);

    resolver = new SystemVariableResolver(properties, Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC));
    LocaleContextHolder.setLocale(Locale.forLanguageTag("ca"));
  }

  @AfterEach
  void tearDown() {
    LocaleContextHolder.resetLocaleContext();
  }

  private static RequestCoordinates coords(
      User user, Territory territory, Application application) {
    RequestCoordinates c = new RequestCoordinates();
    c.setUser(user);
    c.setTerritory(territory);
    c.setApplication(application);
    return c;
  }

  @Test
  void resolve_withSingleUserVariable_replacesCorrectly() {
    String template = "SELECT * FROM users WHERE id = #{USER_ID}";

    String result = resolver.resolve(template, coords(user, territory, application));

    assertThat(result).isEqualTo("SELECT * FROM users WHERE id = 100");
  }

  @Test
  void resolve_withSingleTerritoryVariable_replacesCorrectly() {
    String template = "SELECT * FROM data WHERE territory = '#{TERR_COD}'";

    String result = resolver.resolve(template, coords(user, territory, application));

    assertThat(result).isEqualTo("SELECT * FROM data WHERE territory = '08019'");
  }

  @Test
  void resolve_withMultipleVariables_replacesAllCorrectly() {
    String template =
        "SELECT * FROM logs WHERE user_id = #{USER_ID} AND terr_id = #{TERR_ID} AND app_id = #{APP_ID}";

    String result = resolver.resolve(template, coords(user, territory, application));

    assertThat(result)
        .isEqualTo("SELECT * FROM logs WHERE user_id = 100 AND terr_id = 200 AND app_id = 300");
  }

  @Test
  void resolve_withStringVariable_replacesCorrectly() {
    String template = "User #{USER_NAME} in #{TERR_NAME}";

    String result = resolver.resolve(template, coords(user, territory, application));

    assertThat(result).isEqualTo("User testuser in Test Territory");
  }

  @Test
  void resolve_withNoVariables_returnsUnchanged() {
    String template = "SELECT * FROM data WHERE status = 'active'";

    String result = resolver.resolve(template, coords(user, territory, application));

    assertThat(result).isEqualTo(template);
  }

  @Test
  void resolve_withUndefinedVariable_returnsUnchangedPlaceholder() {
    String template = "SELECT * FROM data WHERE unknown = #{UNDEFINED_VAR}";

    String result = resolver.resolve(template, coords(user, territory, application));

    assertThat(result).isEqualTo("SELECT * FROM data WHERE unknown = #{UNDEFINED_VAR}");
  }

  @Test
  void resolve_withNullTemplate_returnsNull() {
    String result = resolver.resolve(null, coords(user, territory, application));

    assertThat(result).isNull();
  }

  @Test
  void resolve_withEmptyTemplate_returnsEmpty() {
    String result = resolver.resolve("", coords(user, territory, application));

    assertThat(result).isEmpty();
  }

  @Test
  void resolve_withNullUser_handlesGracefully() {
    String template = "SELECT * FROM data WHERE terr_id = #{TERR_ID}";

    String result = resolver.resolve(template, coords(null, territory, application));

    assertThat(result).isEqualTo("SELECT * FROM data WHERE terr_id = 200");
  }

  @Test
  void resolve_withNullTerritory_handlesGracefully() {
    String template = "SELECT * FROM data WHERE user_id = #{USER_ID}";

    String result = resolver.resolve(template, coords(user, null, application));

    assertThat(result).isEqualTo("SELECT * FROM data WHERE user_id = 100");
  }

  @Test
  void resolve_withNullApplication_handlesGracefully() {
    String template = "SELECT * FROM data WHERE user_id = #{USER_ID}";

    String result = resolver.resolve(template, coords(user, territory, null));

    assertThat(result).isEqualTo("SELECT * FROM data WHERE user_id = 100");
  }

  @Test
  void resolve_withAccessToNullEntity_returnsEmptyString() {
    String template = "App ID: #{APP_ID}";

    String result = resolver.resolve(template, coords(user, territory, null));

    // Since application is null, SpEL evaluation will fail gracefully
    // and return the placeholder unchanged
    assertThat(result).isEqualTo("App ID: #{APP_ID}");
  }

  @Test
  void resolve_withSameVariableMultipleTimes_replacesAll() {
    String template = "User #{USER_ID} logged in. Previous user was #{USER_ID}.";

    String result = resolver.resolve(template, coords(user, territory, application));

    assertThat(result).isEqualTo("User 100 logged in. Previous user was 100.");
  }

  @Test
  void resolve_withVariableInMiddleOfWord_replacesOnlyPlaceholder() {
    String template = "prefix_#{TERR_ID}_suffix";

    String result = resolver.resolve(template, coords(user, territory, application));

    assertThat(result).isEqualTo("prefix_200_suffix");
  }

  @Test
  void resolve_withComplexSpelExpression_canAccessNestedProperties() {
    // Add a more complex expression
    properties.getSystem().put("USER_DISPLAY", "#{#user.username + ' (ID: ' + #user.id + ')'}");

    String template = "Logged in as: #{USER_DISPLAY}";

    String result = resolver.resolve(template, coords(user, territory, application));

    assertThat(result).isEqualTo("Logged in as: testuser (ID: 100)");
  }

  @Test
  void getAvailableVariables_returnsAllConfiguredVariables() {
    Map<String, String> available = resolver.getAvailableVariables();

    assertThat(available)
        .containsKeys("USER_ID", "APP_CODIGO", "MUN_INE", "MUN_INES", "DATE", "LANG", "LANGUAGE");
    assertThat(available.get("USER_ID")).isEqualTo("#{#user.id}");
    assertThat(available.get("LANG")).isEqualTo("#{#language}");
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
  void containsSystemVariables_withDigitInName_returnsTrue() {
    assertThat(SystemVariableResolver.containsSystemVariables("#{EXTENSION_MAX_X0}")).isTrue();
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

    String result = resolver.resolve(template, coords(user, territory, application));

    // The malicious code is inserted as-is (string substitution)
    // SQL injection protection should be handled by PreparedStatements in the proxy middleware
    assertThat(result).isEqualTo("SELECT * FROM data WHERE territory = ''; DROP TABLE users; --'");
  }

  @Test
  void resolve_withAlternativeExpressionSyntax_worksCorrectly() {
    // Test that expressions without outer #{} wrapper also work
    properties.getSystem().put("TEST_VAR", "#user.id");

    String template = "Value: #{TEST_VAR}";

    String result = resolver.resolve(template, coords(user, territory, application));

    assertThat(result).isEqualTo("Value: 100");
  }

  @Test
  void resolve_sitmun2Aliases_matchExistingContextFields() {
    String template =
        "#{APP_CODIGO} #{TER_CODIGO} #{USU_CODIGO} #{USUARIO} #{MUN_INE} #{PROYECCION}";

    String result = resolver.resolve(template, coords(user, territory, application));

    assertThat(result).isEqualTo("300 200 100 testuser 08019 EPSG:23031");
  }

  @Test
  void resolve_munInes_withoutChildrenAccess_isCurrentCodeOnly() {
    String result = resolver.resolve("#{MUN_INES}", coords(user, territory, application));

    assertThat(result).isEqualTo("08019");
  }

  @Test
  void resolve_munInes_withChildrenAccess_joinsSelfAndMembers() {
    when(application.getAccessChildrenTerritory()).thenReturn(true);

    String result = resolver.resolve("#{MUN_INES}", coords(user, territory, application));

    assertThat(result).isEqualTo("08019,08021");
  }

  @Test
  void resolve_territoryExtent_matchesSitmun2MaxTokens() {
    String template =
        "#{EXTENSION_MAX_X0} #{EXTENSION_MAX_Y0} #{EXTENSION_MAX_X1} #{EXTENSION_MAX_Y1}";

    String result = resolver.resolve(template, coords(user, territory, application));

    assertThat(result).isEqualTo("1.0 2.0 3.0 4.0");
  }

  @Test
  void resolve_date_usesRequestClockNotEntity() {
    String result = resolver.resolve("at #{DATE}", coords(user, territory, application));

    assertThat(result).isEqualTo("at 13/09/2026 16:54:00");
  }

  @Test
  void resolve_langAndLanguage_useLocaleContextHolder() {
    String result = resolver.resolve("#{LANG}/#{LANGUAGE}", coords(user, territory, application));

    assertThat(result).isEqualTo("ca/ca");
  }

  @Test
  void resolve_dateAndLang_withoutCoordinates() {
    String result = resolver.resolve("#{DATE} #{LANG}", null);

    assertThat(result).isEqualTo("13/09/2026 16:54:00 ca");
  }
}
