package org.sitmun.domain.task.parameter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.sitmun.domain.DomainConstants.Tasks.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.authorization.client.dto.profile.FeatureInfoParameter;
import org.sitmun.authorization.client.dto.profile.QueryParameter;
import org.sitmun.infrastructure.variables.SystemVariableResolver;
import org.springframework.util.StringUtils;

/**
 * Documents and verifies the intentional distinction between query-task parameter DTOs and
 * more-info parameter DTOs. These two shapes serve different contracts and should NOT be merged
 * without explicit audit approval.
 *
 * <p>Query task parameters: execution contract (minimal {@code {type, required}})
 *
 * <p>More-info parameters: feature-field forwarding contract ({@code {name, label, value, type?,
 * required?}})
 */
@DisplayName("Task Parameter Shape Contract Tests")
class TaskParameterShapeContractTest {

  private TaskParameterProcessor processor;

  @BeforeEach
  void setUp() {
    SystemVariableResolver mockResolver = mock(SystemVariableResolver.class);
    when(mockResolver.resolve(anyString(), any())).thenAnswer(inv -> inv.getArgument(0));
    processor = new TaskParameterProcessor(mockResolver);
  }

  @Test
  @DisplayName("Query task parameters use minimal shape: {type, required}")
  void queryTaskParametersUseMinimalShape() {
    // Given: a query parameter (for direct query execution)
    Map<String, Object> paramMap = new HashMap<>();
    paramMap.put(PARAMETERS_VARIABLE, "format");
    paramMap.put(PARAMETERS_VALUE, null);
    paramMap.put(PARAMETERS_TYPE, PARAM_TYPE_QUERY);
    paramMap.put(PARAMETERS_REQUIRED, true);

    TaskParameter parameter = taskParameterFromStorageMap(paramMap);

    // When: converted to simple DTO (used by TaskQuerySqlService, TaskQueryWebService)
    QueryParameter dto = processor.toQueryParameter(parameter, "string");

    // Then: contains only type and required (minimal execution contract)
    // QueryParameter record only has type and required fields
    assertNotNull(dto.type(), "Should have type");
    // No name, label, or value fields exist in QueryParameter by design
  }

  @Test
  @DisplayName(
      "More-info parameters use field-forwarding shape: {name, label, value, type?, required?}")
  void moreInfoParametersUseFieldForwardingShape() {
    // Given: a more-info parameter (for feature-field forwarding)
    Map<String, Object> paramMap = new HashMap<>();
    paramMap.put(PARAMETERS_VARIABLE, "capa");
    paramMap.put(PARAMETERS_FIELD, "layerField");
    paramMap.put(PARAMETERS_TYPE, PARAM_TYPE_QUERY);
    paramMap.put(PARAMETERS_REQUIRED, false);

    TaskParameter parameter = taskParameterFromStorageMap(paramMap);

    // When: converted to viewer DTO (used by TaskMoreInfoService)
    FeatureInfoParameter dto = processor.toFeatureInfoParameter(parameter);

    // Then: contains name, label, value (field-forwarding contract)
    assertNotNull(dto.name(), "Must include name");
    assertNotNull(dto.label(), "Must include label");
    assertNotNull(dto.value(), "Must include value as field name");

    // Value is the feature field name for more-info
    assertEquals(
        "layerField", dto.value(), "Value should be the feature field name for forwarding");

    // Optional fields are also included
    assertNotNull(dto.type());
    assertNotNull(dto.required());
  }

  @Test
  @DisplayName("Parameter shapes are intentionally different and serve distinct contracts")
  void parameterShapesServeDistinctContracts() {
    // Given: same base parameter
    Map<String, Object> paramMap = new HashMap<>();
    paramMap.put(PARAMETERS_VARIABLE, "filter");
    paramMap.put(PARAMETERS_FIELD, "municipio");
    paramMap.put(PARAMETERS_TYPE, PARAM_TYPE_QUERY);
    paramMap.put(PARAMETERS_REQUIRED, true);

    TaskParameter parameter = taskParameterFromStorageMap(paramMap);

    // When: converted to both shapes
    QueryParameter simpleDto = processor.toQueryParameter(parameter, "string");
    FeatureInfoParameter viewerDto = processor.toFeatureInfoParameter(parameter);

    // Then: shapes are fundamentally different
    // Simple DTO has 2 fields (type, required)
    // Viewer DTO has at least 3 always-present fields (name, label, value)
    assertNotNull(simpleDto);
    assertNotNull(viewerDto);

    // Simple DTO only exposes type and required (no name/label/value fields by design)
    assertNotNull(simpleDto.type());

    // Viewer DTO includes name/label/value (field-forwarding contract)
    assertNotNull(viewerDto.name());
    assertNotNull(viewerDto.value());

    // This difference is intentional and must NOT be removed without audit approval
  }

