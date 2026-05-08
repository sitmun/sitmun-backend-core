package org.sitmun.authorization.proxy.service;

import static org.sitmun.authorization.proxy.decorators.QueryPaginationDecorator.SQL_LIMIT;
import static org.sitmun.authorization.proxy.decorators.QueryPaginationDecorator.SQL_OFFSET;

import java.util.*;
import org.jspecify.annotations.Nullable;
import org.sitmun.authorization.proxy.exception.BadRequestException;
import org.sitmun.domain.task.parameter.TaskParameter;
import org.sitmun.domain.task.parameter.TaskParameterProcessor;
import org.springframework.util.StringUtils;

/**
 * Immutable wrapper for client-supplied proxy request parameters.
 *
 * <p>Encapsulates common parameter pipeline operations: pagination extraction, system-variable
 * rejection, and allowed-name filtering. Backed by a {@link LinkedHashMap} for stable iteration
 * order.
 *
 * <p>Constructed via {@link #of(Map)}; most operations return a new instance preserving
 * immutability.
 */
public final class ClientRequestParameters {

  private final LinkedHashMap<String, String> parameters;

  private ClientRequestParameters(LinkedHashMap<String, String> parameters) {
    this.parameters = parameters;
  }

  /**
   * Creates a new instance from raw client parameters.
   *
   * <p>Copies and filters the input map, skipping null or empty keys.
   *
   * @param raw client parameters (may be null)
   * @return new ClientRequestParameters instance
   */
  public static ClientRequestParameters of(@Nullable Map<String, String> raw) {
    LinkedHashMap<String, String> filtered = new LinkedHashMap<>();
    if (raw != null) {
      raw.forEach(
          (k, v) -> {
            if (StringUtils.hasText(k)) {
              filtered.put(k, v);
            }
          });
    }
    return new ClientRequestParameters(filtered);
  }

  /**
   * Extracts pagination values and returns a new instance without those keys.
   *
   * <p>Matches {@code SQL_LIMIT} and {@code SQL_OFFSET} case-insensitively. Last-wins semantics: if
   * multiple keys match (e.g., "limit" and "LIMIT"), the last one in iteration order is used.
   *
   * <p>Returns a {@link PaginationExtractionResult} containing the extracted {@link Pagination} and
   * the updated {@link ClientRequestParameters} without pagination keys.
   *
   * @return extraction result with pagination and remaining parameters
   */
  public PaginationExtractionResult takePagination() {
    String limit = null;
    String offset = null;
    List<String> keysToRemove = new ArrayList<>();

    for (Map.Entry<String, String> e : parameters.entrySet()) {
      String key = e.getKey();
      if (SQL_LIMIT.equalsIgnoreCase(key)) {
        limit = e.getValue();
        keysToRemove.add(key);
      } else if (SQL_OFFSET.equalsIgnoreCase(key)) {
        offset = e.getValue();
        keysToRemove.add(key);
      }
    }

    LinkedHashMap<String, String> remaining = new LinkedHashMap<>(parameters);
    keysToRemove.forEach(remaining::remove);

    return new PaginationExtractionResult(
        new Pagination(limit, offset), new ClientRequestParameters(remaining));
  }

  /**
   * Validates that no client parameter contains system-variable expressions.
   *
   * <p>Delegates to {@link TaskParameterProcessor#rejectClientSystemVariables(Map)} to detect any
   * {@code #{...}} patterns in client values.
   *
   * @param taskParameterProcessor processor with system-variable rejection logic
   * @throws BadRequestException if any parameter value contains {@code #{...}}
   */
  public void rejectSystemVariables(TaskParameterProcessor taskParameterProcessor) {
    taskParameterProcessor.rejectClientSystemVariables(parameters);
  }

  /**
   * Filters parameters to only those allowed by task parameter declarations.
   *
   * <p>Delegates to {@link TaskParameterProcessor#filterClientParameters(List, Map)} to enforce
   * client-allowed parameter names.
   *
   * @param taskParameters declared task parameters
   * @param taskParameterProcessor processor with filtering logic
   * @return new ClientRequestParameters containing only allowed parameters
   */
  public ClientRequestParameters filterToAllowed(
      List<TaskParameter> taskParameters, TaskParameterProcessor taskParameterProcessor) {
    Map<String, String> filtered =
        taskParameterProcessor.filterClientParameters(taskParameters, parameters);
    return new ClientRequestParameters(new LinkedHashMap<>(filtered));
  }

  /**
   * Returns an unmodifiable view of the underlying parameter map.
   *
   * <p>Used as escape hatch for current decorator signatures that expect {@code Map<String,
   * String>}.
   *
   * @return unmodifiable map view
   */
  public Map<String, String> asMap() {
    return Collections.unmodifiableMap(parameters);
  }

  /**
   * Result of pagination extraction containing the extracted values and remaining parameters.
   *
   * @param pagination extracted limit and offset values
   * @param remainingParameters parameters without pagination keys
   */
  public record PaginationExtractionResult(
      Pagination pagination, ClientRequestParameters remainingParameters) {}
}
