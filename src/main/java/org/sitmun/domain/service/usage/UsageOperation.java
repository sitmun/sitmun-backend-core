package org.sitmun.domain.service.usage;

import java.util.Set;

public final class UsageOperation {

  public static final String GET_MAP = "GetMap";
  public static final String GET_TILE = "GetTile";
  public static final String GET_FEATURE_INFO = "GetFeatureInfo";
  public static final String GET_CAPABILITIES = "GetCapabilities";
  public static final String OTHER = "Other";
  public static final String VIEWER_CONFIG = "ViewerConfig";

  /** STM_SERVICE_USAGE.SUS_OPERATION is VARCHAR(32). */
  private static final int STORED_LENGTH = 32;

  private static final Set<String> KNOWN =
      Set.of(GET_MAP, GET_TILE, GET_FEATURE_INFO, GET_CAPABILITIES, OTHER, VIEWER_CONFIG);

  private UsageOperation() {}

  public static String canonical(String operation) {
    if (operation == null || operation.isBlank()) {
      return OTHER;
    }
    String trimmed = operation.trim();
    for (String known : KNOWN) {
      if (known.equalsIgnoreCase(trimmed)) {
        return known;
      }
    }
    if (trimmed.length() > STORED_LENGTH) {
      return trimmed.substring(0, STORED_LENGTH);
    }
    return trimmed;
  }

  public static boolean countsAsRequest(String operation) {
    return !VIEWER_CONFIG.equals(operation);
  }
}
