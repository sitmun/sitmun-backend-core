package org.sitmun.authorization.client.dto;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.sitmun.domain.DomainConstants.Tasks.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sitmun.authorization.client.dto.profile.QueryParameter;
import org.sitmun.authorization.client.service.TaskQuerySqlService;
import org.sitmun.authorization.client.service.TaskQueryWebService;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.type.TaskType;
import org.sitmun.domain.territory.Territory;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * NON-REGRESSION GUARDRAIL 4: Touristic Mobile (touristic-mobile-app) DTO compatibility tests
 *
 * <p>These tests verify backward compatibility with the touristic mobile application which expects:
 *
 * <ul>
 *   <li>task.url present and executable (direct or proxy URL)
 *   <li>task.parameters map with type/required fields (value only for cartography services)
 *   <li>Near-me search continues working (distance, longitude, and latitude parameters)
 *   <li>Event filtering continues working (date/keyword params)
 *   <li>Property-based WFS filtering continues working (propertyname param)
 * </ul>
 */
@DisplayName("Touristic Mobile Compatibility Tests (Guardrail 4)")
class TouristicMobileCompatibilityTest {

  private TaskQuerySqlService sqlService;
  private TaskQueryWebService webService;
  private Application application;
  private Territory territory;

  @BeforeEach
  void setUp() {
    // Create a real TaskParameterProcessor with mock SystemVariableResolver
    org.sitmun.infrastructure.variables.SystemVariableResolver mockResolver =
        mock(org.sitmun.infrastructure.variables.SystemVariableResolver.class);
    when(mockResolver.resolve(anyString(), any())).thenAnswer(inv -> inv.getArgument(0));

    org.sitmun.domain.task.parameter.TaskParameterProcessor processor =
        new org.sitmun.domain.task.parameter.TaskParameterProcessor(mockResolver);

    sqlService = new TaskQuerySqlService(processor);
    ReflectionTestUtils.setField(sqlService, "proxyUrl", "http://localhost:8080/middleware");

    webService = new TaskQueryWebService(processor);
    ReflectionTestUtils.setField(webService, "proxyUrl", "http://localhost:8080/middleware");

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
      properties.put(PROPERTY_SCOPE, SCOPE_SQL_QUERY);
      properties.put(PROPERTY_COMMAND, "SELECT * FROM pois WHERE territory_id = #{TERR_ID}");
      when(task.getProperties()).thenReturn(properties);

      // When
      TaskDto result = sqlService.map(task, application, territory);

      // Then: URL present (touristic-mobile uses this to execute query)
      assertThat(result.getUrl()).isNotNull();
      assertThat(result.getUrl()).isEqualTo("http://localhost:8080/middleware/proxy/10/5/SQL/42");
    }

    @Test
    @DisplayName(
        "web-api-query MUST expose task.url as middleware proxy (upstream never in client url)")
    void webApiQueryScopeUsesProxyUrl() {
      Task task = createWebApiQueryTask();

      Map<String, Object> param = new HashMap<>();
      param.put(PARAMETERS_NAME, "category");
      param.put(PARAMETERS_TYPE, "query");
      param.put(PARAMETERS_REQUIRED, false);
      param.put(PARAMETERS_PROVIDED, false);

      Map<String, Object> properties = new HashMap<>();
      properties.put(PROPERTY_SCOPE, SCOPE_WEB_API_QUERY);
      properties.put(PROPERTY_COMMAND, "https://api.example.com/pois");
      properties.put(PROPERTY_PARAMETERS, List.of(param));
      when(task.getProperties()).thenReturn(properties);

      TaskDto result = webService.map(task, application, territory);

      assertThat(result.getUrl()).isNotNull();
      assertThat(result.getUrl()).isEqualTo("http://localhost:8080/middleware/proxy/10/5/API/42");
      assertThat(result.getScope()).isEqualTo(SCOPE_API);
    }

    @Test
    @DisplayName("web-api-query-no-proxy MUST expose task.url as direct command URL")
    void webApiQueryNoProxyScopeUsesDirectCommandUrl() {
      Task task = createWebApiQueryNoProxyTask();

      Map<String, Object> param = new HashMap<>();
      param.put(PARAMETERS_NAME, "category");
      param.put(PARAMETERS_TYPE, "query");
      param.put(PARAMETERS_REQUIRED, false);
      param.put(PARAMETERS_PROVIDED, false);

      Map<String, Object> properties = new HashMap<>();
      properties.put(PROPERTY_SCOPE, SCOPE_WEB_API_QUERY_NO_PROXY);
      properties.put(PROPERTY_COMMAND, "https://api.example.com/pois");
      properties.put(PROPERTY_PARAMETERS, List.of(param));
      when(task.getProperties()).thenReturn(properties);

      TaskDto result = webService.map(task, application, territory);

      assertThat(result.getUrl()).isNotNull();
      assertThat(result.getUrl()).isEqualTo("https://api.example.com/pois");
      assertThat(result.getScope()).isEqualTo(SCOPE_URL);
    }

