package org.sitmun.authorization.proxy.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
            queryPaginationDecorator);
    ReflectionTestUtils.setField(service, "responseValidityTime", 3600);
  }

  @Test
  @DisplayName("getConfiguration returns OgcWmsPayloadDto for WMS service")
  void getConfigurationReturnsOgcWmsPayloadForWmsService() {
    // Given
    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(1)
            .terId(1)
            .type("WMS")
            .typeId(1)
            .method("GET")
            .parameters(new HashMap<>())
            .build();

    Service mockService = mock(Service.class);
    when(mockService.getServiceURL()).thenReturn("https://example.com/wms");
    when(mockService.getType()).thenReturn("WMS");
    when(mockService.getPasswordSet()).thenReturn(false);
    when(mockService.getParameters()).thenReturn(new HashSet<>());

    when(serviceRepository.findById(1)).thenReturn(Optional.of(mockService));

    // When
    ConfigProxyDto result = service.getConfiguration(request, 0L);

    // Then
    assertNotNull(result);
    assertEquals("WMS", result.getType());
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
            .type("WMS")
            .typeId(1)
            .method("GET")
            .parameters(new HashMap<>())
            .build();

    Service mockService = mock(Service.class);
    when(mockService.getServiceURL()).thenReturn("https://example.com/wms");
    when(mockService.getType()).thenReturn("WMS");
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
            .type("WMS")
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
    when(mockService.getType()).thenReturn("WMS");
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
            .type("WMS")
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
    when(mockService.getType()).thenReturn("WMS");
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
            .type("WMS")
            .typeId(1)
            .method("GET")
            .parameters(null)
            .build();

    Service mockService = mock(Service.class);
    when(mockService.getServiceURL()).thenReturn("https://example.com/wms");
    when(mockService.getType()).thenReturn("WMS");
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
            .type("WMS")
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
            .type("WMS")
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
    taskProperties.put("command", "SELECT * FROM users WHERE id = ${userId}");

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
            .type("SQL")
            .typeId(1)
            .method("GET")
            .parameters(new HashMap<>())
            .build();

    when(taskRepository.findById(1)).thenReturn(Optional.of(mockTask));

    // When
    ConfigProxyDto result = service.getConfiguration(request, 0L);

    // Then
    assertNotNull(result);
    assertEquals("SQL", result.getType());
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
    taskProperties.put("command", "SELECT * FROM users");

    Task mockTask = mock(Task.class);
    when(mockTask.getProperties()).thenReturn(taskProperties);
    when(mockTask.getConnection()).thenReturn(null);

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(1)
            .terId(1)
            .type("SQL")
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
            .type("SQL")
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
    taskProperties.put("command", "https://api.example.com/endpoint");
    taskProperties.put("parameters", Collections.singletonList(param1));

    Task mockTask = mock(Task.class);
    when(mockTask.getProperties()).thenReturn(taskProperties);

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(1)
            .terId(1)
            .type("API")
            .typeId(1)
            .method("GET")
            .parameters(new HashMap<>())
            .build();

    when(taskRepository.findById(1)).thenReturn(Optional.of(mockTask));

    // When
    ConfigProxyDto result = service.getConfiguration(request, 0L);

    // Then
    assertNotNull(result);
    assertEquals("API", result.getType());
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
    taskProperties.put("command", "https://api.example.com/endpoint");
    taskProperties.put("parameters", Collections.emptyList());

    Task mockTask = mock(Task.class);
    when(mockTask.getProperties()).thenReturn(taskProperties);

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(1)
            .terId(1)
            .type("API")
            .typeId(1)
            .method("GET")
            .build();

    when(taskRepository.findById(1)).thenReturn(Optional.of(mockTask));

    // When
    ConfigProxyDto result = service.getConfiguration(request, 0L);

    // Then
    assertNotNull(result);
    assertEquals("API", result.getType());
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
    taskProperties.put("command", "https://api.example.com/endpoint");
    taskProperties.put("body", "{\"key\": \"value\"}");
    taskProperties.put("parameters", Collections.emptyList());

    Task mockTask = mock(Task.class);
    when(mockTask.getProperties()).thenReturn(taskProperties);

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(1)
            .terId(1)
            .type("API")
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
}
