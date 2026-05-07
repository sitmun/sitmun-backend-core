package org.sitmun.domain.task.parameter;

import java.util.Collections;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Immutable view of a task parameter extracted from task properties. Provides a unified model for
 * parameter parsing, classification, and effective-value computation.
 *
 * <p>The {@code name} field is required and resolved from {@code variable > name > label} with
 * fallback for backward compatibility.
 *
 * <p>The {@code raw} field preserves the original parameter map for rare cases where unmapped keys
 * are needed by specific TaskMapper implementations.
 */
public record TaskParameter(
    String name,
    @Nullable String rawValue,
    @Nullable String field,
    @Nullable String type,
    @Nullable Boolean required,
    boolean providedFlag,
    @Nullable String label,
    @Nullable String description,
    Map<String, Object> raw) {

  /**
   * Constructs a TaskParameter with an unmodifiable copy of the raw map.
   *
   * @throws IllegalArgumentException if name is null or blank
   */
  public TaskParameter {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("TaskParameter name cannot be null or blank");
    }
    raw = raw != null ? Collections.unmodifiableMap(raw) : Collections.emptyMap();
  }
}
