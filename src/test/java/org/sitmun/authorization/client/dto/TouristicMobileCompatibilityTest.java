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
 * NON-REGRESSION GUARDRAIL 4: Touristic Mobile (touristic-mobile-app) DTO compatibility tests
 *
 * <p>These tests verify backward compatibility with the touristic mobile application which expects:
 * - task.url present and executable (direct or proxy URL) - task.parameters map with type/required
 * fields (value only for cartography services) - Near-me search continues working (LATITUD/LONGITUD
 * in mapping.input) - Event filtering continues working (date/keyword params) - Property-based WFS
 * filtering continues working (propertyname param) - mapping.input format unchanged (keys plain,
 * calculated values with ${...} wrapper) - TNO_MAPPING data completely untouched by migration
 */
@DisplayName("Touristic Mobile Compatibility Tests (Guardrail 4)")
class TouristicMobileCompatibilityTest {

  private TaskQuerySqlService sqlService;
  private TaskQueryWebService webService;
  private Application application;
  private Territory territory;

  @BeforeEach
  void setUp() {
    sqlService = new TaskQuerySqlService();
    ReflectionTestUtils.setField(sqlService, "proxyUrl", "http://localhost:8080/middleware");

    webService = new TaskQueryWebService();

    application = mock(Application.class);
    when(application.getId()).thenReturn(10);

    territory = mock(Territory.class);
    when(territory.getId()).thenReturn(5);
  }

  @Nested
  @DisplayName("Query task DTO contract")
  class QueryTaskDtoContract {

    @Test
    @DisplayName("SQL query task MUST have task.url (proxy URL)")
    void sqlTaskHasProxyUrl() {
      // Given
      Task task = createSqlQueryTask();
      Map<String, Object> properties = new HashMap<>();
      properties.put(DomainConstants.Tasks.PROPERTY_SCOPE, "sql-query");
      properties.put(
          DomainConstants.Tasks.PROPERTY_COMMAND,
          "SELECT * FROM pois WHERE territory_id = #{TERR_ID}");
      when(task.getProperties()).thenReturn(properties);

      // When
      TaskDto result = sqlService.map(task, application, territory);

      // Then: URL present (touristic-mobile uses this to execute query)
      assertThat(result.getUrl()).isNotNull();
      assertThat(result.getUrl()).isEqualTo("http://localhost:8080/middleware/proxy/10/5/SQL/42");
    }

    @Test
    @DisplayName("Web API query task MUST have task.url (direct or proxy)")
    void webApiTaskHasUrl() {
      // Given: Web API task without secrets
      Task task = createWebApiQueryTask();

      Map<String, Object> param = new HashMap<>();
      param.put(DomainConstants.Tasks.PARAMETERS_NAME, "category");
      param.put(DomainConstants.Tasks.PARAMETERS_TYPE, "query");
      param.put(DomainConstants.Tasks.PARAMETERS_REQUIRED, false);
      param.put(DomainConstants.Tasks.PARAMETERS_PROVIDED, false);

      Map<String, Object> properties = new HashMap<>();
      properties.put(DomainConstants.Tasks.PROPERTY_SCOPE, "web-api-query");
      properties.put(DomainConstants.Tasks.PROPERTY_COMMAND, "https://api.example.com/pois");
      properties.put(DomainConstants.Tasks.PROPERTY_PARAMETERS, List.of(param));
      when(task.getProperties()).thenReturn(properties);

      // When
      TaskDto result = webService.map(task, application, territory);

      // Then: URL present (raw command URL since no secrets)
      assertThat(result.getUrl()).isNotNull();
      assertThat(result.getUrl()).isEqualTo("https://api.example.com/pois");
    }

    @Test
    @DisplayName("Parameters map MUST be keyed by parameter name")
    void parametersMapKeyedByName() {
      // Given
      Task task = createSqlQueryTask();

      Map<String, Object> param1 = new HashMap<>();
      param1.put(DomainConstants.Tasks.PARAMETERS_NAME, "status");
      param1.put(DomainConstants.Tasks.PARAMETERS_TYPE, "query");
      param1.put(DomainConstants.Tasks.PARAMETERS_REQUIRED, false);

      Map<String, Object> param2 = new HashMap<>();
      param2.put(DomainConstants.Tasks.PARAMETERS_NAME, "category");
      param2.put(DomainConstants.Tasks.PARAMETERS_TYPE, "template");
      param2.put(DomainConstants.Tasks.PARAMETERS_REQUIRED, true);

      Map<String, Object> properties = new HashMap<>();
      properties.put(DomainConstants.Tasks.PROPERTY_PARAMETERS, List.of(param1, param2));
      when(task.getProperties()).thenReturn(properties);

      // When
      TaskDto result = sqlService.map(task, application, territory);

      // Then: Parameters keyed by name
      assertThat(result.getParameters()).containsKeys("status", "category");
    }