  @Test
  @DisplayName("Query parameter with default value still uses minimal shape")
  void queryParameterWithDefaultValueUsesMinimalShape() {
    // Given: query parameter with a default value
    Map<String, Object> paramMap = new HashMap<>();
    paramMap.put(PARAMETERS_VARIABLE, "format");
    paramMap.put(PARAMETERS_VALUE, "json");
    paramMap.put(PARAMETERS_TYPE, PARAM_TYPE_QUERY);
    paramMap.put(PARAMETERS_REQUIRED, false);

    TaskParameter parameter = taskParameterFromStorageMap(paramMap);

    // When: converted to simple DTO
    QueryParameter dto = processor.toQueryParameter(parameter, "string");

    // Then: still minimal shape (value is not exposed in simple DTO)
    // QueryParameter record only has type and required fields
    assertNotNull(dto.type(), "Simple DTO should have type");
    // No value field exists in QueryParameter by design
  }

  @Test
  @DisplayName("More-info parameter value represents feature field name, not default value")
  void moreInfoParameterValueRepresentsFeatureFieldName() {
    // Given: more-info parameter with both field and value
    Map<String, Object> paramMap = new HashMap<>();
    paramMap.put(PARAMETERS_VARIABLE, "comarca");
    paramMap.put(PARAMETERS_FIELD, "COMARCA_FIELD");
    paramMap.put(PARAMETERS_VALUE, "DefaultComarca");
    paramMap.put(PARAMETERS_TYPE, PARAM_TYPE_QUERY);

    TaskParameter parameter = taskParameterFromStorageMap(paramMap);

    // When: converted to viewer DTO
    FeatureInfoParameter dto = processor.toFeatureInfoParameter(parameter);

    // Then: value is the FIELD name (for feature data extraction), not the default value
    assertEquals(
        "COMARCA_FIELD",
        dto.value(),
        "Value should be the feature field name (for data forwarding), not the default value");
  }

  /**
   * Builds a {@link TaskParameter} like {@link TaskParameterProcessor#parse} does for each map row.
   */
  private static TaskParameter taskParameterFromStorageMap(Map<String, Object> parameter) {
    Object variableObj = parameter.get(PARAMETERS_VARIABLE);
    String name = variableObj != null ? String.valueOf(variableObj) : null;
    Object labelFallback = parameter.get(PARAMETERS_LABEL);
    String label = labelFallback != null ? String.valueOf(labelFallback) : null;
    if (!StringUtils.hasText(name)) {
      name = label;
    }
    if (!StringUtils.hasText(name)) {
      throw new IllegalArgumentException("test map requires variable or label");
    }

    Object rawValueObj = parameter.get(PARAMETERS_VALUE);
    String rawValue = rawValueObj != null ? String.valueOf(rawValueObj) : null;

    Object fieldObj = parameter.get(PARAMETERS_FIELD);
    String field = fieldObj != null ? String.valueOf(fieldObj) : null;

    Object typeObj = parameter.get(PARAMETERS_TYPE);
    String type = typeObj != null ? String.valueOf(typeObj) : null;

    Object requiredObj = parameter.get(PARAMETERS_REQUIRED);
    Boolean required = requiredObj != null ? Boolean.valueOf(String.valueOf(requiredObj)) : null;

    Object providedObj = parameter.get(PARAMETERS_PROVIDED);
    boolean providedFlag =
        Boolean.TRUE.equals(providedObj) || "true".equalsIgnoreCase(String.valueOf(providedObj));

    Object descriptionObj = parameter.get(PARAMETERS_DESCRIPTION);
    String description = descriptionObj != null ? String.valueOf(descriptionObj) : null;

    return new TaskParameter(
        name, rawValue, field, type, required, providedFlag, label, description, parameter);
  }
}
