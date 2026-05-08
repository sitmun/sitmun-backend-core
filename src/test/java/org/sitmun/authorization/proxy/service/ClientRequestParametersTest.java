package org.sitmun.authorization.proxy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.authorization.proxy.exception.BadRequestException;
import org.sitmun.domain.task.parameter.TaskParameter;
import org.sitmun.domain.task.parameter.TaskParameterProcessor;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClientRequestParameters")
class ClientRequestParametersTest {

  @Mock private TaskParameterProcessor taskParameterProcessor;

  @Nested
  @DisplayName("of() factory")
  class OfFactory {

    @Test
    @DisplayName("creates instance from non-null map")
    void createsInstanceFromNonNullMap() {
      Map<String, String> input = Map.of("key1", "value1", "key2", "value2");

      ClientRequestParameters result = ClientRequestParameters.of(input);

      assertThat(result.asMap()).containsExactlyInAnyOrderEntriesOf(input);
    }

    @Test
    @DisplayName("filters out null, empty, and whitespace-only keys")
    void filtersOutNullEmptyAndWhitespaceOnlyKeys() {
      LinkedHashMap<String, String> input = new LinkedHashMap<>();
      input.put("valid", "value1");
      input.put(null, "ignored1");
      input.put("", "ignored2");
      input.put("   ", "ignored3");
      input.put("another", "value2");

      ClientRequestParameters result = ClientRequestParameters.of(input);

      assertThat(result.asMap())
          .containsOnly(Map.entry("valid", "value1"), Map.entry("another", "value2"));
    }

    @Test
    @DisplayName("handles null input")
    void handlesNullInput() {
      ClientRequestParameters result = ClientRequestParameters.of(null);

      assertThat(result.asMap()).isEmpty();
    }

    @Test
    @DisplayName("returns empty for empty map")
    void returnsEmptyForEmptyMap() {
      ClientRequestParameters result = ClientRequestParameters.of(Map.of());

      assertThat(result.asMap()).isEmpty();
    }
  }

  @Nested
  @DisplayName("takePagination()")
  class TakePagination {

    @Test
    @DisplayName("extracts limit and offset case-insensitively")
    void extractsLimitAndOffsetCaseInsensitively() {
      LinkedHashMap<String, String> input = new LinkedHashMap<>();
      input.put("LIMIT", "100");
      input.put("OFFSET", "50");
      input.put("other", "param");

      ClientRequestParameters params = ClientRequestParameters.of(input);
      ClientRequestParameters.PaginationExtractionResult result = params.takePagination();

      assertThat(result.pagination().limit()).isEqualTo("100");
      assertThat(result.pagination().offset()).isEqualTo("50");
      assertThat(result.remainingParameters().asMap()).containsOnly(Map.entry("other", "param"));
    }

    @Test
    @DisplayName("uses last-wins for duplicate pagination keys")
    void usesLastWinsForDuplicatePaginationKeys() {
      LinkedHashMap<String, String> input = new LinkedHashMap<>();
      input.put("limit", "10");
      input.put("LIMIT", "20");
      input.put("LiMiT", "30");

      ClientRequestParameters params = ClientRequestParameters.of(input);
      ClientRequestParameters.PaginationExtractionResult result = params.takePagination();

      // Last one wins (insertion order)
      assertThat(result.pagination().limit()).isEqualTo("30");
      assertThat(result.remainingParameters().asMap()).isEmpty();
    }

    @Test
    @DisplayName("returns nulls when pagination keys absent")
    void returnsNullsWhenPaginationKeysAbsent() {
      Map<String, String> input = Map.of("key1", "value1");

      ClientRequestParameters params = ClientRequestParameters.of(input);
      ClientRequestParameters.PaginationExtractionResult result = params.takePagination();

      assertThat(result.pagination().limit()).isNull();
      assertThat(result.pagination().offset()).isNull();
      assertThat(result.remainingParameters().asMap()).containsExactlyInAnyOrderEntriesOf(input);
    }

