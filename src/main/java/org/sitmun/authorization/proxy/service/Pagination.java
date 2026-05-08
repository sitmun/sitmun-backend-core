package org.sitmun.authorization.proxy.service;

import org.jspecify.annotations.Nullable;

/**
 * Pagination values extracted from client parameters.
 *
 * <p>Represents {@code limit} and {@code offset} values (case-insensitive keys SQL_LIMIT,
 * SQL_OFFSET) parsed from proxy request parameters. Used by {@link QueryPaginationDecorator} to
 * inject pagination into SQL queries.
 *
 * @param limit maximum number of results (e.g., "100")
 * @param offset starting row index (e.g., "0")
 */
public record Pagination(@Nullable String limit, @Nullable String offset) {}
