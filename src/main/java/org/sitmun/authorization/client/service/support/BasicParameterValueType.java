package org.sitmun.authorization.client.service.support;

import static org.sitmun.domain.DomainConstants.Tasks.*;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

/**
 * Basic task parameter value types supported by SITMUN.
 *
 * <p>Maps type strings from {@link org.sitmun.domain.DomainConstants.Tasks} to an enum for
 * type-safe parameter value conversion and validation.
 */
@Getter
public enum BasicParameterValueType {
  /** String type - any value, null treated as empty string. */
  STRING(TYPE_STRING),

  /** Number type - must parse as {@code Double}. */
  NUMBER(TYPE_NUMBER),

  /** Boolean type - parsed via {@code Boolean.parseBoolean}. */
  BOOLEAN(TYPE_BOOLEAN),

  /** Array type - must parse as JSON array. */
  ARRAY(TYPE_ARRAY),

  /** Object type - must parse as JSON object. */
  OBJECT(TYPE_OBJECT),

  /** Null type - value must be null. */
  NULL(TYPE_NULL);

  private final String typeString;

  BasicParameterValueType(String typeString) {
    this.typeString = typeString;
  }

  /**
   * Converts a type string to its enum constant.
   *
   * @param typeString type string from task properties
   * @return enum constant, or null if not recognized
   */
  @Nullable
  public static BasicParameterValueType from(@Nullable String typeString) {
    if (typeString == null) {
      return null;
    }
    for (BasicParameterValueType type : values()) {
      if (type.typeString.equals(typeString)) {
        return type;
      }
    }
    return null;
  }
}
