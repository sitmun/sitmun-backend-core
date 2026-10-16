package org.sitmun.authorization.proxy.service;

import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.sitmun.authorization.proxy.exception.BadRequestException;

/**
 * Non-negative row count extracted from client {@code LIMIT} and {@code OFFSET} parameters.
 *
 * @param limit maximum number of rows, or null when the client omitted it
 * @param offset number of rows to skip, or null when the client omitted it
 */
public record Pagination(@Nullable Integer limit, @Nullable Integer offset) {

  private static final Pattern DIGITS = Pattern.compile("[0-9]+");

  /**
   * Parses a client row count. Blank means the client omitted the value. Any other non-integer is
   * rejected.
   */
  public static Integer parseCount(@Nullable String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    if (!DIGITS.matcher(raw).matches()) {
      throw new BadRequestException("LIMIT and OFFSET must be non-negative integers");
    }
    try {
      return Integer.valueOf(raw);
    } catch (NumberFormatException ex) {
      throw new BadRequestException("LIMIT and OFFSET must be non-negative integers");
    }
  }
}
