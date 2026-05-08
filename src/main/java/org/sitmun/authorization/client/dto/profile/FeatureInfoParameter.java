package org.sitmun.authorization.client.dto.profile;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.jspecify.annotations.Nullable;

/**
 * Feature-field forwarding parameter shape for more-info and external-link (URL) query tasks.
 *
 * <p>Wire format: {@code {name, label, type?, value, required?}}. {@code label}, {@code value}, and
 * {@code name} are always emitted (even when null) for viewer compatibility. {@code type} and
 * {@code required} are omitted when null.
 *
 * <p>JSON key order pinned from observed HashMap output captured in Phase 1 snapshots.
 *
 * @param name parameter name (always emitted, even when null)
 * @param label display label (always emitted, even when null)
 * @param type parameter storage type; omitted from JSON when null
 * @param value feature data field name or literal value (always emitted, even when null)
 * @param required whether parameter is mandatory; omitted from JSON when null
 */
@JsonPropertyOrder({"name", "label", "type", "value", "required"})
public record FeatureInfoParameter(
    String name,
    @Nullable String label,
    @JsonInclude(JsonInclude.Include.NON_NULL) @Nullable String type,
    @Nullable String value,
    @JsonInclude(JsonInclude.Include.NON_NULL) @Nullable Boolean required)
    implements ProfileParameter {}
