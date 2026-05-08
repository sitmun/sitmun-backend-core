package org.sitmun.authorization.client.dto.profile;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.jspecify.annotations.Nullable;

/**
 * Parameter shape with optional default value for cartography query/edition tasks and cartography
 * slots.
 *
 * <p>Wire format: {@code {type, value?, required}}. {@code value} is omitted when null
 * ({@code @JsonInclude(NON_NULL)}). Must NOT emit {@code label} or {@code name} keys.
 *
 * <p>JSON key order pinned from observed HashMap output captured in Phase 1 snapshots.
 *
 * <p>Reused for cartography proxy slots ({@code service}, {@code layers}, {@code typename}) with
 * {@code required = true} hardcoded.
 *
 * @param type parameter storage type (e.g., "query", "string")
 * @param required whether parameter is mandatory
 * @param value default value; omitted from JSON when null
 */
@JsonPropertyOrder({"type", "value", "required"})
public record ServiceParameter(
    String type,
    boolean required,
    @JsonInclude(JsonInclude.Include.NON_NULL) @Nullable String value)
    implements ProfileParameter {}