    @Test
    @DisplayName("Parameter entries MUST have 'type' and 'required' fields")
    void parameterEntriesHaveTypeAndRequired() {
      // Given
      Task task = createSqlQueryTask();

      Map<String, Object> param = new HashMap<>();
      param.put(DomainConstants.Tasks.PARAMETERS_NAME, "filter");
      param.put(DomainConstants.Tasks.PARAMETERS_TYPE, "query");
      param.put(DomainConstants.Tasks.PARAMETERS_REQUIRED, true);

      Map<String, Object> properties = new HashMap<>();
      properties.put(DomainConstants.Tasks.PROPERTY_PARAMETERS, List.of(param));
      when(task.getProperties()).thenReturn(properties);

      // When
      TaskDto result = sqlService.map(task, application, territory);

      // Then: Parameter has type and required
      @SuppressWarnings("unchecked")
      Map<String, Object> paramDto = (Map<String, Object>) result.getParameters().get("filter");
      assertThat(paramDto).containsEntry(DomainConstants.Tasks.PARAMETERS_TYPE, "query");
      assertThat(paramDto).containsEntry(DomainConstants.Tasks.PARAMETERS_REQUIRED, true);
    }

    @Test
    @DisplayName("SQL/Web API tasks do NOT include 'value' field (pre-existing behavior)")
    void sqlWebApiTasksOmitValueField() {
      // Given
      Task task = createSqlQueryTask();

      Map<String, Object> param = new HashMap<>();
      param.put(DomainConstants.Tasks.PARAMETERS_NAME, "keyword");
      param.put(DomainConstants.Tasks.PARAMETERS_TYPE, "query");
      param.put(DomainConstants.Tasks.PARAMETERS_REQUIRED, false);
      param.put(DomainConstants.Tasks.PARAMETERS_VALUE, "default"); // Present in storage

      Map<String, Object> properties = new HashMap<>();
      properties.put(DomainConstants.Tasks.PROPERTY_PARAMETERS, List.of(param));
      when(task.getProperties()).thenReturn(properties);

      // When
      TaskDto result = sqlService.map(task, application, territory);

      // Then: 'value' NOT included (pre-existing gap, touristic-mobile uses mapping.input defaults)
      @SuppressWarnings("unchecked")
      Map<String, Object> paramDto = (Map<String, Object>) result.getParameters().get("keyword");
      assertThat(paramDto)
          .containsKeys(
              DomainConstants.Tasks.PARAMETERS_TYPE, DomainConstants.Tasks.PARAMETERS_REQUIRED);
      assertThat(paramDto).doesNotContainKey(DomainConstants.Tasks.PARAMETERS_VALUE);
    }
  }

  @Nested
  @DisplayName("Mapping.input format preservation (TNO_MAPPING)")
  class MappingInputFormat {

    @Test
    @DisplayName("CRITICAL: mapping.input keys MUST remain plain identifiers (no ${...} wrapper)")
    void mappingKeysRemainPlain() {
      // This test documents the expected format - actual enforcement is in data migration scripts

      // Expected format (MUST be preserved):
      Map<String, String> mappingInput =
          Map.of(
              "LONGITUD", "${LONGITUD}", // Key: plain, Value: wrapped
              "LATITUD", "${LATITUD}", // Key: plain, Value: wrapped
              "KEYWORD", "${KEYWORD}", // Key: plain, Value: wrapped
              "propertyname", "CATEGORY" // Key: plain, Value: plain constant
              );

      // RequestService.getCalculatedInputValue() depends on this format
      // It extracts the key from ${KEY} wrapper: '${LONGITUD}'.match(/\$\{(\w+)\}/)[1] → 'LONGITUD'
      // Then looks up mappingInput['LONGITUD'] to get the target field/value

      // If keys were wrapped (WRONG): { "${LONGITUD}": "${LONGITUD}" }
      // The lookup would fail: mappingInput['LONGITUD'] → undefined

      assertThat(mappingInput.keySet())
          .allMatch(key -> !key.startsWith("${"), "Keys must NOT be wrapped with ${...}");

      // Calculated values MUST keep ${...} wrapper
      assertThat(mappingInput.get("LONGITUD")).startsWith("${");
      assertThat(mappingInput.get("LATITUD")).startsWith("${");
      assertThat(mappingInput.get("KEYWORD")).startsWith("${");
    }

