package org.sitmun.authorization.client.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sitmun.domain.task.parameter.TaskParameterProcessor.ProfileParameterShape.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.authorization.client.dto.profile.FeatureInfoParameter;
import org.sitmun.authorization.client.dto.profile.QueryParameter;
import org.sitmun.authorization.client.dto.profile.ServiceParameter;
import org.sitmun.domain.task.parameter.TaskParameter;
import org.sitmun.domain.task.parameter.TaskParameterProcessor;
import org.sitmun.infrastructure.variables.SystemVariableResolver;

/**
 * Consumer contract tests for profile parameter JSON wire format.
 *
 * <p>These tests verify contracts that the viewer and admin applications depend on. They provide
 * clearer failure messages than raw snapshot diffs by naming the broken contract.
 */
@DisplayName("Consumer contract verification tests")
class ConsumerContractTest {

  private TaskParameterProcessor processor;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    SystemVariableResolver mockResolver = mock(SystemVariableResolver.class);
    when(mockResolver.resolve(anyString(), any())).thenAnswer(inv -> inv.getArgument(0));
    processor = new TaskParameterProcessor(mockResolver);
    objectMapper = new ObjectMapper();
  }

  @Test
  @DisplayName("Viewer profile must emit label, value, name even when null")
  void viewerProfileMustEmitLabelValueName() throws Exception {
    // Given - all fields null except parameter name
    TaskParameter param =
        new TaskParameter("testParam", null, null, null, null, false, null, null, new HashMap<>());

    // When
    Map<String, Object> result =
        processor.toProfileParameterMap(List.of(param), EXTERNAL_LINK_VIEWER, false);

    // Then
    String json = objectMapper.writeValueAsString(result.get("testParam"));
    assertThat(json)
        .as("Viewer profile must contain label, value, name keys for feature-field forwarding")
        .contains("\"label\"", "\"value\"", "\"name\"");

    // Verify the result is a FeatureInfoParameter record
    assertThat(result.get("testParam"))
        .as("Viewer more-info handler destructures {label, name, value}")
        .isInstanceOf(FeatureInfoParameter.class);

    // Value must be JSON null (not omitted) for falsy short-circuit chain
    assertThat(json).contains("\"value\":null");
  }

  @Test
  @DisplayName("Simple profile must NOT emit label, value, or name")
  void simpleProfileMustNotEmitLabelValueOrName() throws Exception {
    // Given
    TaskParameter param =
        new TaskParameter(
            "testParam",
            "someValue",
            null,
            "string",
            true,
            false,
            "Label",
            "Description",
            new HashMap<>());

    // When
    Map<String, Object> result =
        processor.toProfileParameterMap(List.of(param), SIMPLE_STRING_DEFAULT, false);

    // Then
    String json = objectMapper.writeValueAsString(result.get("testParam"));
    assertThat(json)
        .as(
            "Simple profile must NOT contain label, value, or name - breaks viewer isSqlParamConfig ('key' in obj)")
        .doesNotContain("\"label\"", "\"value\"", "\"name\"");

    // Verify the result is a QueryParameter record (only has type and required fields)
    assertThat(result.get("testParam"))
        .as("SQL parameters must not be treated as feature-field forwarding configs")
        .isInstanceOf(QueryParameter.class);
  }

  @Test
  @DisplayName(
      "Value profile with value present must NOT emit label or name (only type, value, required)")
  void valueProfileMustNotEmitLabelOrName() throws Exception {
    // Given
    TaskParameter param =
        new TaskParameter(
            "testParam",
            "value",
            null,
            "string",
            true,
            false,
            "Label",
            "Description",
            new HashMap<>());

    // When
    Map<String, Object> result =
        processor.toProfileParameterMap(
            List.of(param), CARTOGRAPHY_QUERY_WITH_VALUE_STRING_DEFAULT, false);

    // Then
    String json = objectMapper.writeValueAsString(result.get("testParam"));
    assertThat(json)
        .as("Value profile must NOT contain label or name")
        .doesNotContain("\"label\"", "\"name\"");

    // Verify the result is a ServiceParameter record (only has type, value?, required fields)
    assertThat(result.get("testParam"))
        .as("Value profile with value present should only have type, value, required")
        .isInstanceOf(ServiceParameter.class);
  }

  @Test
  @DisplayName("Never inject top-level keys named queryType or apiUrl")
  void neverInjectQueryTypeOrApiUrl() {
    // Given - parameters with reserved names
    TaskParameter queryTypeParam =
        new TaskParameter(
            "queryType", "value", null, "string", true, false, null, null, new HashMap<>());
    TaskParameter apiUrlParam =
        new TaskParameter(
            "apiUrl", "url", null, "string", true, false, null, null, new HashMap<>());

    // When - map to any profile shape
    Map<String, Object> simpleResult =
        processor.toProfileParameterMap(
            List.of(queryTypeParam, apiUrlParam), SIMPLE_STRING_DEFAULT, false);
    Map<String, Object> viewerResult =
        processor.toProfileParameterMap(
            List.of(queryTypeParam, apiUrlParam), EXTERNAL_LINK_VIEWER, false);

    // Then - these keys should appear as regular parameter slots, not top-level TaskDto fields
    // This test verifies TaskParameterProcessor doesn't reject them (TaskDto.parameters accepts
    // them)
    // The actual boundary is at TaskDto serialization level (verified by controller tests)
    assertThat(simpleResult)
        .as(
            "Parameters named queryType/apiUrl are allowed in parameters map (viewer checks task.parameters.queryType, not TaskDto.queryType)")
        .containsKeys("queryType", "apiUrl");
    assertThat(viewerResult)
        .as("Viewer profile also allows these parameter names")
        .containsKeys("queryType", "apiUrl");
  }

  @Test
  @DisplayName("All shapes: provided flag must never appear in JSON")
  void providedFlagNeverAppearsInJson() throws Exception {
    // Given
    TaskParameter param =
        new TaskParameter(
            "testParam", "value", null, "string", true, false, null, null, new HashMap<>());

    // When - test all shapes
    Map<String, Object> simpleResult =
        processor.toProfileParameterMap(List.of(param), SIMPLE_STRING_DEFAULT, false);
    Map<String, Object> valueResult =
        processor.toProfileParameterMap(
            List.of(param), CARTOGRAPHY_QUERY_WITH_VALUE_STRING_DEFAULT, false);
    Map<String, Object> viewerResult =
        processor.toProfileParameterMap(List.of(param), EXTERNAL_LINK_VIEWER, false);

    // Then
    String simpleJson = objectMapper.writeValueAsString(simpleResult.get("testParam"));
    String valueJson = objectMapper.writeValueAsString(valueResult.get("testParam"));
    String viewerJson = objectMapper.writeValueAsString(viewerResult.get("testParam"));

    assertThat(simpleJson)
        .as("Simple profile must not contain provided flag")
        .doesNotContain("\"provided\"");
    assertThat(valueJson)
        .as("Value profile must not contain provided flag")
        .doesNotContain("\"provided\"");
    assertThat(viewerJson)
        .as("Viewer profile must not contain provided flag")
        .doesNotContain("\"provided\"");
  }

  @Test
  @DisplayName("Provided parameters are excluded from all profile shapes")
  void providedParametersExcludedFromProfiles() {
    // Given
    TaskParameter providedParam =
        new TaskParameter(
            "secret", "#{apiKey}", null, "string", true, true, null, null, new HashMap<>());
    TaskParameter normalParam =
        new TaskParameter("normal", null, null, "string", true, false, null, null, new HashMap<>());

    // When - test all shapes
    Map<String, Object> simpleResult =
        processor.toProfileParameterMap(
            List.of(providedParam, normalParam), SIMPLE_STRING_DEFAULT, false);
    Map<String, Object> valueResult =
        processor.toProfileParameterMap(
            List.of(providedParam, normalParam),
            CARTOGRAPHY_QUERY_WITH_VALUE_STRING_DEFAULT,
            false);
    Map<String, Object> viewerResult =
        processor.toProfileParameterMap(
            List.of(providedParam, normalParam), EXTERNAL_LINK_VIEWER, false);

    // Then - only normal parameter is included
    assertThat(simpleResult)
        .as("Simple profile excludes provided parameters")
        .containsOnlyKeys("normal");
    assertThat(valueResult)
        .as("Value profile excludes provided parameters")
        .containsOnlyKeys("normal");
    assertThat(viewerResult)
        .as("Viewer profile excludes provided parameters")
        .containsOnlyKeys("normal");
  }
}
