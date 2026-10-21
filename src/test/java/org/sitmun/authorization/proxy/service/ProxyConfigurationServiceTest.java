package org.sitmun.authorization.proxy.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.sitmun.domain.DomainConstants.Proxy.TYPE_API;
import static org.sitmun.domain.DomainConstants.Proxy.TYPE_SQL;
import static org.sitmun.domain.DomainConstants.Proxy.TYPE_WMS;
import static org.sitmun.domain.DomainConstants.Tasks.PARAMETERS_PROVIDED;
import static org.sitmun.domain.DomainConstants.Tasks.PROPERTY_AUTHENTICATION_MODE;
import static org.sitmun.domain.DomainConstants.Tasks.PROPERTY_COMMAND;
import static org.sitmun.domain.DomainConstants.Tasks.PROPERTY_HEADERS;
import static org.sitmun.domain.DomainConstants.Tasks.PROPERTY_PASSWORD;
import static org.sitmun.domain.DomainConstants.Tasks.PROPERTY_QUERY_PARAMS;
import static org.sitmun.domain.DomainConstants.Tasks.PROPERTY_USER;
import static org.sitmun.domain.DomainConstants.Tasks.RELATION_TYPE_QUERY_TASK;
import static org.sitmun.domain.DomainConstants.Tasks.TASK_TYPE_ID_LOCATOR;
import static org.sitmun.domain.DomainConstants.Tasks.TASK_TYPE_ID_MORE_INFO;