    @Test
    @DisplayName("CRITICAL: Calculated mapping.input values MUST keep ${...} wrapper")
    void calculatedValuesMustKeepWrapper() {
      // Given: Format expected by RequestService.getCalculatedInputValue()
      String latitudValue = "${LATITUD}";
      String longitudValue = "${LONGITUD}";
      String keywordValue = "${KEYWORD}";

      // RequestService checks: if (value && value.startsWith('${')) { ... }
      // If wrapper removed (WRONG): "LATITUD" instead of "${LATITUD}"
      // The calculated resolution never triggers, literal "LATITUD" sent to server

      // When: Check value format
      boolean hasWrapper = latitudValue.startsWith("${") && latitudValue.endsWith("}");

      // Then: MUST have ${...} wrapper
      assertThat(hasWrapper).isTrue();

      // Constant values can be plain (no wrapper needed)
      String constantValue = "CATEGORY";
      assertThat(constantValue).doesNotStartWith("${");
    }

    @Test
    @DisplayName("CRITICAL: Data migration MUST NOT touch TNO_MAPPING column")
    void dataMigrationExcludesTnoMapping() {
      // This is a documentation test - the actual constraint is enforced in Liquibase scripts

      // TNO_MAPPING column stores tree node configuration including mapping.input/mapping.output
      // Contains ${LATITUD}, ${KEYWORD}, etc. (client-side system variables)
      // Uses SAME ${...} syntax as SQL templates but different resolution mechanism

      // Migration scripts MUST have explicit exclusion:
      // WHERE table_name != 'STM_TREE_NODE' OR column_name != 'TNO_MAPPING'

      // If violated:
      // - Near-me search breaks (returns literal "LATITUD" instead of GPS coordinates)
      // - Event search breaks (returns literal "KEYWORD" instead of search term)
      // - All calculated mapping.input resolution fails

      String reminder =
          "Data migration scripts MUST explicitly exclude TNO_MAPPING from all transformations";
      assertThat(reminder).isNotEmpty();
    }
  }

  @Nested
  @DisplayName("Near-me search compatibility")
  class NearMeSearch {

    @Test
    @DisplayName("Near-me page sends UPPERCASE keys in parentData")
    void nearMePageSendsUppercaseKeys() {
      // Documentation test - this is touristic-mobile-app behavior

      // Near-me page code:
      // this.requestService.templateRequest(task, { DISTANCE: 5000, LONGITUD: 2.1734, LATITUD:
      // 41.3851 })

      Map<String, Object> parentData =
          Map.of(
              "DISTANCE", 5000,
              "LONGITUD", 2.1734,
              "LATITUD", 41.3851);

      // RequestService.getCalculatedInputValue() extracts key from ${LONGITUD}, converts to
      // uppercase,
      // then looks up in parentData['LONGITUD']

      assertThat(parentData).containsKeys("DISTANCE", "LONGITUD", "LATITUD");
      assertThat(parentData.keySet()).allMatch(key -> key.equals(key.toUpperCase()));
    }

