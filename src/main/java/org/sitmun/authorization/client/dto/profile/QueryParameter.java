package org.sitmun.authorization.client.dto.profile;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Query parameter configuration for SQL and proxied web-api query tasks.
 *
 * <p>Wire format: {@code {type, required}} only. Must NOT emit {@code label}, {@code value}, or
 * {@code name} keys.
 *
 * <p>JSON key order pinned from observed HashMap output captured in Phase 1 snapshots.
 *
 * @param type parameter storage type (e.g., "query", "template", "string")
 * @param required whether parameter is mandatory
 */
@JsonPropertyOrder({"type", "required"})
public record QueryParameter(String type, boolean required) implements ProfileParameter {}
