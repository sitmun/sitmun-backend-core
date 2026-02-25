package org.sitmun.authorization.proxy.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.sitmun.domain.DomainConstants.Services.TYPE_API;
import static org.sitmun.domain.DomainConstants.Services.TYPE_WMS;
import static org.sitmun.domain.DomainConstants.Tasks.PROPERTY_COMMAND;
import static org.sitmun.domain.DomainConstants.Tasks.PROXY_TYPE_SQL;

import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.authorization.proxy.decorators.QueryFixedFiltersDecorator;
import org.sitmun.authorization.proxy.decorators.QueryPaginationDecorator;
import org.sitmun.authorization.proxy.decorators.QueryVaryFiltersDecorator;
import org.sitmun.authorization.proxy.dto.ConfigProxyDto;
import org.sitmun.authorization.proxy.dto.ConfigProxyRequestDto;
import org.sitmun.authorization.proxy.exception.BadRequestException;
import org.sitmun.authorization.proxy.protocols.jdbc.JdbcPayloadDto;
import org.sitmun.authorization.proxy.protocols.wms.WmsPayloadDto;
import org.sitmun.domain.database.DatabaseConnection;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.service.ServiceRepository;
import org.sitmun.domain.service.parameter.ServiceParameter;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.TaskRepository;
import org.sitmun.domain.territory.TerritoryRepository;
import org.sitmun.domain.user.UserRepository;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProxyConfigurationServiceTest {

  @Mock private ServiceRepository serviceRepository;
  @Mock private TaskRepository taskRepository;
  @Mock private UserRepository userRepository;
  @Mock private TerritoryRepository territoryRepository;
  @Mock private QueryFixedFiltersDecorator queryFixedFiltersDecorator;
  @Mock private QueryVaryFiltersDecorator queryVaryFiltersDecorator;
  @Mock private QueryPaginationDecorator queryPaginationDecorator;

  private ProxyConfigurationService service;

  @BeforeEach
  void setUp() {
    service =
        new ProxyConfigurationService(
            serviceRepository,
            taskRepository,
            userRepository,
            territoryRepository,
            queryFixedFiltersDecorator,
            queryVaryFiltersDecorator,
            queryPaginationDecorator,
            Collections.emptyList()); // Empty validators list for non-validation tests
    ReflectionTestUtils.setField(service, "responseValidityTime", 3600);
    ReflectionTestUtils.setField(service, "validateUserAccessEnabled", false);
  }

  @Test
  @DisplayName("getConfiguration returns OgcWmsPayloadDto for WMS service")
  void getConfigurationReturnsOgcWmsPayloadForWmsService() {
    // Given
    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(1)
            .terId(1)
            .type(TYPE_WMS)
            .typeId(1)
            .method("GET")
            .parameters(new HashMap<>())
            .build();

    Service mockService = mock(Service.class);
    when(mockService.getServiceURL()).thenReturn("https://example.com/wms");
    when(mockService.getType()).thenReturn(TYPE_WMS);
    when(mockService.getPasswordSet()).thenReturn(false);
    when(mockService.getParameters()).thenReturn(new HashSet<>());

    when(serviceRepository.findById(1)).thenReturn(Optional.of(mockService));

    // When
    ConfigProxyDto result = service.getConfiguration(request, 0L);

    // Then
    assertNotNull(result);
    assertEquals(TYPE_WMS, result.getType());
    assertInstanceOf(WmsPayloadDto.class, result.getPayload());

    WmsPayloadDto payload = (WmsPayloadDto) result.getPayload();
    assertEquals("https://example.com/wms", payload.getUri());
    assertEquals("GET", payload.getMethod());
    assertNull(payload.getSecurity());
  }

  @Test
  @DisplayName("getConfiguration returns OgcWmsPayloadDto with security for authenticated service")
  void getConfigurationReturnsOgcWmsPayloadWithSecurity() {
    // Given
    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(1)
            .terId(1)
            .type(TYPE_WMS)
            .typeId(1)
            .method("GET")
            .parameters(new HashMap<>())
            .build();

    Service mockService = mock(Service.class);
    when(mockService.getServiceURL()).thenReturn("https://example.com/wms");
    when(mockService.getType()).thenReturn(TYPE_WMS);
    when(mockService.getPasswordSet()).thenReturn(true);
    when(mockService.getUser()).thenReturn("wmsuser");
    when(mockService.getPassword()).thenReturn("wmspass");
    when(mockService.getParameters()).thenReturn(new HashSet<>());

    when(serviceRepository.findById(1)).thenReturn(Optional.of(mockService));

    // When
    ConfigProxyDto result = service.getConfiguration(request, 0L);

    // Then
    assertNotNull(result);
    assertInstanceOf(WmsPayloadDto.class, result.getPayload());

    WmsPayloadDto payload = (WmsPayloadDto) result.getPayload();
    assertNotNull(payload.getSecurity());
    assertEquals("http", payload.getSecurity().getType());
    assertEquals("basic", payload.getSecurity().getScheme());
    assertEquals("wmsuser", payload.getSecurity().getUsername());
    assertEquals("wmspass", payload.getSecurity().getPassword());
  }

  @Test
  @DisplayName("getConfiguration includes service parameters in OgcWmsPayloadDto")
  void getConfigurationIncludesServiceParameters() {
    // Given
    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(1)
            .terId(1)
            .type(TYPE_WMS)
            .typeId(1)
            .method("GET")
            .parameters(new HashMap<>())
            .build();

    ServiceParameter param1 = mock(ServiceParameter.class);
    when(param1.getType()).thenReturn("FIXED");
    when(param1.getName()).thenReturn("format");
    when(param1.getValue()).thenReturn("image/png");

    ServiceParameter param2 = mock(ServiceParameter.class);
    when(param2.getType()).thenReturn("VARY");
    when(param2.getName()).thenReturn("layers");

    Set<ServiceParameter> serviceParams = new HashSet<>(Arrays.asList(param1, param2));

    Service mockService = mock(Service.class);
    when(mockService.getServiceURL()).thenReturn("https://example.com/wms");
    when(mockService.getType()).thenReturn(TYPE_WMS);
    when(mockService.getPasswordSet()).thenReturn(false);
    when(mockService.getParameters()).thenReturn(serviceParams);

    when(serviceRepository.findById(1)).thenReturn(Optional.of(mockService));

    // When
    ConfigProxyDto result = service.getConfiguration(request, 0L);

    // Then
    assertNotNull(result);
    assertInstanceOf(WmsPayloadDto.class, result.getPayload());

    WmsPayloadDto payload = (WmsPayloadDto) result.getPayload();
    assertEquals("image/png", payload.getParameters().get("format"));
    assertEquals(List.of("layers"), payload.getVary());
  }

  @Test
  @DisplayName("getConfiguration merges request parameters with service parameters")
  void getConfigurationMergesRequestParameters() {
    // Given
    Map<String, String> requestParams = new HashMap<>();
    requestParams.put("bbox", "0,0,1,1");
    requestParams.put("width", "800");

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(1)
            .terId(1)
            .type(TYPE_WMS)
            .typeId(1)
            .method("GET")
            .parameters(requestParams)
            .build();

    ServiceParameter serviceParam = mock(ServiceParameter.class);
    when(serviceParam.getType()).thenReturn("FIXED");
    when(serviceParam.getName()).thenReturn("format");
    when(serviceParam.getValue()).thenReturn("image/png");

    Set<ServiceParameter> serviceParams = new HashSet<>(List.of(serviceParam));

    Service mockService = mock(Service.class);
    when(mockService.getServiceURL()).thenReturn("https://example.com/wms");
    when(mockService.getType()).thenReturn(TYPE_WMS);
    when(mockService.getPasswordSet()).thenReturn(false);
    when(mockService.getParameters()).thenReturn(serviceParams);

    when(serviceRepository.findById(1)).thenReturn(Optional.of(mockService));

    // When
    ConfigProxyDto result = service.getConfiguration(request, 0L);

    // Then
    assertNotNull(result);
    assertInstanceOf(WmsPayloadDto.class, result.getPayload());

    WmsPayloadDto payload = (WmsPayloadDto) result.getPayload();
    Map<String, String> parameters = payload.getParameters();
    assertEquals("image/png", parameters.get("format"));
    assertEquals("0,0,1,1", parameters.get("bbox"));
    assertEquals("800", parameters.get("width"));
  }

  @Test
  @DisplayName("getConfiguration handles null request parameters")
  void getConfigurationHandlesNullRequestParameters() {
    // Given
    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(1)
            .terId(1)
            .type(TYPE_WMS)
            .typeId(1)
            .method("GET")
            .parameters(null)
            .build();

    Service mockService = mock(Service.class);
    when(mockService.getServiceURL()).thenReturn("https://example.com/wms");
    when(mockService.getType()).thenReturn(TYPE_WMS);
    when(mockService.getPasswordSet()).thenReturn(false);
    when(mockService.getParameters()).thenReturn(new HashSet<>());

    when(serviceRepository.findById(1)).thenReturn(Optional.of(mockService));

    // When
    ConfigProxyDto result = service.getConfiguration(request, 0L);

    // Then
    assertNotNull(result);
    assertInstanceOf(WmsPayloadDto.class, result.getPayload());

    WmsPayloadDto payload = (WmsPayloadDto) result.getPayload();
    assertNotNull(payload.getParameters());
    assertTrue(payload.getParameters().isEmpty());
  }

  @Test
  @DisplayName("getConfiguration returns null payload for non-existent service")
  void getConfigurationReturnsNullForNonExistentService() {
    // Given
    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(1)
            .terId(1)
            .type(TYPE_WMS)
            .typeId(999)
            .method("GET")
            .build();

    when(serviceRepository.findById(999)).thenReturn(Optional.empty());

    // When & Then
    assertThrows(BadRequestException.class, () -> service.getConfiguration(request, 0L));
  }

  @Test
  @DisplayName("validateUserAccess returns true (TODO implementation)")
  void validateUserAccessReturnsTrue() {
    // Given
    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(1)
            .terId(1)
            .type(TYPE_WMS)
            .typeId(1)
            .method("GET")
            .build();

    // When
    boolean result = service.validateUserAccess(request, "testuser");

    // Then
    assertTrue(result);
  }

  @Test
  @DisplayName("getConfiguration returns JdbcPayloadDto for SQL task type")
  void getConfigurationReturnsJdbcPayloadForSqlTaskType() {
    // Given
    Map<String, Object> taskProperties = new HashMap<>();
    taskProperties.put(PROPERTY_COMMAND, "SELECT * FROM users WHERE id = ${userId}");

    DatabaseConnection mockConnection = mock(DatabaseConnection.class);
    when(mockConnection.getUrl()).thenReturn("jdbc:oracle:thin:@localhost:1521:orcl");
    when(mockConnection.getUser()).thenReturn("dbuser");
    when(mockConnection.getPassword()).thenReturn("dbpass");
    when(mockConnection.getDriver()).thenReturn("oracle.jdbc.driver.OracleDriver");

    Task mockTask = mock(Task.class);
    when(mockTask.getProperties()).thenReturn(taskProperties);
    when(mockTask.getConnection()).thenReturn(mockConnection);

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(1)
            .terId(1)
            .type(PROXY_TYPE_SQL)
            .typeId(1)
            .method("GET")
            .parameters(new HashMap<>())
            .build();

    when(taskRepository.findById(1)).thenReturn(Optional.of(mockTask));

    // When
    ConfigProxyDto result = service.getConfiguration(request, 0L);

    // Then
    assertNotNull(result);
    assertEquals(PROXY_TYPE_SQL, result.getType());
    assertInstanceOf(JdbcPayloadDto.class, result.getPayload());

    JdbcPayloadDto payload = (JdbcPayloadDto) result.getPayload();
    assertEquals("jdbc:oracle:thin:@localhost:1521:orcl", payload.getUri());
    assertEquals("dbuser", payload.getUser());
    assertEquals("dbpass", payload.getPassword());
    assertEquals("oracle.jdbc.driver.OracleDriver", payload.getDriver());
    assertEquals("SELECT * FROM users WHERE id = ${userId}", payload.getSql());
  }

  @Test
  @DisplayName("getConfiguration returns JdbcPayloadDto null when task connection is null")
  void getConfigurationReturnsNullJdbcPayloadWhenConnectionIsNull() {
    // Given
    Map<String, Object> taskProperties = new HashMap<>();
    taskProperties.put(PROPERTY_COMMAND, "SELECT * FROM users");

    Task mockTask = mock(Task.class);
    when(mockTask.getProperties()).thenReturn(taskProperties);
    when(mockTask.getConnection()).thenReturn(null);

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(1)
            .terId(1)
            .type(PROXY_TYPE_SQL)
            .typeId(1)
            .method("GET")
            .parameters(new HashMap<>())
            .build();

    when(taskRepository.findById(1)).thenReturn(Optional.of(mockTask));

    // When & Then
    assertThrows(BadRequestException.class, () -> service.getConfiguration(request, 0L));
  }

  @Test
  @DisplayName("getConfiguration throws BadRequestException when SQL command is missing")
  void getConfigurationThrowsExceptionWhenSqlCommandIsMissing() {
    // Given
    Map<String, Object> taskProperties = new HashMap<>();

    Task mockTask = mock(Task.class);
    when(mockTask.getProperties()).thenReturn(taskProperties);

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(1)
            .terId(1)
            .type(PROXY_TYPE_SQL)
            .typeId(1)
            .method("GET")
            .build();

    when(taskRepository.findById(1)).thenReturn(Optional.of(mockTask));

    // When & Then
    assertThrows(BadRequestException.class, () -> service.getConfiguration(request, 0L));
  }

  @Test
  @DisplayName("getConfiguration returns WmsPayloadDto for API task type")
  void getConfigurationReturnsWmsPayloadForApiTaskType() {
    // Given
    Map<String, Object> param1 = new HashMap<>();
    param1.put("label", "apiKey");
    param1.put("value", "secret123");

    Map<String, Object> taskProperties = new HashMap<>();
    taskProperties.put(PROPERTY_COMMAND, "https://api.example.com/endpoint");
    taskProperties.put("parameters", Collections.singletonList(param1));

    Task mockTask = mock(Task.class);
    when(mockTask.getProperties()).thenReturn(taskProperties);

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(1)
            .terId(1)
            .type(TYPE_API)
            .typeId(1)
            .method("GET")
            .parameters(new HashMap<>())
            .build();

    when(taskRepository.findById(1)).thenReturn(Optional.of(mockTask));

    // When
    ConfigProxyDto result = service.getConfiguration(request, 0L);

    // Then
    assertNotNull(result);
    assertEquals(TYPE_API, result.getType());
    assertInstanceOf(WmsPayloadDto.class, result.getPayload());

    WmsPayloadDto payload = (WmsPayloadDto) result.getPayload();
    assertEquals("https://api.example.com/endpoint", payload.getUri());
    assertEquals("GET", payload.getMethod());
    assertEquals("secret123", payload.getParameters().get("apiKey"));
  }

  @Test
  @DisplayName("getConfiguration handles API task with empty parameters")
  void getConfigurationHandlesApiTaskWithEmptyParameters() {
    // Given
    Map<String, Object> taskProperties = new HashMap<>();
    taskProperties.put(PROPERTY_COMMAND, "https://api.example.com/endpoint");
    taskProperties.put("parameters", Collections.emptyList());

    Task mockTask = mock(Task.class);
    when(mockTask.getProperties()).thenReturn(taskProperties);

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(1)
            .terId(1)
            .type(TYPE_API)
            .typeId(1)
            .method("GET")
            .build();

    when(taskRepository.findById(1)).thenReturn(Optional.of(mockTask));

    // When
    ConfigProxyDto result = service.getConfiguration(request, 0L);

    // Then
    assertNotNull(result);
    assertEquals(TYPE_API, result.getType());
    assertInstanceOf(WmsPayloadDto.class, result.getPayload());

    WmsPayloadDto payload = (WmsPayloadDto) result.getPayload();
    assertEquals("https://api.example.com/endpoint", payload.getUri());
    assertTrue(payload.getParameters().isEmpty());
  }

  @Test
  @DisplayName("getConfiguration handles API task with body")
  void getConfigurationHandlesApiTaskWithBody() {
    // Given
    Map<String, Object> taskProperties = new HashMap<>();
    taskProperties.put(PROPERTY_COMMAND, "https://api.example.com/endpoint");
    taskProperties.put("body", "{\"key\": \"value\"}");
    taskProperties.put("parameters", Collections.emptyList());

    Task mockTask = mock(Task.class);
    when(mockTask.getProperties()).thenReturn(taskProperties);

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(1)
            .terId(1)
            .type(TYPE_API)
            .typeId(1)
            .method("POST")
            .build();

    when(taskRepository.findById(1)).thenReturn(Optional.of(mockTask));

    // When
    ConfigProxyDto result = service.getConfiguration(request, 0L);

    // Then
    assertNotNull(result);
    assertInstanceOf(WmsPayloadDto.class, result.getPayload());

    WmsPayloadDto payload = (WmsPayloadDto) result.getPayload();
    assertEquals("{\"key\": \"value\"}", payload.getBody());
  }

  @Test
  @DisplayName("getConfiguration throws BadRequestException when API task properties are null")
  void getConfigurationThrowsExceptionWhenApiTaskPropertiesAreNull() {
    // Given
    Task mockTask = mock(Task.class);
    when(mockTask.getProperties()).thenReturn(null);

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(1)
            .terId(1)
            .type(TYPE_API)
            .typeId(1)
            .method("GET")
            .build();

    when(taskRepository.findById(1)).thenReturn(Optional.of(mockTask));

    // When & Then
    assertThrows(BadRequestException.class, () -> service.getConfiguration(request, 0L));
  }

  @Test
  @DisplayName("getConfiguration throws BadRequestException when API command is null")
  void getConfigurationThrowsExceptionWhenApiCommandIsNull() {
    // Given
    Map<String, Object> taskProperties = new HashMap<>();
    taskProperties.put(PROPERTY_COMMAND, null);
    taskProperties.put("parameters", Collections.emptyList());

    Task mockTask = mock(Task.class);
    when(mockTask.getProperties()).thenReturn(taskProperties);

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(1)
            .terId(1)
            .type(TYPE_API)
            .typeId(1)
            .method("GET")
            .build();

    when(taskRepository.findById(1)).thenReturn(Optional.of(mockTask));

    // When & Then
    assertThrows(BadRequestException.class, () -> service.getConfiguration(request, 0L));
  }

  @Test
  @DisplayName("getConfiguration throws BadRequestException when API command is blank")
  void getConfigurationThrowsExceptionWhenApiCommandIsBlank() {
    // Given
    Map<String, Object> taskProperties = new HashMap<>();
    taskProperties.put(PROPERTY_COMMAND, "   ");
    taskProperties.put("parameters", Collections.emptyList());

    Task mockTask = mock(Task.class);
    when(mockTask.getProperties()).thenReturn(taskProperties);

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(1)
            .terId(1)
            .type(TYPE_API)
            .typeId(1)
            .method("GET")
            .build();

    when(taskRepository.findById(1)).thenReturn(Optional.of(mockTask));

    // When & Then
    assertThrows(BadRequestException.class, () -> service.getConfiguration(request, 0L));
  }

  @Test
  @DisplayName("getConfiguration handles API task with missing parameters key")
  void getConfigurationHandlesApiTaskWithMissingParametersKey() {
    // Given
    Map<String, Object> taskProperties = new HashMap<>();
    taskProperties.put(PROPERTY_COMMAND, "https://api.example.com/endpoint");
    // No "parameters" key

    Task mockTask = mock(Task.class);
    when(mockTask.getProperties()).thenReturn(taskProperties);

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(1)
            .terId(1)
            .type(TYPE_API)
            .typeId(1)
            .method("GET")
            .build();

    when(taskRepository.findById(1)).thenReturn(Optional.of(mockTask));

    // When
    ConfigProxyDto result = service.getConfiguration(request, 0L);

    // Then
    assertNotNull(result);
    assertInstanceOf(WmsPayloadDto.class, result.getPayload());

    WmsPayloadDto payload = (WmsPayloadDto) result.getPayload();
    assertTrue(payload.getParameters().isEmpty());
  }

  @Test
  @DisplayName(
      "getConfiguration normalizes non-String parameter values (expected to fail before fix)")
  void getConfigurationNormalizesNonStringParameterValues() {
    // Given
    Map<String, Object> param1 = new HashMap<>();
    param1.put("label", "count");
    param1.put("value", 42); // Integer value

    Map<String, Object> param2 = new HashMap<>();
    param2.put("label", "price");
    param2.put("value", 19.99); // Double value

    Map<String, Object> taskProperties = new HashMap<>();
    taskProperties.put(PROPERTY_COMMAND, "https://api.example.com/endpoint");
    taskProperties.put("parameters", List.of(param1, param2));

    Task mockTask = mock(Task.class);
    when(mockTask.getProperties()).thenReturn(taskProperties);

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(1)
            .terId(1)
            .type(TYPE_API)
            .typeId(1)
            .method("GET")
            .build();

    when(taskRepository.findById(1)).thenReturn(Optional.of(mockTask));

    // When
    ConfigProxyDto result = service.getConfiguration(request, 0L);

    // Then
    assertNotNull(result);
    assertInstanceOf(WmsPayloadDto.class, result.getPayload());

    WmsPayloadDto payload = (WmsPayloadDto) result.getPayload();
    assertEquals("42", payload.getParameters().get("count"));
    assertEquals("19.99", payload.getParameters().get("price"));
  }

  @Test
  @DisplayName(
      "getConfiguration handles duplicate parameter labels with last-wins strategy (expected to fail before fix)")
  void getConfigurationHandlesDuplicateParameterLabels() {
    // Given
    Map<String, Object> param1 = new HashMap<>();
    param1.put("label", "apiKey");
    param1.put("value", "first");

    Map<String, Object> param2 = new HashMap<>();
    param2.put("label", "apiKey");
    param2.put("value", "second");

    Map<String, Object> taskProperties = new HashMap<>();
    taskProperties.put(PROPERTY_COMMAND, "https://api.example.com/endpoint");
    taskProperties.put("parameters", List.of(param1, param2));

    Task mockTask = mock(Task.class);
    when(mockTask.getProperties()).thenReturn(taskProperties);

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(1)
            .terId(1)
            .type(TYPE_API)
            .typeId(1)
            .method("GET")
            .build();

    when(taskRepository.findById(1)).thenReturn(Optional.of(mockTask));

    // When
    ConfigProxyDto result = service.getConfiguration(request, 0L);

    // Then
    assertNotNull(result);
    assertInstanceOf(WmsPayloadDto.class, result.getPayload());

    WmsPayloadDto payload = (WmsPayloadDto) result.getPayload();
    assertEquals("second", payload.getParameters().get("apiKey")); // last-wins
  }

  @Test
  @DisplayName("validateUserAccess returns true when validation is disabled (default behavior)")
  void validateUserAccessReturnsTrueWhenDisabled() {
    // Given: validation is disabled (default)
    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder().appId(1).terId(1).type(PROXY_TYPE_SQL).typeId(23).build();

    // When
    boolean result = service.validateUserAccess(request, "admin");

    // Then: access is granted regardless of validators
    assertTrue(result);
  }

  @Test
  @DisplayName("validateUserAccess denies access when username is null")
  void validateUserAccessDeniesAccessWhenUsernameNull() {
    // Given: validation is enabled
    ReflectionTestUtils.setField(service, "validateUserAccessEnabled", true);
    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder().appId(1).terId(1).type(PROXY_TYPE_SQL).typeId(23).build();

    // When
    boolean result = service.validateUserAccess(request, null);

    // Then
    assertFalse(result);
  }

  @Test
  @DisplayName("validateUserAccess denies access when username is blank")
  void validateUserAccessDeniesAccessWhenUsernameBlank() {
    // Given: validation is enabled
    ReflectionTestUtils.setField(service, "validateUserAccessEnabled", true);
    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder().appId(1).terId(1).type(PROXY_TYPE_SQL).typeId(23).build();

    // When
    boolean result = service.validateUserAccess(request, "   ");

    // Then
    assertFalse(result);
  }

  @Test
  @DisplayName("validateUserAccess denies access for unknown resource type (no validator)")
  void validateUserAccessDeniesAccessForUnknownType() {
    // Given: validation is enabled but no validators support "UNKNOWN" type
    ReflectionTestUtils.setField(service, "validateUserAccessEnabled", true);
    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder().appId(1).terId(1).type("UNKNOWN").typeId(100).build();

    // When
    boolean result = service.validateUserAccess(request, "admin");

    // Then: deny by default
    assertFalse(result);
  }

  @Test
  @DisplayName("validateUserAccess uses appropriate validator when available")
  void validateUserAccessUsesAppropriateValidator() {
    // Given: validation is enabled with a mock validator
    ReflectionTestUtils.setField(service, "validateUserAccessEnabled", true);

    org.sitmun.authorization.proxy.validator.ResourceAccessValidator mockValidator =
        mock(org.sitmun.authorization.proxy.validator.ResourceAccessValidator.class);
    when(mockValidator.supports(PROXY_TYPE_SQL)).thenReturn(true);
    when(mockValidator.validate(any(), eq("admin"))).thenReturn(true);

    // Create service with the mock validator
    ProxyConfigurationService serviceWithValidator =
        new ProxyConfigurationService(
            serviceRepository,
            taskRepository,
            userRepository,
            territoryRepository,
            queryFixedFiltersDecorator,
            queryVaryFiltersDecorator,
            queryPaginationDecorator,
            List.of(mockValidator));
    ReflectionTestUtils.setField(serviceWithValidator, "responseValidityTime", 3600);
    ReflectionTestUtils.setField(serviceWithValidator, "validateUserAccessEnabled", true);

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder().appId(1).terId(1).type(PROXY_TYPE_SQL).typeId(23).build();

    // When
    boolean result = serviceWithValidator.validateUserAccess(request, "admin");

    // Then
    assertTrue(result);
    verify(mockValidator).validate(any(), eq("admin"));
  }

  @Test
  @DisplayName("validateUserAccess denies access when validator returns false")
  void validateUserAccessDeniesAccessWhenValidatorReturnsFalse() {
    // Given: validation is enabled with a mock validator that denies access
    ReflectionTestUtils.setField(service, "validateUserAccessEnabled", true);

    org.sitmun.authorization.proxy.validator.ResourceAccessValidator mockValidator =
        mock(org.sitmun.authorization.proxy.validator.ResourceAccessValidator.class);
    when(mockValidator.supports(PROXY_TYPE_SQL)).thenReturn(true);
    when(mockValidator.validate(any(), eq("unauthorizedUser"))).thenReturn(false);

    ProxyConfigurationService serviceWithValidator =
        new ProxyConfigurationService(
            serviceRepository,
            taskRepository,
            userRepository,
            territoryRepository,
            queryFixedFiltersDecorator,
            queryVaryFiltersDecorator,
            queryPaginationDecorator,
            List.of(mockValidator));
    ReflectionTestUtils.setField(serviceWithValidator, "responseValidityTime", 3600);
    ReflectionTestUtils.setField(serviceWithValidator, "validateUserAccessEnabled", true);

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder().appId(1).terId(1).type(PROXY_TYPE_SQL).typeId(23).build();

    // When
    boolean result = serviceWithValidator.validateUserAccess(request, "unauthorizedUser");

    // Then
    assertFalse(result);
    verify(mockValidator).validate(any(), eq("unauthorizedUser"));
  }

  @Test
  @DisplayName("validateUserAccess selects first matching validator")
  void validateUserAccessSelectsFirstMatchingValidator() {
    // Given: multiple validators, first match wins
    ReflectionTestUtils.setField(service, "validateUserAccessEnabled", true);

    org.sitmun.authorization.proxy.validator.ResourceAccessValidator validator1 =
        mock(org.sitmun.authorization.proxy.validator.ResourceAccessValidator.class);
    org.sitmun.authorization.proxy.validator.ResourceAccessValidator validator2 =
        mock(org.sitmun.authorization.proxy.validator.ResourceAccessValidator.class);

    when(validator1.supports(TYPE_WMS)).thenReturn(true);
    when(validator1.validate(any(), any())).thenReturn(true);
    // Remove the unnecessary stubbing for validator2.supports since it's never called
    // when(validator2.supports(TYPE_WMS)).thenReturn(true);

    ProxyConfigurationService serviceWithValidators =
        new ProxyConfigurationService(
            serviceRepository,
            taskRepository,
            userRepository,
            territoryRepository,
            queryFixedFiltersDecorator,
            queryVaryFiltersDecorator,
            queryPaginationDecorator,
            List.of(validator1, validator2));
    ReflectionTestUtils.setField(serviceWithValidators, "responseValidityTime", 3600);
    ReflectionTestUtils.setField(serviceWithValidators, "validateUserAccessEnabled", true);

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder().appId(1).terId(0).type(TYPE_WMS).typeId(1).build();

    // When
    boolean result = serviceWithValidators.validateUserAccess(request, "admin");

    // Then: first validator is used, second is never called
    assertTrue(result);
    verify(validator1).validate(any(), eq("admin"));
    verify(validator2, never()).validate(any(), any());
  }
}