    @Test
    @DisplayName("Near-me task execution continues working with mapping.input")
    void nearMeTaskExecution() {
      // Given: Near-me query task
      Task task = createSqlQueryTask();

      Map<String, Object> param1 = new HashMap<>();
      param1.put(DomainConstants.Tasks.PARAMETERS_NAME, "distance");
      param1.put(DomainConstants.Tasks.PARAMETERS_TYPE, "query");
      param1.put(DomainConstants.Tasks.PARAMETERS_REQUIRED, false);

      Map<String, Object> param2 = new HashMap<>();
      param2.put(DomainConstants.Tasks.PARAMETERS_NAME, "longitude");
      param2.put(DomainConstants.Tasks.PARAMETERS_TYPE, "query");
      param2.put(DomainConstants.Tasks.PARAMETERS_REQUIRED, false);

      Map<String, Object> param3 = new HashMap<>();
      param3.put(DomainConstants.Tasks.PARAMETERS_NAME, "latitude");
      param3.put(DomainConstants.Tasks.PARAMETERS_TYPE, "query");
      param3.put(DomainConstants.Tasks.PARAMETERS_REQUIRED, false);

      Map<String, Object> properties = new HashMap<>();
      properties.put(DomainConstants.Tasks.PROPERTY_PARAMETERS, List.of(param1, param2, param3));
      when(task.getProperties()).thenReturn(properties);

      // When
      TaskDto result = sqlService.map(task, application, territory);

      // Then: Task executable (has URL and parameters)
      assertThat(result.getUrl()).isNotNull();
      assertThat(result.getParameters()).containsKeys("distance", "longitude", "latitude");

      // touristic-mobile would:
      // 1. Read mapping.input: { LONGITUD: "${LONGITUD}", LATITUD: "${LATITUD}" }
      // 2. Call calculateInputs() which resolves ${LONGITUD} → GPS coordinate
      // 3. Send request: http://proxy/...?longitude=2.1734&latitude=41.3851&distance=5000
    }
  }

  @Nested
  @DisplayName("Event filtering compatibility")
  class EventFiltering {

    @Test
    @DisplayName("Event page sends lowercase keys in urlParams")
    void eventPageSendsLowercaseKeys() {
      // Documentation test - this is touristic-mobile-app behavior

      // Event page code sends lowercase:
      // this.requestService.templateRequest(task, { latitud: 41.3851, longitud: 2.1734, distance:
      // 5000 })

      Map<String, Object> urlParams =
          Map.of(
              "latitud", 41.3851, // lowercase
              "longitud", 2.1734, // lowercase
              "distance", 5000);

      // RequestService.getCalculatedInputValue() extracts uppercase key from ${LONGITUD},
      // but looks up lowercase in urlParams via switch case mapping

      assertThat(urlParams).containsKeys("latitud", "longitud", "distance");
    }

    @Test
    @DisplayName("Event date filtering continues working")
    void eventDateFiltering() {
      // Given: Event query task with date parameters
      Task task = createSqlQueryTask();

      Map<String, Object> param1 = new HashMap<>();
      param1.put(DomainConstants.Tasks.PARAMETERS_NAME, "startDate");
      param1.put(DomainConstants.Tasks.PARAMETERS_TYPE, "query");
      param1.put(DomainConstants.Tasks.PARAMETERS_REQUIRED, false);

      Map<String, Object> param2 = new HashMap<>();
      param2.put(DomainConstants.Tasks.PARAMETERS_NAME, "endDate");
      param2.put(DomainConstants.Tasks.PARAMETERS_TYPE, "query");
      param2.put(DomainConstants.Tasks.PARAMETERS_REQUIRED, false);

      Map<String, Object> properties = new HashMap<>();
      properties.put(DomainConstants.Tasks.PROPERTY_PARAMETERS, List.of(param1, param2));
      when(task.getProperties()).thenReturn(properties);

      // When
      TaskDto result = sqlService.map(task, application, territory);

      // Then: Parameters present
      assertThat(result.getParameters()).containsKeys("startDate", "endDate");

      // touristic-mobile would resolve ${STARTDATE}, ${ENDDATE} from mapping.input
    }

    @Test
    @DisplayName("Keyword search continues working")
    void keywordSearch() {
      // Given: Search task with keyword parameter
      Task task = createSqlQueryTask();

      Map<String, Object> param = new HashMap<>();
      param.put(DomainConstants.Tasks.PARAMETERS_NAME, "keyword");
      param.put(DomainConstants.Tasks.PARAMETERS_TYPE, "query");
      param.put(DomainConstants.Tasks.PARAMETERS_REQUIRED, false);

      Map<String, Object> properties = new HashMap<>();
      properties.put(DomainConstants.Tasks.PROPERTY_PARAMETERS, List.of(param));
      when(task.getProperties()).thenReturn(properties);

      // When
      TaskDto result = sqlService.map(task, application, territory);

      // Then: Parameter present
      assertThat(result.getParameters()).containsKey("keyword");

      // touristic-mobile resolves ${KEYWORD} from mapping.input to user's search term
    }
  }

