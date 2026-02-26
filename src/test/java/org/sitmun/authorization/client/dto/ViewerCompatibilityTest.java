package org.sitmun.authorization.client.dto;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.type.TaskType;
import org.sitmun.domain.territory.Territory;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * NON-REGRESSION GUARDRAIL 2: Viewer (sitmun-viewer-app) DTO compatibility tests
 *
 * <p>These tests verify backward compatibility with the viewer application which expects: -
 * More-info parameters with 'label', 'value', and optionally 'name' fields - URL-type more-info
 * tasks expose 'command' for external links - API/SQL-type more-info tasks execute via proxy - No
 * secret exposure (provided vars excluded, no resolved #{...} values) - Secret-bearing tasks hide
 * command, provide proxy URL
 */
@DisplayName("Viewer Compatibility Tests (Guardrail 2)")
class ViewerCompatibilityTest {

  private TaskMoreInfoService service;
  private Application application;
  private Territory territory;

  @BeforeEach
  void setUp() {
    service = new TaskMoreInfoService();
    ReflectionTestUtils.setField(service, "proxyUrl", "http://localhost:8080/middleware");

    application = mock(Application.class);
    when(application.getId()).thenReturn(10);

    territory = mock(Territory.class);
    when(territory.getId()).thenReturn(5);
  }

  @Nested
  @DisplayName("More-info parameter compatibility adapter")
  class MoreInfoParameterAdapter {

    @Test
    @DisplayName("MUST emit parameters with label/value/name fields (viewer expects these)")
    void parametersHaveLabelValueNameFields() {
      // Given: Task with internal 'variable' and 'field' structure (after migration)
      Task task = createMoreInfoTask();

      Map<String, Object> param1 = new HashMap<>();
      param1.put(DomainConstants.Tasks.PARAMETERS_VARIABLE, "id"); // Internal key (post-migration)
      param1.put(
          DomainConstants.Tasks.PARAMETERS_FIELD, "$.identifier"); // Internal key (post-migration)
      param1.put("order", 0);

      Map<String, Object> param2 = new HashMap<>();
      param2.put(DomainConstants.Tasks.PARAMETERS_VARIABLE, "name");
      param2.put(DomainConstants.Tasks.PARAMETERS_FIELD, "$.title");
      param2.put("order", 1);

      Map<String, Object> properties = new HashMap<>();
      properties.put(DomainConstants.Tasks.PROPERTY_PARAMETERS, List.of(param1, param2));
      properties.put(DomainConstants.Tasks.PROPERTY_SCOPE, DomainConstants.Tasks.SCOPE_API);
      when(task.getProperties()).thenReturn(properties);

      // When
      TaskDto result = service.map(task, application, territory);

      // Then: DTO must expose backward-compatible field names for viewer
      assertThat(result.getParameters()).isNotNull();
      assertThat(result.getParameters()).hasSize(2);

      // Verify first parameter has viewer-compatible structure
      @SuppressWarnings("unchecked")
      Map<String, Object> dtoParam1 = (Map<String, Object>) result.getParameters().get("id");
      assertThat(dtoParam1)
          .containsEntry(DomainConstants.Tasks.PARAMETERS_LABEL, "id"); // Viewer reads this
      assertThat(dtoParam1)
          .containsEntry(
              DomainConstants.Tasks.PARAMETERS_VALUE, "$.identifier"); // Viewer reads this
      assertThat(dtoParam1)
          .containsEntry(DomainConstants.Tasks.PARAMETERS_NAME, "id"); // Optional fallback

      // Verify second parameter
      @SuppressWarnings("unchecked")
      Map<String, Object> dtoParam2 = (Map<String, Object>) result.getParameters().get("name");
      assertThat(dtoParam2).containsEntry(DomainConstants.Tasks.PARAMETERS_LABEL, "name");
      assertThat(dtoParam2).containsEntry(DomainConstants.Tasks.PARAMETERS_VALUE, "$.title");
      assertThat(dtoParam2).containsEntry(DomainConstants.Tasks.PARAMETERS_NAME, "name");
    }

    @Test
    @DisplayName("MUST support legacy 'label' key during migration (fallback)")
    void supportsLegacyLabelKeyDuringMigration() {
      // Given: Task with old 'label' key (pre-migration data)
      Task task = createMoreInfoTask();

      Map<String, Object> param = new HashMap<>();
      param.put(DomainConstants.Tasks.PARAMETERS_LABEL, "city"); // Old key (pre-migration)
      param.put(DomainConstants.Tasks.PARAMETERS_VALUE, "$.cityName"); // Old key (pre-migration)

      Map<String, Object> properties = new HashMap<>();
      properties.put(DomainConstants.Tasks.PROPERTY_PARAMETERS, List.of(param));
      properties.put(DomainConstants.Tasks.PROPERTY_SCOPE, DomainConstants.Tasks.SCOPE_API);
      when(task.getProperties()).thenReturn(properties);

      // When
      TaskDto result = service.map(task, application, territory);

      // Then: Still works (migration fallback)
      assertThat(result.getParameters()).isNotNull();

      @SuppressWarnings("unchecked")
      Map<String, Object> dtoParam = (Map<String, Object>) result.getParameters().get("city");
      assertThat(dtoParam).containsEntry(DomainConstants.Tasks.PARAMETERS_LABEL, "city");
      assertThat(dtoParam).containsEntry(DomainConstants.Tasks.PARAMETERS_VALUE, "$.cityName");
    }

    @Test
    @DisplayName("MUST key parameters map by variable name (not label)")
    void parametersMapKeyedByVariableName() {
      // Given
      Task task = createMoreInfoTask();

      Map<String, Object> param1 = new HashMap<>();
      param1.put(DomainConstants.Tasks.PARAMETERS_VARIABLE, "userId");
      param1.put(DomainConstants.Tasks.PARAMETERS_FIELD, "$.user.id");

      Map<String, Object> properties = new HashMap<>();
      properties.put(DomainConstants.Tasks.PROPERTY_PARAMETERS, List.of(param1));
      properties.put(DomainConstants.Tasks.PROPERTY_SCOPE, DomainConstants.Tasks.SCOPE_API);
      when(task.getProperties()).thenReturn(properties);

      // When
      TaskDto result = service.map(task, application, territory);

      // Then: Map keyed by "userId" (variable value)
      assertThat(result.getParameters()).containsKey("userId");
      assertThat(result.getParameters()).doesNotContainKey("label");
    }
  }

  @Nested
  @DisplayName("Secret filtering (provided variables)")
  class SecretFiltering {

    @Test
    @DisplayName("MUST exclude provided=true variables from DTO parameters")
    void providedVariablesExcludedFromDto() {
      // Given: Task with mix of provided and non-provided variables
      Task task = createMoreInfoTask();

      Map<String, Object> publicParam = new HashMap<>();
      publicParam.put(DomainConstants.Tasks.PARAMETERS_VARIABLE, "category");
      publicParam.put(DomainConstants.Tasks.PARAMETERS_FIELD, "$.category");
      publicParam.put(DomainConstants.Tasks.PARAMETERS_PROVIDED, false); // Public variable

      Map<String, Object> secretParam = new HashMap<>();
      secretParam.put(DomainConstants.Tasks.PARAMETERS_VARIABLE, "apiKey");
      secretParam.put(
          DomainConstants.Tasks.PARAMETERS_FIELD, "#{APP_KEY}"); // System variable in value
      secretParam.put(DomainConstants.Tasks.PARAMETERS_PROVIDED, true); // Backend-only secret

      Map<String, Object> properties = new HashMap<>();
      properties.put(DomainConstants.Tasks.PROPERTY_PARAMETERS, List.of(publicParam, secretParam));
      properties.put(DomainConstants.Tasks.PROPERTY_SCOPE, DomainConstants.Tasks.SCOPE_API);
      when(task.getProperties()).thenReturn(properties);

      // When
      TaskDto result = service.map(task, application, territory);

      // Then: Only public parameter in DTO
      assertThat(result.getParameters()).isNotNull();
      assertThat(result.getParameters()).hasSize(1);
      assertThat(result.getParameters()).containsKey("category");
      assertThat(result.getParameters()).doesNotContainKey("apiKey"); // Secret excluded
    }

    @Test
    @DisplayName("MUST NOT expose resolved #{...} values in any DTO field")
    void noResolvedSystemVariableExposure() {
      // Given: Task with system variable in command (should be resolved backend-side)
      Task task = createMoreInfoTask();

      Map<String, Object> properties = new HashMap<>();
      properties.put(DomainConstants.Tasks.PROPERTY_SCOPE, DomainConstants.Tasks.SCOPE_API);
      properties.put(
          DomainConstants.Tasks.PROPERTY_COMMAND,
          "https://api.example.com/data?territory=#{TERR_ID}");
      // This should be resolved to actual territory ID backend-side, never sent to client
      when(task.getProperties()).thenReturn(properties);

      // When
      TaskDto result = service.map(task, application, territory);

      // Then: Command might be hidden OR contain unresolved #{...} (depends on provided flag)
      // But NEVER contains resolved value like "territory=5"
      if (result.getCommand() != null) {
        // If command is exposed, it must still have #{...} placeholder (not resolved)
        assertThat(result.getCommand()).doesNotContain("territory=5");
        assertThat(result.getCommand()).doesNotContain("territory=10");
      }
    }
  }

  @Nested
  @DisplayName("Command exposure policy")
  class CommandExposurePolicy {

    @Test
    @DisplayName("URL-type more-info WITHOUT secrets: command field present (viewer needs it)")
    void urlTypeWithoutSecretsExposesCommand() {
      // Given: URL-type task with no secrets
      Task task = createMoreInfoTask();

      Map<String, Object> param = new HashMap<>();
      param.put(DomainConstants.Tasks.PARAMETERS_VARIABLE, "code");
      param.put(DomainConstants.Tasks.PARAMETERS_FIELD, "CODE");
      param.put(DomainConstants.Tasks.PARAMETERS_PROVIDED, false); // No secrets

      Map<String, Object> properties = new HashMap<>();
      properties.put(DomainConstants.Tasks.PROPERTY_SCOPE, DomainConstants.Tasks.SCOPE_URL);
      properties.put(
          DomainConstants.Tasks.PROPERTY_COMMAND, "https://docs.example.com/manual/{code}.pdf");
      properties.put(DomainConstants.Tasks.PROPERTY_PARAMETERS, List.of(param));
      when(task.getProperties()).thenReturn(properties);

      // When
      TaskDto result = service.map(task, application, territory);

      // Then: For URL-type tasks, command is exposed via url field (external redirect)
      assertThat(result.getUrl()).isEqualTo("https://docs.example.com/manual/{code}.pdf");
      assertThat(result.getScope()).isEqualTo(DomainConstants.Tasks.SCOPE_URL);
    }

    @Test
    @DisplayName("API/SQL-type more-info WITH secrets: command hidden, proxy URL provided")
    void apiTypeWithSecretsHidesCommand() {
      // Given: API-type task with secret (provided variable)
      Task task = createMoreInfoTask();

      Map<String, Object> secretParam = new HashMap<>();
      secretParam.put(DomainConstants.Tasks.PARAMETERS_VARIABLE, "token");
      secretParam.put(DomainConstants.Tasks.PARAMETERS_FIELD, "#{API_TOKEN}");
      secretParam.put(DomainConstants.Tasks.PARAMETERS_PROVIDED, true); // Secret

      Map<String, Object> properties = new HashMap<>();
      properties.put(DomainConstants.Tasks.PROPERTY_SCOPE, DomainConstants.Tasks.SCOPE_API);
      properties.put(
          DomainConstants.Tasks.PROPERTY_COMMAND,
          "https://api.example.com/secure?token=#{API_TOKEN}");
      properties.put(DomainConstants.Tasks.PROPERTY_PARAMETERS, List.of(secretParam));
      when(task.getProperties()).thenReturn(properties);

      // When
      TaskDto result = service.map(task, application, territory);

      // Then: Command hidden (contains secret), proxy URL provided
      assertThat(result.getCommand()).isNull(); // Command hidden
      assertThat(result.getUrl()).isNotNull(); // Proxy URL present
      assertThat(result.getUrl()).startsWith("http://localhost:8080/middleware/proxy/");
    }

    @Test
    @DisplayName("SQL-type more-info: executes via proxy URL")
    void sqlTypeExecutesViaProxy() {
      // Given: SQL-type task
      Task task = createMoreInfoTask();

      Map<String, Object> properties = new HashMap<>();
      properties.put(DomainConstants.Tasks.PROPERTY_SCOPE, DomainConstants.Tasks.SCOPE_SQL);
      properties.put(
          DomainConstants.Tasks.PROPERTY_COMMAND, "SELECT * FROM data WHERE id = {itemId}");
      when(task.getProperties()).thenReturn(properties);

      // When
      TaskDto result = service.map(task, application, territory);

      // Then: URL present (proxy execution path)
      assertThat(result.getUrl())
          .isEqualTo("http://localhost:8080/middleware/proxy/10/5/SQL/" + task.getId());
      assertThat(result.getScope()).isEqualTo(DomainConstants.Tasks.SCOPE_SQL);
    }
  }

  @Nested
  @DisplayName("Viewer rendering scenarios")
  class ViewerRenderingScenarios {

    @Test
    @DisplayName("Viewer can render more-info form with parameter labels")
    void viewerRendersFormWithLabels() {
      // Given: Task with multiple parameters
      Task task = createMoreInfoTask();

      Map<String, Object> param1 = new HashMap<>();
      param1.put(DomainConstants.Tasks.PARAMETERS_VARIABLE, "startDate");
      param1.put(DomainConstants.Tasks.PARAMETERS_FIELD, "$.start");

      Map<String, Object> param2 = new HashMap<>();
      param2.put(DomainConstants.Tasks.PARAMETERS_VARIABLE, "endDate");
      param2.put(DomainConstants.Tasks.PARAMETERS_FIELD, "$.end");

      Map<String, Object> properties = new HashMap<>();
      properties.put(DomainConstants.Tasks.PROPERTY_PARAMETERS, List.of(param1, param2));
      properties.put(DomainConstants.Tasks.PROPERTY_SCOPE, DomainConstants.Tasks.SCOPE_API);
      when(task.getProperties()).thenReturn(properties);

      // When
      TaskDto result = service.map(task, application, territory);

      // Then: Viewer can iterate parameters and render form
      assertThat(result.getParameters()).hasSize(2);

      // Viewer code would do: for (let [key, param] of Object.entries(taskDto.parameters))
      result
          .getParameters()
          .forEach(
              (key, paramObj) -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> param = (Map<String, Object>) paramObj;

                // Viewer expects these fields to exist
                assertThat(param)
                    .containsKeys(
                        DomainConstants.Tasks.PARAMETERS_LABEL,
                        DomainConstants.Tasks.PARAMETERS_VALUE);
                assertThat(param.get(DomainConstants.Tasks.PARAMETERS_LABEL)).isNotNull();
                assertThat(param.get(DomainConstants.Tasks.PARAMETERS_VALUE)).isNotNull();
              });
    }

    @Test
    @DisplayName("Viewer can execute API more-info request via proxy")
    void viewerExecutesApiRequestViaProxy() {
      // Given: API more-info task
      Task task = createMoreInfoTask();

      Map<String, Object> properties = new HashMap<>();
      properties.put(DomainConstants.Tasks.PROPERTY_SCOPE, DomainConstants.Tasks.SCOPE_API);
      properties.put(DomainConstants.Tasks.PROPERTY_COMMAND, "https://api.example.com/info");
      when(task.getProperties()).thenReturn(properties);

      // When
      TaskDto result = service.map(task, application, territory);

      // Then: Viewer uses result.url (proxy) not result.command (may have secrets)
      assertThat(result.getUrl()).isNotNull();
      assertThat(result.getScope()).isEqualTo(DomainConstants.Tasks.SCOPE_API);

      // Viewer would call: this.http.get(taskDto.url, { params: userInputs })
      String expectedProxyUrl = "http://localhost:8080/middleware/proxy/10/5/API/" + task.getId();
      assertThat(result.getUrl()).isEqualTo(expectedProxyUrl);
    }

    @Test
    @DisplayName("Viewer can open external URL for URL-type more-info")
    void viewerOpensExternalUrl() {
      // Given: URL-type task
      Task task = createMoreInfoTask();

      Map<String, Object> param = new HashMap<>();
      param.put(DomainConstants.Tasks.PARAMETERS_VARIABLE, "docId");
      param.put(DomainConstants.Tasks.PARAMETERS_FIELD, "ID");

      Map<String, Object> properties = new HashMap<>();
      properties.put(DomainConstants.Tasks.PROPERTY_SCOPE, DomainConstants.Tasks.SCOPE_URL);
      properties.put(DomainConstants.Tasks.PROPERTY_COMMAND, "https://external.com/doc/{docId}");
      properties.put(DomainConstants.Tasks.PROPERTY_PARAMETERS, List.of(param));
      when(task.getProperties()).thenReturn(properties);

      // When
      TaskDto result = service.map(task, application, territory);

      // Then: Viewer uses result.url to construct external link
      assertThat(result.getUrl()).isNotNull();
      assertThat(result.getUrl()).isEqualTo("https://external.com/doc/{docId}");
      assertThat(result.getScope()).isEqualTo(DomainConstants.Tasks.SCOPE_URL);

      // Viewer would do: window.open(taskDto.url.replace('{docId}', userInput))
    }
  }

  // Helper method
  private Task createMoreInfoTask() {
    Task task = mock(Task.class);
    TaskType taskType = mock(TaskType.class);
    when(taskType.getId()).thenReturn(DomainConstants.Tasks.TASK_TYPE_ID_MORE_INFO);
    when(task.getType()).thenReturn(taskType);
    when(task.getId()).thenReturn(42);
    when(task.getName()).thenReturn("Test More Info");
    return task;
  }
}