    @Test
    @DisplayName("Parameters map MUST be keyed by parameter name")
    void parametersMapKeyedByName() {
      // Given
      Task task = createSqlQueryTask();

      Map<String, Object> param1 = new HashMap<>();
      param1.put(PARAMETERS_NAME, "status");
      param1.put(PARAMETERS_TYPE, "query");
      param1.put(PARAMETERS_REQUIRED, false);

      Map<String, Object> param2 = new HashMap<>();
      param2.put(PARAMETERS_NAME, "category");
      param2.put(PARAMETERS_TYPE, "template");
      param2.put(PARAMETERS_REQUIRED, true);

      Map<String, Object> properties = new HashMap<>();
      properties.put(PROPERTY_PARAMETERS, List.of(param1, param2));
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
      param.put(PARAMETERS_NAME, "filter");
      param.put(PARAMETERS_TYPE, "query");
      param.put(PARAMETERS_REQUIRED, true);

      Map<String, Object> properties = new HashMap<>();
      properties.put(PROPERTY_PARAMETERS, List.of(param));
      when(task.getProperties()).thenReturn(properties);

      // When
      TaskDto result = sqlService.map(task, application, territory);

      // Then: Parameter has type and required
      QueryParameter paramDto = (QueryParameter) result.getParameters().get("filter");
      assertThat(paramDto.type()).isEqualTo("query");
      assertThat(paramDto.required()).isTrue();
    }
  }

  @Nested
  @DisplayName("Near-me search compatibility")
  class NearMeSearch {

    @Test
    @DisplayName("Near-me task execution continues working with mapping.input")
    void nearMeTaskExecution() {
      // Given: Near-me query task
      Task task = createSqlQueryTask();

      Map<String, Object> param1 = new HashMap<>();
      param1.put(PARAMETERS_NAME, "distance");
      param1.put(PARAMETERS_TYPE, "query");
      param1.put(PARAMETERS_REQUIRED, false);

      Map<String, Object> param2 = new HashMap<>();
      param2.put(PARAMETERS_NAME, "longitude");
      param2.put(PARAMETERS_TYPE, "query");
      param2.put(PARAMETERS_REQUIRED, false);

      Map<String, Object> param3 = new HashMap<>();
      param3.put(PARAMETERS_NAME, "latitude");
      param3.put(PARAMETERS_TYPE, "query");
      param3.put(PARAMETERS_REQUIRED, false);

      Map<String, Object> properties = new HashMap<>();
      properties.put(PROPERTY_PARAMETERS, List.of(param1, param2, param3));
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
    @DisplayName("Event date filtering continues working")
    void eventDateFiltering() {
      // Given: Event query task with date parameters
      Task task = createSqlQueryTask();

      Map<String, Object> param1 = new HashMap<>();
      param1.put(PARAMETERS_NAME, "startDate");
      param1.put(PARAMETERS_TYPE, "query");
      param1.put(PARAMETERS_REQUIRED, false);

      Map<String, Object> param2 = new HashMap<>();
      param2.put(PARAMETERS_NAME, "endDate");
      param2.put(PARAMETERS_TYPE, "query");
      param2.put(PARAMETERS_REQUIRED, false);

      Map<String, Object> properties = new HashMap<>();
      properties.put(PROPERTY_PARAMETERS, List.of(param1, param2));
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
      param.put(PARAMETERS_NAME, "keyword");
      param.put(PARAMETERS_TYPE, "query");
      param.put(PARAMETERS_REQUIRED, false);

      Map<String, Object> properties = new HashMap<>();
      properties.put(PROPERTY_PARAMETERS, List.of(param));
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
      param.put(PARAMETERS_NAME, "propertyname");
      param.put(PARAMETERS_TYPE, "query");
      param.put(PARAMETERS_REQUIRED, false);

      Map<String, Object> properties = new HashMap<>();
      properties.put(PROPERTY_PARAMETERS, List.of(param));
      when(task.getProperties()).thenReturn(properties);

      // When
      TaskDto result = sqlService.map(task, application, territory);

      // Then: Parameter present
      assertThat(result.getParameters()).containsKey("propertyname");

      // touristic-mobile sends: ?propertyname=CATEGORY&...
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
    properties.put(PROPERTY_SCOPE, SCOPE_SQL_QUERY);
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
    properties.put(PROPERTY_SCOPE, SCOPE_WEB_API_QUERY);
    when(task.getProperties()).thenReturn(properties);

    return task;
  }

  private Task createWebApiQueryNoProxyTask() {
    Task task = mock(Task.class);
    TaskType taskType = mock(TaskType.class);
    when(taskType.getTitle()).thenReturn("Query");
    when(task.getType()).thenReturn(taskType);
    when(task.getId()).thenReturn(42);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_SCOPE, SCOPE_WEB_API_QUERY_NO_PROXY);
    when(task.getProperties()).thenReturn(properties);

    return task;
  }
}
