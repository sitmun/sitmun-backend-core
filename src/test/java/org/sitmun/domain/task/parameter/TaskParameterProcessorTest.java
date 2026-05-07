package org.sitmun.domain.task.parameter;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.authorization.proxy.exception.BadRequestException;
import org.sitmun.authorization.proxy.service.RequestCoordinates;
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.task.Task;
import org.sitmun.infrastructure.variables.SystemVariableResolver;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskParameterProcessor")
class TaskParameterProcessorTest {

  @Mock private SystemVariableResolver mockSystemVariableResolver;

  private TaskParameterProcessor processor;

  @BeforeEach
  void setUp() {
    processor = new TaskParameterProcessor(mockSystemVariableResolver);
    // Default: no-op resolution
    lenient()
        .when(mockSystemVariableResolver.resolve(any(), any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Nested
  @DisplayName("parse")
  class ParseTests {

    @Test
    @DisplayName("returns empty list when task is null")
    void returnsEmptyListWhenTaskIsNull() {
      List<TaskParameter> result = processor.parse(null);
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("returns empty list when task has no properties")
    void returnsEmptyListWhenTaskHasNoProperties() {
      Task task = Task.builder().build();
      List<TaskParameter> result = processor.parse(task);
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("returns empty list when properties has no parameters")
    void returnsEmptyListWhenPropertiesHasNoParameters() {
      Task task = Task.builder().properties(new HashMap<>()).build();
      List<TaskParameter> result = processor.parse(task);
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("parses parameter with variable field")
    void parsesParameterWithVariableField() {
      Map<String, Object> param = new HashMap<>();
      param.put("variable", "userId");
      param.put("value", "123");
      param.put("type", "string");
      param.put("required", true);

      Task task = createTaskWithParameters(List.of(param));

      List<TaskParameter> result = processor.parse(task);

      assertThat(result).hasSize(1);
      TaskParameter parsed = result.get(0);
      assertThat(parsed.name()).isEqualTo("userId");
      assertThat(parsed.rawValue()).isEqualTo("123");
      assertThat(parsed.type()).isEqualTo("string");
      assertThat(parsed.required()).isTrue();
      assertThat(parsed.providedFlag()).isFalse();
    }

    @Test
    @DisplayName("falls back to name field when variable is absent")
    void fallsBackToNameFieldWhenVariableIsAbsent() {
      Map<String, Object> param = new HashMap<>();
      param.put("name", "userName");
      param.put("value", "admin");

      Task task = createTaskWithParameters(List.of(param));

      List<TaskParameter> result = processor.parse(task);

      assertThat(result).hasSize(1);
      assertThat(result.get(0).name()).isEqualTo("userName");
    }

    @Test
    @DisplayName("falls back to label field when variable and name are absent")
    void fallsBackToLabelFieldWhenVariableAndNameAreAbsent() {
      Map<String, Object> param = new HashMap<>();
      param.put("label", "userLabel");
      param.put("value", "test");

      Task task = createTaskWithParameters(List.of(param));

      List<TaskParameter> result = processor.parse(task);

      assertThat(result).hasSize(1);
      assertThat(result.get(0).name()).isEqualTo("userLabel");
    }

    @Test
    @DisplayName("skips parameter with no name, variable, or label")
    void skipsParameterWithNoNameVariableOrLabel() {
      Map<String, Object> param = new HashMap<>();
      param.put("value", "orphan");

      Task task = createTaskWithParameters(List.of(param));

      List<TaskParameter> result = processor.parse(task);

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("captures field property for MoreInfo compatibility")
    void capturesFieldPropertyForMoreInfoCompatibility() {
      Map<String, Object> param = new HashMap<>();
      param.put("variable", "param1");
      param.put("value", "defaultValue");
      param.put("field", "alternateField");

      Task task = createTaskWithParameters(List.of(param));

      List<TaskParameter> result = processor.parse(task);

      assertThat(result).hasSize(1);
      assertThat(result.get(0).field()).isEqualTo("alternateField");
    }

    @Test
    @DisplayName("parses provided flag correctly")
    void parsesProvidedFlagCorrectly() {
      Map<String, Object> providedTrue = new HashMap<>();
      providedTrue.put("variable", "p1");
      providedTrue.put("provided", true);

      Map<String, Object> providedFalse = new HashMap<>();
      providedFalse.put("variable", "p2");
      providedFalse.put("provided", false);

      Map<String, Object> providedString = new HashMap<>();
      providedString.put("variable", "p3");
      providedString.put("provided", "true");

      Task task = createTaskWithParameters(List.of(providedTrue, providedFalse, providedString));

      List<TaskParameter> result = processor.parse(task);

      assertThat(result).hasSize(3);
      assertThat(result.get(0).providedFlag()).isTrue();
      assertThat(result.get(1).providedFlag()).isFalse();
      assertThat(result.get(2).providedFlag()).isTrue();
    }

    @Test
    @DisplayName("preserves label and description")
    void preservesLabelAndDescription() {
      Map<String, Object> param = new HashMap<>();
      param.put("variable", "param1");
      param.put("label", "User Parameter");
      param.put("description", "Parameter description");

      Task task = createTaskWithParameters(List.of(param));

      List<TaskParameter> result = processor.parse(task);

      assertThat(result).hasSize(1);
      assertThat(result.get(0).label()).isEqualTo("User Parameter");
      assertThat(result.get(0).description()).isEqualTo("Parameter description");
    }

    @Test
    @DisplayName("preserves raw map for unmapped keys")
    void preservesRawMapForUnmappedKeys() {
      Map<String, Object> param = new HashMap<>();
      param.put("variable", "param1");
      param.put("customKey", "customValue");

      Task task = createTaskWithParameters(List.of(param));

      List<TaskParameter> result = processor.parse(task);

      assertThat(result).hasSize(1);
      assertThat(result.get(0).raw()).containsEntry("customKey", "customValue");
    }
  }

  @Nested
  @DisplayName("classify")
  class ClassifyTests {

    @Test
    @DisplayName("classifies parameter with #{...} as LOCKED")
    void classifiesParameterWithSystemVariablesAsLocked() {
      TaskParameter param = createTaskParameter("userId", "#{USER_ID}", false);
      assertThat(processor.classify(param)).isEqualTo(TaskParameterType.LOCKED);
    }

    @Test
    @DisplayName("LOCKED takes priority over PROVIDED flag")
    void lockedTakesPriorityOverProvidedFlag() {
      TaskParameter param = createTaskParameter("userId", "#{USER_ID}", true);
      assertThat(processor.classify(param)).isEqualTo(TaskParameterType.LOCKED);
    }

    @Test
    @DisplayName("classifies parameter with provided=true as PROVIDED")
    void classifiesParameterWithProvidedFlagAsProvided() {
      TaskParameter param = createTaskParameter("apiKey", "secret123", true);
      assertThat(processor.classify(param)).isEqualTo(TaskParameterType.PROVIDED);
    }

    @Test
    @DisplayName("classifies parameter with non-blank value as DECLARED_WITH_DEFAULT")
    void classifiesParameterWithNonBlankValueAsDeclaredWithDefault() {
      TaskParameter param = createTaskParameter("format", "json", false);
      assertThat(processor.classify(param)).isEqualTo(TaskParameterType.DECLARED_WITH_DEFAULT);
    }

    @Test
    @DisplayName("classifies parameter with null value as DECLARED_WITHOUT_DEFAULT")
    void classifiesParameterWithNullValueAsDeclaredWithoutDefault() {
      TaskParameter param = createTaskParameter("filter", null, false);
      assertThat(processor.classify(param)).isEqualTo(TaskParameterType.DECLARED_WITHOUT_DEFAULT);
    }

    @Test
    @DisplayName("classifies parameter with blank value as DECLARED_WITHOUT_DEFAULT")
    void classifiesParameterWithBlankValueAsDeclaredWithoutDefault() {
      TaskParameter param = createTaskParameter("query", "  ", false);
      assertThat(processor.classify(param)).isEqualTo(TaskParameterType.DECLARED_WITHOUT_DEFAULT);
    }

    @Test
    @DisplayName("handles #{TERRITORY_ID} pattern as LOCKED")
    void handlesUppercaseSystemVariablePatternAsLocked() {
      TaskParameter param = createTaskParameter("territoryId", "#{TERRITORY_ID}", false);
      assertThat(processor.classify(param)).isEqualTo(TaskParameterType.LOCKED);
    }
  }

  @Nested
  @DisplayName("clientAllowedNames")
  class ClientAllowedNamesTests {

    @Test
    @DisplayName("excludes LOCKED parameters")
    void excludesLockedParameters() {
      List<TaskParameter> params =
          List.of(
              createTaskParameter("userId", "#{USER_ID}", false),
              createTaskParameter("format", "json", false));

      Set<String> allowed = processor.clientAllowedNames(params);

      assertThat(allowed).containsExactly("format");
    }

    @Test
    @DisplayName("excludes PROVIDED parameters")
    void excludesProvidedParameters() {
      List<TaskParameter> params =
          List.of(
              createTaskParameter("apiKey", "secret", true),
              createTaskParameter("query", null, false));

      Set<String> allowed = processor.clientAllowedNames(params);

      assertThat(allowed).containsExactly("query");
    }

    @Test
    @DisplayName("includes DECLARED_WITH_DEFAULT parameters")
    void includesDeclaredWithDefaultParameters() {
      List<TaskParameter> params = List.of(createTaskParameter("format", "xml", false));

      Set<String> allowed = processor.clientAllowedNames(params);

      assertThat(allowed).containsExactly("format");
    }

    @Test
    @DisplayName("includes DECLARED_WITHOUT_DEFAULT parameters")
    void includesDeclaredWithoutDefaultParameters() {
      List<TaskParameter> params = List.of(createTaskParameter("filter", null, false));

      Set<String> allowed = processor.clientAllowedNames(params);

      assertThat(allowed).containsExactly("filter");
    }

    @Test
    @DisplayName("returns empty set for all backend-only parameters")
    void returnsEmptySetForAllBackendOnlyParameters() {
      List<TaskParameter> params =
          List.of(
              createTaskParameter("userId", "#{USER_ID}", false),
              createTaskParameter("apiKey", "secret", true));

      Set<String> allowed = processor.clientAllowedNames(params);

      assertThat(allowed).isEmpty();
    }
  }

  @Nested
  @DisplayName("filterClientParameters")
  class FilterClientParametersTests {

    @Test
    @DisplayName("returns empty map when client parameters are null")
    void returnsEmptyMapWhenClientParametersAreNull() {
      List<TaskParameter> params = List.of(createTaskParameter("param1", "default", false));

      Map<String, String> result = processor.filterClientParameters(params, null);

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("filters out parameters not in declared list")
    void filtersOutParametersNotInDeclaredList() {
      List<TaskParameter> params = List.of(createTaskParameter("allowed", "default", false));

      Map<String, String> clientParams = new LinkedHashMap<>();
      clientParams.put("allowed", "clientValue");
      clientParams.put("unknown", "shouldBeDropped");

      Map<String, String> result = processor.filterClientParameters(params, clientParams);

      assertThat(result).containsOnlyKeys("allowed").containsEntry("allowed", "clientValue");
    }

    @Test
    @DisplayName("filters out LOCKED parameters from client input")
    void filtersOutLockedParametersFromClientInput() {
      List<TaskParameter> params =
          List.of(
              createTaskParameter("userId", "#{USER_ID}", false),
              createTaskParameter("query", null, false));

      Map<String, String> clientParams = new LinkedHashMap<>();
      clientParams.put("userId", "attackValue");
      clientParams.put("query", "safeValue");

      Map<String, String> result = processor.filterClientParameters(params, clientParams);

      assertThat(result).containsOnlyKeys("query").containsEntry("query", "safeValue");
    }

    @Test
    @DisplayName("filters out PROVIDED parameters from client input")
    void filtersOutProvidedParametersFromClientInput() {
      List<TaskParameter> params =
          List.of(
              createTaskParameter("apiKey", "secret", true),
              createTaskParameter("format", "json", false));

      Map<String, String> clientParams = new LinkedHashMap<>();
      clientParams.put("apiKey", "attackValue");
      clientParams.put("format", "xml");

      Map<String, String> result = processor.filterClientParameters(params, clientParams);

      assertThat(result).containsOnlyKeys("format").containsEntry("format", "xml");
    }
  }

  @Nested
  @DisplayName("buildEffectiveParameters")
  class BuildEffectiveParametersTests {

    private RequestCoordinates mockCoordinates;

    @BeforeEach
    void setUp() {
      mockCoordinates = mock(RequestCoordinates.class);
    }

    @Test
    @DisplayName("LOCKED parameter wins over client value")
    void lockedParameterWinsOverClientValue() {
      when(mockSystemVariableResolver.resolve(eq("#{USER_ID}"), any())).thenReturn("999");

      List<TaskParameter> params = List.of(createTaskParameter("userId", "#{USER_ID}", false));

      Map<String, String> clientParams = Map.of("userId", "clientAttempt");

      Map<String, String> result =
          processor.buildEffectiveParameters(params, clientParams, mockCoordinates);

      assertThat(result).containsEntry("userId", "999");
    }

    @Test
    @DisplayName("PROVIDED parameter wins over client value")
    void providedParameterWinsOverClientValue() {
      List<TaskParameter> params = List.of(createTaskParameter("apiKey", "backend123", true));

      Map<String, String> clientParams = Map.of("apiKey", "clientAttempt");

      Map<String, String> result =
          processor.buildEffectiveParameters(params, clientParams, mockCoordinates);

      assertThat(result).containsEntry("apiKey", "backend123");
    }

    @Test
    @DisplayName("client value wins over DECLARED_WITH_DEFAULT")
    void clientValueWinsOverDeclaredWithDefault() {
      List<TaskParameter> params = List.of(createTaskParameter("format", "json", false));

      Map<String, String> clientParams = Map.of("format", "xml");

      Map<String, String> result =
          processor.buildEffectiveParameters(params, clientParams, mockCoordinates);

      assertThat(result).containsEntry("format", "xml");
    }

    @Test
    @DisplayName("DECLARED_WITH_DEFAULT used when client does not supply value")
    void declaredWithDefaultUsedWhenClientDoesNotSupplyValue() {
      List<TaskParameter> params = List.of(createTaskParameter("format", "json", false));

      Map<String, String> result =
          processor.buildEffectiveParameters(params, null, mockCoordinates);

      assertThat(result).containsEntry("format", "json");
    }

    @Test
    @DisplayName("DECLARED_WITHOUT_DEFAULT emits empty string when client absent")
    void declaredWithoutDefaultEmitsEmptyStringWhenClientAbsent() {
      List<TaskParameter> params = List.of(createTaskParameter("filter", null, false));

      Map<String, String> result =
          processor.buildEffectiveParameters(params, null, mockCoordinates);

      assertThat(result).containsEntry("filter", "");
    }

    @Test
    @DisplayName("DECLARED_WITHOUT_DEFAULT uses client value when supplied")
    void declaredWithoutDefaultUsesClientValueWhenSupplied() {
      List<TaskParameter> params = List.of(createTaskParameter("filter", null, false));

      Map<String, String> clientParams = Map.of("filter", "status=active");

      Map<String, String> result =
          processor.buildEffectiveParameters(params, clientParams, mockCoordinates);

      assertThat(result).containsEntry("filter", "status=active");
    }

    @Test
    @DisplayName("LOCKED with #{...} participates even when resolved to empty")
    void lockedWithSystemVariablesParticipatesEvenWhenResolvedToEmpty() {
      when(mockSystemVariableResolver.resolve(eq("#{MISSING}"), any())).thenReturn("");

      List<TaskParameter> params = List.of(createTaskParameter("param", "#{MISSING}", false));

      Map<String, String> result =
          processor.buildEffectiveParameters(params, null, mockCoordinates);

      assertThat(result).containsEntry("param", "");
    }

    @Test
    @DisplayName("all declared parameters appear in result")
    void allDeclaredParametersAppearInResult() {
      when(mockSystemVariableResolver.resolve(eq("#{USER_ID}"), any())).thenReturn("123");

      List<TaskParameter> params =
          List.of(
              createTaskParameter("userId", "#{USER_ID}", false),
              createTaskParameter("apiKey", "secret", true),
              createTaskParameter("format", "json", false),
              createTaskParameter("query", null, false));

      Map<String, String> clientParams = Map.of("format", "xml", "query", "test");

      Map<String, String> result =
          processor.buildEffectiveParameters(params, clientParams, mockCoordinates);

      assertThat(result)
          .hasSize(4)
          .containsEntry("userId", "123")
          .containsEntry("apiKey", "secret")
          .containsEntry("format", "xml")
          .containsEntry("query", "test");
    }
  }

  @Nested
  @DisplayName("rejectClientSystemVariables")
  class RejectClientSystemVariablesTests {

    @Test
    @DisplayName("does not throw when client parameters are null")
    void doesNotThrowWhenClientParametersAreNull() {
      assertThatCode(() -> processor.rejectClientSystemVariables(null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("does not throw when client parameters are empty")
    void doesNotThrowWhenClientParametersAreEmpty() {
      assertThatCode(() -> processor.rejectClientSystemVariables(Collections.emptyMap()))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("throws BadRequestException for uppercase #{USER_ID} pattern")
    void throwsBadRequestExceptionForUppercaseSystemVariablePattern() {
      Map<String, String> clientParams = Map.of("param", "#{USER_ID}");

      assertThatThrownBy(() -> processor.rejectClientSystemVariables(clientParams))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("system variable patterns");
    }

    @Test
    @DisplayName("throws BadRequestException for lowercase #{user.id} pattern")
    void throwsBadRequestExceptionForLowercaseSystemVariablePattern() {
      Map<String, String> clientParams = Map.of("param", "#{user.id}");

      assertThatThrownBy(() -> processor.rejectClientSystemVariables(clientParams))
          .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("throws BadRequestException for whitespace-padded pattern")
    void throwsBadRequestExceptionForWhitespacePaddedPattern() {
      Map<String, String> clientParams = Map.of("param", "#{ USER_ID }");

      assertThatThrownBy(() -> processor.rejectClientSystemVariables(clientParams))
          .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("throws BadRequestException for T(...) SpEL pattern")
    void throwsBadRequestExceptionForSpelPattern() {
      Map<String, String> clientParams = Map.of("param", "#{T(java.lang.Runtime).getRuntime()}");

      assertThatThrownBy(() -> processor.rejectClientSystemVariables(clientParams))
          .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("accepts normal parameter values without #{...}")
    void acceptsNormalParameterValuesWithoutSystemVariables() {
      Map<String, String> clientParams =
          Map.of("format", "json", "query", "status=active", "limit", "100");

      assertThatCode(() -> processor.rejectClientSystemVariables(clientParams))
          .doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("toSimpleParameterDto")
  class ToSimpleParameterDto {

    @Test
    @DisplayName("creates DTO with type and required from parameter")
    void createsDtoWithTypeAndRequired() {
      TaskParameter param =
          new TaskParameter(
              "format", "json", null, "string", true, false, "Format", "Output format", Map.of());

      Map<String, Object> dto = processor.toSimpleParameterDto(param, "default");

      assertThat(dto).containsEntry("type", "string").containsEntry("required", true);
    }

    @Test
    @DisplayName("uses default type when parameter type is null")
    void usesDefaultTypeWhenParameterTypeIsNull() {
      TaskParameter param =
          new TaskParameter("format", "json", null, null, true, false, null, null, Map.of());

      Map<String, Object> dto = processor.toSimpleParameterDto(param, "string");

      assertThat(dto).containsEntry("type", "string").containsEntry("required", true);
    }

    @Test
    @DisplayName("defaults required to false when null")
    void defaultsRequiredToFalseWhenNull() {
      TaskParameter param =
          new TaskParameter("format", "json", null, "string", null, false, null, null, Map.of());

      Map<String, Object> dto = processor.toSimpleParameterDto(param, "default");

      assertThat(dto).containsEntry("type", "string").containsEntry("required", false);
    }

    @Test
    @DisplayName("respects different default type values")
    void respectsDifferentDefaultTypeValues() {
      TaskParameter param =
          new TaskParameter("query", null, null, null, false, false, null, null, Map.of());

      Map<String, Object> dtoString = processor.toSimpleParameterDto(param, "string");
      Map<String, Object> dtoQuery = processor.toSimpleParameterDto(param, "query");

      assertThat(dtoString).containsEntry("type", "string");
      assertThat(dtoQuery).containsEntry("type", "query");
    }

    @Test
    @DisplayName("does not include value field")
    void doesNotIncludeValueField() {
      TaskParameter param =
          new TaskParameter("format", "json", null, "string", true, false, null, null, Map.of());

      Map<String, Object> dto = processor.toSimpleParameterDto(param, "string");

      assertThat(dto).doesNotContainKey("value");
    }
  }

  @Nested
  @DisplayName("toParameterDtoWithValue")
  class ToParameterDtoWithValue {

    @Test
    @DisplayName("creates DTO with type, required, and value")
    void createsDtoWithTypeRequiredAndValue() {
      TaskParameter param =
          new TaskParameter("format", "json", null, "string", true, false, null, null, Map.of());

      Map<String, Object> dto = processor.toParameterDtoWithValue(param, "default");

      assertThat(dto)
          .containsEntry("type", "string")
          .containsEntry("required", true)
          .containsEntry("value", "json");
    }

    @Test
    @DisplayName("omits value field when rawValue is null")
    void omitsValueFieldWhenRawValueIsNull() {
      TaskParameter param =
          new TaskParameter("format", null, null, "string", true, false, null, null, Map.of());

      Map<String, Object> dto = processor.toParameterDtoWithValue(param, "default");

      assertThat(dto)
          .containsEntry("type", "string")
          .containsEntry("required", true)
          .doesNotContainKey("value");
    }

    @Test
    @DisplayName("uses default type when parameter type is null")
    void usesDefaultTypeWhenParameterTypeIsNull() {
      TaskParameter param =
          new TaskParameter("format", "json", null, null, false, false, null, null, Map.of());

      Map<String, Object> dto = processor.toParameterDtoWithValue(param, "query");

      assertThat(dto).containsEntry("type", "query");
    }

    @Test
    @DisplayName("defaults required to false when null")
    void defaultsRequiredToFalseWhenNull() {
      TaskParameter param =
          new TaskParameter("format", "json", null, "string", null, false, null, null, Map.of());

      Map<String, Object> dto = processor.toParameterDtoWithValue(param, "default");

      assertThat(dto).containsEntry("required", false);
    }

    @Test
    @DisplayName("includes empty string as value")
    void includesEmptyStringAsValue() {
      TaskParameter param =
          new TaskParameter("format", "", null, "string", true, false, null, null, Map.of());

      Map<String, Object> dto = processor.toParameterDtoWithValue(param, "default");

      assertThat(dto).containsEntry("value", "");
    }
  }

  @Nested
  @DisplayName("toViewerParameterDto")
  class ToViewerParameterDto {

    @Test
    @DisplayName("creates DTO with label, value from field, and name")
    void createsDtoWithLabelValueFromFieldAndName() {
      TaskParameter param =
          new TaskParameter(
              "format",
              "json",
              "OUTPUT_FORMAT",
              "string",
              true,
              false,
              "Output Format",
              "Select format",
              Map.of());

      Map<String, Object> dto = processor.toViewerParameterDto(param);

      assertThat(dto)
          .containsEntry("label", "format")
          .containsEntry("value", "OUTPUT_FORMAT")
          .containsEntry("name", "format")
          .containsEntry("type", "string")
          .containsEntry("required", true);
    }

    @Test
    @DisplayName("falls back to rawValue when field is null")
    void fallsBackToRawValueWhenFieldIsNull() {
      TaskParameter param =
          new TaskParameter("format", "json", null, "string", true, false, null, null, Map.of());

      Map<String, Object> dto = processor.toViewerParameterDto(param);

      assertThat(dto)
          .containsEntry("label", "format")
          .containsEntry("value", "json")
          .containsEntry("name", "format");
    }

    @Test
    @DisplayName("omits type field when parameter type is null")
    void omitsTypeFieldWhenParameterTypeIsNull() {
      TaskParameter param =
          new TaskParameter("format", "json", null, null, true, false, null, null, Map.of());

      Map<String, Object> dto = processor.toViewerParameterDto(param);

      assertThat(dto)
          .containsEntry("label", "format")
          .containsEntry("value", "json")
          .containsEntry("name", "format")
          .containsEntry("required", true)
          .doesNotContainKey("type");
    }

    @Test
    @DisplayName("omits required field when parameter required is null")
    void omitsRequiredFieldWhenParameterRequiredIsNull() {
      TaskParameter param =
          new TaskParameter("format", "json", null, "string", null, false, null, null, Map.of());

      Map<String, Object> dto = processor.toViewerParameterDto(param);

      assertThat(dto)
          .containsEntry("label", "format")
          .containsEntry("value", "json")
          .containsEntry("name", "format")
          .containsEntry("type", "string")
          .doesNotContainKey("required");
    }

    @Test
    @DisplayName("omits both type and required when both are null")
    void omitsBothTypeAndRequiredWhenBothAreNull() {
      TaskParameter param =
          new TaskParameter("format", "json", null, null, null, false, null, null, Map.of());

      Map<String, Object> dto = processor.toViewerParameterDto(param);

      assertThat(dto)
          .containsEntry("label", "format")
          .containsEntry("value", "json")
          .containsEntry("name", "format")
          .doesNotContainKey("type")
          .doesNotContainKey("required");
    }

    @Test
    @DisplayName("handles null rawValue when field is also null")
    void handlesNullRawValueWhenFieldIsAlsoNull() {
      TaskParameter param =
          new TaskParameter("format", null, null, "string", true, false, null, null, Map.of());

      Map<String, Object> dto = processor.toViewerParameterDto(param);

      assertThat(dto)
          .containsEntry("label", "format")
          .containsEntry("value", null)
          .containsEntry("name", "format");
    }

    @Test
    @DisplayName("prefers field over non-null rawValue")
    void prefersFieldOverNonNullRawValue() {
      TaskParameter param =
          new TaskParameter(
              "format", "json", "XML_FORMAT", "string", true, false, null, null, Map.of());

      Map<String, Object> dto = processor.toViewerParameterDto(param);

      assertThat(dto).containsEntry("value", "XML_FORMAT");
    }
  }

  // Helper methods

  private Task createTaskWithParameters(List<Map<String, Object>> parameters) {
    Map<String, Object> properties = new HashMap<>();
    properties.put(DomainConstants.Tasks.PROPERTY_PARAMETERS, parameters);
    return Task.builder().properties(properties).build();
  }

  private TaskParameter createTaskParameter(String name, String rawValue, boolean providedFlag) {
    Map<String, Object> raw = new HashMap<>();
    raw.put("variable", name);
    if (rawValue != null) {
      raw.put("value", rawValue);
    }
    raw.put("provided", providedFlag);

    return new TaskParameter(name, rawValue, null, null, null, providedFlag, null, null, raw);
  }
}
