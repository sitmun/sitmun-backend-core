package org.sitmun.authorization.client.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.sitmun.authorization.client.AuthorizationConstants.TaskDto.PARAMETER_LAYERS;
import static org.sitmun.authorization.client.AuthorizationConstants.TaskDto.PARAMETER_SERVICE;
import static org.sitmun.authorization.client.AuthorizationConstants.TaskDto.PARAMETER_WFS_TYPENAME;
import static org.sitmun.domain.DomainConstants.Services.TYPE_WFS;
import static org.sitmun.domain.DomainConstants.Services.TYPE_WMS;
import static org.sitmun.domain.DomainConstants.Tasks.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.authorization.client.dto.TaskDto;
import org.sitmun.authorization.client.dto.profile.ServiceParameter;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.parameter.TaskParameterProcessor;
import org.sitmun.domain.territory.Territory;
import org.sitmun.infrastructure.variables.SystemVariableResolver;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("TaskQueryCartographyService parameter DTO mapping")
class TaskQueryCartographyServiceTest {

  private TaskQueryCartographyService service;

  @BeforeEach
  void setUp() {
    SystemVariableResolver resolver = mock(SystemVariableResolver.class);
    when(resolver.resolve(anyString(), any())).thenAnswer(inv -> inv.getArgument(0));
    TaskParameterProcessor processor = new TaskParameterProcessor(resolver);
    service = new TaskQueryCartographyService(processor);
    ReflectionTestUtils.setField(service, "proxyUrl", "http://proxy.test");
  }

  @Test
  @DisplayName("map passes through template and query types and injects service and layers maps")
  void mapPassesThroughParameterTypesAndInjectsServiceAndLayers() {
    Task task = mock(Task.class);
    Application application = mock(Application.class);
    Territory territory = mock(Territory.class);
    Cartography cartography = mock(Cartography.class);
    Service wmsService = mock(Service.class);

    when(task.getId()).thenReturn(300);
    when(task.getCartography()).thenReturn(cartography);
    when(cartography.getId()).thenReturn(77);
    when(cartography.getLayers()).thenReturn(List.of("A", "B"));
    when(cartography.getService()).thenReturn(wmsService);
    when(wmsService.getId()).thenReturn(15);
    when(wmsService.getType()).thenReturn(TYPE_WMS);
    when(application.getId()).thenReturn(2);
    when(territory.getId()).thenReturn(3);

    Map<String, Object> templateParam = new HashMap<>();
    templateParam.put(PARAMETERS_VARIABLE, "filterExpr");
    templateParam.put(PARAMETERS_TYPE, PARAM_TYPE_TEMPLATE);
    templateParam.put(PARAMETERS_REQUIRED, true);
    templateParam.put(PARAMETERS_VALUE, "defaultExpr");

    Map<String, Object> queryParam = new HashMap<>();
    queryParam.put(PARAMETERS_VARIABLE, "maxFeatures");
    queryParam.put(PARAMETERS_TYPE, PARAM_TYPE_QUERY);
    queryParam.put(PARAMETERS_REQUIRED, false);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_PARAMETERS, List.of(templateParam, queryParam));
    when(task.getProperties()).thenReturn(properties);

    TaskDto result = service.map(task, application, territory);

    assertEquals("77", result.getCartographyId());
    assertEquals(PROFILE_LAYER_ID_PREFIX + "77", result.getLayer());
    assertEquals(
        "http://proxy.test/proxy/2/3/WMS/15",
        result.getUrl(),
        "WMS cartography uses service type and id in proxy path");

    Map<String, Object> params = result.getParameters();
    assertNotNull(params);

    ServiceParameter filterDto = (ServiceParameter) params.get("filterExpr");
    assertEquals(PARAM_TYPE_TEMPLATE, filterDto.type());
    assertTrue(filterDto.required());
    assertEquals("defaultExpr", filterDto.value());

    ServiceParameter maxDto = (ServiceParameter) params.get("maxFeatures");
    assertEquals(PARAM_TYPE_QUERY, maxDto.type());
    assertFalse(maxDto.required());

    ServiceParameter serviceBlock = (ServiceParameter) params.get(PARAMETER_SERVICE);
    assertEquals(PARAM_TYPE_QUERY, serviceBlock.type());
    assertEquals(TYPE_WMS, serviceBlock.value());
    assertTrue(serviceBlock.required());

    ServiceParameter layersBlock = (ServiceParameter) params.get(PARAMETER_LAYERS);
    assertEquals("A,B", layersBlock.value());
  }

  @Test
  @DisplayName("map uses typename block for WFS services")
  void mapUsesTypenameForWfsService() {
    Task task = mock(Task.class);
    Application application = mock(Application.class);
    Territory territory = mock(Territory.class);
    Cartography cartography = mock(Cartography.class);
    Service wfsService = mock(Service.class);

    when(task.getId()).thenReturn(301);
    when(task.getCartography()).thenReturn(cartography);
    when(cartography.getId()).thenReturn(88);
    when(cartography.getLayers()).thenReturn(List.of("ns:LayerX"));
    when(cartography.getService()).thenReturn(wfsService);
    when(wfsService.getId()).thenReturn(16);
    when(wfsService.getType()).thenReturn(TYPE_WFS);
    when(application.getId()).thenReturn(1);
    when(territory.getId()).thenReturn(1);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_PARAMETERS, List.of());
    when(task.getProperties()).thenReturn(properties);

    TaskDto result = service.map(task, application, territory);

    assertTrue(result.getParameters().containsKey(PARAMETER_WFS_TYPENAME));
    ServiceParameter typename =
        (ServiceParameter) result.getParameters().get(PARAMETER_WFS_TYPENAME);
    assertEquals("ns:LayerX", typename.value());
  }

  @Test
  @DisplayName("map defaults missing user parameter type to string")
  void mapDefaultsMissingUserParameterTypeToString() {
    Task task = mock(Task.class);
    Application application = mock(Application.class);
    Territory territory = mock(Territory.class);
    Cartography cartography = mock(Cartography.class);
    Service wmsService = mock(Service.class);

    when(task.getId()).thenReturn(302);
    when(task.getCartography()).thenReturn(cartography);
    when(cartography.getId()).thenReturn(1);
    when(cartography.getLayers()).thenReturn(List.of("L"));
    when(cartography.getService()).thenReturn(wmsService);
    when(wmsService.getId()).thenReturn(1);
    when(wmsService.getType()).thenReturn(TYPE_WMS);
    when(application.getId()).thenReturn(1);
    when(territory.getId()).thenReturn(1);

    Map<String, Object> raw = new HashMap<>();
    raw.put(PARAMETERS_VARIABLE, "plain");
    raw.put(PARAMETERS_REQUIRED, false);

    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_PARAMETERS, List.of(raw));
    when(task.getProperties()).thenReturn(properties);

    TaskDto result = service.map(task, application, territory);
    ServiceParameter dto = (ServiceParameter) result.getParameters().get("plain");
    assertEquals(TYPE_STRING, dto.type());
  }
}