import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.authorization.access.UserApplicationAccessPolicy;
import org.sitmun.authorization.proxy.decorators.HttpUserParametrizationDecorator;
import org.sitmun.authorization.proxy.decorators.QueryPaginationDecorator;
import org.sitmun.authorization.proxy.decorators.SqlUserParametrizationDecorator;
import org.sitmun.authorization.proxy.dto.ConfigProxyDto;
import org.sitmun.authorization.proxy.dto.ConfigProxyRequestDto;
import org.sitmun.authorization.proxy.exception.BadRequestException;
import org.sitmun.authorization.proxy.protocols.jdbc.JdbcPayloadDto;
import org.sitmun.authorization.proxy.protocols.wms.WmsPayloadDto;
import org.sitmun.authorization.proxy.validator.ResourceAccessValidator;
import org.sitmun.domain.application.ApplicationRepository;
import org.sitmun.domain.database.DatabaseConnection;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.service.ServiceRepository;
import org.sitmun.domain.service.parameter.ServiceParameter;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.TaskRepository;
import org.sitmun.domain.task.parameter.TaskParameterProcessor;
import org.sitmun.domain.task.relation.TaskRelation;
import org.sitmun.domain.task.type.TaskType;
import org.sitmun.domain.territory.TerritoryRepository;
import org.sitmun.domain.user.UserRepository;
import org.sitmun.infrastructure.variables.SystemVariableResolver;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProxyConfigurationServiceTest {

  @Mock private ServiceRepository serviceRepository;
  @Mock private TaskRepository taskRepository;
  @Mock private UserRepository userRepository;
  @Mock private TerritoryRepository territoryRepository;
  @Mock private ApplicationRepository applicationRepository;
  @Mock private SqlUserParametrizationDecorator sqlUserParametrizationDecorator;
  @Mock private HttpUserParametrizationDecorator httpUserParametrizationDecorator;
  @Mock private QueryPaginationDecorator queryPaginationDecorator;
  @Mock private SystemVariableResolver systemVariableResolver;
  @Mock private org.sitmun.domain.task.MoreInfoTaskResolver moreInfoTaskResolver;

  private ProxyConfigurationService service;

  @BeforeEach
  void setUp() {
    // Mock SystemVariableResolver to return template unchanged (no variable resolution in tests)
    // Use lenient() because not all tests call resolve()
    lenient()
        .when(systemVariableResolver.resolve(anyString(), any(RequestCoordinates.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Mock MoreInfoTaskResolver to return the input task unchanged (no resolution in tests)
    lenient()
        .when(moreInfoTaskResolver.resolveOrSelf(any(Task.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Use real TaskParameterProcessor with mocked SystemVariableResolver
    TaskParameterProcessor taskParameterProcessor =
        new TaskParameterProcessor(systemVariableResolver);

    service =
        new ProxyConfigurationService(
            serviceRepository,
            taskRepository,
            userRepository,
            territoryRepository,
            applicationRepository,
            sqlUserParametrizationDecorator,
            httpUserParametrizationDecorator,
            queryPaginationDecorator,
            Collections.emptyList(), // Empty validators list for non-validation tests
            systemVariableResolver,
            moreInfoTaskResolver,
            taskParameterProcessor,
            new UserApplicationAccessPolicy(userRepository, applicationRepository),
            mock(org.sitmun.domain.cartography.CartographyRepository.class),
            mock(
                org.sitmun.domain.cartography.parameter.CartographyServiceParameterRepository
                    .class));
    ReflectionTestUtils.setField(service, "responseValidityTime", 3600);
    ReflectionTestUtils.setField(service, "validateUserAccessEnabled", false);
  }

  private RequestCoordinates coordinatesFor(ConfigProxyRequestDto request) {
    return service.getRequestCoordinates(request, "testuser");
  }

  /**
   * {@link ProxyConfigurationService} wired like {@link #setUp()} but with custom access
   * validators.
   */
  private ProxyConfigurationService proxyServiceWithAccessValidators(
      List<ResourceAccessValidator> validators) {
    TaskParameterProcessor taskParameterProcessor =
        new TaskParameterProcessor(systemVariableResolver);
    ProxyConfigurationService serviceWithValidator =
        new ProxyConfigurationService(
            serviceRepository,
            taskRepository,
            userRepository,
            territoryRepository,
            applicationRepository,
            sqlUserParametrizationDecorator,
            httpUserParametrizationDecorator,
            queryPaginationDecorator,
            validators,
            systemVariableResolver,
            moreInfoTaskResolver,
            taskParameterProcessor,
            new UserApplicationAccessPolicy(userRepository, applicationRepository),
            mock(org.sitmun.domain.cartography.CartographyRepository.class),
            mock(
                org.sitmun.domain.cartography.parameter.CartographyServiceParameterRepository
                    .class));
    ReflectionTestUtils.setField(serviceWithValidator, "responseValidityTime", 3600);
    ReflectionTestUtils.setField(serviceWithValidator, "validateUserAccessEnabled", true);
    return serviceWithValidator;
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
    ConfigProxyDto result = service.getConfiguration(request, 0L, coordinatesFor(request));

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
    ConfigProxyDto result = service.getConfiguration(request, 0L, coordinatesFor(request));

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
    when(param1.getType()).thenReturn("WMS");
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
    ConfigProxyDto result = service.getConfiguration(request, 0L, coordinatesFor(request));

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
    when(serviceParam.getType()).thenReturn("WMS");
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
    ConfigProxyDto result = service.getConfiguration(request, 0L, coordinatesFor(request));

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
  @DisplayName(
      "cartography parameters replace service parameters ignoring case and survive the HTTP decorator")
  void cartographyParametersReplaceServiceParametersIgnoringCase() {
    var cartographies = mock(org.sitmun.domain.cartography.CartographyRepository.class);
    var layerParameters =
        mock(org.sitmun.domain.cartography.parameter.CartographyServiceParameterRepository.class);
    ProxyConfigurationService configured =
        new ProxyConfigurationService(
            serviceRepository,
            taskRepository,
            userRepository,
            territoryRepository,
            applicationRepository,
            sqlUserParametrizationDecorator,
            new HttpUserParametrizationDecorator(),
            queryPaginationDecorator,
            Collections.emptyList(),
            systemVariableResolver,
            moreInfoTaskResolver,
            new org.sitmun.domain.task.parameter.TaskParameterProcessor(systemVariableResolver),
            new UserApplicationAccessPolicy(userRepository, applicationRepository),
            cartographies,
            layerParameters);
    ReflectionTestUtils.setField(configured, "responseValidityTime", 3600);
    ReflectionTestUtils.setField(configured, "validateUserAccessEnabled", false);

    Map<String, String> requestParams = new HashMap<>();
    requestParams.put("cql_filter", "1=1");
    requestParams.put("LAYERS", "roads");
    requestParams.put("bbox", "0,0,1,1");
    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(1)
            .terId(1)
            .type(TYPE_WMS)
            .typeId(1)
            .method("GET")
            .cartographyId(42)
            .parameters(requestParams)
            .build();

    ServiceParameter serviceParam = mock(ServiceParameter.class);
    when(serviceParam.getType()).thenReturn("WMS");
    when(serviceParam.getName()).thenReturn("CQL_FILTER");
    when(serviceParam.getValue()).thenReturn("IN(1)");

    Service mockService = mock(Service.class);
    when(mockService.getId()).thenReturn(1);
    when(mockService.getServiceURL()).thenReturn("https://example.com/wms");
    when(mockService.getType()).thenReturn(TYPE_WMS);
    when(mockService.getPasswordSet()).thenReturn(false);
    when(mockService.getParameters()).thenReturn(Set.of(serviceParam));
    when(serviceRepository.findById(1)).thenReturn(Optional.of(mockService));

    org.sitmun.domain.cartography.Cartography cartography =
        mock(org.sitmun.domain.cartography.Cartography.class);
    when(cartography.getId()).thenReturn(42);
    when(cartography.getService()).thenReturn(mockService);
    when(cartographies.findById(42)).thenReturn(Optional.of(cartography));

    org.sitmun.domain.cartography.parameter.CartographyServiceParameter layerParam =
        mock(org.sitmun.domain.cartography.parameter.CartographyServiceParameter.class);
    when(layerParam.getType()).thenReturn("WMS");
    when(layerParam.getName()).thenReturn("CQL_FILTER");
    when(layerParam.getValue()).thenReturn("IN(2)");
    when(layerParameters.findByCartography_Id(42)).thenReturn(List.of(layerParam));

    RequestCoordinates coordinates = configured.getRequestCoordinates(request, "testuser");
    ConfigProxyDto result = configured.getConfiguration(request, 0L, coordinates);
    configured.applyDecorators(result, request, coordinates);

    WmsPayloadDto payload = (WmsPayloadDto) result.getPayload();
    assertEquals("IN(2)", payload.getParameters().get("CQL_FILTER"));
    assertEquals("roads", payload.getParameters().get("LAYERS"));
    assertEquals("0,0,1,1", payload.getParameters().get("bbox"));
    assertFalse(payload.getParameters().containsKey("cql_filter"));
  }

  @Test
  @DisplayName("WMS parameters stay on every operation, including GetCapabilities")
  void wmsParameterAppliesToCapabilitiesGetMapAndGetFeatureInfo() {
    for (String request : List.of("GetCapabilities", "GetMap", "GetFeatureInfo")) {
      Map<String, String> parameters =
          upstreamParameters(
              request, List.of(serviceParameter("WMS", "VERSION", "1.1.1")), List.of());
      assertEquals("1.1.1", parameters.get("VERSION"), request);
    }
  }

  @Test
  @DisplayName("INFO applies only to GetFeatureInfo and GETMAP only to GetMap")
  void operationCodesApplyOnlyToTheirRequest() {
    List<ServiceParameter> rows =
        List.of(
            serviceParameter("WMS", "VERSION", "1.1.1"),
            serviceParameter("INFO", "INFO_FORMAT", "application/json"),
            serviceParameter("GETMAP", "FORMAT", "image/png"),
            serviceParameter("OLPARAM", "opacity", "0.5"));

    Map<String, String> getMap = upstreamParameters("GetMap", rows, List.of());
    assertEquals("1.1.1", getMap.get("VERSION"));
    assertEquals("image/png", getMap.get("FORMAT"));
    assertFalse(getMap.containsKey("INFO_FORMAT"));
    assertFalse(getMap.containsKey("opacity"));

    Map<String, String> info = upstreamParameters("GetFeatureInfo", rows, List.of());
    assertEquals("1.1.1", info.get("VERSION"));
    assertEquals("application/json", info.get("INFO_FORMAT"));
    assertFalse(info.containsKey("FORMAT"));
    assertFalse(info.containsKey("opacity"));
  }

  @Test
  @DisplayName("INFO replaces a WMS parameter of the same name on GetFeatureInfo only")
  void infoReplacesProtocolParameterOnGetFeatureInfo() {
    List<ServiceParameter> rows =
        List.of(
            serviceParameter("WMS", "CQL_FILTER", "IN(1)"),
            serviceParameter("INFO", "CQL_FILTER", "IN(2)"));

    assertEquals("IN(1)", upstreamParameters("GetMap", rows, List.of()).get("CQL_FILTER"));
    assertEquals("IN(2)", upstreamParameters("GetFeatureInfo", rows, List.of()).get("CQL_FILTER"));
  }

  @Test
  @DisplayName("A layer protocol parameter replaces the service parameter on every operation")
  void layerProtocolParameterReplacesServiceParameter() {
    List<org.sitmun.domain.cartography.parameter.CartographyServiceParameter> layer =
        List.of(layerParameter("WMS", "CQL_FILTER", "IN(2)"));
    List<ServiceParameter> serviceRows = List.of(serviceParameter("WMS", "CQL_FILTER", "IN(1)"));

    assertEquals("IN(2)", upstreamParameters("GetMap", serviceRows, layer).get("CQL_FILTER"));
    assertEquals(
        "IN(2)", upstreamParameters("GetFeatureInfo", serviceRows, layer).get("CQL_FILTER"));
  }

  @Test
  @DisplayName("A layer INFO parameter replaces the service WMS parameter only on GetFeatureInfo")
  void layerInfoReplacesServiceParameterOnlyOnGetFeatureInfo() {
    List<org.sitmun.domain.cartography.parameter.CartographyServiceParameter> layer =
        List.of(layerParameter("INFO", "CQL_FILTER", "IN(9)"));
    List<ServiceParameter> serviceRows = List.of(serviceParameter("WMS", "CQL_FILTER", "IN(1)"));

    assertEquals("IN(1)", upstreamParameters("GetMap", serviceRows, layer).get("CQL_FILTER"));
    assertEquals(
        "IN(9)", upstreamParameters("GetFeatureInfo", serviceRows, layer).get("CQL_FILTER"));
  }

  @Test
  @DisplayName("OLPARAM and WMTS parameters are not sent upstream")
  void clientAndProfileParametersAreNotSentUpstream() {
    List<ServiceParameter> rows =
        List.of(
            serviceParameter("WMS", "VERSION", "1.1.1"),
            serviceParameter("OLPARAM", "opacity", "0.5"),
            serviceParameter("WMTS", "matrixSet", "EPSG:25831"));

    Map<String, String> parameters = upstreamParameters("GetMap", rows, List.of());
    assertEquals("1.1.1", parameters.get("VERSION"));
    assertFalse(parameters.containsKey("opacity"));
    assertFalse(parameters.containsKey("matrixSet"));
  }

  private Map<String, String> upstreamParameters(
      String requestName,
      List<ServiceParameter> serviceRows,
      List<org.sitmun.domain.cartography.parameter.CartographyServiceParameter> layerRows) {
    var cartographies = mock(org.sitmun.domain.cartography.CartographyRepository.class);
    var layerParameters =
        mock(org.sitmun.domain.cartography.parameter.CartographyServiceParameterRepository.class);
    ProxyConfigurationService configured =
        new ProxyConfigurationService(
            serviceRepository,
            taskRepository,
            userRepository,
            territoryRepository,
            applicationRepository,
            sqlUserParametrizationDecorator,
            new HttpUserParametrizationDecorator(),
            queryPaginationDecorator,
            Collections.emptyList(),
            systemVariableResolver,
            moreInfoTaskResolver,
            new TaskParameterProcessor(systemVariableResolver),
            new UserApplicationAccessPolicy(userRepository, applicationRepository),
            cartographies,
            layerParameters);
    ReflectionTestUtils.setField(configured, "responseValidityTime", 3600);
    ReflectionTestUtils.setField(configured, "validateUserAccessEnabled", false);

    Map<String, String> requestParams = new HashMap<>();
    requestParams.put("REQUEST", requestName);
    requestParams.put("LAYERS", "roads");
    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(1)
            .terId(1)
            .type(TYPE_WMS)
            .typeId(1)
            .method("GET")
            .cartographyId(layerRows.isEmpty() ? null : 42)
            .parameters(requestParams)
            .build();

    Service mockService = mock(Service.class);
    when(mockService.getServiceURL()).thenReturn("https://example.com/wms");
    when(mockService.getType()).thenReturn(TYPE_WMS);
    when(mockService.getPasswordSet()).thenReturn(false);
    when(mockService.getParameters()).thenReturn(new HashSet<>(serviceRows));
    when(serviceRepository.findById(1)).thenReturn(Optional.of(mockService));

    if (!layerRows.isEmpty()) {
      org.sitmun.domain.cartography.Cartography cartography =
          mock(org.sitmun.domain.cartography.Cartography.class);
      when(mockService.getId()).thenReturn(1);
      when(cartography.getId()).thenReturn(42);
      when(cartography.getService()).thenReturn(mockService);
      when(cartographies.findById(42)).thenReturn(Optional.of(cartography));
      when(layerParameters.findByCartography_Id(42)).thenReturn(layerRows);
    }

    RequestCoordinates coordinates = configured.getRequestCoordinates(request, "testuser");
    ConfigProxyDto result = configured.getConfiguration(request, 0L, coordinates);
    configured.applyDecorators(result, request, coordinates);
    return ((WmsPayloadDto) result.getPayload()).getParameters();
  }

  private static ServiceParameter serviceParameter(String type, String name, String value) {
    ServiceParameter parameter = mock(ServiceParameter.class);
    when(parameter.getType()).thenReturn(type);
    when(parameter.getName()).thenReturn(name);
    when(parameter.getValue()).thenReturn(value);
    return parameter;
  }

  private static org.sitmun.domain.cartography.parameter.CartographyServiceParameter layerParameter(
      String type, String name, String value) {
    org.sitmun.domain.cartography.parameter.CartographyServiceParameter parameter =
        mock(org.sitmun.domain.cartography.parameter.CartographyServiceParameter.class);
    when(parameter.getType()).thenReturn(type);
    when(parameter.getName()).thenReturn(name);
    when(parameter.getValue()).thenReturn(value);
    return parameter;
  }

  @Test
  @DisplayName("getConfiguration resolves system variables in WMS service URL and fixed parameters")
  void getConfigurationResolvesSystemVariablesInWmsServiceUrlAndFixedParameters() {
    // Given
    reset(systemVariableResolver);
    when(systemVariableResolver.resolve(anyString(), any(RequestCoordinates.class)))
        .thenAnswer(
            invocation -> {
              String input = invocation.getArgument(0);
              return input.replace("#{TERR_ID}", "99");
            });

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(1)
            .terId(1)
            .type(TYPE_WMS)
            .typeId(1)
            .method("GET")
            .parameters(new HashMap<>())
            .build();

    ServiceParameter fixedParam1 = mock(ServiceParameter.class);
    when(fixedParam1.getType()).thenReturn("WMS");
    when(fixedParam1.getName()).thenReturn("env1");
    when(fixedParam1.getValue()).thenReturn("munine:#{TERR_ID}");

    ServiceParameter fixedParam2 = mock(ServiceParameter.class);
    when(fixedParam2.getType()).thenReturn("WMS");
    when(fixedParam2.getName()).thenReturn("env2");
    when(fixedParam2.getValue()).thenReturn("#{TERR_ID}");

    ServiceParameter fixedParam3 = mock(ServiceParameter.class);
    when(fixedParam3.getType()).thenReturn("WMS");
    when(fixedParam3.getName()).thenReturn("env3");
    when(fixedParam3.getValue()).thenReturn("TERR_ID");

    ServiceParameter varyParam = mock(ServiceParameter.class);
    when(varyParam.getType()).thenReturn("VARY");
    when(varyParam.getName()).thenReturn("layers");

    Service mockService = mock(Service.class);
    when(mockService.getType()).thenReturn(TYPE_WMS);
    when(mockService.getPasswordSet()).thenReturn(false);
    when(mockService.getServiceURL()).thenReturn("https://example.com/wms/#{TERR_ID}");
    when(mockService.getParameters())
        .thenReturn(new HashSet<>(Arrays.asList(fixedParam1, fixedParam2, fixedParam3, varyParam)));

    when(serviceRepository.findById(1)).thenReturn(Optional.of(mockService));

    // When
    ConfigProxyDto result = service.getConfiguration(request, 0L, coordinatesFor(request));

    // Then
    assertNotNull(result);
    assertInstanceOf(WmsPayloadDto.class, result.getPayload());

    WmsPayloadDto payload = (WmsPayloadDto) result.getPayload();
    assertEquals("https://example.com/wms/99", payload.getUri());
    assertEquals("munine:99", payload.getParameters().get("env1"));
    assertEquals("99", payload.getParameters().get("env2"));
    assertEquals("TERR_ID", payload.getParameters().get("env3"));
    assertEquals(List.of("layers"), payload.getVary());
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
    ConfigProxyDto result = service.getConfiguration(request, 0L, coordinatesFor(request));

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
    RequestCoordinates coordinates = coordinatesFor(request);
    assertThrows(
        BadRequestException.class, () -> service.getConfiguration(request, 0L, coordinates));
  }

  @Test
  @DisplayName("getConfiguration resolves linked query task for SQL more-info tasks")
  void getConfigurationResolvesLinkedQueryTaskForSqlMoreInfoTasks() {
    Map<String, Object> queryTaskProperties = new HashMap<>();
    queryTaskProperties.put(PROPERTY_COMMAND, "SELECT * FROM users WHERE id = ${userId}");

    DatabaseConnection mockConnection = mock(DatabaseConnection.class);
    when(mockConnection.getUrl()).thenReturn("jdbc:oracle:thin:@localhost:1521:orcl");
    when(mockConnection.getUser()).thenReturn("dbuser");
    when(mockConnection.getPassword()).thenReturn("dbpass");
    when(mockConnection.getDriver()).thenReturn("oracle.jdbc.driver.OracleDriver");

    Task relatedQueryTask = mock(Task.class);
    when(relatedQueryTask.getProperties()).thenReturn(queryTaskProperties);
    when(relatedQueryTask.getConnection()).thenReturn(mockConnection);

    TaskRelation relation =
        TaskRelation.builder()
            .relationType(RELATION_TYPE_QUERY_TASK)
            .relatedTask(relatedQueryTask)
            .build();

    Task moreInfoTask = mock(Task.class);
    TaskType moreInfoType = mock(TaskType.class);
    lenient().when(moreInfoType.getId()).thenReturn(TASK_TYPE_ID_MORE_INFO);
    lenient().when(moreInfoTask.getType()).thenReturn(moreInfoType);
    lenient().when(moreInfoTask.getRelations()).thenReturn(Set.of(relation));

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(1)
            .terId(1)
            .type(TYPE_SQL)
            .typeId(42)
            .method("GET")
            .parameters(new HashMap<>())
            .build();

    when(taskRepository.findById(42)).thenReturn(Optional.of(moreInfoTask));
    when(moreInfoTaskResolver.resolveOrSelf(moreInfoTask)).thenReturn(relatedQueryTask);

    ConfigProxyDto result = service.getConfiguration(request, 0L, coordinatesFor(request));

    assertNotNull(result);
    assertEquals(TYPE_SQL, result.getType());
    assertInstanceOf(JdbcPayloadDto.class, result.getPayload());

    JdbcPayloadDto payload = (JdbcPayloadDto) result.getPayload();
    assertEquals("jdbc:oracle:thin:@localhost:1521:orcl", payload.getUri());
    assertEquals("SELECT * FROM users WHERE id = ${userId}", payload.getSql());
  }

  @Test
  @DisplayName("getConfiguration resolves linked query task for SQL locator tasks")
  void getConfigurationResolvesLinkedQueryTaskForSqlLocatorTasks() {
    Map<String, Object> queryTaskProperties = new HashMap<>();
    queryTaskProperties.put(PROPERTY_COMMAND, "SELECT * FROM events WHERE id = ${eventId}");

    DatabaseConnection mockConnection = mock(DatabaseConnection.class);
    when(mockConnection.getUrl()).thenReturn("jdbc:oracle:thin:@localhost:1521:orcl");
    when(mockConnection.getUser()).thenReturn("dbuser");
    when(mockConnection.getPassword()).thenReturn("dbpass");
    when(mockConnection.getDriver()).thenReturn("oracle.jdbc.driver.OracleDriver");

    Task relatedQueryTask = mock(Task.class);
    when(relatedQueryTask.getProperties()).thenReturn(queryTaskProperties);
    when(relatedQueryTask.getConnection()).thenReturn(mockConnection);

    TaskRelation relation =
        TaskRelation.builder()
            .relationType(RELATION_TYPE_QUERY_TASK)
            .relatedTask(relatedQueryTask)
            .build();

    Task locatorTask = mock(Task.class);
    TaskType locatorType = mock(TaskType.class);
    lenient().when(locatorType.getId()).thenReturn(TASK_TYPE_ID_LOCATOR);
    lenient().when(locatorTask.getType()).thenReturn(locatorType);
    lenient().when(locatorTask.getRelations()).thenReturn(Set.of(relation));

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(1)
            .terId(1)
            .type(TYPE_SQL)
            .typeId(99)
            .method("GET")
            .parameters(new HashMap<>())
            .build();

    when(taskRepository.findById(99)).thenReturn(Optional.of(locatorTask));
    when(moreInfoTaskResolver.resolveOrSelf(locatorTask)).thenReturn(relatedQueryTask);

    ConfigProxyDto result = service.getConfiguration(request, 0L, coordinatesFor(request));

    assertNotNull(result);
    assertEquals(TYPE_SQL, result.getType());
    assertInstanceOf(JdbcPayloadDto.class, result.getPayload());

    JdbcPayloadDto payload = (JdbcPayloadDto) result.getPayload();
    assertEquals("jdbc:oracle:thin:@localhost:1521:orcl", payload.getUri());
    assertEquals("SELECT * FROM events WHERE id = ${eventId}", payload.getSql());
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
            .type(TYPE_SQL)
            .typeId(1)
            .method("GET")
            .parameters(new HashMap<>())
            .build();

    when(taskRepository.findById(1)).thenReturn(Optional.of(mockTask));

    // When
    ConfigProxyDto result = service.getConfiguration(request, 0L, coordinatesFor(request));

    // Then
    assertNotNull(result);
    assertEquals(TYPE_SQL, result.getType());
    assertInstanceOf(JdbcPayloadDto.class, result.getPayload());

    JdbcPayloadDto payload = (JdbcPayloadDto) result.getPayload();
    assertEquals("jdbc:oracle:thin:@localhost:1521:orcl", payload.getUri());
    assertEquals("dbuser", payload.getUser());
    assertEquals("dbpass", payload.getPassword());
    assertEquals("oracle.jdbc.driver.OracleDriver", payload.getDriver());
    assertEquals("SELECT * FROM users WHERE id = ${userId}", payload.getSql());
    assertNotNull(payload.getParameters());
    assertTrue(payload.getParameters().isEmpty());
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
            .type(TYPE_SQL)
            .typeId(1)
            .method("GET")
            .parameters(new HashMap<>())
            .build();

    when(taskRepository.findById(1)).thenReturn(Optional.of(mockTask));

    // When & Then
    RequestCoordinates coordinates = coordinatesFor(request);
    assertThrows(
        BadRequestException.class, () -> service.getConfiguration(request, 0L, coordinates));
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
            .type(TYPE_SQL)
            .typeId(1)
            .method("GET")
            .build();

    when(taskRepository.findById(1)).thenReturn(Optional.of(mockTask));

    // When & Then
    RequestCoordinates coordinates = coordinatesFor(request);
    assertThrows(
        BadRequestException.class, () -> service.getConfiguration(request, 0L, coordinates));
  }

  @Test
  @DisplayName("getConfiguration returns WmsPayloadDto for API task type")
  void getConfigurationReturnsWmsPayloadForApiTaskType() {
    // Given
    Map<String, Object> param1 = new HashMap<>();
    param1.put("label", "apiKey");
    param1.put("value", "secret123");
    param1.put("provided", true); // Backend-provided parameter appears in configuration payload

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
    ConfigProxyDto result = service.getConfiguration(request, 0L, coordinatesFor(request));

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
  @DisplayName(
      "getConfiguration sets OpenAPI-style http/basic security on API task when auth is configured")
  void getConfigurationSetsOpenApiHttpBasicSecurityForApiTask() {
    Map<String, Object> taskProperties = new HashMap<>();
    taskProperties.put(PROPERTY_COMMAND, "https://api.example.com/endpoint");
    taskProperties.put(PROPERTY_AUTHENTICATION_MODE, "HTTP Basic authentication");
    taskProperties.put(PROPERTY_USER, "apiUser");
    taskProperties.put(PROPERTY_PASSWORD, "apiSecret");
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
            .parameters(new HashMap<>())
            .build();

    when(taskRepository.findById(1)).thenReturn(Optional.of(mockTask));

    ConfigProxyDto result = service.getConfiguration(request, 0L, coordinatesFor(request));

    WmsPayloadDto payload = (WmsPayloadDto) result.getPayload();
    assertNotNull(payload.getSecurity());
    assertEquals("http", payload.getSecurity().getType());
    assertEquals("basic", payload.getSecurity().getScheme());
    assertEquals("apiUser", payload.getSecurity().getUsername());
    assertEquals("apiSecret", payload.getSecurity().getPassword());
  }

  @Test
  @DisplayName(
      "getConfiguration sets OpenAPI-style apiKey security on API task when headers map is present")
  void getConfigurationSetsOpenApiApiKeySecurityWhenHeadersMapPresent() {
    Map<String, Object> headers = new LinkedHashMap<>();
    headers.put("X-API-Key", "secret-key");
    headers.put("Authorization", "Bearer token");

    Map<String, Object> taskProperties = new HashMap<>();
    taskProperties.put(PROPERTY_COMMAND, "https://api.example.com/endpoint");
    taskProperties.put(PROPERTY_HEADERS, headers);
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
            .parameters(new HashMap<>())
            .build();

    when(taskRepository.findById(1)).thenReturn(Optional.of(mockTask));

    ConfigProxyDto result = service.getConfiguration(request, 0L, coordinatesFor(request));

    WmsPayloadDto payload = (WmsPayloadDto) result.getPayload();
    assertNotNull(payload.getSecurity());
    assertEquals("apiKey", payload.getSecurity().getType());
    assertNull(payload.getSecurity().getScheme());
    assertNotNull(payload.getSecurity().getHeaders());
    assertEquals("secret-key", payload.getSecurity().getHeaders().get("X-API-Key"));
    assertEquals("Bearer token", payload.getSecurity().getHeaders().get("Authorization"));
  }

  @Test
  @DisplayName(
      "getConfiguration sets OpenAPI-style apiKey security on API task when query params map is present")
  void getConfigurationSetsOpenApiApiKeySecurityWhenQueryParamsMapPresent() {
    Map<String, Object> queryParams = new LinkedHashMap<>();
    queryParams.put("api_key", "secret-key");
    queryParams.put("token", "access-token");

    Map<String, Object> taskProperties = new HashMap<>();
    taskProperties.put(PROPERTY_COMMAND, "https://api.example.com/endpoint");
    taskProperties.put(PROPERTY_QUERY_PARAMS, queryParams);
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
            .parameters(new HashMap<>())
            .build();

    when(taskRepository.findById(1)).thenReturn(Optional.of(mockTask));

    ConfigProxyDto result = service.getConfiguration(request, 0L, coordinatesFor(request));

    WmsPayloadDto payload = (WmsPayloadDto) result.getPayload();
    assertNotNull(payload.getSecurity());
    assertEquals("apiKey", payload.getSecurity().getType());
    assertNull(payload.getSecurity().getScheme());
    assertNotNull(payload.getSecurity().getQueryParams());
    assertEquals("secret-key", payload.getSecurity().getQueryParams().get("api_key"));
    assertEquals("access-token", payload.getSecurity().getQueryParams().get("token"));
  }

  @Test
  @DisplayName(
      "getConfiguration HTTP Basic auth takes precedence over API key when both are configured")
  void getConfigurationHttpBasicAuthTakesPrecedenceOverApiKey() {
    Map<String, Object> headers = new LinkedHashMap<>();
    headers.put("X-API-Key", "should-be-ignored");

    Map<String, Object> taskProperties = new HashMap<>();
    taskProperties.put(PROPERTY_COMMAND, "https://api.example.com/endpoint");
    taskProperties.put(PROPERTY_AUTHENTICATION_MODE, "HTTP Basic authentication");
    taskProperties.put(PROPERTY_USER, "admin");
    taskProperties.put(PROPERTY_PASSWORD, "secret");
    taskProperties.put(PROPERTY_HEADERS, headers);
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
            .parameters(new HashMap<>())
            .build();

    when(taskRepository.findById(1)).thenReturn(Optional.of(mockTask));

    ConfigProxyDto result = service.getConfiguration(request, 0L, coordinatesFor(request));

    WmsPayloadDto payload = (WmsPayloadDto) result.getPayload();
    assertNotNull(payload.getSecurity());
    assertEquals("http", payload.getSecurity().getType());
    assertEquals("basic", payload.getSecurity().getScheme());
    assertEquals("admin", payload.getSecurity().getUsername());
    assertEquals("secret", payload.getSecurity().getPassword());
    assertNull(payload.getSecurity().getHeaders());
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
    ConfigProxyDto result = service.getConfiguration(request, 0L, coordinatesFor(request));

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
    ConfigProxyDto result = service.getConfiguration(request, 0L, coordinatesFor(request));

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
    RequestCoordinates coordinates = coordinatesFor(request);
    assertThrows(
        BadRequestException.class, () -> service.getConfiguration(request, 0L, coordinates));
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
    RequestCoordinates coordinates = coordinatesFor(request);
    assertThrows(
        BadRequestException.class, () -> service.getConfiguration(request, 0L, coordinates));
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
    RequestCoordinates coordinates = coordinatesFor(request);
    assertThrows(
        BadRequestException.class, () -> service.getConfiguration(request, 0L, coordinates));
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
    ConfigProxyDto result = service.getConfiguration(request, 0L, coordinatesFor(request));

    // Then
    assertNotNull(result);
    assertInstanceOf(WmsPayloadDto.class, result.getPayload());

    WmsPayloadDto payload = (WmsPayloadDto) result.getPayload();
    assertTrue(payload.getParameters().isEmpty());
  }

  @Test
  @DisplayName("getConfiguration omits API task parameters whose configured value is null")
  void getConfigurationOmitsApiTaskParametersWithNullValue() {
    Map<String, Object> param1 = new HashMap<>();
    param1.put("label", "capa");
    param1.put("value", null);

    Map<String, Object> param2 = new HashMap<>();
    param2.put("label", "where");
    param2.put("value", null);

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

    ConfigProxyDto result = service.getConfiguration(request, 0L, coordinatesFor(request));

    assertNotNull(result);
    assertInstanceOf(WmsPayloadDto.class, result.getPayload());

    WmsPayloadDto payload = (WmsPayloadDto) result.getPayload();
    assertFalse(payload.getParameters().containsKey("capa"));
    assertFalse(payload.getParameters().containsKey("where"));
  }

  @Test
  @DisplayName("applyDecorators merges incoming HTTP request params into API payload parameters")
  void applyDecoratorsMergesIncomingHttpRequestParamsIntoApiPayloadParameters() {
    doCallRealMethod().when(httpUserParametrizationDecorator).apply(any(), any());
    doCallRealMethod().when(httpUserParametrizationDecorator).accept(any(), any());
    doCallRealMethod().when(httpUserParametrizationDecorator).addBehavior(any(), any());

    Map<String, Object> staticParam = new HashMap<>();
    staticParam.put("label", "f");
    staticParam.put("value", "pjson");

    Map<String, Object> declaredRequestParam = new HashMap<>();
    declaredRequestParam.put("label", "where");
    declaredRequestParam.put("value", null);

    Map<String, Object> taskProperties = new HashMap<>();
    taskProperties.put(PROPERTY_COMMAND, "https://api.example.com/query?backendParam=#{TERR_COD}");
    taskProperties.put("parameters", List.of(staticParam, declaredRequestParam));

    Task mockTask = mock(Task.class);
    when(mockTask.getProperties()).thenReturn(taskProperties);

    Map<String, String> requestParams = new HashMap<>();
    requestParams.put("where", "Mitjana");

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(12)
            .terId(4)
            .type(TYPE_API)
            .typeId(32289)
            .method("GET")
            .parameters(requestParams)
            .build();

    when(taskRepository.findById(32289)).thenReturn(Optional.of(mockTask));

    RequestCoordinates coordinates = coordinatesFor(request);
    ConfigProxyDto result = service.getConfiguration(request, 0L, coordinates);
    service.applyDecorators(result, request, coordinates);

    assertNotNull(result);
    assertInstanceOf(WmsPayloadDto.class, result.getPayload());

    WmsPayloadDto payload = (WmsPayloadDto) result.getPayload();
    assertEquals("Mitjana", payload.getParameters().get("where"));
    assertEquals("pjson", payload.getParameters().get("f"));
  }

  @Test
  @DisplayName(
      "applyDecorators expands URI templates using static payload params and keeps dynamic request params")
  void applyDecoratorsExpandsUriTemplatesUsingStaticPayloadParamsAndKeepsDynamicRequestParams() {
    doCallRealMethod().when(httpUserParametrizationDecorator).apply(any(), any());
    doCallRealMethod().when(httpUserParametrizationDecorator).accept(any(), any());
    doCallRealMethod().when(httpUserParametrizationDecorator).addBehavior(any(), any());

    Map<String, Object> templateParam = new HashMap<>();
    templateParam.put("label", "capa");
    templateParam.put("value", "agol_precio_m2");

    Map<String, Object> queryParam = new HashMap<>();
    queryParam.put("label", "f");
    queryParam.put("value", "pjson");

    Map<String, Object> declaredRequestParam = new HashMap<>();
    declaredRequestParam.put("label", "where");
    declaredRequestParam.put("value", null);

    Map<String, Object> taskProperties = new HashMap<>();
    taskProperties.put(
        PROPERTY_COMMAND,
        "https://services-eu1.arcgis.com/UpPGybwp9RK4YtZj/ArcGIS/rest/services/{capa}/FeatureServer/3/query");
    taskProperties.put("parameters", List.of(templateParam, queryParam, declaredRequestParam));

    Task mockTask = mock(Task.class);
    when(mockTask.getProperties()).thenReturn(taskProperties);

    Map<String, String> requestParams = new HashMap<>();
    requestParams.put("where", "Mitjana");

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(12)
            .terId(4)
            .type(TYPE_API)
            .typeId(32289)
            .method("GET")
            .parameters(requestParams)
            .build();

    when(taskRepository.findById(32289)).thenReturn(Optional.of(mockTask));

    RequestCoordinates coordinates = coordinatesFor(request);
    ConfigProxyDto result = service.getConfiguration(request, 0L, coordinates);
    service.applyDecorators(result, request, coordinates);

    assertNotNull(result);
    assertInstanceOf(WmsPayloadDto.class, result.getPayload());

    WmsPayloadDto payload = (WmsPayloadDto) result.getPayload();
    assertEquals(
        "https://services-eu1.arcgis.com/UpPGybwp9RK4YtZj/ArcGIS/rest/services/agol_precio_m2/FeatureServer/3/query",
        payload.getUri());
    assertFalse(payload.getParameters().containsKey("capa"));
    assertEquals("Mitjana", payload.getParameters().get("where"));
    assertEquals("pjson", payload.getParameters().get("f"));
  }

  @Test
  @DisplayName(
      "applyDecorators ignores unmatched incoming SQL params when task does not declare them")
  void applyDecoratorsIgnoresUnmatchedIncomingSqlParamsWhenTaskDoesNotDeclareThem() {
    Map<String, Object> taskProperties = new HashMap<>();
    taskProperties.put(PROPERTY_COMMAND, "SELECT * FROM STM_TSK_UI");
    taskProperties.put("parameters", Collections.emptyList());

    Task mockTask = mock(Task.class);
    when(mockTask.getProperties()).thenReturn(taskProperties);
    when(mockTask.getConnection()).thenReturn(mock(DatabaseConnection.class));
    when(mockTask.getConnection().getUrl()).thenReturn("jdbc:postgresql://postgres:5432/sitmun3");
    when(mockTask.getConnection().getUser()).thenReturn("sitmun3");
    when(mockTask.getConnection().getPassword()).thenReturn("sitmun3");
    when(mockTask.getConnection().getDriver()).thenReturn("org.postgresql.Driver");

    Map<String, String> requestParams = new HashMap<>();
    requestParams.put("test", "Mitjana");

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(12)
            .terId(4)
            .type(TYPE_SQL)
            .typeId(32290)
            .method("GET")
            .parameters(requestParams)
            .build();

    when(taskRepository.findById(32290)).thenReturn(Optional.of(mockTask));

    RequestCoordinates coordinates = coordinatesFor(request);
    ConfigProxyDto result = service.getConfiguration(request, 0L, coordinates);
    service.applyDecorators(result, request, coordinates);

    assertNotNull(result);
    assertInstanceOf(JdbcPayloadDto.class, result.getPayload());

    JdbcPayloadDto payload = (JdbcPayloadDto) result.getPayload();
    assertEquals("SELECT * FROM STM_TSK_UI", payload.getSql());
    assertTrue(payload.getParameters().isEmpty());
  }

  @Test
  @DisplayName(
      "applyDecorators substitutes explicit SQL placeholders from task default parameter values")
  void applyDecoratorsSubstitutesExplicitSqlPlaceholdersFromTaskDefaultParameterValues() {
    doCallRealMethod().when(sqlUserParametrizationDecorator).apply(any(), any());
    doCallRealMethod().when(sqlUserParametrizationDecorator).accept(any(), any());
    doCallRealMethod().when(sqlUserParametrizationDecorator).addBehavior(any(), any());

    Map<String, Object> declaredParam = new HashMap<>();
    declaredParam.put("label", "test");
    declaredParam.put("value", "Media");

    Map<String, Object> taskProperties = new HashMap<>();
    taskProperties.put(PROPERTY_COMMAND, "SELECT * FROM STM_TSK_UI WHERE tui_name = ${test}");
    taskProperties.put("parameters", List.of(declaredParam));

    DatabaseConnection mockConnection = mock(DatabaseConnection.class);
    when(mockConnection.getUrl()).thenReturn("jdbc:postgresql://postgres:5432/sitmun3");
    when(mockConnection.getUser()).thenReturn("sitmun3");
    when(mockConnection.getPassword()).thenReturn("sitmun3");
    when(mockConnection.getDriver()).thenReturn("org.postgresql.Driver");

    Task mockTask = mock(Task.class);
    when(mockTask.getProperties()).thenReturn(taskProperties);
    when(mockTask.getConnection()).thenReturn(mockConnection);

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(12)
            .terId(4)
            .type(TYPE_SQL)
            .typeId(32290)
            .method("GET")
            .parameters(new HashMap<>())
            .build();

    when(taskRepository.findById(32290)).thenReturn(Optional.of(mockTask));

    RequestCoordinates coordinates = coordinatesFor(request);
    ConfigProxyDto result = service.getConfiguration(request, 0L, coordinates);
    service.applyDecorators(result, request, coordinates);

    assertNotNull(result);
    assertInstanceOf(JdbcPayloadDto.class, result.getPayload());

    JdbcPayloadDto payload = (JdbcPayloadDto) result.getPayload();
    assertEquals("SELECT * FROM STM_TSK_UI WHERE tui_name = ?", payload.getSql());
    assertEquals(List.of("Media"), payload.getParameters());
  }

  @Test
  @DisplayName(
      "applyDecorators allows client to override literal SQL ${placeholder} defaults (client wins when not locked)")
  void applyDecoratorsAllowsClientToOverrideLiteralSqlPlaceholderDefaults() {
    doCallRealMethod().when(sqlUserParametrizationDecorator).apply(any(), any());
    doCallRealMethod().when(sqlUserParametrizationDecorator).accept(any(), any());
    doCallRealMethod().when(sqlUserParametrizationDecorator).addBehavior(any(), any());

    Map<String, Object> declaredParam = new HashMap<>();
    declaredParam.put("label", "test");
    declaredParam.put("value", "Media"); // Literal default, client can override

    Map<String, Object> taskProperties = new HashMap<>();
    taskProperties.put(PROPERTY_COMMAND, "SELECT * FROM STM_TSK_UI WHERE tui_name = ${test}");
    taskProperties.put("parameters", List.of(declaredParam));

    DatabaseConnection mockConnection = mock(DatabaseConnection.class);
    when(mockConnection.getUrl()).thenReturn("jdbc:postgresql://postgres:5432/sitmun3");
    when(mockConnection.getUser()).thenReturn("sitmun3");
    when(mockConnection.getPassword()).thenReturn("sitmun3");
    when(mockConnection.getDriver()).thenReturn("org.postgresql.Driver");

    Task mockTask = mock(Task.class);
    when(mockTask.getProperties()).thenReturn(taskProperties);
    when(mockTask.getConnection()).thenReturn(mockConnection);

    Map<String, String> requestParams = new HashMap<>();
    requestParams.put("test", "ClientShouldWin");

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(12)
            .terId(4)
            .type(TYPE_SQL)
            .typeId(32290)
            .method("GET")
            .parameters(requestParams)
            .build();

    when(taskRepository.findById(32290)).thenReturn(Optional.of(mockTask));

    RequestCoordinates coordinates = coordinatesFor(request);
    ConfigProxyDto result = service.getConfiguration(request, 0L, coordinates);
    service.applyDecorators(result, request, coordinates);

    assertNotNull(result);
    assertInstanceOf(JdbcPayloadDto.class, result.getPayload());

    JdbcPayloadDto payload = (JdbcPayloadDto) result.getPayload();
    assertEquals("SELECT * FROM STM_TSK_UI WHERE tui_name = ?", payload.getSql());
    // Client value wins over literal default
    assertEquals(List.of("ClientShouldWin"), payload.getParameters());
  }

  @Test
  @DisplayName(
      "applyDecorators allows client to override literal HTTP payload parameter defaults (client wins when not locked)")
  void applyDecoratorsAllowsClientToOverrideLiteralHttpPayloadParameterDefaults() {
    doCallRealMethod().when(httpUserParametrizationDecorator).apply(any(), any());
    doCallRealMethod().when(httpUserParametrizationDecorator).accept(any(), any());
    doCallRealMethod().when(httpUserParametrizationDecorator).addBehavior(any(), any());

    Map<String, Object> backendFixed = new HashMap<>();
    backendFixed.put("variable", "f");
    backendFixed.put("value", "pjson"); // Literal default, client can override

    Map<String, Object> declaredRequestParam = new HashMap<>();
    declaredRequestParam.put("variable", "outFields");
    declaredRequestParam.put("value", null);

    Map<String, Object> taskProperties = new HashMap<>();
    taskProperties.put(PROPERTY_COMMAND, "https://api.example.com/query");
    taskProperties.put("parameters", List.of(backendFixed, declaredRequestParam));

    Task mockTask = mock(Task.class);
    when(mockTask.getProperties()).thenReturn(taskProperties);

    Map<String, String> requestParams = new HashMap<>();
    requestParams.put("f", "geojson");
    requestParams.put("outFields", "*");

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(12)
            .terId(4)
            .type(TYPE_API)
            .typeId(32289)
            .method("GET")
            .parameters(requestParams)
            .build();

    when(taskRepository.findById(32289)).thenReturn(Optional.of(mockTask));

    RequestCoordinates coordinates = coordinatesFor(request);
    ConfigProxyDto result = service.getConfiguration(request, 0L, coordinates);
    service.applyDecorators(result, request, coordinates);

    assertNotNull(result);
    assertInstanceOf(WmsPayloadDto.class, result.getPayload());

    WmsPayloadDto payload = (WmsPayloadDto) result.getPayload();
    // Client value wins over literal default
    assertEquals("geojson", payload.getParameters().get("f"));
    assertEquals("*", payload.getParameters().get("outFields"));
  }

  @Test
  @DisplayName(
      "applyDecorators uses client HTTP parameter when task backend default is blank (empty string)")
  void applyDecoratorsUsesClientHttpParameterWhenTaskBackendDefaultIsBlankString() {
    doCallRealMethod().when(httpUserParametrizationDecorator).apply(any(), any());
    doCallRealMethod().when(httpUserParametrizationDecorator).accept(any(), any());
    doCallRealMethod().when(httpUserParametrizationDecorator).addBehavior(any(), any());

    Map<String, Object> backendBlank = new HashMap<>();
    backendBlank.put("variable", "f");
    backendBlank.put("value", "");

    Map<String, Object> declaredRequestParam = new HashMap<>();
    declaredRequestParam.put("variable", "where");
    declaredRequestParam.put("value", null);

    Map<String, Object> taskProperties = new HashMap<>();
    taskProperties.put(PROPERTY_COMMAND, "https://api.example.com/query");
    taskProperties.put("parameters", List.of(backendBlank, declaredRequestParam));

    Task mockTask = mock(Task.class);
    when(mockTask.getProperties()).thenReturn(taskProperties);

    Map<String, String> requestParams = new HashMap<>();
    requestParams.put("f", "geojson");
    requestParams.put("where", "CLIENT_WHERE");

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(12)
            .terId(4)
            .type(TYPE_API)
            .typeId(33204)
            .method("GET")
            .parameters(requestParams)
            .build();

    when(taskRepository.findById(33204)).thenReturn(Optional.of(mockTask));

    RequestCoordinates coordinates = coordinatesFor(request);
    ConfigProxyDto result = service.getConfiguration(request, 0L, coordinates);
    service.applyDecorators(result, request, coordinates);

    WmsPayloadDto payload = (WmsPayloadDto) result.getPayload();
    assertEquals("geojson", payload.getParameters().get("f"));
    assertEquals("CLIENT_WHERE", payload.getParameters().get("where"));
  }

  @Test
  @DisplayName(
      "applyDecorators uses client for SQL ${placeholder} when task configured default is blank")
  void applyDecoratorsUsesClientForSqlPlaceholderWhenTaskConfiguredDefaultIsBlankString() {
    doCallRealMethod().when(sqlUserParametrizationDecorator).apply(any(), any());
    doCallRealMethod().when(sqlUserParametrizationDecorator).accept(any(), any());
    doCallRealMethod().when(sqlUserParametrizationDecorator).addBehavior(any(), any());

    Map<String, Object> declaredParam = new HashMap<>();
    declaredParam.put("label", "test");
    declaredParam.put("value", "");

    Map<String, Object> taskProperties = new HashMap<>();
    taskProperties.put(PROPERTY_COMMAND, "SELECT * FROM STM_TSK_UI WHERE tui_name = ${test}");
    taskProperties.put("parameters", List.of(declaredParam));

    DatabaseConnection mockConnection = mock(DatabaseConnection.class);
    when(mockConnection.getUrl()).thenReturn("jdbc:postgresql://postgres:5432/sitmun3");
    when(mockConnection.getUser()).thenReturn("sitmun3");
    when(mockConnection.getPassword()).thenReturn("sitmun3");
    when(mockConnection.getDriver()).thenReturn("org.postgresql.Driver");

    Task mockTask = mock(Task.class);
    when(mockTask.getProperties()).thenReturn(taskProperties);
    when(mockTask.getConnection()).thenReturn(mockConnection);

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(12)
            .terId(4)
            .type(TYPE_SQL)
            .typeId(33205)
            .method("GET")
            .parameters(Map.of("test", "FromClient"))
            .build();

    when(taskRepository.findById(33205)).thenReturn(Optional.of(mockTask));

    RequestCoordinates coordinates = coordinatesFor(request);
    ConfigProxyDto result = service.getConfiguration(request, 0L, coordinates);
    service.applyDecorators(result, request, coordinates);

    JdbcPayloadDto payload = (JdbcPayloadDto) result.getPayload();
    assertEquals("SELECT * FROM STM_TSK_UI WHERE tui_name = ?", payload.getSql());
    assertEquals(List.of("FromClient"), payload.getParameters());
  }

  @Test
  @DisplayName(
      "applyDecorators prefers resolved #{…} task parameter values over client even when blank")
  void applyDecoratorsPrefersResolvedSystemVariableParameterOverClientEvenWhenBlank() {
    reset(systemVariableResolver);
    lenient()
        .when(systemVariableResolver.resolve(anyString(), any(RequestCoordinates.class)))
        .thenAnswer(inv -> "#{PROXY_EMPTY}".equals(inv.getArgument(0)) ? "" : inv.getArgument(0));

    doCallRealMethod().when(httpUserParametrizationDecorator).apply(any(), any());
    doCallRealMethod().when(httpUserParametrizationDecorator).accept(any(), any());
    doCallRealMethod().when(httpUserParametrizationDecorator).addBehavior(any(), any());

    Map<String, Object> computedParam = new HashMap<>();
    computedParam.put("variable", "token");
    computedParam.put("value", "#{PROXY_EMPTY}");

    Map<String, Object> taskProperties = new HashMap<>();
    taskProperties.put(PROPERTY_COMMAND, "https://api.example.com/data");
    taskProperties.put("parameters", List.of(computedParam));

    Task mockTask = mock(Task.class);
    when(mockTask.getProperties()).thenReturn(taskProperties);

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(12)
            .terId(4)
            .type(TYPE_API)
            .typeId(33206)
            .method("GET")
            .parameters(Map.of("token", "client-token"))
            .build();

    when(taskRepository.findById(33206)).thenReturn(Optional.of(mockTask));

    RequestCoordinates coordinates = coordinatesFor(request);
    ConfigProxyDto result = service.getConfiguration(request, 0L, coordinates);
    service.applyDecorators(result, request, coordinates);

    WmsPayloadDto payload = (WmsPayloadDto) result.getPayload();
    assertEquals("", payload.getParameters().get("token"));
  }

  @Test
  @DisplayName(
      "applyDecorators ignores client attempts to supply backend-only provided parameter names")
  void applyDecoratorsIgnoresClientAttemptsToSupplyBackendOnlyProvidedParameterNames() {
    doCallRealMethod().when(httpUserParametrizationDecorator).apply(any(), any());
    doCallRealMethod().when(httpUserParametrizationDecorator).accept(any(), any());
    doCallRealMethod().when(httpUserParametrizationDecorator).addBehavior(any(), any());

    Map<String, Object> secretParam = new HashMap<>();
    secretParam.put("variable", "apiKey");
    secretParam.put(PARAMETERS_PROVIDED, true);
    secretParam.put("value", "backend-secret");

    Map<String, Object> declaredRequestParam = new HashMap<>();
    declaredRequestParam.put("variable", "where");
    declaredRequestParam.put("value", null);

    Map<String, Object> taskProperties = new HashMap<>();
    taskProperties.put(PROPERTY_COMMAND, "https://api.example.com/data?token={apiKey}");
    taskProperties.put("parameters", List.of(secretParam, declaredRequestParam));

    Task mockTask = mock(Task.class);
    when(mockTask.getProperties()).thenReturn(taskProperties);

    Map<String, String> requestParams = new HashMap<>();
    requestParams.put("apiKey", "client-evil");
    requestParams.put("where", "1=1");

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(12)
            .terId(4)
            .type(TYPE_API)
            .typeId(32289)
            .method("GET")
            .parameters(requestParams)
            .build();

    when(taskRepository.findById(32289)).thenReturn(Optional.of(mockTask));

    RequestCoordinates coordinates = coordinatesFor(request);
    ConfigProxyDto result = service.getConfiguration(request, 0L, coordinates);
    service.applyDecorators(result, request, coordinates);

    assertNotNull(result);
    assertInstanceOf(WmsPayloadDto.class, result.getPayload());

    WmsPayloadDto payload = (WmsPayloadDto) result.getPayload();
    assertEquals("https://api.example.com/data?token=backend-secret", payload.getUri());
    assertFalse(payload.getParameters().containsKey("apiKey"));
    assertEquals("1=1", payload.getParameters().get("where"));
  }

  @Test
  @DisplayName(
      "applyDecorators keeps client HTTP parameter values when the task declares no backend value")
  void applyDecoratorsKeepsClientHttpParameterValuesWhenTaskDeclaresNoBackendValue() {
    doCallRealMethod().when(httpUserParametrizationDecorator).apply(any(), any());
    doCallRealMethod().when(httpUserParametrizationDecorator).accept(any(), any());
    doCallRealMethod().when(httpUserParametrizationDecorator).addBehavior(any(), any());

    Map<String, Object> declaredOnly = new HashMap<>();
    declaredOnly.put("variable", "where");
    declaredOnly.put("value", null);

    Map<String, Object> taskProperties = new HashMap<>();
    taskProperties.put(PROPERTY_COMMAND, "https://api.example.com/search");
    taskProperties.put("parameters", List.of(declaredOnly));

    Task mockTask = mock(Task.class);
    when(mockTask.getProperties()).thenReturn(taskProperties);

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(12)
            .terId(4)
            .type(TYPE_API)
            .typeId(33201)
            .method("GET")
            .parameters(Map.of("where", "CLIENT_PREDICATE"))
            .build();

    when(taskRepository.findById(33201)).thenReturn(Optional.of(mockTask));

    RequestCoordinates coordinates = coordinatesFor(request);
    ConfigProxyDto result = service.getConfiguration(request, 0L, coordinates);
    service.applyDecorators(result, request, coordinates);

    WmsPayloadDto payload = (WmsPayloadDto) result.getPayload();
    assertEquals("https://api.example.com/search", payload.getUri());
    assertEquals("CLIENT_PREDICATE", payload.getParameters().get("where"));
  }

  @Test
  @DisplayName(
      "applyDecorators uses client values for SQL ${placeholder} when task parameter has no default value")
  void applyDecoratorsUsesClientValuesForSqlPlaceholderWhenTaskParameterHasNoDefaultValue() {
    doCallRealMethod().when(sqlUserParametrizationDecorator).apply(any(), any());
    doCallRealMethod().when(sqlUserParametrizationDecorator).accept(any(), any());
    doCallRealMethod().when(sqlUserParametrizationDecorator).addBehavior(any(), any());

    Map<String, Object> declaredParam = new HashMap<>();
    declaredParam.put("label", "test");

    Map<String, Object> taskProperties = new HashMap<>();
    taskProperties.put(PROPERTY_COMMAND, "SELECT * FROM STM_TSK_UI WHERE tui_name = ${test}");
    taskProperties.put("parameters", List.of(declaredParam));

    DatabaseConnection mockConnection = mock(DatabaseConnection.class);
    when(mockConnection.getUrl()).thenReturn("jdbc:postgresql://postgres:5432/sitmun3");
    when(mockConnection.getUser()).thenReturn("sitmun3");
    when(mockConnection.getPassword()).thenReturn("sitmun3");
    when(mockConnection.getDriver()).thenReturn("org.postgresql.Driver");

    Task mockTask = mock(Task.class);
    when(mockTask.getProperties()).thenReturn(taskProperties);
    when(mockTask.getConnection()).thenReturn(mockConnection);

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(12)
            .terId(4)
            .type(TYPE_SQL)
            .typeId(33202)
            .method("GET")
            .parameters(Map.of("test", "FromClient"))
            .build();

    when(taskRepository.findById(33202)).thenReturn(Optional.of(mockTask));

    RequestCoordinates coordinates = coordinatesFor(request);
    ConfigProxyDto result = service.getConfiguration(request, 0L, coordinates);
    service.applyDecorators(result, request, coordinates);

    JdbcPayloadDto payload = (JdbcPayloadDto) result.getPayload();
    assertEquals("SELECT * FROM STM_TSK_UI WHERE tui_name = ?", payload.getSql());
    assertEquals(List.of("FromClient"), payload.getParameters());
  }

  @Test
  @DisplayName(
      "applyDecorators uses client vary-filter values when SQL has no ${placeholder} for task defaults")
  void applyDecoratorsUsesClientVaryFilterValuesWhenSqlHasNoPlaceholderForTaskDefaults() {
    doCallRealMethod().when(sqlUserParametrizationDecorator).apply(any(), any());
    doCallRealMethod().when(sqlUserParametrizationDecorator).accept(any(), any());
    doCallRealMethod().when(sqlUserParametrizationDecorator).addBehavior(any(), any());

    Map<String, Object> taskDefaultUnusedWithoutPlaceholder = new HashMap<>();
    taskDefaultUnusedWithoutPlaceholder.put("variable", "columnA");
    taskDefaultUnusedWithoutPlaceholder.put("value", "BACKEND_DEFAULT_NOT_REFERENCED_IN_SQL");

    Map<String, Object> taskProperties = new HashMap<>();
    taskProperties.put(PROPERTY_COMMAND, "SELECT * FROM STM_EXAMPLE");
    taskProperties.put("parameters", List.of(taskDefaultUnusedWithoutPlaceholder));

    DatabaseConnection mockConnection = mock(DatabaseConnection.class);
    when(mockConnection.getUrl()).thenReturn("jdbc:postgresql://postgres:5432/sitmun3");
    when(mockConnection.getUser()).thenReturn("sitmun3");
    when(mockConnection.getPassword()).thenReturn("sitmun3");
    when(mockConnection.getDriver()).thenReturn("org.postgresql.Driver");

    Task mockTask = mock(Task.class);
    when(mockTask.getProperties()).thenReturn(taskProperties);
    when(mockTask.getConnection()).thenReturn(mockConnection);

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(12)
            .terId(4)
            .type(TYPE_SQL)
            .typeId(33203)
            .method("GET")
            .parameters(Map.of("columnA", "client_filter"))
            .build();

    when(taskRepository.findById(33203)).thenReturn(Optional.of(mockTask));

    RequestCoordinates coordinates = coordinatesFor(request);
    ConfigProxyDto result = service.getConfiguration(request, 0L, coordinates);
    service.applyDecorators(result, request, coordinates);

    JdbcPayloadDto payload = (JdbcPayloadDto) result.getPayload();
    assertEquals("SELECT * FROM STM_EXAMPLE WHERE 1=1 AND columnA=?", payload.getSql());
    assertEquals(List.of("client_filter"), payload.getParameters());
  }

  @Test
  @DisplayName("getConfiguration stores integer and double parameter values as strings")
  void getConfigurationNormalizesNonStringParameterValues() {
    // Given
    Map<String, Object> param1 = new HashMap<>();
    param1.put("label", "count");
    param1.put("value", 42); // Integer value
    param1.put("provided", true); // Backend-provided parameter

    Map<String, Object> param2 = new HashMap<>();
    param2.put("label", "price");
    param2.put("value", 19.99); // Double value
    param2.put("provided", true); // Backend-provided parameter

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
    ConfigProxyDto result = service.getConfiguration(request, 0L, coordinatesFor(request));

    // Then
    assertNotNull(result);
    assertInstanceOf(WmsPayloadDto.class, result.getPayload());

    WmsPayloadDto payload = (WmsPayloadDto) result.getPayload();
    assertEquals("42", payload.getParameters().get("count"));
    assertEquals("19.99", payload.getParameters().get("price"));
  }

  @Test
  @DisplayName("Duplicate parameter labels keep the last value")
  void getConfigurationHandlesDuplicateParameterLabels() {
    // Given
    Map<String, Object> param1 = new HashMap<>();
    param1.put("label", "apiKey");
    param1.put("value", "first");
    param1.put("provided", true); // Backend-provided parameter

    Map<String, Object> param2 = new HashMap<>();
    param2.put("label", "apiKey");
    param2.put("value", "second");
    param2.put("provided", true); // Backend-provided parameter

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
    ConfigProxyDto result = service.getConfiguration(request, 0L, coordinatesFor(request));

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
        ConfigProxyRequestDto.builder().appId(1).terId(1).type(TYPE_SQL).typeId(23).build();

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
        ConfigProxyRequestDto.builder().appId(1).terId(1).type(TYPE_SQL).typeId(23).build();

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
        ConfigProxyRequestDto.builder().appId(1).terId(1).type(TYPE_SQL).typeId(23).build();

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
    ResourceAccessValidator mockValidator = mock(ResourceAccessValidator.class);
    when(mockValidator.supports(TYPE_SQL)).thenReturn(true);
    when(mockValidator.validate(any(), eq("admin"))).thenReturn(true);

    ProxyConfigurationService serviceWithValidator =
        proxyServiceWithAccessValidators(List.of(mockValidator));

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder().appId(1).terId(1).type(TYPE_SQL).typeId(23).build();

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
    ResourceAccessValidator mockValidator = mock(ResourceAccessValidator.class);
    when(mockValidator.supports(TYPE_SQL)).thenReturn(true);
    when(mockValidator.validate(any(), eq("unauthorizedUser"))).thenReturn(false);

    ProxyConfigurationService serviceWithValidator =
        proxyServiceWithAccessValidators(List.of(mockValidator));

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder().appId(1).terId(1).type(TYPE_SQL).typeId(23).build();

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
    ResourceAccessValidator validator1 = mock(ResourceAccessValidator.class);
    ResourceAccessValidator validator2 = mock(ResourceAccessValidator.class);

    when(validator1.supports(TYPE_WMS)).thenReturn(true);
    when(validator1.validate(any(), any())).thenReturn(true);

    ProxyConfigurationService serviceWithValidators =
        proxyServiceWithAccessValidators(List.of(validator1, validator2));

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder().appId(1).terId(0).type(TYPE_WMS).typeId(1).build();

    // When
    boolean result = serviceWithValidators.validateUserAccess(request, "admin");

    // Then: first validator is used, second is never called
    assertTrue(result);
    verify(validator1).validate(any(), eq("admin"));
    verify(validator2, never()).validate(any(), any());
  }

  // Security tests for #{...} client rejection

  @Test
  @DisplayName("applyDecorators rejects client parameter value with uppercase #{USER_ID} pattern")
  void applyDecoratorsRejectsClientParameterWithUppercaseSystemVariable() {
    Map<String, String> requestParams = new HashMap<>();
    requestParams.put("userId", "#{USER_ID}");

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(12)
            .terId(4)
            .type(TYPE_API)
            .typeId(1)
            .method("GET")
            .parameters(requestParams)
            .build();

    ConfigProxyDto mockConfig = mock(ConfigProxyDto.class);
    lenient().when(mockConfig.getPayload()).thenReturn(mock(WmsPayloadDto.class));

    RequestCoordinates coordinates = coordinatesFor(request);
    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () -> service.applyDecorators(mockConfig, request, coordinates));
    assertTrue(exception.getMessage().contains("#{...}"));
  }

  @Test
  @DisplayName(
      "applyDecorators rejects client parameter value with lowercase #{user.id} SpEL pattern")
  void applyDecoratorsRejectsClientParameterWithLowercaseSpelPattern() {
    Map<String, String> requestParams = new HashMap<>();
    requestParams.put("userId", "#{user.id}");

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(12)
            .terId(4)
            .type(TYPE_API)
            .typeId(1)
            .method("GET")
            .parameters(requestParams)
            .build();

    ConfigProxyDto mockConfig = mock(ConfigProxyDto.class);
    lenient().when(mockConfig.getPayload()).thenReturn(mock(WmsPayloadDto.class));

    RequestCoordinates coordinates = coordinatesFor(request);
    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () -> service.applyDecorators(mockConfig, request, coordinates));
    assertTrue(exception.getMessage().contains("#{...}"));
  }

  @Test
  @DisplayName("applyDecorators rejects client parameter value with whitespace-padded #{X} pattern")
  void applyDecoratorsRejectsClientParameterWithWhitespacePaddedSystemVariable() {
    Map<String, String> requestParams = new HashMap<>();
    requestParams.put("test", "  #{X}  ");

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(12)
            .terId(4)
            .type(TYPE_SQL)
            .typeId(1)
            .method("GET")
            .parameters(requestParams)
            .build();

    ConfigProxyDto mockConfig = mock(ConfigProxyDto.class);
    lenient().when(mockConfig.getPayload()).thenReturn(mock(JdbcPayloadDto.class));

    RequestCoordinates coordinates = coordinatesFor(request);
    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () -> service.applyDecorators(mockConfig, request, coordinates));
    assertTrue(exception.getMessage().contains("#{...}"));
  }

  @Test
  @DisplayName(
      "applyDecorators rejects LIMIT parameter with #{X} pattern before pagination stripping")
  void applyDecoratorsRejectsPaginationParameterWithSystemVariable() {
    Map<String, String> requestParams = new HashMap<>();
    requestParams.put("LIMIT", "#{X}");

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(12)
            .terId(4)
            .type(TYPE_SQL)
            .typeId(1)
            .method("GET")
            .parameters(requestParams)
            .build();

    ConfigProxyDto mockConfig = mock(ConfigProxyDto.class);
    lenient().when(mockConfig.getPayload()).thenReturn(mock(JdbcPayloadDto.class));

    RequestCoordinates coordinates = coordinatesFor(request);
    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () -> service.applyDecorators(mockConfig, request, coordinates));
    assertTrue(exception.getMessage().contains("#{...}"));
  }

  @Test
  @DisplayName(
      "applyDecorators drops client attempt to override provided: true parameter and uses backend value")
  void applyDecoratorsDropsClientAttemptToOverrideProvidedParameter() {
    doCallRealMethod().when(sqlUserParametrizationDecorator).apply(any(), any());
    doCallRealMethod().when(sqlUserParametrizationDecorator).accept(any(), any());
    doCallRealMethod().when(sqlUserParametrizationDecorator).addBehavior(any(), any());

    Map<String, Object> providedParam = new HashMap<>();
    providedParam.put("variable", "secret");
    providedParam.put("value", "BackendSecret");
    providedParam.put("provided", true);

    Map<String, Object> taskProperties = new HashMap<>();
    taskProperties.put(PROPERTY_COMMAND, "SELECT * FROM data WHERE key = ${secret}");
    taskProperties.put("parameters", List.of(providedParam));

    DatabaseConnection mockConnection = mock(DatabaseConnection.class);
    when(mockConnection.getUrl()).thenReturn("jdbc:postgresql://postgres:5432/sitmun3");
    when(mockConnection.getUser()).thenReturn("sitmun3");
    when(mockConnection.getPassword()).thenReturn("sitmun3");
    when(mockConnection.getDriver()).thenReturn("org.postgresql.Driver");

    Task mockTask = mock(Task.class);
    when(mockTask.getProperties()).thenReturn(taskProperties);
    when(mockTask.getConnection()).thenReturn(mockConnection);

    Map<String, String> requestParams = new HashMap<>();
    requestParams.put("secret", "ClientAttemptedOverride");

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(12)
            .terId(4)
            .type(TYPE_SQL)
            .typeId(100)
            .method("GET")
            .parameters(requestParams)
            .build();

    when(taskRepository.findById(100)).thenReturn(Optional.of(mockTask));

    RequestCoordinates coordinates = coordinatesFor(request);
    ConfigProxyDto result = service.getConfiguration(request, 0L, coordinates);
    service.applyDecorators(result, request, coordinates);

    assertNotNull(result);
    assertInstanceOf(JdbcPayloadDto.class, result.getPayload());

    JdbcPayloadDto payload = (JdbcPayloadDto) result.getPayload();
    // Backend provided value wins, client attempt is dropped
    assertEquals(List.of("BackendSecret"), payload.getParameters());
  }

  @Test
  @DisplayName(
      "Strict provided contract: only boolean true or string 'true' marks parameter as provided")
  void strictProvidedContract() {
    doCallRealMethod().when(httpUserParametrizationDecorator).apply(any(), any());
    doCallRealMethod().when(httpUserParametrizationDecorator).accept(any(), any());
    doCallRealMethod().when(httpUserParametrizationDecorator).addBehavior(any(), any());

    // false, "yes", 1, and missing provided should all be treated as NOT provided
    Map<String, Object> providedFalse = new HashMap<>();
    providedFalse.put("variable", "p1");
    providedFalse.put("value", "default1");
    providedFalse.put("provided", false);

    Map<String, Object> providedYes = new HashMap<>();
    providedYes.put("variable", "p2");
    providedYes.put("value", "default2");
    providedYes.put("provided", "yes");

    Map<String, Object> providedOne = new HashMap<>();
    providedOne.put("variable", "p3");
    providedOne.put("value", "default3");
    providedOne.put("provided", 1);

    Map<String, Object> providedMissing = new HashMap<>();
    providedMissing.put("variable", "p4");
    providedMissing.put("value", "default4");
    // no 'provided' key

    Map<String, Object> taskProperties = new HashMap<>();
    taskProperties.put(PROPERTY_COMMAND, "https://api.example.com/query");
    taskProperties.put(
        "parameters", List.of(providedFalse, providedYes, providedOne, providedMissing));

    Task mockTask = mock(Task.class);
    when(mockTask.getProperties()).thenReturn(taskProperties);

    Map<String, String> requestParams = new HashMap<>();
    requestParams.put("p1", "client1");
    requestParams.put("p2", "client2");
    requestParams.put("p3", "client3");
    requestParams.put("p4", "client4");

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(12)
            .terId(4)
            .type(TYPE_API)
            .typeId(101)
            .method("GET")
            .parameters(requestParams)
            .build();

    when(taskRepository.findById(101)).thenReturn(Optional.of(mockTask));

    RequestCoordinates coordinates = coordinatesFor(request);
    ConfigProxyDto result = service.getConfiguration(request, 0L, coordinates);
    service.applyDecorators(result, request, coordinates);

    assertNotNull(result);
    assertInstanceOf(WmsPayloadDto.class, result.getPayload());

    WmsPayloadDto payload = (WmsPayloadDto) result.getPayload();
    // All should allow client override since they're not strictly provided: true
    assertEquals("client1", payload.getParameters().get("p1"));
    assertEquals("client2", payload.getParameters().get("p2"));
    assertEquals("client3", payload.getParameters().get("p3"));
    assertEquals("client4", payload.getParameters().get("p4"));
  }

  // Functional tests for default-or-empty emission

  @Test
  @DisplayName(
      "HTTP declared param with no default and client absent emits empty string in parameters")
  void httpDeclaredParamWithNoDefaultAndClientAbsentEmitsEmptyString() {
    doCallRealMethod().when(httpUserParametrizationDecorator).apply(any(), any());
    doCallRealMethod().when(httpUserParametrizationDecorator).accept(any(), any());
    doCallRealMethod().when(httpUserParametrizationDecorator).addBehavior(any(), any());

    Map<String, Object> declaredNoDefault = new HashMap<>();
    declaredNoDefault.put("variable", "optional");
    declaredNoDefault.put("value", null);

    Map<String, Object> taskProperties = new HashMap<>();
    taskProperties.put(PROPERTY_COMMAND, "https://api.example.com/query");
    taskProperties.put("parameters", List.of(declaredNoDefault));

    Task mockTask = mock(Task.class);
    when(mockTask.getProperties()).thenReturn(taskProperties);

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(12)
            .terId(4)
            .type(TYPE_API)
            .typeId(102)
            .method("GET")
            .parameters(new HashMap<>()) // Client doesn't provide the parameter
            .build();

    when(taskRepository.findById(102)).thenReturn(Optional.of(mockTask));

    RequestCoordinates coordinates = coordinatesFor(request);
    ConfigProxyDto result = service.getConfiguration(request, 0L, coordinates);
    service.applyDecorators(result, request, coordinates);

    assertNotNull(result);
    assertInstanceOf(WmsPayloadDto.class, result.getPayload());

    WmsPayloadDto payload = (WmsPayloadDto) result.getPayload();
    // Parameter should be present with empty string value
    assertTrue(payload.getParameters().containsKey("optional"));
    assertEquals("", payload.getParameters().get("optional"));
  }

  @Test
  @DisplayName("SQL ${name} declared with no default and client absent binds empty string")
  void sqlPlaceholderDeclaredWithNoDefaultAndClientAbsentBindsEmptyString() {
    doCallRealMethod().when(sqlUserParametrizationDecorator).apply(any(), any());
    doCallRealMethod().when(sqlUserParametrizationDecorator).accept(any(), any());
    doCallRealMethod().when(sqlUserParametrizationDecorator).addBehavior(any(), any());

    Map<String, Object> declaredNoDefault = new HashMap<>();
    declaredNoDefault.put("variable", "filter");
    declaredNoDefault.put("value", null);

    Map<String, Object> taskProperties = new HashMap<>();
    taskProperties.put(PROPERTY_COMMAND, "SELECT * FROM data WHERE category = ${filter}");
    taskProperties.put("parameters", List.of(declaredNoDefault));

    DatabaseConnection mockConnection = mock(DatabaseConnection.class);
    when(mockConnection.getUrl()).thenReturn("jdbc:postgresql://postgres:5432/sitmun3");
    when(mockConnection.getUser()).thenReturn("sitmun3");
    when(mockConnection.getPassword()).thenReturn("sitmun3");
    when(mockConnection.getDriver()).thenReturn("org.postgresql.Driver");

    Task mockTask = mock(Task.class);
    when(mockTask.getProperties()).thenReturn(taskProperties);
    when(mockTask.getConnection()).thenReturn(mockConnection);

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(12)
            .terId(4)
            .type(TYPE_SQL)
            .typeId(103)
            .method("GET")
            .parameters(new HashMap<>()) // Client doesn't provide the parameter
            .build();

    when(taskRepository.findById(103)).thenReturn(Optional.of(mockTask));

    RequestCoordinates coordinates = coordinatesFor(request);
    ConfigProxyDto result = service.getConfiguration(request, 0L, coordinates);
    service.applyDecorators(result, request, coordinates);

    assertNotNull(result);
    assertInstanceOf(JdbcPayloadDto.class, result.getPayload());

    JdbcPayloadDto payload = (JdbcPayloadDto) result.getPayload();
    assertEquals("SELECT * FROM data WHERE category = ?", payload.getSql());
    // Empty string should be bound
    assertEquals(List.of(""), payload.getParameters());
  }

  @Test
  @DisplayName("SQL vary-filter declared with no default and client absent binds empty string")
  void sqlVaryFilterDeclaredWithNoDefaultAndClientAbsentBindsEmptyString() {
    doCallRealMethod().when(sqlUserParametrizationDecorator).apply(any(), any());
    doCallRealMethod().when(sqlUserParametrizationDecorator).accept(any(), any());
    doCallRealMethod().when(sqlUserParametrizationDecorator).addBehavior(any(), any());

    Map<String, Object> declaredNoDefault = new HashMap<>();
    declaredNoDefault.put("variable", "status");
    declaredNoDefault.put("value", null);

    Map<String, Object> taskProperties = new HashMap<>();
    taskProperties.put(PROPERTY_COMMAND, "SELECT * FROM orders");
    taskProperties.put("parameters", List.of(declaredNoDefault));

    DatabaseConnection mockConnection = mock(DatabaseConnection.class);
    when(mockConnection.getUrl()).thenReturn("jdbc:postgresql://postgres:5432/sitmun3");
    when(mockConnection.getUser()).thenReturn("sitmun3");
    when(mockConnection.getPassword()).thenReturn("sitmun3");
    when(mockConnection.getDriver()).thenReturn("org.postgresql.Driver");

    Task mockTask = mock(Task.class);
    when(mockTask.getProperties()).thenReturn(taskProperties);
    when(mockTask.getConnection()).thenReturn(mockConnection);

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(12)
            .terId(4)
            .type(TYPE_SQL)
            .typeId(104)
            .method("GET")
            .parameters(new HashMap<>()) // Client doesn't provide the parameter
            .build();

    when(taskRepository.findById(104)).thenReturn(Optional.of(mockTask));

    RequestCoordinates coordinates = coordinatesFor(request);
    ConfigProxyDto result = service.getConfiguration(request, 0L, coordinates);
    service.applyDecorators(result, request, coordinates);

    assertNotNull(result);
    assertInstanceOf(JdbcPayloadDto.class, result.getPayload());

    JdbcPayloadDto payload = (JdbcPayloadDto) result.getPayload();
    // Vary-filter adds WHERE clause with empty string bind
    assertEquals("SELECT * FROM orders WHERE 1=1 AND status=?", payload.getSql());
    assertEquals(List.of(""), payload.getParameters());
  }

  @Test
  @DisplayName(
      "SQL vary-filter with #{...} locked default filters out client value and binds resolved backend value")
  void sqlVaryFilterWithLockedDefaultFiltersClientAndBindsBackendValue() {
    doCallRealMethod().when(sqlUserParametrizationDecorator).apply(any(), any());
    doCallRealMethod().when(sqlUserParametrizationDecorator).accept(any(), any());
    doCallRealMethod().when(sqlUserParametrizationDecorator).addBehavior(any(), any());

    // Mock systemVariableResolver to resolve #{ADMIN_FILTER} to 'ADMIN_ONLY'
    when(systemVariableResolver.resolve(eq("#{ADMIN_FILTER}"), any(RequestCoordinates.class)))
        .thenReturn("ADMIN_ONLY");

    Map<String, Object> lockedVaryFilter = new HashMap<>();
    lockedVaryFilter.put("variable", "role");
    lockedVaryFilter.put("value", "#{ADMIN_FILTER}");

    Map<String, Object> taskProperties = new HashMap<>();
    taskProperties.put(PROPERTY_COMMAND, "SELECT * FROM users");
    taskProperties.put("parameters", List.of(lockedVaryFilter));

    DatabaseConnection mockConnection = mock(DatabaseConnection.class);
    when(mockConnection.getUrl()).thenReturn("jdbc:postgresql://postgres:5432/sitmun3");
    when(mockConnection.getUser()).thenReturn("sitmun3");
    when(mockConnection.getPassword()).thenReturn("sitmun3");
    when(mockConnection.getDriver()).thenReturn("org.postgresql.Driver");

    Task mockTask = mock(Task.class);
    when(mockTask.getProperties()).thenReturn(taskProperties);
    when(mockTask.getConnection()).thenReturn(mockConnection);

    Map<String, String> requestParams = new HashMap<>();
    requestParams.put("role", "ClientAttemptedValue");

    ConfigProxyRequestDto request =
        ConfigProxyRequestDto.builder()
            .appId(12)
            .terId(4)
            .type(TYPE_SQL)
            .typeId(105)
            .method("GET")
            .parameters(requestParams)
            .build();

    when(taskRepository.findById(105)).thenReturn(Optional.of(mockTask));

    RequestCoordinates coordinates = coordinatesFor(request);
    ConfigProxyDto result = service.getConfiguration(request, 0L, coordinates);
    service.applyDecorators(result, request, coordinates);

    assertNotNull(result);
    assertInstanceOf(JdbcPayloadDto.class, result.getPayload());

    JdbcPayloadDto payload = (JdbcPayloadDto) result.getPayload();
    // Vary-filter with locked default binds resolved backend value, not client
    assertEquals("SELECT * FROM users WHERE 1=1 AND role=?", payload.getSql());
    assertEquals(List.of("ADMIN_ONLY"), payload.getParameters());
  }
}