    @Test
    @DisplayName("preserves iteration order in remaining parameters")
    void preservesIterationOrderInRemainingParameters() {
      LinkedHashMap<String, String> input = new LinkedHashMap<>();
      input.put("a", "1");
      input.put("limit", "100");
      input.put("b", "2");
      input.put("c", "3");

      ClientRequestParameters params = ClientRequestParameters.of(input);
      ClientRequestParameters.PaginationExtractionResult result = params.takePagination();

      assertThat(result.remainingParameters().asMap().keySet()).containsExactly("a", "b", "c");
    }
  }

  @Nested
  @DisplayName("rejectSystemVariables()")
  class RejectSystemVariables {

    @Test
    @DisplayName("delegates to TaskParameterProcessor")
    void delegatesToTaskParameterProcessor() {
      Map<String, String> input = Map.of("param", "value");
      ClientRequestParameters params = ClientRequestParameters.of(input);

      params.rejectSystemVariables(taskParameterProcessor);

      verify(taskParameterProcessor).rejectClientSystemVariables(input);
    }

    @Test
    @DisplayName("throws BadRequestException when processor detects system variables")
    void throwsBadRequestExceptionWhenProcessorDetectsSystemVariables() {
      Map<String, String> input = Map.of("param", "#{USER_ID}");
      ClientRequestParameters params = ClientRequestParameters.of(input);

      doThrow(new BadRequestException("System variables not allowed"))
          .when(taskParameterProcessor)
          .rejectClientSystemVariables(any());

      assertThatThrownBy(() -> params.rejectSystemVariables(taskParameterProcessor))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("System variables not allowed");
    }
  }

  @Nested
  @DisplayName("filterToAllowed()")
  class FilterToAllowed {

    @Test
    @DisplayName("delegates to TaskParameterProcessor.filterClientParameters")
    void delegatesToTaskParameterProcessorFilterClientParameters() {
      Map<String, String> input = Map.of("allowed", "value1", "notAllowed", "value2");
      ClientRequestParameters params = ClientRequestParameters.of(input);

      List<TaskParameter> taskParams = List.of();
      Map<String, String> filtered = Map.of("allowed", "value1");
      when(taskParameterProcessor.filterClientParameters(taskParams, input)).thenReturn(filtered);

      ClientRequestParameters result = params.filterToAllowed(taskParams, taskParameterProcessor);

      assertThat(result.asMap()).containsExactlyInAnyOrderEntriesOf(filtered);
      verify(taskParameterProcessor).filterClientParameters(taskParams, input);
    }

    @Test
    @DisplayName("returns new instance with filtered parameters")
    void returnsNewInstanceWithFilteredParameters() {
      Map<String, String> input = Map.of("p1", "v1", "p2", "v2", "p3", "v3");
      ClientRequestParameters original = ClientRequestParameters.of(input);

      List<TaskParameter> taskParams = List.of();
      Map<String, String> filtered = Map.of("p1", "v1", "p3", "v3");
      when(taskParameterProcessor.filterClientParameters(any(), any())).thenReturn(filtered);

      ClientRequestParameters result = original.filterToAllowed(taskParams, taskParameterProcessor);

      // Original unchanged
      assertThat(original.asMap()).hasSize(3);
      // Result is filtered
      assertThat(result.asMap()).containsOnly(Map.entry("p1", "v1"), Map.entry("p3", "v3"));
    }
  }

  @Nested
  @DisplayName("asMap()")
  class AsMap {

    @Test
    @DisplayName("returns unmodifiable map")
    void returnsUnmodifiableMap() {
      Map<String, String> input = Map.of("key", "value");
      ClientRequestParameters params = ClientRequestParameters.of(input);

      Map<String, String> result = params.asMap();

      assertThatThrownBy(() -> result.put("new", "value"))
          .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("returns map with same content")
    void returnsMapWithSameContent() {
      Map<String, String> input = Map.of("k1", "v1", "k2", "v2");
      ClientRequestParameters params = ClientRequestParameters.of(input);

      assertThat(params.asMap()).containsExactlyInAnyOrderEntriesOf(input);
    }
  }
}