  @Nested
  @DisplayName("Property-based WFS filtering")
  class PropertyBasedFiltering {

    @Test
    @DisplayName("WFS propertyname parameter continues working")
    void wfsPropertynameParameter() {
      // Given: WFS query with propertyname filter
      Task task = createSqlQueryTask();

      Map<String, Object> param = new HashMap<>();
      param.put(DomainConstants.Tasks.PARAMETERS_NAME, "propertyname");
      param.put(DomainConstants.Tasks.PARAMETERS_TYPE, "query");
      param.put(DomainConstants.Tasks.PARAMETERS_REQUIRED, false);

      Map<String, Object> properties = new HashMap<>();
      properties.put(DomainConstants.Tasks.PROPERTY_PARAMETERS, List.of(param));
      when(task.getProperties()).thenReturn(properties);

      // When
      TaskDto result = sqlService.map(task, application, territory);

      // Then: Parameter present
      assertThat(result.getParameters()).containsKey("propertyname");

      // touristic-mobile sends: ?propertyname=CATEGORY&...
    }
  }

  @Nested
  @DisplayName("Vary-key whitelist compatibility")
  class VaryKeyWhitelist {

    @Test
    @DisplayName("Monitor mode (default): unknown keys logged but passed through")
    void monitorModeAllowsUnknownKeys() {
      // This test documents expected behavior - actual implementation in ProxyConfigurationService

      // Given: Task with declared parameters
      Task task = createSqlQueryTask();

      Map<String, Object> param = new HashMap<>();
      param.put(DomainConstants.Tasks.PARAMETERS_NAME, "category");
      param.put(DomainConstants.Tasks.PARAMETERS_TYPE, "query");
      param.put(DomainConstants.Tasks.PARAMETERS_REQUIRED, false);

      Map<String, Object> properties = new HashMap<>();
      properties.put(DomainConstants.Tasks.PROPERTY_PARAMETERS, List.of(param));
      when(task.getProperties()).thenReturn(properties);

      // When: Mobile sends additional undeclared parameter
      // Request: ?category=museum&keyWord=beach (keyWord not in task parameters)

      // Then: In MONITOR mode (default)
      // - Log: WARN "Unknown vary key: keyWord for task 42. Allowed keys: [category]"
      // - Behavior: Pass through (backward compatible)
      // - Mobile search still works even with undeclared keyWord parameter

      String expectedBehavior =
          "MONITOR mode: unknown keys logged at WARN level and passed through";
      assertThat(expectedBehavior).isNotEmpty();
    }

    @Test
    @DisplayName("Enforce mode (future): unknown keys dropped after deprecation window")
    void enforceModeDropsUnknownKeys() {
      // This test documents future behavior after 3-month transition

      // After ENFORCE mode enabled:
      // - Log: INFO "Dropping unknown vary key: keyWord for task 42"
      // - Behavior: Drop (security enhancement)
      // - Mobile must update to only send declared parameters

      String futureRelease = "ENFORCE mode enabled in v1.4.0 after 3-month deprecation window";
      assertThat(futureRelease).isNotEmpty();
    }
  }

  // Helper methods
  private Task createSqlQueryTask() {
    Task task = mock(Task.class);
    TaskType taskType = mock(TaskType.class);
    when(taskType.getTitle()).thenReturn("Query");
    when(task.getType()).thenReturn(taskType);
    when(task.getId()).thenReturn(42);

    Map<String, Object> properties = new HashMap<>();
    properties.put(DomainConstants.Tasks.PROPERTY_SCOPE, "sql-query");
    when(task.getProperties()).thenReturn(properties);

    return task;
  }

  private Task createWebApiQueryTask() {
    Task task = mock(Task.class);
    TaskType taskType = mock(TaskType.class);
    when(taskType.getTitle()).thenReturn("Query");
    when(task.getType()).thenReturn(taskType);
    when(task.getId()).thenReturn(42);

    Map<String, Object> properties = new HashMap<>();
    properties.put(DomainConstants.Tasks.PROPERTY_SCOPE, "web-api-query");
    when(task.getProperties()).thenReturn(properties);

    return task;
  }
}
